from fastapi.testclient import TestClient

from app.core.config import settings
from app.main import app


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
            "user_email": "you@example.com",
            "platform": platform,
            "handle": f"@test_{platform}",
            "access_token": f"mock-token-{platform}",
        },
    )
    assert create_response.status_code == 200
    return int(create_response.json()["id"])


def test_config_status_includes_last_check_fields() -> None:
    with TestClient(app) as client:
        response = client.get("/config/status", headers=_auth_headers())
        assert response.status_code == 200
        payload = response.json()

        for platform in ["x", "linkedin", "instagram"]:
            status = payload[platform]
            assert "last_checked_at" in status
            assert "last_check_success" in status
            assert "last_check_error" in status
            assert "check_count" in status


def test_config_check_increments_check_count() -> None:
    with TestClient(app) as client:
        headers = _auth_headers()
        before = client.get("/config/status", headers=headers)
        assert before.status_code == 200
        before_payload = before.json()

        check_response = client.post("/config/check", headers=headers)
        assert check_response.status_code == 200

        after = client.get("/config/status", headers=headers)
        assert after.status_code == 200
        after_payload = after.json()

        for platform in ["x", "linkedin", "instagram"]:
            assert after_payload[platform]["check_count"] >= before_payload[platform]["check_count"]


def test_dry_run_history_filters_limits_and_clear() -> None:
    with TestClient(app) as client:
        headers = _auth_headers()
        clear_all = client.delete("/posts/dry-run-history", headers=headers)
        assert clear_all.status_code == 200

        x_account = _ensure_account(client, "x")
        linkedin_account = _ensure_account(client, "linkedin")
        instagram_account = _ensure_account(client, "instagram")

        for account_id in [x_account, linkedin_account, instagram_account]:
            dry_run = client.post(
                "/posts/dry-run",
                headers=headers,
                json={
                    "user_email": "you@example.com",
                    "body": "history filter regression test",
                    "image_url": "https://example.com/image.jpg",
                    "account_ids": [account_id],
                },
            )
            assert dry_run.status_code == 200
            assert dry_run.json()["dry_run"] is True

        history_limit = client.get("/posts/dry-run-history?limit=2", headers=headers)
        assert history_limit.status_code == 200
        history_limit_payload = history_limit.json()
        assert history_limit_payload["limit"] == 2
        assert history_limit_payload["total_returned"] == len(history_limit_payload["items"])
        assert len(history_limit_payload["items"]) <= 2

        x_history = client.get("/posts/dry-run-history?platform=x&limit=20", headers=headers)
        assert x_history.status_code == 200
        x_history_payload = x_history.json()
        assert x_history_payload["platform"] == "x"
        for item in x_history_payload["items"]:
            assert any(result["platform"] == "x" for result in item["results"])

        clear_x = client.delete("/posts/dry-run-history?platform=x", headers=headers)
        assert clear_x.status_code == 200
        clear_x_payload = clear_x.json()
        assert clear_x_payload["platform"] == "x"
        assert clear_x_payload["cleared_count"] >= 1

        x_history_after_clear = client.get("/posts/dry-run-history?platform=x&limit=20", headers=headers)
        assert x_history_after_clear.status_code == 200
        assert x_history_after_clear.json()["total_returned"] == 0
