<#
.SYNOPSIS
    Automated Development Environment Installer for LibreCut-Rust (Android 14+).
    Installs:
      1. Rust (rustup + Android NDK cross-compilation targets)
      2. cargo-ndk (Rust NDK build helper)
      3. Android Studio & Android SDK/NDK
      4. Git for Windows
      5. GitHub CLI (gh)
#>

$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  LibreCut-Rust: Android 14+ Dev Environment Installer     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Verify winget
$wingetPath = Get-Command winget.exe -ErrorAction SilentlyContinue
if (-not $wingetPath) {
    Write-Host "[ERROR] Windows Package Manager (winget) is not available." -ForegroundColor Red
    Write-Host "Please update the 'App Installer' from Microsoft Store or Windows Updates." -ForegroundColor Yellow
    exit 1
}

# Helper function to install via winget
function Install-PackageWinget {
    param (
        [string]$Id,
        [string]$Name
    )
    Write-Host "`n--> Checking $Name ($Id)..." -ForegroundColor Yellow
    winget list --id $Id --exact | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "    [OK] $Name is already installed." -ForegroundColor Green
    } else {
        Write-Host "    [INSTALLING] Downloading and installing $Name..." -ForegroundColor Cyan
        winget install --id $Id -e --silent --accept-package-agreements --accept-source-agreements
        if ($LASTEXITCODE -eq 0) {
            Write-Host "    [SUCCESS] $Name installed successfully!" -ForegroundColor Green
        } else {
            Write-Host "    [WARNING] $Name installer returned code $LASTEXITCODE. You may need to run PowerShell as Administrator." -ForegroundColor Magenta
        }
    }
}

# Step 1: Install Git & GitHub CLI
Install-PackageWinget -Id "Git.Git" -Name "Git for Windows"
Install-PackageWinget -Id "GitHub.cli" -Name "GitHub CLI (gh)"

# Step 2: Install Rust via Rustup
Install-PackageWinget -Id "Rustlang.Rustup" -Name "Rust Toolchain (Rustup)"

# Step 3: Install Android Studio
Install-PackageWinget -Id "Google.AndroidStudio" -Name "Android Studio"

# Refresh environment variables for the current session
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

# Step 4: Configure Rust Android NDK Targets
Write-Host "`n--> Configuring Rust Android cross-compilation targets..." -ForegroundColor Yellow
$rustupCmd = Get-Command rustup.exe -ErrorAction SilentlyContinue
if ($rustupCmd) {
    rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
    Write-Host "    [SUCCESS] Android targets added to Rust toolchain." -ForegroundColor Green
} else {
    Write-Host "    [INFO] Rustup not yet in current session PATH. Please restart terminal, then run:" -ForegroundColor Yellow
    Write-Host "    rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android" -ForegroundColor White
}

# Step 5: Install cargo-ndk
Write-Host "`n--> Installing cargo-ndk..." -ForegroundColor Yellow
$cargoCmd = Get-Command cargo.exe -ErrorAction SilentlyContinue
if ($cargoCmd) {
    cargo install cargo-ndk
    Write-Host "    [SUCCESS] cargo-ndk installed." -ForegroundColor Green
} else {
    Write-Host "    [INFO] After terminal restart, run: cargo install cargo-ndk" -ForegroundColor White
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  Setup Completed!                                       " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Restart your terminal / PowerShell session to pick up PATH changes."
Write-Host "2. Open Android Studio once to complete SDK & NDK r26d installation."
Write-Host "3. Run '.\create-github-repo.ps1' to publish your fork to GitHub!"

