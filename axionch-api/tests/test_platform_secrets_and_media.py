from uuid import uuid4
from pathlib import Path
import tempfile

from fastapi.testclient import TestClient
from sqlalchemy import select
from PIL import Image

from app.core.config import settings
from app.db.models import Platform, User, VaultEntry
from app.db.session import SessionLocal
from app.main import app
from app.services.publish.base import PublishResult


def _master_headers(api_key: str) -> dict[str, str]:
    return {"X-Axionch-Api-Key": api_key}


def test_platform_secret_migration_updates_status_sources() -> None:
    original_api_key = settings.api_key
    original_vault_key = settings.vault_encryption_key
    original_x_api_key = settings.x_api_key
    original_x_api_secret = settings.x_api_secret
    original_x_access_token = settings.x_access_token
    original_x_access_token_secret = settings.x_access_token_secret

    settings.api_key = "platform-secret-migrate-key"
    settings.vault_encryption_key = f"vault-platform-migrate-{uuid4().hex}{uuid4().hex}"
    settings.x_api_key = f"x-api-{uuid4().hex[:8]}"
    settings.x_api_secret = f"x-secret-{uuid4().hex[:8]}"
    settings.x_access_token = f"x-access-{uuid4().hex[:8]}"
    settings.x_access_token_secret = f"x-access-secret-{uuid4().hex[:8]}"

    user_email = f"platform-migrate-{uuid4().hex[:10]}@example.com"

    try:
        with TestClient(app) as client:
            headers = _master_headers("platform-secret-migrate-key")

            migrate = client.post(
                "/config/platform-secrets/migrate-env",
                headers=headers,
                json={"user_email": user_email, "overwrite_existing": True},
            )
            assert migrate.status_code == 200
            payload = migrate.json()
            assert payload["user_email"] == user_email

            x_result = next(item for item in payload["results"] if item["platform"] == "x")
            assert set(x_result["updated_fields"]) >= {
                "X_API_KEY",
                "X_API_SECRET",
                "X_ACCESS_TOKEN",
                "X_ACCESS_TOKEN_SECRET",
            }

            status = client.get(f"/config/status?user_email={user_email}", headers=headers)
            assert status.status_code == 200
            status_payload = status.json()["x"]
            assert status_payload["configured"] is True
            assert status_payload["mode"] == "real"
            for field in ["X_API_KEY", "X_API_SECRET", "X_ACCESS_TOKEN", "X_ACCESS_TOKEN_SECRET"]:
                assert status_payload["source_by_field"][field] == "vault"

            db = SessionLocal()
            try:
                user = db.execute(select(User).where(User.email == user_email)).scalar_one_or_none()
                assert user is not None
                rows = db.execute(
                    select(VaultEntry).where(
                        VaultEntry.user_id == user.id,
                        VaultEntry.service_name.like("platform-secret:x:%"),
                    )
                ).scalars().all()
                assert len(rows) >= 4
            finally:
                db.close()
    finally:
        settings.api_key = original_api_key
        settings.vault_encryption_key = original_vault_key
        settings.x_api_key = original_x_api_key
        settings.x_api_secret = original_x_api_secret
        settings.x_access_token = original_x_access_token
        settings.x_access_token_secret = original_x_access_token_secret


def test_dry_run_passes_only_target_platform_secrets(monkeypatch) -> None:
    original_api_key = settings.api_key
    original_vault_key = settings.vault_encryption_key

    settings.api_key = "platform-scope-key"
    settings.vault_encryption_key = f"vault-platform-scope-{uuid4().hex}{uuid4().hex}"

    user_email = f"platform-scope-{uuid4().hex[:10]}@example.com"
    captured: dict[str, str | None] = {}

    try:
        with TestClient(app) as client:
            headers = _master_headers("platform-scope-key")

            write = client.put(
                "/config/platform-secrets/x",
                headers=headers,
                json={
                    "user_email": user_email,
                    "secrets": {
                        "X_API_KEY": "x_key",
                        "X_API_SECRET": "x_secret",
                        "X_ACCESS_TOKEN": "x_access",
                        "X_ACCESS_TOKEN_SECRET": "x_access_secret",
                    },
                    "overwrite_existing": True,
                },
            )
            assert write.status_code == 200

            account = client.post(
                "/accounts",
                headers=headers,
                json={
                    "user_email": user_email,
                    "platform": "x",
                    "handle": "@scope_x",
                    "access_token": "mock-token-x",
                },
            )
            assert account.status_code == 200
            account_id = int(account.json()["id"])

            from app.services.publish.registry import registry
            from app.services.publish.x_adapter import XAdapter

            def _fake_dry_run(self, body: str, image_url: str | None, secrets=None):
                captured.clear()
                captured.update(secrets or {})
                return PublishResult(platform=Platform.X, success=True, remote_post_id="dryrun-x")

            monkeypatch.setattr(XAdapter, "dry_run", _fake_dry_run)

            dry_run = client.post(
                "/posts/dry-run",
                headers=headers,
                json={
                    "user_email": user_email,
                    "body": "Scoped secret dry run test",
                    "image_url": "https://example.com/image.jpg",
                    "account_ids": [account_id],
                },
            )
            assert dry_run.status_code == 200
            assert dry_run.json()["status"] == "success"
            assert registry.get(Platform.X, prefer_mock=False)

            assert set(captured.keys()) == {
                "X_API_KEY",
                "X_API_SECRET",
                "X_ACCESS_TOKEN",
                "X_ACCESS_TOKEN_SECRET",
            }
            assert "LINKEDIN_ACCESS_TOKEN" not in captured
            assert "INSTAGRAM_ACCESS_TOKEN" not in captured
    finally:
        settings.api_key = original_api_key
        settings.vault_encryption_key = original_vault_key


def test_media_filters_contract_when_ffmpeg_missing() -> None:
    original_api_key = settings.api_key
    settings.api_key = "media-filters-key"

    try:
        with TestClient(app) as client:
            headers = _master_headers("media-filters-key")

            filters_response = client.get("/media/filters", headers=headers)
            assert filters_response.status_code == 200
            filters_payload = filters_response.json()
            assert isinstance(filters_payload.get("presets"), list)
            assert len(filters_payload["presets"]) >= 1

            apply_response = client.post(
                "/media/filters/apply",
                headers=headers,
                json={
                    "preset_id": "clean-pop",
                    "source_path": "C:/does/not/exist.mp4",
                    "sepia": True,
                    "black_white": True,
                    "saturation_factor": 1.2,
                    "brightness_delta": 0.1,
                    "text_overlay": "AxionCH",
                    "sound_volume": 0.9,
                },
            )
            assert apply_response.status_code == 200
            apply_payload = apply_response.json()
            assert apply_payload["preset_id"] == "clean-pop"
            assert apply_payload["success"] is False
            assert "ffmpeg" in apply_payload["message"].lower() or "source_path" in apply_payload["message"].lower()
    finally:
        settings.api_key = original_api_key


def test_image_filters_list_and_apply() -> None:
    original_api_key = settings.api_key
    settings.api_key = "image-filters-key"

    temp_dir = tempfile.TemporaryDirectory()
    source_path = Path(temp_dir.name) / "source.jpg"
    overlay_path = Path(temp_dir.name) / "overlay.png"
    Image.new("RGB", (64, 64), color=(120, 60, 220)).save(source_path, format="JPEG")
    Image.new("RGBA", (20, 20), color=(255, 255, 255, 180)).save(overlay_path, format="PNG")

    try:
        with TestClient(app) as client:
            headers = _master_headers("image-filters-key")

            list_response = client.get("/media/image-filters", headers=headers)
            assert list_response.status_code == 200
            list_payload = list_response.json()
            assert list_payload["pillow_available"] is True
            assert isinstance(list_payload.get("presets"), list)
            assert len(list_payload["presets"]) >= 1

            apply_response = client.post(
                "/media/image-filters/apply",
                headers=headers,
                json={
                    "preset_id": "clean-bright",
                    "source_path": str(source_path),
                    "sepia": True,
                    "cartoonify": True,
                    "caricature": True,
                    "saturation_factor": 1.2,
                    "brightness_factor": 1.05,
                    "contrast_factor": 1.1,
                    "overlay_path": str(overlay_path),
                    "text_overlay": "AxionCH",
                },
            )
            assert apply_response.status_code == 200
            apply_payload = apply_response.json()
            assert apply_payload["success"] is True
            assert apply_payload["preset_id"] == "clean-bright"
            output_path = apply_payload.get("output_path")
            assert output_path
            assert Path(output_path).exists()
    finally:
        temp_dir.cleanup()
        settings.api_key = original_api_key


def test_media_source_url_blocks_localhost_ssrf_target() -> None:
    original_api_key = settings.api_key
    original_allow_http = settings.media_allow_http_source_urls

    settings.api_key = "media-ssrf-key"
    settings.media_allow_http_source_urls = True

    try:
        with TestClient(app) as client:
            headers = _master_headers("media-ssrf-key")
            response = client.post(
                "/media/image-filters/apply",
                headers=headers,
                json={
                    "preset_id": "clean-bright",
                    "source_url": "http://127.0.0.1:8010/internal.jpg",
                },
            )
            assert response.status_code == 200
            payload = response.json()
            assert payload["success"] is False
            assert "blocked source host" in payload["message"].lower()
    finally:
        settings.api_key = original_api_key
        settings.media_allow_http_source_urls = original_allow_http
