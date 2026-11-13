@echo off
rem Builds FishStock as a UCI chess engine for lichess-bot, cutechess-cli, fastchess, Arena GUI...
rem Output (in this folder): fishstock-uci.jar and fishstock-uci.bat (point your chess program at the .bat).
rem Uses the Java that comes with Android Studio, or JAVA_HOME, or java on the PATH.
setlocal EnableDelayedExpansion
cd /d "%~dp0"

set "JDK="
if exist "C:\Program Files\Android\Android Studio\jbr\bin\javac.exe" set "JDK=C:\Program Files\Android\Android Studio\jbr\bin\"
if not defined JDK if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JDK=%JAVA_HOME%\bin\"
if defined JDK (echo Using Java in !JDK!) else (echo Using javac from the PATH)

set "SRC=app\src\main\java\com\example\fishstock"
if exist build\uci rmdir /s /q build\uci
mkdir build\uci
if exist uci_sources.txt del /q uci_sources.txt
rem (FOR /R can't take a folder that comes from another FOR, so DIR /S /B lists the files instead.)
for %%P in (engine eval search agents arena endgame uci nn) do (
  for /f "delims=" %%F in ('dir /s /b "%SRC%\%%P\*.java"') do (
    set "F=%%F"
    echo "!F:\=/!">> uci_sources.txt
  )
)
if not exist uci_sources.txt (echo No source files found under %SRC% & goto fail)

"%JDK%javac" -nowarn -encoding UTF-8 -d build\uci @uci_sources.txt
if errorlevel 1 goto fail
rem The jar also carries Aleph_0's trained network (app\src\main\resources), if there is one.
if exist app\src\main\resources (
  "%JDK%jar" cfe fishstock-uci.jar com.example.fishstock.uci.UciEngine -C build\uci . -C app\src\main\resources .
) else (
  "%JDK%jar" cfe fishstock-uci.jar com.example.fishstock.uci.UciEngine -C build\uci .
)
if errorlevel 1 goto fail
del /q uci_sources.txt

if defined JDK (
  > fishstock-uci.bat echo @"%JDK%java" -jar "%%~dp0fishstock-uci.jar" %%*
) else (
  > fishstock-uci.bat echo @java -jar "%%~dp0fishstock-uci.jar" %%*
)
echo.
echo Done: fishstock-uci.jar and fishstock-uci.bat
echo Test it: run fishstock-uci.bat and type   uci   then   isready   then   go movetime 1000
if /i not "%~1"=="nopause" pause
exit /b 0

:fail
echo.
echo Build failed (see the messages above).
if /i not "%~1"=="nopause" pause
exit /b 1
