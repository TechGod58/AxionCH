from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.db.models import Platform, User, VaultEntry
from app.services.vault.crypto import decrypt_secret, encrypt_secret, vault_crypto_status

PLATFORM_REQUIRED_FIELDS: dict[Platform, list[str]] = {
    Platform.X: [
        "X_API_KEY",
        "X_API_SECRET",
        "X_ACCESS_TOKEN",
        "X_ACCESS_TOKEN_SECRET",
    ],
    Platform.LINKEDIN: [
        "LINKEDIN_CLIENT_ID",
        "LINKEDIN_CLIENT_SECRET",
        "LINKEDIN_ACCESS_TOKEN",
        "LINKEDIN_AUTHOR_URN",
    ],
    Platform.INSTAGRAM: [
        "INSTAGRAM_APP_ID",
        "INSTAGRAM_APP_SECRET",
        "INSTAGRAM_ACCESS_TOKEN",
        "INSTAGRAM_BUSINESS_ACCOUNT_ID",
    ],
}

LEGACY_SERVICE_NAMES: dict[Platform, list[str]] = {
    Platform.X: ["platform-x", "axionch-platform-x"],
    Platform.LINKEDIN: ["platform-linkedin", "axionch-platform-linkedin"],
    Platform.INSTAGRAM: ["platform-instagram", "axionch-platform-instagram"],
}


@dataclass
class PlatformSecretResolution:
    platform: Platform
    values: dict[str, str | None]
    required_fields: list[str]
    configured_fields: list[str]
    source_by_field: dict[str, str]

    @property
    def configured(self) -> bool:
        return len(self.configured_fields) == len(self.required_fields)

    @property
    def mode(self) -> str:
        return "real" if self.configured else "mock"


def platform_secret_service_name(platform: Platform, field_name: str) -> str:
    normalized = field_name.strip().upper()
    return f"platform-secret:{platform.value}:{normalized}"


def platform_required_fields(platform: Platform) -> list[str]:
    return list(PLATFORM_REQUIRED_FIELDS[platform])


def _env_values(platform: Platform) -> dict[str, str | None]:
    if platform == Platform.X:
        return {
            "X_API_KEY": settings.x_api_key,
            "X_API_SECRET": settings.x_api_secret,
            "X_ACCESS_TOKEN": settings.x_access_token,
            "X_ACCESS_TOKEN_SECRET": settings.x_access_token_secret,
        }
    if platform == Platform.LINKEDIN:
        return {
            "LINKEDIN_CLIENT_ID": settings.linkedin_client_id,
            "LINKEDIN_CLIENT_SECRET": settings.linkedin_client_secret,
            "LINKEDIN_ACCESS_TOKEN": settings.linkedin_access_token,
            "LINKEDIN_AUTHOR_URN": settings.linkedin_author_urn,
        }
    return {
        "INSTAGRAM_APP_ID": settings.instagram_app_id,
        "INSTAGRAM_APP_SECRET": settings.instagram_app_secret,
        "INSTAGRAM_ACCESS_TOKEN": settings.instagram_access_token,
        "INSTAGRAM_BUSINESS_ACCOUNT_ID": settings.instagram_business_account_id,
    }


def _resolve_user(db: Session, user_email: str | None) -> User | None:
    if not user_email:
        return None
    return db.execute(select(User).where(User.email == user_email)).scalar_one_or_none()


def resolve_platform_secrets(
    db: Session,
    *,
    platform: Platform,
    user_email: str | None = None,
    include_env_fallback: bool | None = None,
) -> PlatformSecretResolution:
    required_fields = platform_required_fields(platform)
    include_env = settings.platform_secrets_env_fallback if include_env_fallback is None else include_env_fallback

    env_values = _env_values(platform)
    values: dict[str, str | None] = {key: None for key in required_fields}
    source_by_field: dict[str, str] = {key: "missing" for key in required_fields}
    if include_env:
        for field in required_fields:
            current = env_values.get(field)
            values[field] = current
            source_by_field[field] = "env" if current else "missing"

    ready, _ = vault_crypto_status()
    if not ready:
        configured_fields = [field for field in required_fields if values.get(field)]
        return PlatformSecretResolution(
            platform=platform,
            values=values,
            required_fields=required_fields,
            configured_fields=configured_fields,
            source_by_field=source_by_field,
        )

    user = _resolve_user(db, user_email)
    if user is None:
        configured_fields = [field for field in required_fields if values.get(field)]
        return PlatformSecretResolution(
            platform=platform,
            values=values,
            required_fields=required_fields,
            configured_fields=configured_fields,
            source_by_field=source_by_field,
        )

    deterministic_service_names = [platform_secret_service_name(platform, field) for field in required_fields]
    allowed_service_names = deterministic_service_names + LEGACY_SERVICE_NAMES[platform]
    rows = db.execute(
        select(VaultEntry)
        .where(
            VaultEntry.user_id == user.id,
            VaultEntry.service_name.in_(allowed_service_names),
        )
        .order_by(VaultEntry.updated_at.desc(), VaultEntry.id.desc())
    ).scalars().all()

    seen_fields: set[str] = set()
    deterministic_prefix = f"platform-secret:{platform.value}:"
    for row in rows:
        field_name: str | None = None

        if row.service_name.startswith(deterministic_prefix):
            field_name = row.service_name[len(deterministic_prefix):].strip().upper()
        elif row.service_name in LEGACY_SERVICE_NAMES[platform]:
            # Legacy format used username as the env-field name.
            try:
                field_name = decrypt_secret(row.username_encrypted).strip().upper()
            except Exception:
                field_name = None

        if not field_name or field_name not in required_fields or field_name in seen_fields:
            continue

        try:
            decrypted = decrypt_secret(row.password_encrypted)
        except Exception:
            continue

        values[field_name] = decrypted
        source_by_field[field_name] = "vault"
        seen_fields.add(field_name)

    configured_fields = [field for field in required_fields if values.get(field)]
    return PlatformSecretResolution(
        platform=platform,
        values=values,
        required_fields=required_fields,
        configured_fields=configured_fields,
        source_by_field=source_by_field,
    )


def upsert_platform_secrets(
    db: Session,
    *,
    user_email: str,
    platform: Platform,
    secrets: dict[str, str],
    overwrite_existing: bool = True,
    note_prefix: str = "Platform secret",
) -> tuple[list[str], list[str]]:
    ready, message = vault_crypto_status()
    if not ready:
        raise ValueError(message)

    user = _resolve_user(db, user_email)
    if user is None:
        user = User(email=user_email)
        db.add(user)
        db.flush()

    required_fields = platform_required_fields(platform)
    normalized_input = {
        key.strip().upper(): value.strip()
        for key, value in secrets.items()
        if key and value and value.strip()
    }

    updated_fields: list[str] = []
    skipped_fields: list[str] = []

    for field_name in required_fields:
        value = normalized_input.get(field_name)
        if not value:
            continue

        service_name = platform_secret_service_name(platform, field_name)
        existing = db.execute(
            select(VaultEntry).where(
                VaultEntry.user_id == user.id,
                VaultEntry.service_name == service_name,
            )
        ).scalar_one_or_none()

        if existing is not None and not overwrite_existing:
            skipped_fields.append(field_name)
            continue

        notes = f"{note_prefix} for {platform.value} ({field_name}) at {datetime.now(timezone.utc).isoformat()}"
        if existing is None:
            db.add(
                VaultEntry(
                    user_id=user.id,
                    service_name=service_name,
                    username_encrypted=encrypt_secret(field_name),
                    password_encrypted=encrypt_secret(value),
                    notes_encrypted=encrypt_secret(notes),
                )
            )
        else:
            existing.username_encrypted = encrypt_secret(field_name)
            existing.password_encrypted = encrypt_secret(value)
            existing.notes_encrypted = encrypt_secret(notes)
        updated_fields.append(field_name)

    db.commit()
    return updated_fields, skipped_fields


def env_values_for_platform(platform: Platform) -> dict[str, str]:
    required_fields = platform_required_fields(platform)
    values = _env_values(platform)
    return {field: values[field].strip() for field in required_fields if (values.get(field) or "").strip()}
