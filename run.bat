@echo off
cd /d "%~dp0"
title LumberJack Attack
echo Starting LibGDX game...
echo.
echo First launch after a while can take a minute while Gradle downloads/updates.
echo Keep this window open.
echo.

call gradlew.bat run
set EXITCODE=%ERRORLEVEL%

echo.
if %EXITCODE% neq 0 (
    echo Game failed to start. Error code: %EXITCODE%
    echo.
    pause
    exit /b %EXITCODE%
)
