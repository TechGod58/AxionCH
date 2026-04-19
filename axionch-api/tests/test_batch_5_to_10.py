from datetime import datetime, timedelta

from fastapi.testclient import TestClient

from app.core.config import settings
from app.db.models import Platform, SocialAccount
from app.db.session import SessionLocal
from app.main import app
import app.routes.oauth as oauth_routes
from app.services.oauth.exchange import OAuthTokenBundle
from app.services.oauth.token_crypto import decrypt_token_value


def _auth_headers() -> dict[str, str]:
    api_key = (settings.api_key or "").strip()
    if not api_key:
        settings.api_key = "test-suite-api-key"
        api_key = settings.api_key
    return {"X-Axionch-Api-Key": api_key}


def _ensure_account(client: TestClient, platform: str) -> int:
    headers = _auth_headers()
    accounts = client.get("/accounts", headers=headers).json()
    for account in accounts:
        if account["platform"] == platform:
            return int(account["id"])

    create_response = client.post(
        "/accounts",
        headers=headers,
        json={
            "user_email": "queue-tests@example.com",
            "platform": platform,
            "handle": f"@queue_{platform}",
            "access_token": f"mock-token-{platform}",
        },
    )
    assert create_response.status_code == 200
    return int(create_response.json()["id"])


def test_api_key_auth_gate() -> None:
    original_api_key = settings.api_key
    settings.api_key = "unit-test-api-key"

    try:
        with TestClient(app) as client:
            without_header = client.get("/accounts")
            assert without_header.status_code == 401

            with_header = client.get("/accounts", headers={"X-Axionch-Api-Key": "unit-test-api-key"})
            assert with_header.status_code == 200
    finally:
        settings.api_key = original_api_key


def test_config_security_endpoint() -> None:
    with TestClient(app) as client:
        response = client.get("/config/security", headers=_auth_headers())
        assert response.status_code == 200
        payload = response.json()
        assert "production_ready" in payload
        assert isinstance(payload.get("warnings", []), list)
        assert isinstance(payload.get("checks", []), list)


def test_queue_publish_job_contract() -> None:
    with TestClient(app) as client:
        headers = _auth_headers()
        account_id = _ensure_account(client, "x")

        queue_response = client.post(
            "/posts/queue",
            headers=headers,
            json={
                "user_email": "queue-tests@example.com",
                "body": "queue job contract test",
                "image_url": "https://example.com/image.jpg",
                "account_ids": [account_id],
            },
        )
        assert queue_response.status_code == 200
        queue_payload = queue_response.json()
        assert queue_payload["job_id"] > 0
        assert queue_payload["post_id"] > 0

        status_response = client.get(f"/posts/jobs/{queue_payload['job_id']}", headers=headers)
        assert status_response.status_code == 200
        status_payload = status_response.json()
        assert status_payload["job_id"] == queue_payload["job_id"]
        assert isinstance(status_payload.get("results", []), list)


def test_oauth_scaffold_start_and_callback(monkeypatch) -> None:
    original_client_id = settings.linkedin_client_id
    original_redirect_uri = settings.linkedin_redirect_uri

    settings.linkedin_client_id = settings.linkedin_client_id or "linkedin-test-client"
    settings.linkedin_redirect_uri = settings.linkedin_redirect_uri or "https://example.com/oauth/linkedin/callback"

    def _mock_exchange(platform: Platform, code: str) -> OAuthTokenBundle:
        assert platform == Platform.LINKEDIN
        assert code == "oauth-test-code"
        return OAuthTokenBundle(
            access_token="oauth-access-token",
            refresh_token="oauth-refresh-token",
            token_type="Bearer",
            scope="openid profile w_member_social",
            expires_at=datetime.utcnow() + timedelta(hours=1),
        )

    monkeypatch.setattr(oauth_routes, "exchange_oauth_code", _mock_exchange)

    try:
        with TestClient(app) as client:
            headers = _auth_headers()

            start_response = client.get(
                "/oauth/linkedin/start",
                headers=headers,
                params={"user_email": "oauth-tests@example.com"},
            )
            assert start_response.status_code == 200
            start_payload = start_response.json()
            assert start_payload["platform"] == "linkedin"
            assert start_payload["state"]

            callback_response = client.get(
                "/oauth/linkedin/callback",
                headers=headers,
                params={"code": "oauth-test-code", "state": start_payload["state"]},
            )
            assert callback_response.status_code == 200
            callback_payload = callback_response.json()
            assert callback_payload["state_valid"] is True
            assert callback_payload["account_id"] is not None

            db = SessionLocal()
            try:
                account = db.get(SocialAccount, int(callback_payload["account_id"]))
                assert account is not None
                assert account.platform == Platform.LINKEDIN
                assert decrypt_token_value(account.access_token) == "oauth-access-token"
                assert decrypt_token_value(account.refresh_token) == "oauth-refresh-token"
                assert account.token_type == "Bearer"
                assert account.token_scope == "openid profile w_member_social"
                assert account.token_expires_at is not None
            finally:
                db.close()
    finally:
        settings.linkedin_client_id = original_client_id
        settings.linkedin_redirect_uri = original_redirect_uri
