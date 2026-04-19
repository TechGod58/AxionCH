from dataclasses import dataclass

from fastapi import Header, HTTPException, Request

from app.core.config import settings
from app.db.session import SessionLocal
from app.services.auth.credentials import any_active_user_keys, verify_user_key


@dataclass
class AuthIdentity:
    auth_mode: str
    user_email: str | None = None


SCOPED_AUTH_MODES = {"user_key", "scoped_global", "scoped_bootstrap"}


def _normalize_email(email: str | None) -> str | None:
    normalized = (email or "").strip().lower()
    return normalized or None


def is_user_scoped(auth: AuthIdentity) -> bool:
    return auth.auth_mode in SCOPED_AUTH_MODES


def scoped_user_email(auth: AuthIdentity) -> str | None:
    if not is_user_scoped(auth):
        return None
    return _normalize_email(auth.user_email)


def enforce_user_scope(auth: AuthIdentity, requested_user_email: str | None = None) -> str | None:
    scoped_email = scoped_user_email(auth)
    requested = _normalize_email(requested_user_email)

    if is_user_scoped(auth):
        if scoped_email is None:
            raise HTTPException(status_code=401, detail="Scoped authentication missing user email")
        if requested is not None and requested != scoped_email:
            raise HTTPException(status_code=403, detail="Authenticated user is not authorized for this user_email")
        return scoped_email

    return requested


def require_api_key(
    request: Request,
    x_axionch_api_key: str | None = Header(default=None),
    x_axionch_user_email: str | None = Header(default=None),
) -> None:
    scoped_email = _normalize_email(x_axionch_user_email)
    configured_key = (settings.api_key or "").strip()
    if configured_key and x_axionch_api_key == configured_key:
        request.state.auth_identity = AuthIdentity(
            auth_mode="scoped_global" if scoped_email else "global",
            user_email=scoped_email,
        )
        return

    db = SessionLocal()
    try:
        if scoped_email and x_axionch_api_key:
            if verify_user_key(db, user_email=scoped_email, raw_key=x_axionch_api_key):
                request.state.auth_identity = AuthIdentity(auth_mode="user_key", user_email=scoped_email)
                return

        has_user_keys = any_active_user_keys(db)
    finally:
        db.close()

    if configured_key or has_user_keys:
        raise HTTPException(status_code=401, detail="Invalid or missing API key")

    if settings.enforce_api_auth and not settings.allow_bootstrap_without_api_key:
        raise HTTPException(
            status_code=401,
            detail=(
                "API authentication is enforced. Configure API_KEY or create a per-user key with an authenticated"
                " bootstrap session."
            ),
        )

    # Optional bootstrap/dev mode: no global key and no user keys yet.
    request.state.auth_identity = AuthIdentity(
        auth_mode="scoped_bootstrap" if scoped_email else "bootstrap",
        user_email=scoped_email,
    )
    return


def get_auth_identity(request: Request) -> AuthIdentity:
    identity = getattr(request.state, "auth_identity", None)
    if isinstance(identity, AuthIdentity):
        return identity
    return AuthIdentity(auth_mode="bootstrap", user_email=None)
