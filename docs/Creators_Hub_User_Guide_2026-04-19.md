# Creator's Hub User Guide (Android + Windows)

Date: 2026-04-19

This guide covers your current Creator's Hub stack:
- Android app source pack: `C:\AxionCH\axionch-android-template`
- Windows desktop app (canonical): `C:\AxionCHPC`
- Backend API: `C:\AxionCH\axionch-api`

## 1) Quick Start

### Backend (required)
1. Open PowerShell in `C:\AxionCH\axionch-api`.
2. Activate venv: `.venv\Scripts\activate`
3. Start API: `uvicorn app.main:app --host 127.0.0.1 --port 8010 --reload`
4. Confirm health: open `http://127.0.0.1:8010/health`

### Desktop app (Windows)
1. Open PowerShell in `C:\AxionCHPC`.
2. Run: `./gradlew.bat run`
3. In app, open **Client Config** and set:
   - Base URL: `http://127.0.0.1:8010/`
   - User Email: your creator/operator email
   - API Key: optional (required if backend auth is enforced)
4. Click **Save Client Config**.

### Android app
1. Build/install from Android Studio using the template pack.
2. On Dashboard, set Base URL/User Email/API Key.
3. Save config and run health/config checks.

## 2) Core Workflow (Both Platforms)

### Step A: Accounts
1. Add or load social accounts.
2. For testing, use mock account creation where available.

### Step B: Compose
1. Write post body.
2. Optionally add image URL.
3. Select destination accounts.
4. Run **Dry Run** first.

### Step C: Review Results
1. Open Results and confirm platform-by-platform outcomes.
2. Check Dry Run history before live publish.

### Step D: Publish Live
1. Press **Publish** only after dry-run success.
2. If queue/dead-letter entries appear, open drilldown and requeue.

## 3) Security + Vault

### Password Vault
Use Vault to store:
- service usernames/passwords
- API keys
- notes per service

Recommended usage:
1. Save platform credentials in Vault (not plaintext docs).
2. Use strong unique credentials.
3. Rotate secrets if exposed.

## 4) Windows Desktop (C:\AxionCHPC)

### Client Config + Skin
- Base URL / User Email / API Key
- Skin colors: Blue, Red, Green, Pink, Purple, Gold

### Creator Studio
- **Video Editor**: preset-based filters, text/overlay, audio bed, brightness/saturation controls.
- **Photo Editor**: sepia, B/W, cartoonify, caricature, brightness/saturation/contrast, overlay/text.
- **Preview Window profiles**: Desktop, Phone Portrait, Phone Landscape.

### Capture Studio (desktop)
- Detect camera/mic/speaker devices.
- Start mic and speaker meters.
- Select popular live effects (clean pop, cinematic, vintage, mono, sepia, soft glam).
- Capture photo/video with live effects.
- Save and load AV profiles (camera + mic + speaker + levels).

### Advanced Media Studio panel
- Timeline compose (clips, split points, transitions, keyframes)
- Motion tracking
- Caption transcription + sample SRT build
- Image healing/object remove
- Asset workflow with licensing metadata

### Operational panels
- Config status + credential checks
- Dry-run history filter/clear
- Dead-letter drilldown with payload copy/prettify and requeue

## 5) Android App

Navigation:
- Dashboard
- Accounts
- Composer
- Results
- Dry Run History
- Vault
- Media Studio
- Realtime Capture

### Media Studio tabs
- Video
- Timeline
- Captions
- Pro
- Live
- Assets

### Realtime Capture (Android)
- CameraX + GPU live filter graph
- Record while filtered
- Front/back camera switch
- Mic on/off
- Save output to MediaStore (Movies/CreatorsHub)

### Live tab (Android)
- Configure session name/source
- Enter ingest URL + stream key per platform
- Start/stop multicast session
- Refresh and inspect live session statuses

## 6) Live Multicast (Important)

Multicast means one source stream is sent to multiple platforms at once.

Inputs required per destination:
- Ingest URL (RTMP/RTMPS)
- Stream key

General steps:
1. In each platform's live producer, create/get stream endpoint.
2. Copy ingest URL + stream key.
3. Paste into Creator's Hub live controls.
4. Start in dry-run mode first where supported.
5. Start live when preview/status are good.

## 7) Recommended Operating Order

1. Start backend
2. Save client config
3. Run health/config checks
4. Verify account list
5. Dry-run publish
6. Optional media editing/capture
7. Live publish or live multicast
8. Monitor dead-letter/metrics and requeue if needed

## 8) Current Gap: Useful Live Feature Still Missing

Most useful missing item right now:
- **Desktop live multicast UI in C:\AxionCHPC**

Android has a live multicast tab, but the canonical desktop app does not yet expose matching controls. Backend support exists, so adding desktop UI parity is straightforward and high impact.

Additional high-value live improvements:
1. Auto-reconnect and failover destination handling
2. In-app live chat aggregation (X/LinkedIn/Instagram where API allows)
3. Stream health telemetry (bitrate/fps/drop frames) panel
4. Preflight validator (ingest/key checks + network readiness)
5. Scheduled go-live presets per platform

## 9) Troubleshooting

- Health fails:
  - Check backend running on `127.0.0.1:8010`
  - Verify API key and user email headers/config

- Media actions fail:
  - Verify FFmpeg is installed and reachable by backend/desktop capture pipeline
  - Confirm source path/URL exists and is allowed by media safety rules

- Live stream fails:
  - Re-copy ingest URL/key from platform live producer
  - Ensure key not expired/revoked
  - Try RTMPS endpoint when available

## 10) File Locations

- This guide (Markdown): `C:\AxionCH\docs\Creators_Hub_User_Guide_2026-04-19.md`
- This guide (PDF): `C:\AxionCH\docs\Creators_Hub_User_Guide_2026-04-19.pdf`
- Mirror PDF for desktop repo: `C:\AxionCHPC\docs\Creators_Hub_User_Guide_2026-04-19.pdf`
