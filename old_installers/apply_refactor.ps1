# Swaps the refactored FishStock code into this project.
# 1. Copies the current code into refactor_backup_<date>\ (nothing is lost; it's also in git).
# 2. Removes the old Java sources and the broken landscape tournament layout.
# 3. Unpacks FishStock_refactor.zip over the project.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Test-Path 'FishStock_refactor.zip')) { throw 'FishStock_refactor.zip was not found next to this script.' }

$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$backup = "refactor_backup_$stamp"
$items = @('app\src\main\java', 'app\src\test\java', 'app\src\main\res\layout', 'app\src\main\res\layout-land',
           'app\src\main\AndroidManifest.xml', 'app\build.gradle')
foreach ($item in $items) {
  if (Test-Path $item) {
    $dest = Join-Path $backup $item
    New-Item -ItemType Directory -Force -Path (Split-Path $dest) | Out-Null
    Copy-Item -Path $item -Destination $dest -Recurse -Force
  }
}
Write-Host "Old code backed up to $backup"

Remove-Item -Recurse -Force 'app\src\main\java\com\example\fishstock' -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force 'app\src\test\java\com\example\fishstock' -ErrorAction SilentlyContinue
Remove-Item -Force 'app\src\main\res\layout-land\activity_tournament.xml' -ErrorAction SilentlyContinue

Expand-Archive -Path 'FishStock_refactor.zip' -DestinationPath '.' -Force
Write-Host ''
Write-Host 'Done. In Android Studio: File > Sync Project with Gradle Files, then Run.'
Write-Host 'Read ENGINE_NOTES.md for a tour of the new code.'
