from __future__ import annotations

from pathlib import Path
from uuid import uuid4

from fastapi.testclient import TestClient

from app.core.config import settings
from app.core.cors import parse_csv, validate_cors_for_runtime
from app.main import app
from app.services.media.safety import validate_local_source_path, validate_source_url


def test_api_auth_enforced_smoke() -> None:
    original_api_key = settings.api_key
    original_enforce_auth = settings.enforce_api_auth
    original_allow_bootstrap = settings.allow_bootstrap_without_api_key

    settings.api_key = "smoke-auth-key"
    settings.enforce_api_auth = True
    settings.allow_bootstrap_without_api_key = False

    try:
        with TestClient(app) as client:
            no_key = client.get("/accounts")
            assert no_key.status_code == 401

            with_key = client.get("/accounts", headers={"X-Axionch-Api-Key": "smoke-auth-key"})
            assert with_key.status_code == 200
    finally:
        settings.api_key = original_api_key
        settings.enforce_api_auth = original_enforce_auth
        settings.allow_bootstrap_without_api_key = original_allow_bootstrap


def test_production_cors_disallows_wildcard_smoke() -> None:
    try:
        validate_cors_for_runtime(["*"], runtime_environment="production")
        assert False, "Expected wildcard production CORS validation to fail"
    except RuntimeError as exc:
        assert "Wildcard CORS origins are not allowed" in str(exc)


def test_production_env_template_has_strict_cors_smoke() -> None:
    env_template = Path.cwd() / ".env.production.example"
    assert env_template.exists(), ".env.production.example is required for production config reviews"

    cors_value = ""
    for line in env_template.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        if stripped.startswith("CORS_ALLOWED_ORIGINS="):
            cors_value = stripped.split("=", 1)[1].strip()
            break

    origins = parse_csv(cors_value)
    validate_cors_for_runtime(origins, runtime_environment="production")
    assert "*" not in origins


def test_media_ssrf_localhost_block_smoke() -> None:
    original_allow_http = settings.media_allow_http_source_urls
    settings.media_allow_http_source_urls = True
    try:
        try:
            validate_source_url("http://127.0.0.1/internal.mp4")
            assert False, "Expected localhost source URL validation to fail"
        except ValueError as exc:
            assert "Blocked source host" in str(exc)
    finally:
        settings.media_allow_http_source_urls = original_allow_http


def test_media_file_size_limit_smoke() -> None:
    original_max_bytes = settings.media_max_download_bytes
    settings.media_max_download_bytes = 100

    temp_path = Path.cwd() / f"media-size-smoke-{uuid4().hex}.bin"
    temp_path.write_bytes(b"x" * 150)
    try:
        try:
            validate_local_source_path(str(temp_path), allowed_mime_prefixes=[])
            assert False, "Expected local media size validation to fail"
        except ValueError as exc:
            assert "MEDIA_MAX_DOWNLOAD_BYTES" in str(exc)
    finally:
        settings.media_max_download_bytes = original_max_bytes
        if temp_path.exists():
            temp_path.unlink()
