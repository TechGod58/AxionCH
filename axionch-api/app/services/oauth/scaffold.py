from dataclasses import dataclass
from datetime import datetime, timedelta
from secrets import token_urlsafe
from threading import Lock
from urllib.parse import urlencode

from app.core.config import settings
from app.db.models import Platform


@dataclass
class OAuthStatePayload:
    platform: Platform
    user_email: str
    expires_at: datetime


class OAuthStateStore:
    def __init__(self) -> None:
        self._lock = Lock()
        self._states: dict[str, OAuthStatePayload] = {}

    def issue(self, platform: Platform, user_email: str) -> str:
        state = token_urlsafe(24)
        payload = OAuthStatePayload(
            platform=platform,
            user_email=user_email,
            expires_at=datetime.utcnow() + timedelta(seconds=max(60, settings.oauth_state_ttl_seconds)),
        )
        with self._lock:
            self._states[state] = payload
        return state

    def consume(self, state: str, platform: Platform) -> OAuthStatePayload | None:
        with self._lock:
            payload = self._states.pop(state, None)
        if payload is None:
            return None
        if payload.platform != platform:
            return None
        if payload.expires_at < datetime.utcnow():
            return None
        return payload


def oauth_config(platform: Platform) -> tuple[str | None, str | None, list[str], str]:
    if platform == Platform.X:
        scopes = ["tweet.read", "tweet.write", "users.read", "offline.access"]
        auth_endpoint = "https://twitter.com/i/oauth2/authorize"
        return settings.x_client_id, settings.x_redirect_uri, scopes, auth_endpoint
    if platform == Platform.LINKEDIN:
        scopes = ["openid", "profile", "w_member_social"]
        auth_endpoint = "https://www.linkedin.com/oauth/v2/authorization"
        return settings.linkedin_client_id, settings.linkedin_redirect_uri, scopes, auth_endpoint
    scopes = ["instagram_basic", "instagram_content_publish", "pages_show_list"]
    auth_endpoint = "https://www.facebook.com/v20.0/dialog/oauth"
    return settings.instagram_app_id, settings.instagram_redirect_uri, scopes, auth_endpoint


def build_authorize_url(platform: Platform, state: str) -> tuple[str, list[str], str]:
    client_id, redirect_uri, scopes, auth_endpoint = oauth_config(platform)
    if not client_id:
        raise ValueError(f"{platform.value} OAuth client id is not configured")
    if not redirect_uri:
        raise ValueError(f"{platform.value} OAuth redirect URI is not configured")

    params = {
        "response_type": "code",
        "client_id": client_id,
        "redirect_uri": redirect_uri,
        "scope": " ".join(scopes),
        "state": state,
    }
    return f"{auth_endpoint}?{urlencode(params)}", scopes, redirect_uri


oauth_state_store = OAuthStateStore()
