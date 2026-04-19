from sqlalchemy import select
from sqlalchemy.orm import Session
from fastapi import APIRouter, Depends, HTTPException

from app.core.auth import AuthIdentity, enforce_user_scope, get_auth_identity
from app.db.models import SocialAccount, User
from app.db.session import get_db
from app.schemas.account import AccountResponse, CreateAccountRequest
from app.services.oauth.token_crypto import encrypt_token_value

router = APIRouter()


@router.get("", response_model=list[AccountResponse])
def list_accounts(
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
):
    scoped_email = enforce_user_scope(auth)

    query = select(SocialAccount).order_by(SocialAccount.id.desc())
    if scoped_email is not None:
        user = db.execute(select(User).where(User.email == scoped_email)).scalar_one_or_none()
        if user is None:
            return []
        query = query.where(SocialAccount.user_id == user.id)

    accounts = db.execute(query).scalars().all()
    return accounts


@router.post("", response_model=AccountResponse)
def create_account(
    payload: CreateAccountRequest,
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
):
    effective_email = enforce_user_scope(auth, payload.user_email) or payload.user_email

    user = db.execute(select(User).where(User.email == effective_email)).scalar_one_or_none()
    if user is None:
        user = User(email=effective_email)
        db.add(user)
        db.flush()

    account = SocialAccount(
        user_id=user.id,
        platform=payload.platform,
        handle=payload.handle,
        access_token=encrypt_token_value(payload.access_token),
    )
    db.add(account)
    db.commit()
    db.refresh(account)
    return account


@router.delete("/{account_id}")
def delete_account(
    account_id: int,
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
):
    account = db.get(SocialAccount, account_id)
    if account is None:
        raise HTTPException(status_code=404, detail="Account not found")

    scoped_email = enforce_user_scope(auth)
    if scoped_email is not None:
        owner = db.execute(select(User).where(User.id == account.user_id)).scalar_one_or_none()
        if owner is None or owner.email != scoped_email:
            raise HTTPException(status_code=403, detail="Account does not belong to authenticated user scope")

    db.delete(account)
    db.commit()
    return {"deleted": True, "account_id": account_id}
