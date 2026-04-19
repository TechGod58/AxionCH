from dataclasses import dataclass

from app.core.cors import parse_csv
from app.core.config import settings


@dataclass
class SecretCheck:
    name: str
    configured: bool
    strong: bool
    message: str


def _is_strong_secret(secret: str, minimum_length: int = 24) -> bool:
    lowered = secret.lower()
    return len(secret) >= minimum_length and "replace-me" not in lowered and "changeme" not in lowered


def evaluate_security_checks() -> list[SecretCheck]:
    checks: list[SecretCheck] = []

    token_key = (settings.token_encryption_key or "").strip()
    checks.append(
        SecretCheck(
            name="TOKEN_ENCRYPTION_KEY",
            configured=bool(token_key),
            strong=_is_strong_secret(token_key, minimum_length=32),
            message="Used to protect encrypted token storage.",
        )
    )

    vault_key = (settings.vault_encryption_key or "").strip()
    checks.append(
        SecretCheck(
            name="VAULT_ENCRYPTION_KEY",
            configured=bool(vault_key),
            strong=_is_strong_secret(vault_key, minimum_length=32),
            message="Used to encrypt credential vault usernames/passwords at rest.",
        )
    )

    api_key = (settings.api_key or "").strip()
    checks.append(
        SecretCheck(
            name="API_KEY",
            configured=bool(api_key),
            strong=(not api_key) or _is_strong_secret(api_key, minimum_length=24),
            message="Optional request auth key for API routes.",
        )
    )

    hash_salt = (settings.api_key_hash_salt or "").strip()
    checks.append(
        SecretCheck(
            name="API_KEY_HASH_SALT",
            configured=bool(hash_salt),
            strong=_is_strong_secret(hash_salt, minimum_length=24),
            message="Salt used to hash per-user API keys.",
        )
    )

    return checks


def security_warnings() -> list[str]:
    warnings: list[str] = []
    for check in evaluate_security_checks():
        if not check.configured:
            if check.name == "API_KEY":
                if settings.enforce_api_auth:
                    warnings.append("API_KEY is not configured; per-user keys must be provisioned before API access.")
                else:
                    warnings.append("API_KEY is not configured; API auth is disabled.")
            else:
                warnings.append(f"{check.name} is not configured.")
        elif not check.strong:
            warnings.append(f"{check.name} appears weak and should be rotated.")

    if settings.debug:
        warnings.append("DEBUG is enabled; disable it before production.")

    if settings.allow_bootstrap_without_api_key:
        warnings.append("ALLOW_BOOTSTRAP_WITHOUT_API_KEY is enabled; disable for production.")

    cors_origins = parse_csv(settings.cors_allowed_origins)
    if "*" in cors_origins:
        warnings.append("CORS allows wildcard origins; restrict CORS_ALLOWED_ORIGINS for production.")

    runtime = (settings.runtime_environment or "").strip().lower()
    if runtime == "production":
        if not cors_origins:
            warnings.append("CORS_ALLOWED_ORIGINS is empty in production.")
        for origin in cors_origins:
            lowered = origin.lower()
            if "localhost" in lowered or "127.0.0.1" in lowered or "10.0.2.2" in lowered:
                warnings.append(
                    f"CORS origin '{origin}' is local-only and should be removed in production."
                )

    if settings.database_url.startswith("sqlite"):
        warnings.append("Using SQLite; switch to managed Postgres for production scale/reliability.")

    return warnings
