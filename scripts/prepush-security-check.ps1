param(
    [Parameter(Mandatory = $false)]
    [string]$RepoRoot = "C:\AxionCH",
    [Parameter(Mandatory = $false)]
    [switch]$RunFullTests
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$apiRoot = Join-Path $RepoRoot "axionch-api"
if (-not (Test-Path $apiRoot)) {
    throw "axionch-api not found under: $RepoRoot"
}

Push-Location $apiRoot
try {
    if (-not (Test-Path ".\.venv\Scripts\python.exe")) {
        throw "Expected Python venv missing at .\.venv\Scripts\python.exe"
    }

    $env:PYTHONPATH = "."
    Write-Host "Running security smoke tests..."
    .\.venv\Scripts\python.exe -m pytest -q tests/test_security_smoke_ci.py

    if ($RunFullTests) {
        Write-Host "Running full backend test suite..."
        .\.venv\Scripts\python.exe -m pytest -q
    }

    Write-Host "Pre-push checks passed."
} finally {
    Pop-Location
}
