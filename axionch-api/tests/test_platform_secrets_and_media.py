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


def test_motion_track_extended_contract_fields() -> None:
    original_api_key = settings.api_key
    settings.api_key = "motion-track-contract-key"

    try:
        with TestClient(app) as client:
            headers = _master_headers("motion-track-contract-key")
            response = client.post(
                "/media/video/motion-track",
                headers=headers,
                json={
                    "source_path": "C:/does/not/exist.mp4",
                    "sample_every_n_frames": 2,
                    "roi_x": 10,
                    "roi_y": 10,
                    "roi_width": 200,
                    "roi_height": 180,
                    "smoothing_window": 5,
                    "min_confidence": 0.08,
                    "output_overlay_name": "track_overlay_contract.mp4",
                },
            )
            assert response.status_code == 200
            payload = response.json()
            assert payload["success"] is False
            assert "average_confidence" in payload
            assert "overlay_path" in payload
            assert isinstance(payload["track_points"], list)
    finally:
        settings.api_key = original_api_key


def test_image_heal_extended_contract_fields() -> None:
    original_api_key = settings.api_key
    settings.api_key = "image-heal-contract-key"

    try:
        with TestClient(app) as client:
            headers = _master_headers("image-heal-contract-key")
            response = client.post(
                "/media/image/heal",
                headers=headers,
                json={
                    "source_path": "C:/does/not/exist.png",
                    "method": "telea",
                    "fill_strategy": "hybrid",
                    "edge_blend": 0.6,
                    "denoise_strength": 2.0,
                },
            )
            assert response.status_code == 200
            payload = response.json()
            assert payload["success"] is False
            assert "message" in payload
    finally:
        settings.api_key = original_api_key


def test_creator_templates_include_reels_shorts_tiktok() -> None:
    original_api_key = settings.api_key
    settings.api_key = "creator-templates-key"

    try:
        with TestClient(app) as client:
            headers = _master_headers("creator-templates-key")
            response = client.get("/media/templates", headers=headers)
            assert response.status_code == 200
            payload = response.json()
            templates = payload.get("templates") or []
            ids = {item.get("id") for item in templates}
            assert "reels-1080x1920" in ids
            assert "shorts-1080x1920" in ids
            assert "tiktok-1080x1920" in ids

            reels = client.get("/media/templates?platform=instagram&media_type=video", headers=headers)
            assert reels.status_code == 200
            reels_payload = reels.json()
            reels_ids = {item.get("id") for item in reels_payload.get("templates") or []}
            assert "reels-1080x1920" in reels_ids
    finally:
        settings.api_key = original_api_key


def test_export_pipeline_presets_queue_and_job_status_contract() -> None:
    original_api_key = settings.api_key
    settings.api_key = "export-pipeline-key"

    try:
        with TestClient(app) as client:
            headers = _master_headers("export-pipeline-key")

            presets_response = client.get("/media/exports/presets", headers=headers)
            assert presets_response.status_code == 200
            presets_payload = presets_response.json()
            assert isinstance(presets_payload.get("presets"), list)
            assert len(presets_payload["presets"]) >= 1
            preset_ids = {item["id"] for item in presets_payload["presets"]}
            assert "reels-h264-1080x1920" in preset_ids

            queue_response = client.post(
                "/media/exports/queue",
                headers=headers,
                json={
                    "source_paths": ["C:/does/not/exist.mp4"],
                    "source_urls": [],
                    "preset_id": "reels-h264-1080x1920",
                    "output_prefix": "contract_export",
                    "hardware_acceleration": True,
                },
            )
            assert queue_response.status_code == 200
            queue_payload = queue_response.json()
            assert queue_payload["queued_items"] == 1
            assert queue_payload["job_id"]

            status_response = client.get(f"/media/exports/jobs/{queue_payload['job_id']}", headers=headers)
            assert status_response.status_code == 200
            status_payload = status_response.json()
            assert status_payload["job_id"] == queue_payload["job_id"]
            assert status_payload["preset_id"] == "reels-h264-1080x1920"
            assert status_payload["queued_items"] == 1
            assert isinstance(status_payload.get("items"), list)
            assert len(status_payload["items"]) == 1
    finally:
        settings.api_key = original_api_key


def test_asset_workflow_stock_audio_overlay_with_license_metadata() -> None:
    original_api_key = settings.api_key
    original_media_output_dir = settings.media_output_dir
    settings.api_key = "asset-workflow-key"
    temp_dir = tempfile.TemporaryDirectory()
    settings.media_output_dir = temp_dir.name

    try:
        with TestClient(app) as client:
            headers = _master_headers("asset-workflow-key")

            audio_response = client.post(
                "/media/assets",
                headers=headers,
                json={
                    "kind": "audio",
                    "title": "LoFi Chill Bed",
                    "source": "creator-marketplace",
                    "license_name": "Royalty-Free Creator License",
                    "license_url": "https://example.com/licenses/creator-audio",
                    "attribution_required": True,
                    "attribution_text": "Music by Creator Marketplace",
                    "tags": ["lofi", "chill", "bed"],
                    "local_path": "C:/media/stock/lofi_chill.wav",
                },
            )
            assert audio_response.status_code == 200
            audio_asset = audio_response.json()["asset"]
            assert audio_asset["kind"] == "audio"
            assert audio_asset["license_name"] == "Royalty-Free Creator License"
            assert audio_asset["attribution_required"] is True
            assert "lofi" in audio_asset["tags"]

            overlay_response = client.post(
                "/media/assets",
                headers=headers,
                json={
                    "kind": "overlay",
                    "title": "Neon Frame Overlay",
                    "source": "brand-pack",
                    "license_name": "Internal Brand License",
                    "attribution_required": False,
                    "tags": ["branding", "frame"],
                    "remote_url": "https://cdn.example.com/overlays/neon_frame.png",
                },
            )
            assert overlay_response.status_code == 200
            overlay_asset = overlay_response.json()["asset"]
            assert overlay_asset["kind"] == "overlay"
            assert overlay_asset["remote_url"] == "https://cdn.example.com/overlays/neon_frame.png"

            audio_list = client.get("/media/assets?kind=audio", headers=headers)
            assert audio_list.status_code == 200
            audio_payload = audio_list.json()
            assert audio_payload["kind"] == "audio"
            assert audio_payload["total_returned"] >= 1
            assert any(item["id"] == audio_asset["id"] for item in audio_payload["items"])

            overlay_list = client.get("/media/assets?kind=overlay&tag=branding", headers=headers)
            assert overlay_list.status_code == 200
            overlay_payload = overlay_list.json()
            assert overlay_payload["kind"] == "overlay"
            assert overlay_payload["tag"] == "branding"
            assert any(item["id"] == overlay_asset["id"] for item in overlay_payload["items"])

            delete_response = client.delete(f"/media/assets/{overlay_asset['id']}", headers=headers)
            assert delete_response.status_code == 200
            delete_payload = delete_response.json()
            assert delete_payload["deleted"] is True

            overlay_after_delete = client.get("/media/assets?kind=overlay", headers=headers)
            assert overlay_after_delete.status_code == 200
            remaining_overlay_ids = {item["id"] for item in overlay_after_delete.json()["items"]}
            assert overlay_asset["id"] not in remaining_overlay_ids
    finally:
        temp_dir.cleanup()
        settings.api_key = original_api_key
        settings.media_output_dir = original_media_output_dir


def test_live_multicast_dry_run_start_list_stop_contract() -> None:
    original_api_key = settings.api_key
    settings.api_key = "live-multicast-dry-run-key"

    try:
        with TestClient(app) as client:
            headers = _master_headers("live-multicast-dry-run-key")

            start_response = client.post(
                "/media/live/multicast/start",
                headers=headers,
                json={
                    "session_name": "contract-dry-run",
                    "source_url": "https://example.com/live-source.mp4",
                    "dry_run": True,
                    "destinations": [
                        {
                            "platform": "x",
                            "label": "X",
                            "ingest_url": "rtmps://example.com:443/stream",
                            "stream_key": "secret-key",
                            "enabled": True,
                        },
                        {
                            "platform": "linkedin",
                            "label": "LinkedIn",
                            "ingest_url": "rtmp://example.org/app",
                            "stream_key": "linkedin-key",
                            "enabled": True,
                        },
                    ],
                },
            )
            assert start_response.status_code == 200
            started = start_response.json()
            assert started["dry_run"] is True
            assert started["status"] == "live_simulated"
            assert started["active_destinations"] == 2
            assert started["failed_destinations"] == 0
            assert started["session_id"]
            session_id = started["session_id"]

            list_response = client.get("/media/live/multicast/sessions", headers=headers)
            assert list_response.status_code == 200
            listing = list_response.json()
            assert listing["total_returned"] >= 1
            assert any(item["session_id"] == session_id for item in listing["sessions"])

            get_response = client.get(f"/media/live/multicast/sessions/{session_id}", headers=headers)
            assert get_response.status_code == 200
            fetched = get_response.json()
            assert fetched["session_id"] == session_id
            assert isinstance(fetched["destinations"], list)
            assert len(fetched["destinations"]) == 2
            assert fetched["destinations"][0]["ingest_url_masked"].endswith("/***")

            stop_response = client.post(f"/media/live/multicast/sessions/{session_id}/stop", headers=headers)
            assert stop_response.status_code == 200
            stopped = stop_response.json()
            assert stopped["session_id"] == session_id
            assert stopped["stopped"] is True
            assert stopped["status"] in {"stopped", "failed"}
    finally:
        settings.api_key = original_api_key


def test_live_multicast_validation_requires_destination() -> None:
    original_api_key = settings.api_key
    settings.api_key = "live-multicast-validation-key"

    try:
        with TestClient(app) as client:
            headers = _master_headers("live-multicast-validation-key")
            response = client.post(
                "/media/live/multicast/start",
                headers=headers,
                json={
                    "source_url": "https://example.com/live-source.mp4",
                    "dry_run": True,
                    "destinations": [],
                },
            )
            assert response.status_code == 400
            assert "at least one enabled destination" in response.json()["detail"].lower()
    finally:
        settings.api_key = original_api_key
