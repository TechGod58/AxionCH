from uuid import uuid4

from fastapi.testclient import TestClient

from app.core.config import settings
from app.db.models import VaultEntry
from app.db.session import SessionLocal
from app.main import app


def _master_headers(api_key: str) -> dict[str, str]:
    return {"X-Axionch-Api-Key": api_key}


def test_vault_crud_and_encryption_at_rest() -> None:
    original_api_key = settings.api_key
    original_vault_key = settings.vault_encryption_key
    settings.api_key = "vault-master-key"
    settings.vault_encryption_key = f"vault-test-key-{uuid4().hex}{uuid4().hex}"

    user_email = f"vault-{uuid4().hex[:10]}@example.com"
    secret_password = "Sup3rSecret!123"

    try:
        with TestClient(app) as client:
            headers = _master_headers("vault-master-key")

            create_resp = client.post(
                "/vault",
                headers=headers,
                json={
                    "user_email": user_email,
                    "service_name": "github",
                    "username": "techgod",
                    "password": secret_password,
                    "notes": "primary account",
                },
            )
            assert create_resp.status_code == 200
            created = create_resp.json()
            entry_id = int(created["id"])
            assert created["password"] == secret_password

            list_resp = client.get(f"/vault?user_email={user_email}", headers=headers)
            assert list_resp.status_code == 200
            listing = list_resp.json()
            assert listing["total_returned"] >= 1
            assert any(item["id"] == entry_id for item in listing["entries"])

            detail_resp = client.get(f"/vault/{entry_id}?user_email={user_email}", headers=headers)
            assert detail_resp.status_code == 200
            detail = detail_resp.json()
            assert detail["username"] == "techgod"
            assert detail["password"] == secret_password

            db = SessionLocal()
            try:
                row = db.get(VaultEntry, entry_id)
                assert row is not None
                assert row.password_encrypted != secret_password
                assert "Sup3rSecret" not in row.password_encrypted
                assert row.username_encrypted != "techgod"
            finally:
                db.close()

            patch_resp = client.patch(
                f"/vault/{entry_id}?user_email={user_email}",
                headers=headers,
                json={"password": "N3wSecret!456"},
            )
            assert patch_resp.status_code == 200
            assert patch_resp.json()["password"] == "N3wSecret!456"

            delete_resp = client.delete(f"/vault/{entry_id}?user_email={user_email}", headers=headers)
            assert delete_resp.status_code == 200
            assert delete_resp.json()["deleted"] is True

            not_found_resp = client.get(f"/vault/{entry_id}?user_email={user_email}", headers=headers)
            assert not_found_resp.status_code == 404
    finally:
        settings.api_key = original_api_key
        settings.vault_encryption_key = original_vault_key


def test_vault_scoped_auth_blocks_cross_user_access() -> None:
    original_api_key = settings.api_key
    original_vault_key = settings.vault_encryption_key
    settings.api_key = "vault-scope-master-key"
    settings.vault_encryption_key = f"vault-scope-key-{uuid4().hex}{uuid4().hex}"

    user_a = f"vault-a-{uuid4().hex[:8]}@example.com"
    user_b = f"vault-b-{uuid4().hex[:8]}@example.com"

    try:
        with TestClient(app) as client:
            master = _master_headers("vault-scope-master-key")
            key_a = client.post("/auth/keys", headers=master, json={"user_email": user_a, "label": "vault-a"})
            assert key_a.status_code == 200
            raw_key_a = key_a.json()["raw_api_key"]
            headers_a = {
                "X-Axionch-Api-Key": raw_key_a,
                "X-Axionch-User-Email": user_a,
            }

            cross_create = client.post(
                "/vault",
                headers=headers_a,
                json={
                    "user_email": user_b,
                    "service_name": "x",
                    "username": "cross-user",
                    "password": "forbidden",
                },
            )
            assert cross_create.status_code == 403
    finally:
        settings.api_key = original_api_key
        settings.vault_encryption_key = original_vault_key


def test_vault_rejects_requests_when_key_is_weak() -> None:
    original_api_key = settings.api_key
    original_vault_key = settings.vault_encryption_key
    settings.api_key = "vault-weak-key-master"
    settings.vault_encryption_key = "replace-me-vault-key"

    user_email = f"vault-weak-{uuid4().hex[:10]}@example.com"

    try:
        with TestClient(app) as client:
            headers = _master_headers("vault-weak-key-master")

            status_resp = client.get("/vault/status", headers=headers)
            assert status_resp.status_code == 200
            assert status_resp.json()["ready"] is False

            create_resp = client.post(
                "/vault",
                headers=headers,
                json={
                    "user_email": user_email,
                    "service_name": "github",
                    "username": "techgod",
                    "password": "weak-key-test",
                },
            )
            assert create_resp.status_code == 503
            assert "VAULT_ENCRYPTION_KEY" in create_resp.json()["detail"]
    finally:
        settings.api_key = original_api_key
        settings.vault_encryption_key = original_vault_key
