# Adds the fourth October 2026 FishStock update to this project:
#   20 more collection puzzles (30 in all), the puzzle page waits for GENERATE PUZZLE, "you found the
#   checkmate" in the move review, draw offers between two humans need ACCEPT, Agro thinks faster and
#   Timid slower (re-levelled), and the home screen asks AGENT or HUMAN first; the board no longer turns.
# 1. Copies every file it is about to replace into update4_backup_<date>\ (nothing is lost; it's also in git).
# 2. Unpacks FishStock_update4.zip over the project. Nothing is deleted.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Test-Path 'FishStock_update4.zip')) { throw 'FishStock_update4.zip was not found next to this script.' }

$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$backup = "update4_backup_$stamp"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Join-Path $PSScriptRoot 'FishStock_update4.zip'))
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

Expand-Archive -Path 'FishStock_update4.zip' -DestinationPath '.' -Force
Write-Host ''
Write-Host 'Done. In Android Studio: File > Sync Project with Gradle Files, then Run.'
Write-Host 'ENGINE_NOTES.md describes the new pieces (openings, move review, puzzle collection).'
