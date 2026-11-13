@echo off
rem Plays one FishStock agent against Stockfish limited to a chosen Elo, using cutechess-cli,
rem and prints the Elo difference at the end. Edit the five settings below first.
rem Needs: fishstock-uci.jar (run build_uci_engine.bat), cutechess-cli.exe, stockfish.exe.
setlocal
cd /d "%~dp0"

rem ---------------------------------------------------------------- settings
set "CUTECHESS=C:\Program Files (x86)\Cute Chess\cutechess-cli.exe"
rem Stockfish must be unzipped first (right-click the zip > Extract All); this is where Extract All puts it.
set "STOCKFISH=C:\Users\Admin\Downloads\stockfish-windows-x86-64-universal\stockfish\stockfish-windows-x86-64-universal.exe"
rem Agent to test: Simple, Lazy, MinMax, Agro, Timid, FishStock, Aleph_0 (or Lazy+ / MinMax+)
set "AGENT=FishStock"
rem Stockfish's strength (1320 to 3190, CCRL Blitz scale)
set "ELO=2000"
rem Games (pairs are played with colours reversed) and time control: 60 s + 0.6 s per move
set "GAMES=20"
set "TC=60+0.6"
rem ----------------------------------------------------------------

set "JAVA=java"
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" set "JAVA=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
rem (No brackets around these checks: a path like "Program Files (x86)" would break them.)
set "PROBLEM="
if not exist "%CUTECHESS%" set "PROBLEM=cutechess-cli.exe not found at: %CUTECHESS%"
if not exist "%CUTECHESS%" goto problem
if not exist "%STOCKFISH%" set "PROBLEM=Stockfish not found at: %STOCKFISH%  (unzip it first, then fix the path in this file)"
if not exist "%STOCKFISH%" goto problem
if not exist fishstock-uci.jar set "PROBLEM=fishstock-uci.jar not found - run build_uci_engine.bat first"
if not exist fishstock-uci.jar goto problem
set /a ROUNDS=%GAMES%/2

"%CUTECHESS%" ^
 -engine name=%AGENT% cmd="%JAVA%" arg=-jar arg="%~dp0fishstock-uci.jar" proto=uci option.Agent=%AGENT% ^
 -engine name=Stockfish-%ELO% cmd="%STOCKFISH%" proto=uci option.UCI_LimitStrength=true option.UCI_Elo=%ELO% ^
 -each tc=%TC% -rounds %ROUNDS% -games 2 -repeat -recover -concurrency 1 ^
 -pgnout "elo_%AGENT%_vs_SF%ELO%.pgn"

echo.
echo Your agent's Elo is roughly %ELO% plus the "Elo difference" printed above.
echo If it won almost everything (or nothing), move ELO up (or down) and run again.
pause
exit /b 0

:problem
echo.
echo %PROBLEM%
pause
exit /b 1
