@echo off
setlocal

cd /d "%~dp0"

set "JAVAC_CMD=javac"
set "JAVA_CMD=java"

where javac >nul 2>nul
if errorlevel 1 (
    for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk-*") do (
        if exist "%%~fd\bin\javac.exe" (
            set "JAVAC_CMD=%%~fd\bin\javac.exe"
            set "JAVA_CMD=%%~fd\bin\java.exe"
        )
    )
)

if not exist out mkdir out

if exist sources.txt del sources.txt
for /r src %%f in (*.java) do (
    echo %%f>> sources.txt
)

"%JAVAC_CMD%" -d out @sources.txt
if errorlevel 1 (
    del sources.txt
    echo.
    echo Build failed. Make sure Java JDK is installed and javac is available in PATH.
    echo Install it with: winget install --id EclipseAdoptium.Temurin.21.JDK -e
    pause
    exit /b 1
)

del sources.txt
"%JAVA_CMD%" -cp "out;lib/*" com.attendance.Main .

pause
