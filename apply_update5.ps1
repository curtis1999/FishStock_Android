# Adds the fifth October 2026 FishStock update to this project:
#   analysis opens at the first move; key moments and TRAIN (puzzles from your mistakes, then
#   collection puzzles on the same themes); FishStock as a UCI engine (run build_uci_engine.bat).
# 1. Copies every file it is about to replace into update5_backup_<date>\ (nothing is lost; it's also in git).
# 2. Unpacks FishStock_update5.zip over the project. Nothing is deleted.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Test-Path 'FishStock_update5.zip')) { throw 'FishStock_update5.zip was not found next to this script.' }

$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$backup = "update5_backup_$stamp"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Join-Path $PSScriptRoot 'FishStock_update5.zip'))
try {
  foreach ($entry in $zip.Entries) {
    if ($entry.FullName.EndsWith('/')) { continue }
    $path = $entry.FullName -replace '/', '\'
    if (Test-Path $path) {
      $dest = Join-Path $backup $path
      New-Item -ItemType Directory -Force -Path (Split-Path $dest) | Out-Null
      Copy-Item -Path $path -Destination $dest -Force
    }
  }
} finally {
  $zip.Dispose()
}
Write-Host "Files being replaced were backed up to $backup"

Expand-Archive -Path 'FishStock_update5.zip' -DestinationPath '.' -Force
Write-Host ''
Write-Host 'Done. In Android Studio: File > Sync Project with Gradle Files, then Run.'
Write-Host 'ENGINE_NOTES.md describes the new pieces (training, the UCI engine for Lichess and engine matches).'
