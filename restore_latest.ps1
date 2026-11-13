# Undoes the accidental re-run of apply_refactor.bat on 20 Oct 2026.
# apply_refactor.bat is the FIRST refactor (5 Oct) installer: it deletes the app's code and unpacks
# that old version. Before doing so it saved the up-to-date code in refactor_backup_20261020_131034.
# This script:
#   1. saves the current (old, regressed) code in regressed_backup_<date>\, just in case;
#   2. puts the up-to-date code from refactor_backup_20261020_131034 back;
#   3. deletes app\build so the next build starts clean;
#   4. moves the one-time installers (apply_*.bat/.ps1 and their zips) into old_installers\
#      so they can't be run again by mistake.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$src = 'refactor_backup_20261020_131034'
if (-not (Test-Path "$src\app\src\main\java\com\example\fishstock\RecordGameActivity.java")) {
  throw "$src doesn't contain the latest code; nothing was changed."
}

$items = @('app\src\main\java', 'app\src\test\java', 'app\src\main\res\layout', 'app\src\main\res\layout-land',
           'app\src\main\AndroidManifest.xml', 'app\build.gradle')

$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$save = "regressed_backup_$stamp"
foreach ($item in $items) {
  if (Test-Path $item) {
    $dest = Join-Path $save $item
    New-Item -ItemType Directory -Force -Path (Split-Path $dest) | Out-Null
    Copy-Item -Path $item -Destination $dest -Recurse -Force
  }
}
Write-Host "The old code that was in place is saved in $save"

foreach ($item in $items) {
  if (Test-Path $item) { Remove-Item -Recurse -Force $item }
  $from = Join-Path $src $item
  if (Test-Path $from) { Copy-Item -Path $from -Destination $item -Recurse -Force }
}
Write-Host "Latest code restored from $src"

if (Test-Path 'app\build') { Remove-Item -Recurse -Force 'app\build' -ErrorAction SilentlyContinue }

New-Item -ItemType Directory -Force -Path 'old_installers' | Out-Null
Get-ChildItem -File -Path . | Where-Object { $_.Name -like 'apply_refactor.*' -or $_.Name -like 'apply_update*.*' -or $_.Name -like 'FishStock_refactor.zip' -or $_.Name -like 'FishStock_update*.zip' } |
  ForEach-Object { Move-Item -Force $_.FullName (Join-Path 'old_installers' $_.Name) }
Write-Host 'Old one-time installers moved to old_installers\ (do not run them again).'

Write-Host ''
Write-Host 'Done. In Android Studio: File > Sync Project with Gradle Files, Build > Clean Project, then Run.'
