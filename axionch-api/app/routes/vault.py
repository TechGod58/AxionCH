from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.auth import AuthIdentity, enforce_user_scope, get_auth_identity
from app.db.models import User, VaultEntry
from app.db.session import get_db
from app.schemas.vault import (
    VaultCryptoStatusResponse,
    VaultDeleteResponse,
    VaultEntryCreateRequest,
    VaultEntryResponse,
    VaultEntrySummary,
    VaultEntryUpdateRequest,
    VaultListResponse,
)
from app.services.vault.crypto import decrypt_secret, encrypt_secret, vault_crypto_status

router = APIRouter()


def _mask_password(password: str) -> str:
    if len(password) <= 4:
        return "*" * len(password)
    return f"{password[:2]}{'*' * max(2, len(password) - 4)}{password[-2:]}"


def _resolve_user_or_404(db: Session, email: str) -> User:
    user = db.execute(select(User).where(User.email == email)).scalar_one_or_none()
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return user


def _ensure_vault_ready() -> None:
    ready, message = vault_crypto_status()
    if not ready:
        raise HTTPException(status_code=503, detail=message)


def _serialize_summary(entry: VaultEntry, user_email: str) -> VaultEntrySummary:
    password = decrypt_secret(entry.password_encrypted)
    username = decrypt_secret(entry.username_encrypted)
    return VaultEntrySummary(
        id=entry.id,
        user_email=user_email,
        service_name=entry.service_name,
        username=username,
        password_mask=_mask_password(password),
        created_at=entry.created_at.isoformat(),
        updated_at=entry.updated_at.isoformat(),
    )


def _serialize_detail(entry: VaultEntry, user_email: str) -> VaultEntryResponse:
    return VaultEntryResponse(
        id=entry.id,
        user_email=user_email,
        service_name=entry.service_name,
        username=decrypt_secret(entry.username_encrypted),
        password=decrypt_secret(entry.password_encrypted),
        notes=decrypt_secret(entry.notes_encrypted) if entry.notes_encrypted else None,
        created_at=entry.created_at.isoformat(),
        updated_at=entry.updated_at.isoformat(),
    )


@router.post("", response_model=VaultEntryResponse)
def create_vault_entry(
    payload: VaultEntryCreateRequest,
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> VaultEntryResponse:
    _ensure_vault_ready()
    effective_email = enforce_user_scope(auth, payload.user_email) or payload.user_email

    user = db.execute(select(User).where(User.email == effective_email)).scalar_one_or_none()
    if user is None:
        user = User(email=effective_email)
        db.add(user)
        db.flush()

    entry = VaultEntry(
        user_id=user.id,
        service_name=payload.service_name.strip(),
        username_encrypted=encrypt_secret(payload.username),
        password_encrypted=encrypt_secret(payload.password),
        notes_encrypted=encrypt_secret(payload.notes) if payload.notes else None,
    )
    db.add(entry)
    db.commit()
    db.refresh(entry)

    return _serialize_detail(entry, effective_email)


@router.get("/status", response_model=VaultCryptoStatusResponse)
def vault_status() -> VaultCryptoStatusResponse:
    ready, message = vault_crypto_status()
    return VaultCryptoStatusResponse(ready=ready, message=message)


@router.get("", response_model=VaultListResponse)
def list_vault_entries(
    user_email: str = Query(min_length=3),
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> VaultListResponse:
    _ensure_vault_ready()
    effective_email = enforce_user_scope(auth, user_email) or user_email
    user = _resolve_user_or_404(db, effective_email)

    entries = db.execute(
        select(VaultEntry)
        .where(VaultEntry.user_id == user.id)
        .order_by(VaultEntry.updated_at.desc(), VaultEntry.id.desc())
    ).scalars().all()

    return VaultListResponse(
        entries=[_serialize_summary(item, effective_email) for item in entries],
        total_returned=len(entries),
    )


@router.get("/{entry_id}", response_model=VaultEntryResponse)
def get_vault_entry(
    entry_id: int,
    user_email: str = Query(min_length=3),
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> VaultEntryResponse:
    _ensure_vault_ready()
    effective_email = enforce_user_scope(auth, user_email) or user_email
    user = _resolve_user_or_404(db, effective_email)

    entry = db.execute(
        select(VaultEntry).where(VaultEntry.id == entry_id, VaultEntry.user_id == user.id)
    ).scalar_one_or_none()
    if entry is None:
        raise HTTPException(status_code=404, detail="Vault entry not found")

    return _serialize_detail(entry, effective_email)


@router.patch("/{entry_id}", response_model=VaultEntryResponse)
def update_vault_entry(
    entry_id: int,
    payload: VaultEntryUpdateRequest,
    user_email: str = Query(min_length=3),
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> VaultEntryResponse:
    _ensure_vault_ready()
    effective_email = enforce_user_scope(auth, user_email) or user_email
    user = _resolve_user_or_404(db, effective_email)

    entry = db.execute(
        select(VaultEntry).where(VaultEntry.id == entry_id, VaultEntry.user_id == user.id)
    ).scalar_one_or_none()
    if entry is None:
        raise HTTPException(status_code=404, detail="Vault entry not found")

    if payload.service_name is not None:
        entry.service_name = payload.service_name.strip()
    if payload.username is not None:
        entry.username_encrypted = encrypt_secret(payload.username)
    if payload.password is not None:
        entry.password_encrypted = encrypt_secret(payload.password)
    if payload.notes is not None:
        entry.notes_encrypted = encrypt_secret(payload.notes) if payload.notes else None

    db.commit()
    db.refresh(entry)
    return _serialize_detail(entry, effective_email)


@router.delete("/{entry_id}", response_model=VaultDeleteResponse)
def delete_vault_entry(
    entry_id: int,
    user_email: str = Query(min_length=3),
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> VaultDeleteResponse:
    _ensure_vault_ready()
    effective_email = enforce_user_scope(auth, user_email) or user_email
    user = _resolve_user_or_404(db, effective_email)

    entry = db.execute(
        select(VaultEntry).where(VaultEntry.id == entry_id, VaultEntry.user_id == user.id)
    ).scalar_one_or_none()
    if entry is None:
        raise HTTPException(status_code=404, detail="Vault entry not found")

    db.delete(entry)
    db.commit()
    return VaultDeleteResponse(deleted=True, entry_id=entry_id)
