# Adds the third October 2026 FishStock update to this project:
#   opponent dropdown on the home screen (re-levelled agents), move verdicts and explanations on the
#   analysis screen, a puzzle collection with a Select theme menu, a scrollable Tournament screen
#   with VIEW RESULTS / VIEW GAME, and opening books matched to each agent's aggression.
# 1. Copies every file it is about to replace into update3_backup_<date>\ (nothing is lost; it's also in git).
# 2. Unpacks FishStock_update3.zip over the project. Nothing is deleted.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Test-Path 'FishStock_update3.zip')) { throw 'FishStock_update3.zip was not found next to this script.' }

$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$backup = "update3_backup_$stamp"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Join-Path $PSScriptRoot 'FishStock_update3.zip'))
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

Expand-Archive -Path 'FishStock_update3.zip' -DestinationPath '.' -Force
Write-Host ''
Write-Host 'Done. In Android Studio: File > Sync Project with Gradle Files, then Run.'
Write-Host 'ENGINE_NOTES.md describes the new pieces (openings, move review, puzzle collection).'
