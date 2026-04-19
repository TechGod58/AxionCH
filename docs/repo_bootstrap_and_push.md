# Repo Bootstrap And Push Guide

## Goal
Initialize `C:\AxionCH` as a git repository, connect `origin`, run pre-push security checks, and push `main`.

## 1) Initialize and connect remote
From PowerShell:

```powershell
cd C:\AxionCH
.\scripts\init-repo-and-push.ps1 -RemoteUrl "https://github.com/<org-or-user>/<repo>.git"
```

This will:
- create git repo if missing
- create/switch to `main`
- add/update `origin`
- create initial commit if no commit exists yet

## 2) Run security gate before push
```powershell
cd C:\AxionCH
.\scripts\prepush-security-check.ps1 -RunFullTests
```

## 3) Push
```powershell
cd C:\AxionCH
.\scripts\init-repo-and-push.ps1 -Push
```

Or direct git:
```powershell
git push -u origin main
```

## 4) PR behavior
CI is configured to run:
- security smoke tests
- backend tests
- android compile

Workflow: `C:\AxionCH\.github\workflows\ci.yml`
