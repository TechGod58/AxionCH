# Immediate next step

## 1. Extract starter to your repo root
Target root:

`C:\AxionCH`

## 2. Start backend first
From:

`C:\AxionCH\axionch-api`

Run:

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
docker compose up -d
uvicorn app.main:app --reload
```

## 3. Verify backend
Open:

- `http://127.0.0.1:8010/health`
- `http://127.0.0.1:8010/docs`

## 4. Create Android Studio project
Use:

- Name: `AxionCH`
- Package: `com.axionch.app`
- Minimum SDK: `26`

## 5. Replace Android source package
Canonical source lives at:

`axionch-api/app/src/main/java/com/axionch/app/`

Template mirror:

`axionch-android-template/app/src/main/java/com/axionch/app/`

Copy either one into the matching source directory in the Android Studio project.

## 6. Desktop source of truth
Use:

`C:\AxionCHPC`

Legacy desktop project under `C:\AxionCH\axionch-desktop` is reference-only.

## 7. First test
- launch backend
- launch emulator
- start app
- open Accounts
- tap **Create Mock Accounts**
- open Compose
- tap **Load Account IDs**
- tap **Publish**
- open Results

If that flow works, you have a working baseline.

## 8. Production security baseline
- Use `C:\AxionCH\axionch-api\.env.production.example` as the production template.
- Keep `RUNTIME_ENVIRONMENT=production` and set deployed-only `CORS_ALLOWED_ORIGINS`.
- Run rotation/util scripts before cutover:
  - `scripts/security_rotation.py migrate-plaintext-social-tokens --apply`
  - `scripts/security_rotation.py rotate-token-encryption-key ... --apply`
  - `scripts/security_rotation.py rotate-vault-master-key ... --apply`
