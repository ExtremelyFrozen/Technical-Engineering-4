<#
.SYNOPSIS
    Gradle wrapper with enforced UTF-8 encoding for PowerShell 5.1.
.DESCRIPTION
    Saves/restores console encoding, code page, and $OutputEncoding,
    changes to project root, then delegates to gradlew.bat with all remaining arguments.
    Ensures the caller's working directory and console settings are restored on exit.
.EXAMPLE
    .\scripts\gradle-utf8.ps1 compileExtraJava --rerun-tasks
    .\scripts\gradle-utf8.ps1 runAdapterTestServer
#>

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$RemainingArguments
)

# ── Save original settings ─────────────────────────────────────────────
$savedInputEncoding   = [Console]::InputEncoding
$savedOutputEncoding  = [Console]::OutputEncoding
$savedVariableEncoding = $OutputEncoding
$savedCodePage = (chcp).ToString().Trim() -replace '\D', ''

# ── Derive project root from script location ──────────────────────────
$projectRoot = Split-Path -Parent $PSScriptRoot
$gradlew     = Join-Path -Path $projectRoot -ChildPath "gradlew.bat"

# ── Force UTF-8 ────────────────────────────────────────────────────────
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::InputEncoding  = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
chcp 65001 | Out-Null

# ── Track whether we pushed to the stack (for safe restore) ────────────
$pushed = $false
$exitCode = 1

try {
    if (-not (Test-Path -LiteralPath $gradlew)) {
        Write-Error "gradlew.bat not found at: $gradlew"
        exit 1
    }

    # Change to project root so Gradle finds build.gradle
    Push-Location -LiteralPath $projectRoot
    $pushed = $true

    # Invoke gradlew.bat with remaining arguments, capture exit code
    & $gradlew @RemainingArguments
    $exitCode = $LASTEXITCODE
}
finally {
    # Restore working directory (only if we pushed)
    if ($pushed) { Pop-Location }

    # Restore console encodings and code page
    [Console]::OutputEncoding = $savedOutputEncoding
    [Console]::InputEncoding  = $savedInputEncoding
    $OutputEncoding = $savedVariableEncoding
    chcp $savedCodePage | Out-Null

    exit $exitCode
}
