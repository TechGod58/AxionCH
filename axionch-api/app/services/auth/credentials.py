from datetime import datetime
from secrets import token_hex, token_urlsafe

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.db.models import ApiCredential, User
from app.services.auth.key_hashing import hash_api_key, is_argon2_hash, verify_api_key_hash


def create_user_key(db: Session, user_email: str, label: str) -> tuple[ApiCredential, str]:
    user = db.execute(select(User).where(User.email == user_email)).scalar_one_or_none()
    if user is None:
        user = User(email=user_email)
        db.add(user)
        db.flush()

    raw_key = token_urlsafe(32)
    key_id = f"ak_{token_hex(8)}"
    credential = ApiCredential(
        user_id=user.id,
        key_id=key_id,
        key_hash=hash_api_key(raw_key),
        label=label,
        is_active=True,
    )
    db.add(credential)
    db.commit()
    db.refresh(credential)
    return credential, raw_key


def list_user_keys(db: Session, user_email: str) -> list[ApiCredential]:
    user = db.execute(select(User).where(User.email == user_email)).scalar_one_or_none()
    if user is None:
        return []
    return db.execute(
        select(ApiCredential)
        .where(ApiCredential.user_id == user.id)
        .order_by(ApiCredential.created_at.desc())
    ).scalars().all()


def revoke_user_key(db: Session, user_email: str, key_id: str) -> bool:
    user = db.execute(select(User).where(User.email == user_email)).scalar_one_or_none()
    if user is None:
        return False
    credential = db.execute(
        select(ApiCredential).where(ApiCredential.user_id == user.id, ApiCredential.key_id == key_id)
    ).scalar_one_or_none()
    if credential is None:
        return False
    credential.is_active = False
    credential.revoked_at = datetime.utcnow()
    db.commit()
    return True


def any_active_user_keys(db: Session) -> bool:
    count = int(
        db.execute(
            select(func.count(ApiCredential.id)).where(ApiCredential.is_active.is_(True))
        ).scalar_one()
        or 0
    )
    return count > 0


def verify_user_key(db: Session, user_email: str, raw_key: str) -> bool:
    if not user_email or not raw_key:
        return False
    user = db.execute(select(User).where(User.email == user_email)).scalar_one_or_none()
    if user is None:
        return False

    active_credentials = db.execute(
        select(ApiCredential).where(
            ApiCredential.user_id == user.id,
            ApiCredential.is_active.is_(True),
        )
    ).scalars().all()

    for credential in active_credentials:
        if not verify_api_key_hash(credential.key_hash, raw_key):
            continue
        if not is_argon2_hash(credential.key_hash):
            credential.key_hash = hash_api_key(raw_key)
        credential.last_used_at = datetime.utcnow()
        db.commit()
        return True

    return False
