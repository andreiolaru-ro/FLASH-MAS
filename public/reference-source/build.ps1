<#
If not allowed to execute run
	Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

.SYNOPSIS
    Build script for document.tex and slides.tex (PowerShell equivalent of the Makefile).

.DESCRIPTION
    Runs pdflatex on document.tex and slides.tex (double-pass on success),
    runs a_clean-tex.bat, then renames/moves the resulting PDFs to the parent directory.

.PARAMETER Task
    Which step to run: All, Document, Slides, Clean, Install. Default: All.

.EXAMPLE
    ./build.ps1
    ./build.ps1 -Task Slides
#>

param(
    [ValidateSet("All", "Document", "Slides", "Clean", "Install")]
    [string]$Task = "All"
)

$ErrorActionPreference = "Stop"

function Build-Tex {
    param(
        [Parameter(Mandatory)][string]$Name   # e.g. "document" or "slides"
    )

    $texFile = "$Name.tex"
    $log1 = "$Name.pass1.log"
    $log2 = "$Name.pass2.log"

    Write-Host "==> [$Name] Pass 1: pdflatex $texFile"
    & pdflatex -interaction=nonstopmode -halt-on-error $texFile *> $log1
    $pass1Success = $LASTEXITCODE -eq 0

    if (-not $pass1Success) {
        Write-Host "==> [$Name] BUILD FAILED on pass 1. See $log1" -ForegroundColor Red
        throw "$Name build failed on pass 1"
    }

    Write-Host "==> [$Name] Pass 1 succeeded. Running pass 2..."
    & pdflatex -interaction=nonstopmode -halt-on-error $texFile *> $log2
    $pass2Success = $LASTEXITCODE -eq 0

    if (-not $pass2Success) {
        Write-Host "==> [$Name] BUILD FAILED on pass 2. See $log2" -ForegroundColor Red
        throw "$Name build failed on pass 2"
    }

    Write-Host "==> [$Name] Pass 2 succeeded. Build complete." -ForegroundColor Green
}

function Invoke-Clean {
    Write-Host "==> [clean] Running a_clean-tex.bat"

    if (-not (Test-Path "./a_clean-tex.bat")) {
        throw "a_clean-tex.bat not found in current directory"
    }

	.\a_clean-tex.bat

    Write-Host "==> [clean] a_clean-tex.bat finished" -ForegroundColor Green
}

function Install-Pdfs {
    Write-Host "==> [install] Renaming document.pdf -> Quick-Reference.pdf and moving to parent dir"
    if (-not (Test-Path "document.pdf")) {
        throw "document.pdf not found - was it built successfully?"
    }
    Move-Item -Path "document.pdf" -Destination "../Quick-Reference.pdf" -Force

    Write-Host "==> [install] Renaming slides.pdf -> Quick-Slides.pdf and moving to parent dir"
    if (-not (Test-Path "slides.pdf")) {
        throw "slides.pdf not found - was it built successfully?"
    }
    Move-Item -Path "slides.pdf" -Destination "../Quick-Slides.pdf" -Force

    Write-Host "==> [install] Move complete" -ForegroundColor Green
}

# --- Main dispatch ---

try {
    switch ($Task) {
        "Document" { Build-Tex -Name "document" }
        "Slides"   { Build-Tex -Name "slides" }
        "Clean"    { Invoke-Clean }
        "Install"  { Install-Pdfs }
        "All" {
            Build-Tex -Name "document"
            Build-Tex -Name "slides"
            Invoke-Clean
            Install-Pdfs
            Write-Host "==> All steps complete." -ForegroundColor Green
        }
    }
}
catch {
    Write-Host "==> Build stopped: $_" -ForegroundColor Red
    exit 1
}
