param(
    [string]$Port
)

$ErrorActionPreference = 'Stop'
$idfPath = 'C:\esp\v6.1\esp-idf'
$activationScript = 'C:\Espressif\tools\Microsoft.v6.1.PowerShell_profile.ps1'

$env:PYTHONUTF8 = '1'
$null = chcp.com 65001

if (-not (Test-Path -LiteralPath $activationScript)) {
    throw "ESP-IDF could not be found at $idfPath. Update scripts/build.ps1 with your ESP-IDF path."
}

. $activationScript

if ($Port) {
    idf.py -p $Port flash
} else {
    idf.py build
}
