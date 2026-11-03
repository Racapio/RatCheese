# Builds Rat Cheese Helper for every supported Minecraft version into build/libs.
# Usage: powershell -ExecutionPolicy Bypass -File buildAll.ps1
$ErrorActionPreference = 'Stop'
$versions = Get-ChildItem "$PSScriptRoot\versions\*.properties" | ForEach-Object { $_.BaseName }
foreach ($v in $versions) {
	Write-Host "=== Minecraft $v ==="
	& "$PSScriptRoot\gradlew.bat" build "-Pmc=$v"
	if ($LASTEXITCODE -ne 0) { throw "Build failed for $v" }
}
Get-ChildItem "$PSScriptRoot\build\libs\*.jar" | Select-Object -ExpandProperty Name
