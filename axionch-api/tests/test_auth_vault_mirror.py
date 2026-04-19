from uuid import uuid4

from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.config import settings
from app.db.models import ApiCredential, User
from app.db.session import SessionLocal
from app.main import app


def _master_headers(api_key: str) -> dict[str, str]:
    return {"X-Axionch-Api-Key": api_key}


def test_auth_key_creation_mirrors_raw_key_to_vault() -> None:
    original_api_key = settings.api_key
    original_vault_key = settings.vault_encryption_key
    settings.api_key = "auth-vault-master-key"
    settings.vault_encryption_key = f"auth-vault-key-{uuid4().hex}{uuid4().hex}"

    user_email = f"auth-vault-{uuid4().hex[:10]}@example.com"
    label = "desktop-key"

    try:
        with TestClient(app) as client:
            headers = _master_headers("auth-vault-master-key")

            create_resp = client.post(
                "/auth/keys",
                headers=headers,
                json={"user_email": user_email, "label": label},
            )
            assert create_resp.status_code == 200
            created = create_resp.json()
            raw_api_key = created["raw_api_key"]
            key_id = created["key_id"]

            list_resp = client.get(f"/vault?user_email={user_email}", headers=headers)
            assert list_resp.status_code == 200
            entries = list_resp.json()["entries"]
            vault_summary = next(
                item for item in entries
                if item["service_name"] == "axionch-api-keys" and item["username"] == key_id
            )

            detail_resp = client.get(f"/vault/{vault_summary['id']}?user_email={user_email}", headers=headers)
            assert detail_resp.status_code == 200
            detail = detail_resp.json()

            assert detail["password"] == raw_api_key
            assert label in (detail.get("notes") or "")

            db = SessionLocal()
            try:
                user = db.execute(select(User).where(User.email == user_email)).scalar_one_or_none()
                assert user is not None
                credential = db.execute(
                    select(ApiCredential).where(ApiCredential.user_id == user.id, ApiCredential.key_id == key_id)
                ).scalar_one_or_none()
                assert credential is not None
                assert credential.key_hash.startswith("$argon2")
            finally:
                db.close()
    finally:
        settings.api_key = original_api_key
        settings.vault_encryption_key = original_vault_key
