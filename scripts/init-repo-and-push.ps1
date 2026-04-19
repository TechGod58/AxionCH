param(
    [Parameter(Mandatory = $false)]
    [string]$RepoRoot = "C:\AxionCH",
    [Parameter(Mandatory = $false)]
    [string]$RemoteUrl = "",
    [Parameter(Mandatory = $false)]
    [string]$Branch = "main",
    [Parameter(Mandatory = $false)]
    [switch]$Push
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path $RepoRoot)) {
    throw "RepoRoot not found: $RepoRoot"
}

Write-Host "Repo root: $RepoRoot"
Push-Location $RepoRoot
try {
    $isGitRepo = $false
    try {
        git rev-parse --is-inside-work-tree *> $null
        if ($LASTEXITCODE -eq 0) {
            $isGitRepo = $true
        }
    } catch {
        $isGitRepo = $false
    }

    if (-not $isGitRepo) {
        Write-Host "Initializing git repository..."
        git init
    }

    git checkout -B $Branch

    if ($RemoteUrl) {
        $existing = git remote get-url origin 2>$null
        if ($LASTEXITCODE -ne 0 -or -not $existing) {
            git remote add origin $RemoteUrl
            Write-Host "Added origin: $RemoteUrl"
        } elseif ($existing -ne $RemoteUrl) {
            git remote set-url origin $RemoteUrl
            Write-Host "Updated origin URL: $RemoteUrl"
        } else {
            Write-Host "Origin already configured."
        }
    }

    $headExists = $false
    try {
        git rev-parse --verify HEAD *> $null
        if ($LASTEXITCODE -eq 0) {
            $headExists = $true
        }
    } catch {
        $headExists = $false
    }

    if (-not $headExists) {
        Write-Host "Creating initial commit..."
        git add .
        git commit -m "chore: initialize AxionCH repository baseline"
    }

    if ($Push) {
        if (-not $RemoteUrl) {
            $origin = git remote get-url origin 2>$null
            if ($LASTEXITCODE -ne 0 -or -not $origin) {
                throw "Push requested, but origin is not configured. Provide -RemoteUrl."
            }
        }
        git push -u origin $Branch
    } else {
        Write-Host "Initialization complete. Use -Push to push branch '$Branch'."
    }
} finally {
    Pop-Location
}
