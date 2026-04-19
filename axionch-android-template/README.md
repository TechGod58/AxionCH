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
