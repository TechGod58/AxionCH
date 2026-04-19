# AxionCH Desktop (Compose Multiplatform)

This is a desktop scaffold for AxionCH built with Compose Multiplatform.
It consumes the shared Kotlin module at `C:\AxionCH\axionch-shared` via Gradle composite build.

## What it does
- Checks backend health
- Loads account IDs
- Creates mock accounts
- Publishes a post through the existing AxionCH API
- Runs credential checks and shows last-check diagnostics by platform
- Shows dry-run history with platform filter + filtered clear
- Shows dead-letter entries with payload drilldown + requeue action for failed queue jobs
- Supports payload JSON prettify + copy helpers in dead-letter drilldown

## Backend URL
Default API base URL:

`http://127.0.0.1:8010/`

Override with:

`AXIONCH_API_BASE_URL`

Optional API key header:

`AXIONCH_API_KEY`

Optional user identity header (used with per-user API keys):

`AXIONCH_USER_EMAIL`

Example:

```powershell
$env:AXIONCH_API_BASE_URL="http://127.0.0.1:8010/"
```

## Run
From:

`C:\AxionCH\axionch-desktop`

If Java is not on PATH, set `JAVA_HOME` first (example for Android Studio JBR on Windows):

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

```powershell
.\gradlew.bat run
```

## Windows Packaging
Windows native packaging is configured for `EXE` + `MSI` in `build.gradle.kts`.

Run:

```powershell
.\gradlew.bat packageReleaseDistributionForCurrentOS
```

Note: release packaging uses ProGuard; desktop-specific keep/dontwarn rules are in `proguard-rules.pro`.
