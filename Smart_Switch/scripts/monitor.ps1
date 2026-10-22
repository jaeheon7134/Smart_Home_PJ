param(
    [Parameter(Mandatory = $true)]
    [string]$Port
)

$ErrorActionPreference = 'Stop'
$idfPath = 'C:\esp\v6.1\esp-idf'
$activationScript = 'C:\Espressif\tools\Microsoft.v6.1.PowerShell_profile.ps1'

$env:PYTHONUTF8 = '1'
$null = chcp.com 65001

if (-not (Test-Path -LiteralPath $activationScript)) {
    throw "ESP-IDF could not be found at $idfPath. Update scripts/monitor.ps1 with your ESP-IDF path."
}

. $activationScript
idf.py -p $Port monitor
