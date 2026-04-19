# Vault Cleanup And Rotation

## Problem this solves
Legacy vault rows can become undecryptable when historical keys are unknown or mixed.  
This flow safely quarantines undecryptable rows, then allows clean key rotation for active rows.

## Commands (from `C:\AxionCH\axionch-api`)

### 1) Audit only
```powershell
.venv\Scripts\python.exe .\scripts\vault_cleanup.py --audit-only
```

### 2) Dry-run quarantine
```powershell
.venv\Scripts\python.exe .\scripts\vault_cleanup.py
```

### 3) Apply quarantine (with auto sqlite backup)
```powershell
.venv\Scripts\python.exe .\scripts\vault_cleanup.py --apply
```

### 4) Rotate vault key with historical old keys
```powershell
.venv\Scripts\python.exe .\scripts\security_rotation.py rotate-vault-master-key `
  --old-key "<old-key-a>" `
  --old-key "<old-key-b>" `
  --new-key "<new-key>" `
  --apply
```

### 5) Confirm clean decryptability
```powershell
.venv\Scripts\python.exe .\scripts\vault_cleanup.py --audit-only
```

Expected clean state:
- `Undecryptable fields: 0`
- `Undecryptable rows: 0`

## Data retention
Quarantined rows are preserved in DB table:
- `vault_entry_quarantine`

Original rows are removed from:
- `vault_entries`

For sqlite deployments, backup copy is automatically saved under:
- `C:\AxionCH\axionch-api\backups\`
