# AxionCH Key Rotation Playbook

## Scope
- `TOKEN_ENCRYPTION_KEY` (social OAuth token encryption at rest)
- `VAULT_ENCRYPTION_KEY` (vault credential encryption at rest)

## Preconditions
1. Put API in maintenance mode (pause publishing queue).
2. Take a verified database backup before any rotation.
3. Generate new keys (`>=32` chars, random, never reused).
4. Keep old key material available until post-rotation verification is complete.

## Commands (from `C:\AxionCH\axionch-api`)

### 0) Optional: quarantine undecryptable legacy vault rows first
```powershell
.venv\Scripts\python.exe .\scripts\vault_cleanup.py --audit-only
.venv\Scripts\python.exe .\scripts\vault_cleanup.py --apply
```

### 1) Encrypt any legacy plaintext social tokens
Dry run:
```powershell
.venv\Scripts\python.exe .\scripts\security_rotation.py migrate-plaintext-social-tokens
```
Apply:
```powershell
.venv\Scripts\python.exe .\scripts\security_rotation.py migrate-plaintext-social-tokens --apply
```

### 2) Rotate social token key (`TOKEN_ENCRYPTION_KEY`)
Dry run:
```powershell
.venv\Scripts\python.exe .\scripts\security_rotation.py rotate-token-encryption-key --old-key "<old-token-key>" --new-key "<new-token-key>"
```
Apply:
```powershell
.venv\Scripts\python.exe .\scripts\security_rotation.py rotate-token-encryption-key --old-key "<old-token-key>" --new-key "<new-token-key>" --apply
```

### 3) Rotate vault key (`VAULT_ENCRYPTION_KEY`)
Dry run:
```powershell
.venv\Scripts\python.exe .\scripts\security_rotation.py rotate-vault-master-key --old-key "<old-vault-key>" --new-key "<new-vault-key>"
```
Apply:
```powershell
.venv\Scripts\python.exe .\scripts\security_rotation.py rotate-vault-master-key --old-key "<old-vault-key>" --new-key "<new-vault-key>" --apply
```

If data may have been encrypted with multiple historical keys, pass repeated `--old-key` values:
```powershell
.venv\Scripts\python.exe .\scripts\security_rotation.py rotate-vault-master-key --old-key "<old-a>" --old-key "<old-b>" --new-key "<new>" --apply
```

### 4) Update runtime secrets
Set `.env`/secret manager to new keys:
- `TOKEN_ENCRYPTION_KEY=<new-token-key>`
- `VAULT_ENCRYPTION_KEY=<new-vault-key>`

Restart API.

## Post-rotation verification
1. `GET /health` returns healthy.
2. `GET /config/security` reports production-ready checks (or expected warnings only).
3. `GET /config/status?user_email=<test-user>` shows platform secrets resolved from vault when configured.
4. OAuth refresh worker runs without decrypt failures in logs.
5. Test publish dry-run/live with one account per platform.

## Rollback
1. Revert env keys to previous values.
2. Restore DB backup taken pre-rotation.
3. Restart API and re-run verification.
