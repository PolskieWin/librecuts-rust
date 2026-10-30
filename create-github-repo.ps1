<#
.SYNOPSIS
    Initializes Git repository and creates a public/private GitHub repository for LibreCut-Rust.
#>

param (
    [string]$RepoName = "librecuts-rust",
    [switch]$Private = $false
)

$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  LibreCut-Rust: GitHub Repository Publisher             " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Check Git
$gitCmd = Get-Command git.exe -ErrorAction SilentlyContinue
if (-not $gitCmd) {
    Write-Host "[ERROR] Git is not installed or not in PATH." -ForegroundColor Red
    Write-Host "Please run .\setup-dev-environment.ps1 first, then restart terminal." -ForegroundColor Yellow
    exit 1
}

# 1. Initialize Git repository
if (-not (Test-Path ".git")) {
    Write-Host "`n--> Initializing Git repository..." -ForegroundColor Yellow
    git init -b main
    git add .
    git commit -m "feat: initial commit - LibreCut rebuilt in Rust for Android 14+ (NDK AMediaCodec, Smart Render, SIMD)"
    Write-Host "    [SUCCESS] Local git repository initialized with main branch." -ForegroundColor Green
} else {
    Write-Host "    [OK] Git repository already initialized." -ForegroundColor Green
}

# 2. GitHub CLI Check
$ghCmd = Get-Command gh.exe -ErrorAction SilentlyContinue
if ($ghCmd) {
    Write-Host "`n--> Checking GitHub CLI authentication..." -ForegroundColor Yellow
    gh auth status
    if ($LASTEXITCODE -eq 0) {
        $visibility = if ($Private) { "--private" } else { "--public" }
        Write-Host "--> Creating GitHub repository '$RepoName' ($visibility)..." -ForegroundColor Cyan
        gh repo create $RepoName $visibility --source=. --remote=origin --push
        if ($LASTEXITCODE -eq 0) {
            Write-Host "`n[SUCCESS] Repository created and pushed to GitHub!" -ForegroundColor Green
            Write-Host "Visit: https://github.com/$(gh api user -q .login)/$RepoName" -ForegroundColor Cyan
            exit 0
        }
    } else {
        Write-Host "--> GitHub CLI is not logged in. Run 'gh auth login' to connect your account." -ForegroundColor Yellow
    }
}

Write-Host "`nManual Push Instructions:" -ForegroundColor Cyan
Write-Host "1. Create a new repository on https://github.com/new named '$RepoName'"
Write-Host "2. Run the following commands in this directory:"
Write-Host "   git remote add origin https://github.com/<your-username>/$RepoName.git" -ForegroundColor White
Write-Host "   git branch -M main" -ForegroundColor White
Write-Host "   git push -u origin main" -ForegroundColor White

