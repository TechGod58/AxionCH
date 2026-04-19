from datetime import datetime, timedelta
from time import sleep, time
from uuid import uuid4

from fastapi.testclient import TestClient

import app.services.oauth.refresh_worker as refresh_worker_module
from app.core.config import settings
from app.db.models import (
    ConnectionStatus,
    Platform,
    PlatformPost,
    Post,
    PublishJob,
    PublishJobStatus,
    PublishStatus,
    SocialAccount,
    User,
)
from app.db.session import SessionLocal
from app.main import app
from app.services.oauth.exchange import OAuthTokenBundle
from app.services.oauth.token_crypto import decrypt_token_value
from app.services.publish.queue_service import publish_queue_service


def _master_headers(api_key: str) -> dict[str, str]:
    return {"X-Axionch-Api-Key": api_key}


def test_token_refresh_worker_updates_due_accounts(monkeypatch) -> None:
    original_refresh_enabled = settings.oauth_refresh_enabled
    original_refresh_ahead = settings.oauth_refresh_ahead_seconds

    settings.oauth_refresh_enabled = True
    settings.oauth_refresh_ahead_seconds = 3600

    user_email = f"refresh-b15-{uuid4().hex[:10]}@example.com"
    refresh_token = f"refresh-{uuid4().hex[:8]}"

    with TestClient(app):
        db = SessionLocal()
        try:
            user = User(email=user_email)
            db.add(user)
            db.flush()
            account = SocialAccount(
                user_id=user.id,
                platform=Platform.LINKEDIN,
                handle=f"refresh_{uuid4().hex[:6]}",
                access_token="stale-access-token",
                refresh_token=refresh_token,
                token_type="Bearer",
                token_scope="openid profile",
                token_expires_at=datetime.utcnow() - timedelta(minutes=2),
                status=ConnectionStatus.CONNECTED,
            )
            db.add(account)
            db.commit()
            account_id = account.id
        finally:
            db.close()

        def _mock_refresh(platform: Platform, token: str) -> OAuthTokenBundle:
            assert platform == Platform.LINKEDIN
            assert token == refresh_token
            return OAuthTokenBundle(
                access_token="fresh-access-token",
                refresh_token="fresh-refresh-token",
                token_type="Bearer",
                scope="openid profile w_member_social",
                expires_at=datetime.utcnow() + timedelta(hours=3),
            )

        monkeypatch.setattr(refresh_worker_module, "refresh_oauth_token", _mock_refresh)

        worker = refresh_worker_module.OAuthRefreshWorkerService()
        worker.run_once()

        db = SessionLocal()
        try:
            refreshed = db.get(SocialAccount, account_id)
            assert refreshed is not None
            assert decrypt_token_value(refreshed.access_token) == "fresh-access-token"
            assert decrypt_token_value(refreshed.refresh_token) == "fresh-refresh-token"
            assert refreshed.token_scope == "openid profile w_member_social"
            assert refreshed.token_expires_at is not None
            assert refreshed.status == ConnectionStatus.CONNECTED
            metrics = worker.metrics()
            assert metrics["refreshed_total"] >= 1
        finally:
            db.close()

    settings.oauth_refresh_enabled = original_refresh_enabled
    settings.oauth_refresh_ahead_seconds = original_refresh_ahead


def test_user_scoped_authorization_limits_cross_user_access() -> None:
    original_api_key = settings.api_key
    settings.api_key = "batch16-master-key"

    user_a = f"user-a-b16-{uuid4().hex[:8]}@example.com"
    user_b = f"user-b-b16-{uuid4().hex[:8]}@example.com"

    try:
        with TestClient(app) as client:
            master = _master_headers("batch16-master-key")

            key_a = client.post("/auth/keys", headers=master, json={"user_email": user_a, "label": "scope-a"})
            key_b = client.post("/auth/keys", headers=master, json={"user_email": user_b, "label": "scope-b"})
            assert key_a.status_code == 200
            assert key_b.status_code == 200

            raw_key_a = key_a.json()["raw_api_key"]
            user_a_headers = {
                "X-Axionch-Api-Key": raw_key_a,
                "X-Axionch-User-Email": user_a,
            }

            create_a = client.post(
                "/accounts",
                headers=user_a_headers,
                json={
                    "user_email": user_a,
                    "platform": "x",
                    "handle": "@scope_a",
                    "access_token": "scope-a-token",
                },
            )
            assert create_a.status_code == 200
            account_a_id = int(create_a.json()["id"])

            create_cross = client.post(
                "/accounts",
                headers=user_a_headers,
                json={
                    "user_email": user_b,
                    "platform": "x",
                    "handle": "@scope_cross",
                    "access_token": "scope-cross-token",
                },
            )
            assert create_cross.status_code == 403

            create_b = client.post(
                "/accounts",
                headers=master,
                json={
                    "user_email": user_b,
                    "platform": "x",
                    "handle": "@scope_b",
                    "access_token": "scope-b-token",
                },
            )
            assert create_b.status_code == 200
            account_b_id = int(create_b.json()["id"])

            list_scoped = client.get("/accounts", headers=user_a_headers)
            assert list_scoped.status_code == 200
            scoped_ids = {item["id"] for item in list_scoped.json()}
            assert account_a_id in scoped_ids
            assert account_b_id not in scoped_ids

            queue_cross = client.post(
                "/posts/queue",
                headers=user_a_headers,
                json={
                    "user_email": user_a,
                    "body": "Attempt cross-user account publish",
                    "image_url": "https://example.com/image.jpg",
                    "account_ids": [account_b_id],
                },
            )
            assert queue_cross.status_code == 403
    finally:
        settings.api_key = original_api_key


def test_scheduler_restart_replays_due_jobs_from_database() -> None:
    original_api_key = settings.api_key
    settings.api_key = "batch18-master-key"

    user_email = f"replay-b18-{uuid4().hex[:10]}@example.com"

    try:
        with TestClient(app) as client:
            headers = _master_headers("batch18-master-key")
            publish_queue_service.stop()

            db = SessionLocal()
            try:
                user = User(email=user_email)
                db.add(user)
                db.flush()

                account = SocialAccount(
                    user_id=user.id,
                    platform=Platform.X,
                    handle=f"@replay_{uuid4().hex[:6]}",
                    access_token="mock-token-x",
                    status=ConnectionStatus.CONNECTED,
                )
                db.add(account)
                db.flush()

                post = Post(
                    user_id=user.id,
                    body="Replay queued job from persisted DB state",
                    image_url="https://example.com/image.jpg",
                    status=PublishStatus.READY,
                )
                db.add(post)
                db.flush()

                db.add(
                    PlatformPost(
                        post_id=post.id,
                        social_account_id=account.id,
                        platform=Platform.X,
                        transformed_body=post.body,
                        publish_status=PublishStatus.READY,
                    )
                )

                job = PublishJob(
                    post_id=post.id,
                    status=PublishJobStatus.QUEUED,
                    attempt_count=0,
                    max_attempts=1,
                    next_run_at=datetime.utcnow() - timedelta(seconds=1),
                )
                db.add(job)
                db.commit()
                job_id = job.id
            finally:
                db.close()

            publish_queue_service.start()

            deadline = time() + 8.0
            terminal_payload: dict | None = None
            while time() < deadline:
                response = client.get(f"/posts/jobs/{job_id}", headers=headers)
                assert response.status_code == 200
                payload = response.json()
                if payload["status"] in {"success", "failed", "partial"}:
                    terminal_payload = payload
                    break
                sleep(0.15)

            assert terminal_payload is not None, "Expected queued job replay to complete after service restart"
            assert terminal_payload["attempts"] >= 1
    finally:
        publish_queue_service.start()
        settings.api_key = original_api_key
