# AxionCH Incident Recovery Runbook

## Trigger Conditions
- Suspected secret exposure (`TOKEN_ENCRYPTION_KEY`, `VAULT_ENCRYPTION_KEY`, `API_KEY`).
- Unexpected auth bypass behavior.
- Suspicious media URL activity (possible SSRF probing).
- Repeated decrypt failures after deployment.

## Immediate Containment (first 15 minutes)
1. Pause publish queue/worker traffic.
2. Restrict inbound access at gateway/WAF to trusted operators only.
3. Rotate `API_KEY` immediately (or disable global key and require per-user keys).
4. Capture immutable snapshots:
   - API logs
   - recent deploy metadata
   - DB backup

## Credential and Key Response
1. Run plaintext-token migration (if needed):
```powershell
.venv\Scripts\python.exe .\scripts\security_rotation.py migrate-plaintext-social-tokens --apply
```
2. Rotate token encryption key:
```powershell
.venv\Scripts\python.exe .\scripts\security_rotation.py rotate-token-encryption-key --old-key "<old>" --new-key "<new>" --apply
```
3. Rotate vault master key:
```powershell
.venv\Scripts\python.exe .\scripts\security_rotation.py rotate-vault-master-key --old-key "<old>" --new-key "<new>" --apply
```
If multiple historical keys were used, pass multiple `--old-key` flags.
4. Update `.env` / secret manager and restart API.

## CORS/Auth Safety Checks
1. Ensure `RUNTIME_ENVIRONMENT=production`.
2. Ensure `CORS_ALLOWED_ORIGINS` contains only deployed frontend domains (no wildcard).
3. Ensure:
   - `ENFORCE_API_AUTH=true`
   - `ALLOW_BOOTSTRAP_WITHOUT_API_KEY=false`

## Validation Gates
1. Run smoke tests locally/CI:
```powershell
.venv\Scripts\python.exe -m pytest -q tests/test_security_smoke_ci.py
```
2. Confirm endpoint behavior:
   - unauthenticated `/accounts` returns `401`
   - authenticated requests pass
   - media localhost SSRF inputs are blocked

## Recovery Completion Criteria
- No decrypt errors in logs for 30 minutes under normal traffic.
- Successful dry-run checks for all configured platforms.
- CI smoke tests pass on main branch.
- Incident timeline + root-cause notes documented.

## After-Action
1. Revoke and re-issue impacted OAuth app credentials with providers.
2. Rotate any user/API credentials potentially exposed.
3. Add new detection rules for the observed failure pattern.
