from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.core.auth import AuthIdentity, enforce_user_scope, get_auth_identity
from app.core.security_checks import evaluate_security_checks, security_warnings
from app.db.models import Platform
from app.db.session import get_db
from app.schemas.config import (
    ConfigCheckResponse,
    ConfigCheckResult,
    ConfigSecurityResponse,
    ConfigStatusResponse,
    PlatformConfigStatus,
    PlatformSecretMigrationRequest,
    PlatformSecretMigrationResponse,
    PlatformSecretWriteRequest,
    PlatformSecretWriteResult,
    SecretCheckStatus,
)
from app.services.publish.credential_checks import credential_check_tracker
from app.services.publish.platform_secrets import (
    env_values_for_platform,
    resolve_platform_secrets,
    upsert_platform_secrets,
)
from app.services.publish.registry import registry

router = APIRouter()


def _status_with_checks(status: PlatformConfigStatus, platform: Platform) -> PlatformConfigStatus:
    check = credential_check_tracker.get(platform)
    return PlatformConfigStatus(
        mode=status.mode,
        configured=status.configured,
        required_fields=status.required_fields,
        configured_fields=status.configured_fields,
        source_by_field=status.source_by_field,
        last_checked_at=check.get("last_checked_at"),
        last_check_success=check.get("last_check_success"),
        last_check_error=check.get("last_check_error"),
        check_count=int(check.get("check_count") or 0),
    )


def _platform_status(
    db: Session,
    *,
    platform: Platform,
    user_email: str | None,
) -> PlatformConfigStatus:
    resolution = resolve_platform_secrets(
        db,
        platform=platform,
        user_email=user_email,
    )
    return PlatformConfigStatus(
        mode=resolution.mode,
        configured=resolution.configured,
        required_fields=resolution.required_fields,
        configured_fields=resolution.configured_fields,
        source_by_field=resolution.source_by_field,
    )


@router.get("/status", response_model=ConfigStatusResponse)
def config_status(
    user_email: str | None = Query(default=None, min_length=3),
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> ConfigStatusResponse:
    effective_email = enforce_user_scope(auth, user_email) or user_email

    x_status = _status_with_checks(
        _platform_status(db, platform=Platform.X, user_email=effective_email),
        Platform.X,
    )
    linkedin_status = _status_with_checks(
        _platform_status(db, platform=Platform.LINKEDIN, user_email=effective_email),
        Platform.LINKEDIN,
    )
    instagram_status = _status_with_checks(
        _platform_status(db, platform=Platform.INSTAGRAM, user_email=effective_email),
        Platform.INSTAGRAM,
    )
    return ConfigStatusResponse(
        x=x_status,
        linkedin=linkedin_status,
        instagram=instagram_status,
    )


@router.post("/check", response_model=ConfigCheckResponse)
def run_config_checks(
    user_email: str | None = Query(default=None, min_length=3),
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> ConfigCheckResponse:
    effective_email = enforce_user_scope(auth, user_email) or user_email
    test_body = "Credential check (dry run)"
    test_image = "https://example.com/credential-check.jpg"

    results = []
    for platform in [Platform.X, Platform.LINKEDIN, Platform.INSTAGRAM]:
        resolution = resolve_platform_secrets(
            db,
            platform=platform,
            user_email=effective_email,
        )
        adapter = registry.get(platform, prefer_mock=not resolution.configured)
        publish_result = adapter.dry_run(
            test_body,
            test_image,
            secrets=resolution.values if resolution.configured else None,
        )
        check = credential_check_tracker.get(platform)
        results.append(
            ConfigCheckResult(
                platform=platform.value,
                success=publish_result.success,
                error_message=publish_result.error_message,
                last_checked_at=check.get("last_checked_at"),
                check_count=int(check.get("check_count") or 0),
            )
        )

    return ConfigCheckResponse(
        checked_at=datetime.now(timezone.utc).isoformat(),
        results=results,
    )


@router.get("/security", response_model=ConfigSecurityResponse)
def config_security_status() -> ConfigSecurityResponse:
    checks = evaluate_security_checks()
    warnings = security_warnings()
    return ConfigSecurityResponse(
        production_ready=len(warnings) == 0,
        warnings=warnings,
        checks=[
            SecretCheckStatus(
                name=check.name,
                configured=check.configured,
                strong=check.strong,
                message=check.message,
            )
            for check in checks
        ],
    )


def _write_platform_secret_result(
    db: Session,
    *,
    platform: Platform,
    user_email: str,
    updated_fields: list[str],
    skipped_fields: list[str],
) -> PlatformSecretWriteResult:
    status = _platform_status(db, platform=platform, user_email=user_email)
    return PlatformSecretWriteResult(
        platform=platform.value,
        user_email=user_email,
        updated_fields=updated_fields,
        skipped_fields=skipped_fields,
        status=status,
    )


@router.put("/platform-secrets/{platform}", response_model=PlatformSecretWriteResult)
def write_platform_secrets(
    platform: Platform,
    payload: PlatformSecretWriteRequest,
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> PlatformSecretWriteResult:
    effective_email = enforce_user_scope(auth, payload.user_email) or payload.user_email
    try:
        updated_fields, skipped_fields = upsert_platform_secrets(
            db,
            user_email=effective_email,
            platform=platform,
            secrets=payload.secrets,
            overwrite_existing=payload.overwrite_existing,
            note_prefix="Direct platform secret update",
        )
    except ValueError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    return _write_platform_secret_result(
        db,
        platform=platform,
        user_email=effective_email,
        updated_fields=updated_fields,
        skipped_fields=skipped_fields,
    )


@router.post("/platform-secrets/migrate-env", response_model=PlatformSecretMigrationResponse)
def migrate_env_secrets_to_vault(
    payload: PlatformSecretMigrationRequest,
    db: Session = Depends(get_db),
    auth: AuthIdentity = Depends(get_auth_identity),
) -> PlatformSecretMigrationResponse:
    effective_email = enforce_user_scope(auth, payload.user_email) or payload.user_email

    results: list[PlatformSecretWriteResult] = []
    for platform in [Platform.X, Platform.LINKEDIN, Platform.INSTAGRAM]:
        env_secrets = env_values_for_platform(platform)
        if not env_secrets:
            results.append(
                _write_platform_secret_result(
                    db,
                    platform=platform,
                    user_email=effective_email,
                    updated_fields=[],
                    skipped_fields=[],
                )
            )
            continue
        try:
            updated_fields, skipped_fields = upsert_platform_secrets(
                db,
                user_email=effective_email,
                platform=platform,
                secrets=env_secrets,
                overwrite_existing=payload.overwrite_existing,
                note_prefix="Migrated from environment variable",
            )
        except ValueError as exc:
            raise HTTPException(status_code=503, detail=str(exc)) from exc

        results.append(
            _write_platform_secret_result(
                db,
                platform=platform,
                user_email=effective_email,
                updated_fields=updated_fields,
                skipped_fields=skipped_fields,
            )
        )

    return PlatformSecretMigrationResponse(
        user_email=effective_email,
        results=results,
    )
