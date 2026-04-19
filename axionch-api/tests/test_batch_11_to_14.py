from datetime import datetime, timedelta
from time import sleep, time
from uuid import uuid4

from fastapi.testclient import TestClient

import app.routes.oauth as oauth_routes
from app.core.config import settings
from app.db.models import Platform, SocialAccount
from app.db.session import SessionLocal
from app.main import app
from app.services.oauth.exchange import OAuthTokenBundle
from app.services.oauth.token_crypto import decrypt_token_value


def _master_headers(api_key: str) -> dict[str, str]:
    return {"X-Axionch-Api-Key": api_key}


def _wait_for_terminal_job_status(
    client: TestClient,
    headers: dict[str, str],
    job_id: int,
    timeout_seconds: float = 8.0,
) -> dict:
    deadline = time() + timeout_seconds
    last_payload: dict | None = None
    while time() < deadline:
        response = client.get(f"/posts/jobs/{job_id}", headers=headers)
        assert response.status_code == 200
        payload = response.json()
        last_payload = payload
        if payload["status"] in {"success", "failed", "partial"}:
            return payload
        sleep(0.15)
    raise AssertionError(f"Timed out waiting for job {job_id}. Last payload: {last_payload}")


def test_oauth_callback_persists_token_bundle_with_mock_exchange(monkeypatch) -> None:
    original_api_key = settings.api_key
    original_client_id = settings.linkedin_client_id
    original_client_secret = settings.linkedin_client_secret
    original_redirect_uri = settings.linkedin_redirect_uri

    settings.api_key = "batch11-master-key"
    settings.linkedin_client_id = "linkedin-test-client"
    settings.linkedin_client_secret = "linkedin-test-secret"
    settings.linkedin_redirect_uri = "https://example.com/oauth/linkedin/callback"

    def _mock_exchange(platform: Platform, code: str) -> OAuthTokenBundle:
        assert platform == Platform.LINKEDIN
        assert code == "oauth-integration-code"
        return OAuthTokenBundle(
            access_token="linkedin-access-token-b11",
            refresh_token="linkedin-refresh-token-b11",
            token_type="Bearer",
            scope="openid profile w_member_social",
            expires_at=datetime.utcnow() + timedelta(hours=2),
        )

    monkeypatch.setattr(oauth_routes, "exchange_oauth_code", _mock_exchange)

    user_email = f"oauth-b11-{uuid4().hex[:10]}@example.com"

    try:
        with TestClient(app) as client:
            headers = _master_headers("batch11-master-key")
            start_response = client.get(
                "/oauth/linkedin/start",
                headers=headers,
                params={"user_email": user_email},
            )
            assert start_response.status_code == 200
            state = start_response.json()["state"]

            callback_response = client.get(
                "/oauth/linkedin/callback",
                headers=headers,
                params={"code": "oauth-integration-code", "state": state},
            )
            assert callback_response.status_code == 200
            callback_payload = callback_response.json()
            account_id = int(callback_payload["account_id"])

            db = SessionLocal()
            try:
                account = db.get(SocialAccount, account_id)
                assert account is not None
                assert account.platform == Platform.LINKEDIN
                assert decrypt_token_value(account.access_token) == "linkedin-access-token-b11"
                assert decrypt_token_value(account.refresh_token) == "linkedin-refresh-token-b11"
                assert account.token_type == "Bearer"
                assert account.token_scope == "openid profile w_member_social"
                assert account.token_expires_at is not None
            finally:
                db.close()
    finally:
        settings.api_key = original_api_key
        settings.linkedin_client_id = original_client_id
        settings.linkedin_client_secret = original_client_secret
        settings.linkedin_redirect_uri = original_redirect_uri


def test_per_user_api_key_auth_lifecycle() -> None:
    original_api_key = settings.api_key
    user_email = f"user-key-b12-{uuid4().hex[:10]}@example.com"

    settings.api_key = "batch12-master-key"

    try:
        with TestClient(app) as client:
            master_headers = _master_headers("batch12-master-key")

            create_response = client.post(
                "/auth/keys",
                headers=master_headers,
                json={"user_email": user_email, "label": "batch-12-test-key"},
            )
            assert create_response.status_code == 200
            created = create_response.json()
            key_id = created["key_id"]
            raw_user_key = created["raw_api_key"]

            list_response = client.get("/auth/keys", headers=master_headers, params={"user_email": user_email})
            assert list_response.status_code == 200
            list_payload = list_response.json()
            assert any(item["key_id"] == key_id for item in list_payload["keys"])

            anchor_user_email = f"user-key-b12-anchor-{uuid4().hex[:8]}@example.com"
            anchor_key_response = client.post(
                "/auth/keys",
                headers=master_headers,
                json={"user_email": anchor_user_email, "label": "batch-12-anchor-key"},
            )
            assert anchor_key_response.status_code == 200

            settings.api_key = None

            denied_without_headers = client.get("/accounts")
            assert denied_without_headers.status_code == 401

            user_headers = {
                "X-Axionch-Api-Key": raw_user_key,
                "X-Axionch-User-Email": user_email,
            }
            allowed_with_user_key = client.get("/accounts", headers=user_headers)
            assert allowed_with_user_key.status_code == 200

            revoke_response = client.delete(
                f"/auth/keys/{key_id}",
                headers=user_headers,
                params={"user_email": user_email},
            )
            assert revoke_response.status_code == 200
            assert revoke_response.json()["revoked"] is True

            denied_after_revoke = client.get("/accounts", headers=user_headers)
            assert denied_after_revoke.status_code == 401
    finally:
        settings.api_key = original_api_key


def test_dead_letter_metrics_and_requeue_flow() -> None:
    original_api_key = settings.api_key
    original_max_attempts = settings.publish_queue_max_attempts
    original_backoff_seconds = settings.publish_queue_base_backoff_seconds

    settings.api_key = "batch13-master-key"
    settings.publish_queue_max_attempts = 1
    settings.publish_queue_base_backoff_seconds = 0.01

    user_email = f"queue-b13-{uuid4().hex[:10]}@example.com"

    try:
        with TestClient(app) as client:
            headers = _master_headers("batch13-master-key")

            create_account = client.post(
                "/accounts",
                headers=headers,
                json={
                    "user_email": user_email,
                    "platform": "x",
                    "handle": f"@{uuid4().hex[:8]}",
                    "access_token": "mock-token-x",
                },
            )
            assert create_account.status_code == 200
            account_id = int(create_account.json()["id"])

            queue_response = client.post(
                "/posts/queue",
                headers=headers,
                json={
                    "user_email": user_email,
                    "body": "x" * 350,
                    "image_url": "https://example.com/image.jpg",
                    "account_ids": [account_id],
                },
            )
            assert queue_response.status_code == 200
            queued_payload = queue_response.json()

            terminal_status = _wait_for_terminal_job_status(client, headers, queued_payload["job_id"])
            assert terminal_status["status"] in {"failed", "partial"}
            assert terminal_status["post_status"] in {"failed", "partial"}

            dead_letters_response = client.get("/posts/dead-letters", headers=headers, params={"limit": 200})
            assert dead_letters_response.status_code == 200
            dead_letters = dead_letters_response.json()["items"]
            matching_dead_letters = [item for item in dead_letters if item["post_id"] == queued_payload["post_id"]]
            assert matching_dead_letters, "Expected at least one dead-letter entry for the failed queued post"
            dead_letter_id = int(matching_dead_letters[0]["id"])

            metrics_response = client.get("/posts/metrics", headers=headers)
            assert metrics_response.status_code == 200
            metrics_payload = metrics_response.json()
            assert metrics_payload["dead_letter_count"] >= 1
            assert metrics_payload["worker_failure_total"] >= 1
            assert isinstance(metrics_payload["jobs_by_status"], dict)

            requeue_response = client.post(f"/posts/dead-letters/{dead_letter_id}/requeue", headers=headers)
            assert requeue_response.status_code == 200
            requeue_payload = requeue_response.json()
            assert requeue_payload["new_job_id"] > 0
            assert requeue_payload["post_id"] == queued_payload["post_id"]
    finally:
        settings.api_key = original_api_key
        settings.publish_queue_max_attempts = original_max_attempts
        settings.publish_queue_base_backoff_seconds = original_backoff_seconds
