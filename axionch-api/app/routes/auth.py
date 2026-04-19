from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.auth import AuthIdentity, enforce_user_scope, get_auth_identity
from app.db.models import User, VaultEntry
from app.db.session import get_db
from app.schemas.auth import (
    ApiKeyListItem,
    ApiKeyListResponse,
    ApiKeyResponse,
    ApiKeyRevokeResponse,
    CreateApiKeyRequest,
)
from app.services.auth.credentials import create_user_key, list_user_keys, revoke_user_key
from app.services.vault.crypto import encrypt_secret, vault_crypto_status

router = APIRouter()


def _persist_api_key_to_vault(
    db: Session,
    *,
    user_email: str,
    key_id: str,
    label: str,
    raw_key: str,
    created_at_iso: str,
) -> None:
    ready, _ = vault_crypto_status()
    if not ready:
        return

    user = db.execute(select(User).where(User.email == user_email)).scalar_one_or_none()
    if user is None:
        return

    notes = f"AxionCH API key label={label}; key_id={key_id}; created_at={created_at_iso}"
    entry = VaultEntry(
        user_id=user.id,
        service_name="axionch-api-keys",
        username_encrypted=encrypt_secret(key_id),
        password_encrypted=encrypt_secret(raw_key),
        notes_encrypted=encrypt_secret(notes),
    )
    db.add(entry)
    db.commit()


@router.post("/keys", response_model=ApiKeyResponse)
def create_api_key(
    payload: CreateApiKeyRequest,
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> ApiKeyResponse:
    effective_email = enforce_user_scope(auth, payload.user_email) or payload.user_email

    credential, raw_key = create_user_key(db, user_email=effective_email, label=payload.label)
    try:
        _persist_api_key_to_vault(
            db,
            user_email=effective_email,
            key_id=credential.key_id,
            label=credential.label,
            raw_key=raw_key,
            created_at_iso=credential.created_at.isoformat(),
        )
    except Exception as exc:
        # Preserve existing API-key creation behavior even if vault write fails.
        print(f"[AUTH WARNING] Failed to mirror raw API key into vault: {exc}")

    return ApiKeyResponse(
        key_id=credential.key_id,
        user_email=effective_email,
        label=credential.label,
        raw_api_key=raw_key,
        created_at=credential.created_at.isoformat(),
    )


@router.get("/keys", response_model=ApiKeyListResponse)
def get_api_keys(
    user_email: str = Query(min_length=3),
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> ApiKeyListResponse:
    effective_email = enforce_user_scope(auth, user_email) or user_email

    keys = list_user_keys(db, user_email=effective_email)
    return ApiKeyListResponse(
        user_email=effective_email,
        keys=[
            ApiKeyListItem(
                key_id=item.key_id,
                label=item.label,
                is_active=item.is_active,
                created_at=item.created_at.isoformat(),
                last_used_at=item.last_used_at.isoformat() if item.last_used_at else None,
                revoked_at=item.revoked_at.isoformat() if item.revoked_at else None,
            )
            for item in keys
        ],
    )


@router.delete("/keys/{key_id}", response_model=ApiKeyRevokeResponse)
def delete_api_key(
    key_id: str,
    user_email: str = Query(min_length=3),
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> ApiKeyRevokeResponse:
    effective_email = enforce_user_scope(auth, user_email) or user_email

    revoked = revoke_user_key(db, user_email=effective_email, key_id=key_id)
    if not revoked:
        raise HTTPException(status_code=404, detail="API key not found")
    return ApiKeyRevokeResponse(user_email=effective_email, key_id=key_id, revoked=True)
