# AxionCH Android Template

This folder is a source pack, not a full generated Android Studio project.
The canonical Android source currently lives at:

`C:\AxionCH\axionch-api\app\src\main\java\com\axionch\app`

This template mirrors that source.

## Create the base project
Open Android Studio and create:

- **Name**: AxionCH
- **Package**: `com.axionch.app`
- **Minimum SDK**: 26
- **Template**: Empty Compose Activity

Then replace the generated package source files with the files in:

`app/src/main/java/com/axionch/app/`

## Shared module setup (required)
This Android source now uses the shared Kotlin module at:

`C:\AxionCH\axionch-shared`

If your Android project lives inside this repo, add this line to your `settings.gradle.kts`:

```kotlin
includeBuild("../axionch-shared")
```

## Add these dependencies to `app/build.gradle.kts`

```kotlin
implementation("androidx.navigation:navigation-compose:2.8.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
implementation("com.axionch:axionch-shared:0.1.0")
```

## Internet permission
Add this to `app/src/main/AndroidManifest.xml` above `<application>`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Local backend on Android emulator
At first launch, go to Dashboard and set **Client Config**:

- `Base URL`: `http://10.0.2.2:8010/`
- `User Email`: your AxionCH user email
- `API Key`: optional (required only when backend `API_KEY` auth is enabled)

`10.0.2.2` maps the Android emulator to your host machine.

## Password vault
The Android app includes a **Password Vault** screen reachable from Dashboard.
Vault entries are stored encrypted-at-rest on the backend (`/vault`) using `VAULT_ENCRYPTION_KEY`.

## Realtime capture stack (CameraX + filter graph)
The new `RealtimeCaptureScreen` uses CameraX preview + recording and live filter rendering.
Add these dependencies to your Android app module:

```kotlin
implementation("androidx.camera:camera-core:1.3.4")
implementation("androidx.camera:camera-camera2:1.3.4")
implementation("androidx.camera:camera-lifecycle:1.3.4")
implementation("androidx.camera:camera-video:1.3.4")
implementation("androidx.camera:camera-view:1.3.4")
implementation("androidx.camera:camera-media3-effect:1.5.0-beta01")
implementation("androidx.media3:media3-effect:1.6.1")
```

Add these permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

At runtime, request camera + microphone permission before entering `RealtimeCaptureScreen`.
`RealtimeCaptureScreen` now uses a live GPU filter graph (`Media3Effect`) applied to both preview and encoded video capture.
