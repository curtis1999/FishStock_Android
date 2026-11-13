@echo off
rem Trains Aleph_0, the neural-network agent, from scratch by self-play (AlphaZero style).
rem Leave it running as long as you like: every generation is saved, so you can close the window
rem at any time and run this again later to carry on where it stopped.
rem Progress: aleph0_work\progress.csv (open it in Excel). Details: ENGINE_NOTES.md, "Aleph_0".
setlocal
cd /d "%~dp0"

rem ---------------------------------------------------------------- settings
rem How long to train this time, in minutes (0 = until you close the window)
set "MINUTES=60"
rem Self-play games per generation, and search simulations per move (more = stronger but slower)
set "GAMES=200"
set "SIMS=100"
rem CPU threads to use: "all", or a number (fewer keeps the laptop usable and cooler)
set "THREADS=all"
rem ----------------------------------------------------------------

call build_uci_engine.bat nopause
if errorlevel 1 (echo Build failed. & pause & exit /b 1)

set "JAVA=java"
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" set "JAVA=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"

echo.
echo Training Aleph_0. Close this window to stop (the last finished generation is kept).
echo.
"%JAVA%" -Xmx2g -cp fishstock-uci.jar com.example.fishstock.nn.ZeroTrainer generations=0 minutes=%MINUTES% games=%GAMES% sims=%SIMS% threads=%THREADS% dir=aleph0_work publish=app\src\main\resources\com\example\fishstock\nn\aleph0.bin

echo.
echo The newest network is in the app now (app\src\main\resources\com\example\fishstock\nn\aleph0.bin).
echo Rebuild the app in Android Studio to play it, and run build_uci_engine.bat again for cutechess.
pause
