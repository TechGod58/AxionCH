from datetime import datetime, timedelta
from uuid import uuid4

from fastapi.testclient import TestClient

import app.services.oauth.refresh_worker as refresh_worker_module
from app.core.config import settings
from app.db.models import ConnectionStatus, Platform, SocialAccount, User
from app.db.session import SessionLocal
from app.main import app
from app.services.oauth.exchange import OAuthExchangeError, OAuthTokenBundle
from app.services.oauth.token_crypto import decrypt_token_value


def test_instagram_refresh_uses_access_token_when_refresh_token_missing(monkeypatch) -> None:
    original_refresh_enabled = settings.oauth_refresh_enabled
    original_refresh_ahead = settings.oauth_refresh_ahead_seconds

    settings.oauth_refresh_enabled = True
    settings.oauth_refresh_ahead_seconds = 3600

    user_email = f"ig-refresh-b19-{uuid4().hex[:10]}@example.com"
    original_access_token = f"ig-long-lived-{uuid4().hex[:8]}"

    with TestClient(app):
        db = SessionLocal()
        try:
            user = User(email=user_email)
            db.add(user)
            db.flush()
            account = SocialAccount(
                user_id=user.id,
                platform=Platform.INSTAGRAM,
                handle=f"ig_{uuid4().hex[:6]}",
                access_token=original_access_token,
                refresh_token=None,
                token_type="Bearer",
                token_scope="instagram_basic instagram_content_publish",
                token_expires_at=datetime.utcnow() - timedelta(minutes=1),
                status=ConnectionStatus.CONNECTED,
            )
            db.add(account)
            db.commit()
            account_id = account.id
        finally:
            db.close()

        def _mock_refresh(platform: Platform, credential: str) -> OAuthTokenBundle:
            assert platform == Platform.INSTAGRAM
            assert credential == original_access_token
            return OAuthTokenBundle(
                access_token="ig-fresh-access-token",
                refresh_token=None,
                token_type="Bearer",
                scope="instagram_basic instagram_content_publish",
                expires_at=datetime.utcnow() + timedelta(days=55),
            )

        monkeypatch.setattr(refresh_worker_module, "refresh_oauth_token", _mock_refresh)

        worker = refresh_worker_module.OAuthRefreshWorkerService()
        worker.run_once()

        db = SessionLocal()
        try:
            refreshed = db.get(SocialAccount, account_id)
            assert refreshed is not None
            assert decrypt_token_value(refreshed.access_token) == "ig-fresh-access-token"
            assert refreshed.status == ConnectionStatus.CONNECTED
            assert refreshed.token_expires_at is not None
            metrics = worker.metrics()
            assert metrics["refreshed_total"] >= 1
        finally:
            db.close()

    settings.oauth_refresh_enabled = original_refresh_enabled
    settings.oauth_refresh_ahead_seconds = original_refresh_ahead


def test_non_retryable_refresh_error_marks_account_expired(monkeypatch) -> None:
    original_refresh_enabled = settings.oauth_refresh_enabled
    original_refresh_ahead = settings.oauth_refresh_ahead_seconds

    settings.oauth_refresh_enabled = True
    settings.oauth_refresh_ahead_seconds = 3600

    user_email = f"li-refresh-b19-{uuid4().hex[:10]}@example.com"
    refresh_token = f"li-refresh-{uuid4().hex[:8]}"

    with TestClient(app):
        db = SessionLocal()
        try:
            user = User(email=user_email)
            db.add(user)
            db.flush()
            account = SocialAccount(
                user_id=user.id,
                platform=Platform.LINKEDIN,
                handle=f"li_{uuid4().hex[:6]}",
                access_token="li-old-access-token",
                refresh_token=refresh_token,
                token_type="Bearer",
                token_scope="openid profile w_member_social",
                token_expires_at=datetime.utcnow() - timedelta(minutes=1),
                status=ConnectionStatus.CONNECTED,
            )
            db.add(account)
            db.commit()
            account_id = account.id
        finally:
            db.close()

        def _mock_refresh_failure(platform: Platform, credential: str) -> OAuthTokenBundle:
            assert platform == Platform.LINKEDIN
            assert credential == refresh_token
            raise OAuthExchangeError(
                "LinkedIn token refresh failed (400): invalid_grant",
                provider="LinkedIn",
                status_code=400,
                retryable=False,
                error_code="invalid_grant",
            )

        monkeypatch.setattr(refresh_worker_module, "refresh_oauth_token", _mock_refresh_failure)

        worker = refresh_worker_module.OAuthRefreshWorkerService()
        worker.run_once()

        db = SessionLocal()
        try:
            refreshed = db.get(SocialAccount, account_id)
            assert refreshed is not None
            assert refreshed.status == ConnectionStatus.EXPIRED
            metrics = worker.metrics()
            assert metrics["failed_total"] >= 1
        finally:
            db.close()

    settings.oauth_refresh_enabled = original_refresh_enabled
    settings.oauth_refresh_ahead_seconds = original_refresh_ahead
