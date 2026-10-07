@echo off
rem Plays one FishStock agent against Stockfish limited to a chosen Elo, using cutechess-cli,
rem and prints the Elo difference at the end. Edit the five settings below first.
rem Needs: fishstock-uci.jar (run build_uci_engine.bat), cutechess-cli.exe, stockfish.exe.
setlocal
cd /d "%~dp0"

rem ---------------------------------------------------------------- settings
set "CUTECHESS=C:\Chess\cutechess\cutechess-cli.exe"
set "STOCKFISH=C:\Chess\stockfish\stockfish.exe"
rem Agent to test: Simple, Lazy, MinMax, Agro, Timid, FishStock (or Lazy+ / MinMax+)
set "AGENT=FishStock"
rem Stockfish's strength (1320 to 3190, CCRL Blitz scale)
set "ELO=1500"
rem Games (pairs are played with colours reversed) and time control: 60 s + 0.6 s per move
set "GAMES=100"
set "TC=60+0.6"
rem ----------------------------------------------------------------

set "JAVA=java"
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" set "JAVA=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
if not exist "%CUTECHESS%" (echo cutechess-cli not found at %CUTECHESS% - edit this file & pause & exit /b 1)
if not exist "%STOCKFISH%" (echo Stockfish not found at %STOCKFISH% - edit this file & pause & exit /b 1)
if not exist fishstock-uci.jar (echo fishstock-uci.jar not found - run build_uci_engine.bat first & pause & exit /b 1)
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
