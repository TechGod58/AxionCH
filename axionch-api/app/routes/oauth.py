from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.auth import AuthIdentity, enforce_user_scope, get_auth_identity
from app.db.models import ConnectionStatus, Platform, SocialAccount, User
from app.db.session import get_db
from app.schemas.oauth import OAuthCallbackResponse, OAuthStartResponse
from app.services.oauth.exchange import OAuthExchangeError, exchange_oauth_code
from app.services.oauth.scaffold import build_authorize_url, oauth_state_store
from app.services.oauth.token_crypto import store_account_tokens

router = APIRouter()


@router.get("/{platform}/start", response_model=OAuthStartResponse)
def oauth_start(
    platform: Platform,
    user_email: str = Query(min_length=3),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> OAuthStartResponse:
    effective_email = enforce_user_scope(auth, user_email) or user_email
    state = oauth_state_store.issue(platform=platform, user_email=effective_email)
    try:
        auth_url, scopes, redirect_uri = build_authorize_url(platform=platform, state=state)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    return OAuthStartResponse(
        platform=platform,
        auth_url=auth_url,
        state=state,
        redirect_uri=redirect_uri,
        scopes=scopes,
    )


@router.get("/{platform}/callback", response_model=OAuthCallbackResponse)
def oauth_callback(
    platform: Platform,
    code: str = Query(min_length=3),
    state: str = Query(min_length=10),
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> OAuthCallbackResponse:
    payload = oauth_state_store.consume(state=state, platform=platform)
    if payload is None:
        raise HTTPException(status_code=400, detail="Invalid or expired OAuth state")
    enforce_user_scope(auth, payload.user_email)

    user = db.execute(select(User).where(User.email == payload.user_email)).scalar_one_or_none()
    if user is None:
        user = User(email=payload.user_email)
        db.add(user)
        db.flush()

    try:
        token_bundle = exchange_oauth_code(platform=platform, code=code)
    except OAuthExchangeError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

    account = db.execute(
        select(SocialAccount).where(
            SocialAccount.user_id == user.id,
            SocialAccount.platform == platform,
        )
    ).scalar_one_or_none()
    if account is None:
        account = SocialAccount(
            user_id=user.id,
            platform=platform,
            handle=f"oauth_{platform.value}_{user.id}",
            status=ConnectionStatus.CONNECTED,
        )
        store_account_tokens(
            account=account,
            access_token=token_bundle.access_token,
            refresh_token=token_bundle.refresh_token,
            token_type=token_bundle.token_type,
            token_scope=token_bundle.scope,
            token_expires_at=token_bundle.expires_at,
        )
        db.add(account)
    else:
        store_account_tokens(
            account=account,
            access_token=token_bundle.access_token,
            refresh_token=token_bundle.refresh_token,
            token_type=token_bundle.token_type,
            token_scope=token_bundle.scope,
            token_expires_at=token_bundle.expires_at,
        )
        account.status = ConnectionStatus.CONNECTED

    db.commit()
    db.refresh(account)

    return OAuthCallbackResponse(
        platform=platform,
        state_valid=True,
        account_id=account.id,
        message="OAuth callback processed with token exchange and stored on social account.",
    )
