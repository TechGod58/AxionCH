from dataclasses import dataclass
from datetime import datetime, timedelta

import httpx

from app.core.config import settings
from app.db.models import Platform


class OAuthExchangeError(Exception):
    def __init__(
        self,
        message: str,
        *,
        provider: str | None = None,
        status_code: int | None = None,
        retryable: bool = True,
        error_code: str | None = None,
    ) -> None:
        super().__init__(message)
        self.provider = provider
        self.status_code = status_code
        self.retryable = retryable
        self.error_code = error_code


@dataclass
class OAuthTokenBundle:
    access_token: str
    refresh_token: str | None
    token_type: str | None
    scope: str | None
    expires_at: datetime | None


def _safe_json(response: httpx.Response) -> dict:
    if not response.content:
        return {}
    try:
        payload = response.json()
        if isinstance(payload, dict):
            return payload
    except Exception:
        pass
    return {}


def _is_retryable_failure(status_code: int | None, error_code: str | None) -> bool:
    if status_code is not None:
        if status_code == 429 or status_code >= 500:
            return True
        if status_code in {400, 401, 403, 404, 422}:
            return False

    code = (error_code or "").strip().lower()
    if code in {
        "invalid_grant",
        "invalid_client",
        "invalid_request",
        "unauthorized_client",
        "unsupported_grant_type",
        "insufficient_scope",
    }:
        return False

    # Default conservative posture: treat unknown failures as retryable.
    return True


def _raise_oauth_error(provider: str, action: str, response: httpx.Response) -> None:
    payload = _safe_json(response)
    error_code = str(payload.get("error") or payload.get("code") or "").strip() or None
    error_description = str(
        payload.get("error_description")
        or payload.get("message")
        or payload.get("error_message")
        or response.text[:400]
        or "Unknown OAuth failure"
    )
    retryable = _is_retryable_failure(response.status_code, error_code)
    message = f"{provider} token {action} failed ({response.status_code}): {error_description}"
    raise OAuthExchangeError(
        message,
        provider=provider,
        status_code=response.status_code,
        retryable=retryable,
        error_code=error_code,
    )


def _token_expiry(expires_in: int | float | str | None) -> datetime | None:
    if expires_in is None:
        return None
    try:
        seconds = int(float(expires_in))
    except Exception:
        return None
    if seconds <= 0:
        return None
    return datetime.utcnow() + timedelta(seconds=seconds)


def _extract_bundle(payload: dict) -> OAuthTokenBundle:
    access_token = str(payload.get("access_token") or "").strip()
    if not access_token:
        raise OAuthExchangeError("OAuth token exchange response did not include access_token")
    refresh_token = payload.get("refresh_token")
    if refresh_token is not None:
        refresh_token = str(refresh_token)
    token_type = payload.get("token_type")
    if token_type is not None:
        token_type = str(token_type)
    scope = payload.get("scope")
    if isinstance(scope, list):
        scope = " ".join(str(item) for item in scope)
    elif scope is not None:
        scope = str(scope)

    expires_at = _token_expiry(payload.get("expires_in"))
    return OAuthTokenBundle(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type=token_type,
        scope=scope,
        expires_at=expires_at,
    )


def _exchange_linkedin(code: str) -> OAuthTokenBundle:
    if not settings.linkedin_client_id or not settings.linkedin_client_secret or not settings.linkedin_redirect_uri:
        raise OAuthExchangeError("LinkedIn OAuth is not fully configured")

    response = httpx.post(
        settings.linkedin_oauth_token_url,
        data={
            "grant_type": "authorization_code",
            "code": code,
            "redirect_uri": settings.linkedin_redirect_uri,
            "client_id": settings.linkedin_client_id,
            "client_secret": settings.linkedin_client_secret,
        },
        timeout=max(5.0, settings.oauth_exchange_timeout_seconds),
    )
    if not response.is_success:
        _raise_oauth_error("LinkedIn", "exchange", response)
    payload = _safe_json(response)
    return _extract_bundle(payload)


def _exchange_instagram(code: str) -> OAuthTokenBundle:
    if not settings.instagram_app_id or not settings.instagram_app_secret or not settings.instagram_redirect_uri:
        raise OAuthExchangeError("Instagram OAuth is not fully configured")

    response = httpx.post(
        settings.instagram_oauth_token_url,
        data={
            "client_id": settings.instagram_app_id,
            "client_secret": settings.instagram_app_secret,
            "grant_type": "authorization_code",
            "redirect_uri": settings.instagram_redirect_uri,
            "code": code,
        },
        timeout=max(5.0, settings.oauth_exchange_timeout_seconds),
    )
    if not response.is_success:
        _raise_oauth_error("Instagram", "exchange", response)
    payload = _safe_json(response)
    return _extract_bundle(payload)


def _exchange_x(code: str) -> OAuthTokenBundle:
    if not settings.x_client_id or not settings.x_api_secret or not settings.x_redirect_uri:
        raise OAuthExchangeError("X OAuth is not fully configured")

    response = httpx.post(
        settings.x_oauth_token_url,
        data={
            "grant_type": "authorization_code",
            "code": code,
            "redirect_uri": settings.x_redirect_uri,
            "client_id": settings.x_client_id,
        },
        auth=(settings.x_client_id, settings.x_api_secret),
        timeout=max(5.0, settings.oauth_exchange_timeout_seconds),
    )
    if not response.is_success:
        _raise_oauth_error("X", "exchange", response)
    payload = _safe_json(response)
    return _extract_bundle(payload)


def _refresh_linkedin(refresh_token: str) -> OAuthTokenBundle:
    if not settings.linkedin_client_id or not settings.linkedin_client_secret:
        raise OAuthExchangeError("LinkedIn OAuth refresh is not fully configured")

    response = httpx.post(
        settings.linkedin_oauth_token_url,
        data={
            "grant_type": "refresh_token",
            "refresh_token": refresh_token,
            "client_id": settings.linkedin_client_id,
            "client_secret": settings.linkedin_client_secret,
        },
        timeout=max(5.0, settings.oauth_exchange_timeout_seconds),
    )
    if not response.is_success:
        _raise_oauth_error("LinkedIn", "refresh", response)
    payload = _safe_json(response)
    return _extract_bundle(payload)


def _refresh_x(refresh_token: str) -> OAuthTokenBundle:
    if not settings.x_client_id or not settings.x_api_secret:
        raise OAuthExchangeError("X OAuth refresh is not fully configured")

    response = httpx.post(
        settings.x_oauth_token_url,
        data={
            "grant_type": "refresh_token",
            "refresh_token": refresh_token,
            "client_id": settings.x_client_id,
        },
        auth=(settings.x_client_id, settings.x_api_secret),
        timeout=max(5.0, settings.oauth_exchange_timeout_seconds),
    )
    if not response.is_success:
        _raise_oauth_error("X", "refresh", response)
    payload = _safe_json(response)
    return _extract_bundle(payload)


def _refresh_instagram(access_token: str) -> OAuthTokenBundle:
    # Instagram long-lived token refresh uses ig_refresh_token + current long-lived token.
    response = httpx.get(
        settings.instagram_oauth_refresh_url,
        params={
            "grant_type": "ig_refresh_token",
            "access_token": access_token,
        },
        timeout=max(5.0, settings.oauth_exchange_timeout_seconds),
    )
    if response.is_success:
        payload = _safe_json(response)
        return _extract_bundle(payload)

    # Fallback generic refresh grant for providers requiring client credentials.
    if not settings.instagram_app_id or not settings.instagram_app_secret:
        _raise_oauth_error("Instagram", "refresh", response)

    fallback = httpx.post(
        settings.instagram_oauth_token_url,
        data={
            "grant_type": "refresh_token",
            "refresh_token": access_token,
            "client_id": settings.instagram_app_id,
            "client_secret": settings.instagram_app_secret,
        },
        timeout=max(5.0, settings.oauth_exchange_timeout_seconds),
    )
    if not fallback.is_success:
        _raise_oauth_error("Instagram", "refresh", fallback)
    payload = _safe_json(fallback)
    return _extract_bundle(payload)


def exchange_oauth_code(platform: Platform, code: str) -> OAuthTokenBundle:
    if platform == Platform.X:
        return _exchange_x(code)
    if platform == Platform.LINKEDIN:
        return _exchange_linkedin(code)
    if platform == Platform.INSTAGRAM:
        return _exchange_instagram(code)
    raise OAuthExchangeError(f"Unsupported platform for OAuth exchange: {platform.value}")


def refresh_oauth_token(platform: Platform, refresh_credential: str) -> OAuthTokenBundle:
    token = (refresh_credential or "").strip()
    if not token:
        raise OAuthExchangeError("Refresh token is missing")

    if platform == Platform.X:
        return _refresh_x(token)
    if platform == Platform.LINKEDIN:
        return _refresh_linkedin(token)
    if platform == Platform.INSTAGRAM:
        return _refresh_instagram(token)
    raise OAuthExchangeError(f"Unsupported platform for OAuth refresh: {platform.value}")
