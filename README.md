# AxionCH Stronger Starter

This package is a stronger starter for the **AxionCH** creator app:

## Legal Notice
This repository is proprietary software and is **not** licensed for public or third-party use.
See:

- `C:\AxionCH\LICENSE`
- `C:\AxionCH\NOTICE`
- `C:\AxionCH\docs\legal_terms.md`

- **axionch-api**: FastAPI backend scaffold with:
  - settings
  - DB session
  - SQLAlchemy models
  - Pydantic schemas
  - account routes
  - post routes
  - mock publish adapters
  - Docker Compose for PostgreSQL

- **axionch-android-template**: Android source pack with:
  - Jetpack Compose app shell
  - navigation
  - dashboard/accounts/composer/results screens
  - repository + ViewModel skeletons backed by shared API client/DTOs

- **axionch-shared**: Shared Kotlin module with:
  - common DTO models
  - common network client logic (OkHttp + Gson)
  - reused by Android and Desktop apps

- **axionch-desktop**: legacy desktop scaffold (kept for reference)
- **C:\AxionCHPC**: canonical Windows desktop app source of truth

## Important
The Android folder is a **template source pack**.
The current canonical Android source of truth is:

`C:\AxionCH\axionch-api\app\src\main\java\com\axionch\app`

The template at `axionch-android-template` is synchronized from that source.

Desktop source-of-truth is now:

`C:\AxionCHPC`

The `C:\AxionCH\axionch-desktop` project is legacy and no longer the release target.

Create a fresh Android Studio project with:

- **Project name**: AxionCH
- **Package name**: `com.axionch.app`
- **Template**: Empty Compose Activity
- **Minimum SDK**: 26

Then replace the generated `app/src/main/java/com/axionch/app/` package contents with the files from:

`axionch-android-template/app/src/main/java/com/axionch/app/`

You can also copy the Gradle snippets from the Android template docs.

## Suggested local repo path
You said your repo path is:

`C:\AxionCH`

Extract this starter there if you want the layout:

`C:\AxionCH\axionch-api`
`C:\AxionCH\axionch-android-template`
`C:\AxionCH\axionch-shared`
`C:\AxionCHPC`

## Backend quick start

Working directory:

`C:\AxionCH\axionch-api`

Create venv and install:

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

Create `.env` from `.env.example`, then start Postgres:

```bash
docker compose up -d
```

Set platform API credentials in `.env` as needed:

- `X_API_KEY`, `X_API_SECRET`, `X_ACCESS_TOKEN`, `X_ACCESS_TOKEN_SECRET`
- `LINKEDIN_CLIENT_ID`, `LINKEDIN_CLIENT_SECRET`, `LINKEDIN_ACCESS_TOKEN`, `LINKEDIN_AUTHOR_URN`
- `INSTAGRAM_APP_ID`, `INSTAGRAM_APP_SECRET`, `INSTAGRAM_ACCESS_TOKEN`, `INSTAGRAM_BUSINESS_ACCOUNT_ID`
- Optional API auth key: `API_KEY` (when set, all non-health endpoints require `X-Axionch-Api-Key`)
- Optional per-user API key hash salt: `API_KEY_HASH_SALT`
- Auth policy controls: `ENFORCE_API_AUTH`, `ALLOW_BOOTSTRAP_WITHOUT_API_KEY`
- CORS controls: `CORS_ALLOWED_ORIGINS`, `CORS_ALLOW_CREDENTIALS`
- Runtime mode: `RUNTIME_ENVIRONMENT` (`development` or `production`; production blocks wildcard/local CORS origins at startup)
- Vault encryption key (required for secure credential vault storage): `VAULT_ENCRYPTION_KEY` (vault endpoints return `503` until a strong key is set)
- OAuth refresh worker controls: `OAUTH_REFRESH_ENABLED`, `OAUTH_REFRESH_INTERVAL_SECONDS`, `OAUTH_REFRESH_AHEAD_SECONDS`, `OAUTH_REFRESH_FAILURE_BACKOFF_SECONDS`
- OAuth redirect URIs: `X_REDIRECT_URI`, `LINKEDIN_REDIRECT_URI`, `INSTAGRAM_REDIRECT_URI`
- OAuth token endpoints (override if needed): `X_OAUTH_TOKEN_URL`, `LINKEDIN_OAUTH_TOKEN_URL`, `INSTAGRAM_OAUTH_TOKEN_URL`, `INSTAGRAM_OAUTH_REFRESH_URL`
- Platform secret behavior: `PLATFORM_SECRETS_ENV_FALLBACK` (set `false` after vault migration to stop plaintext env fallback)
- Video filter processing: `FFMPEG_BINARY`, `MEDIA_OUTPUT_DIR`
- Photo filter processing: `IMAGE_OUTPUT_DIR`
- Media safety controls: `MEDIA_MAX_DOWNLOAD_BYTES`, `MEDIA_ALLOW_HTTP_SOURCE_URLS`, `MEDIA_ALLOWED_SOURCE_DOMAINS`, `MEDIA_BLOCKED_SOURCE_HOSTS`, `MEDIA_BLOCK_PRIVATE_NETWORKS`, `MEDIA_ALLOWED_VIDEO_MIME_PREFIXES`, `MEDIA_ALLOWED_IMAGE_MIME_PREFIXES`, `FFMPEG_TIMEOUT_SECONDS`

To migrate existing env-based platform secrets into the encrypted vault per user:

- `POST /config/platform-secrets/migrate-env` with body `{ "user_email": "...", "overwrite_existing": true }`
- Or write platform secrets directly with `PUT /config/platform-secrets/{platform}`

To rotate encrypted-at-rest materials, use:

- `python scripts/security_rotation.py migrate-plaintext-social-tokens --apply`
- `python scripts/security_rotation.py rotate-token-encryption-key --old-key "<old>" --new-key "<new>" --apply`
- `python scripts/security_rotation.py rotate-vault-master-key --old-key "<old>" --new-key "<new>" --apply`
- repeat `--old-key` when you need multiple historical key candidates

To quarantine undecryptable legacy vault rows before key rotation:

- `python scripts/vault_cleanup.py --audit-only`
- `python scripts/vault_cleanup.py --apply`

Production template with strict CORS origins:

- `C:\AxionCH\axionch-api\.env.production.example`
- `C:\AxionCH\axionch-api\.env.production` (working production config snapshot; keep secrets in secret manager)
- `C:\AxionCH\docs\repo_bootstrap_and_push.md`
- `C:\AxionCH\docs\vault_cleanup_and_rotation.md`

Run backend:

```bash
uvicorn app.main:app --reload
```

Health check:

`http://127.0.0.1:8010/health`

Config status:

`http://127.0.0.1:8010/config/status`

Credential check (no live post):

`http://127.0.0.1:8010/config/check`

Config security status:

`http://127.0.0.1:8010/config/security`

Dry-run publish:

`http://127.0.0.1:8010/posts/dry-run`

Dry-run history:

`http://127.0.0.1:8010/posts/dry-run-history`

Dry-run history with filter:

`http://127.0.0.1:8010/posts/dry-run-history?limit=50&platform=x`

Queue live publish:

`http://127.0.0.1:8010/posts/queue`

Queue job status:

`http://127.0.0.1:8010/posts/jobs/{job_id}`

Queue metrics:

`http://127.0.0.1:8010/posts/metrics`

Dead letters:

`http://127.0.0.1:8010/posts/dead-letters?limit=50`

Auth keys:

`http://127.0.0.1:8010/auth/keys`

Newly created per-user API keys are also mirrored into the encrypted vault service (`service_name = axionch-api-keys`) when vault crypto is configured.

Password vault entries:

`http://127.0.0.1:8010/vault`

Vault crypto readiness/status:

`http://127.0.0.1:8010/vault/status`

Video filter presets:

`http://127.0.0.1:8010/media/filters`

Apply video filter:

`http://127.0.0.1:8010/media/filters/apply`

Photo filter presets:

`http://127.0.0.1:8010/media/image-filters`

Apply photo filter:

`http://127.0.0.1:8010/media/image-filters/apply`

OAuth start:

`http://127.0.0.1:8010/oauth/linkedin/start?user_email=you@example.com`

OAuth callback (real code exchange + token persistence):

`http://127.0.0.1:8010/oauth/linkedin/callback?code=AUTH_CODE&state=STATE_VALUE`

## First milestone
After booting backend and Android app, the first useful target is:

1. app loads dashboard
2. in Dashboard, set **Client Config** (`Base URL`, `User Email`, optional `API Key`) and press **Save Client Config**
3. app calls backend health
4. app creates a draft post
5. backend returns a mock publish result

That gives you a real end-to-end vertical slice quickly.

## Desktop quick start
Working directory:

`C:\AxionCHPC`

Run:

```bash
.\gradlew.bat run
```

The desktop app points to:

`http://127.0.0.1:8010/`

Desktop now includes:

- separate live publish result and dry-run preview cards
- dry-run history list + clear action
- credential-check action and per-platform last check diagnostics
- dry-run platform filtering (All, X, LinkedIn, Instagram)
- supports API auth header via `AXIONCH_API_KEY` env var
- supports user identity header via `AXIONCH_USER_EMAIL` env var (for per-user key auth flow)
- includes dead-letter payload drilldown panel for queue diagnostics
- includes dead-letter requeue action directly from the drilldown panel
- includes creator-focused video filter controls: sepia, B&W, saturation/brightness, overlays, text, optional sound track and volume (backend-powered via `/media/filters`)
- includes creator-focused photo filter controls: sepia, B&W, cartoonify, caricature style, saturation/brightness/contrast, overlays, and text (backend-powered via `/media/image-filters`)
- includes Android Capture Studio flow: capture photo/video from device camera in-app and auto-apply creator presets after capture
- includes desktop Capture Studio panel: DirectShow webcam/mic capture with FFmpeg live effects during recording

## Backend tests
From:

`C:\AxionCH\axionch-api`

Install dev deps:

```bash
pip install -r requirements-dev.txt
```

Run:

```bash
python -m pytest tests
```

## Assessment checklist
Latest checklist and remaining batch plan:

`C:\AxionCH\docs\assessment_checklist.md`

## CI
GitHub Actions workflow added:

`.github/workflows/ci.yml`

Desktop Windows installer pipeline and Store submission pack automation now live in:

`C:\AxionCHPC\.github\workflows\windows-installers.yml`
