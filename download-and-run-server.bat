@echo off
echo ===================================================
echo  Download and Run RemotePC Server
echo ===================================================

set REPO_URL=https://github.com/Chung-Do/nhap_mon_lap_trinh_v1
set DOWNLOAD_URL=%REPO_URL%/releases/download/latest/RemotePC-Server.zip
set ZIP_FILE=RemotePC-Server.zip
set EXTRACT_DIR=RemotePC-Server
set EXE_PATH=%EXTRACT_DIR%\RemotePC-Server\RemotePC-Server.exe

echo [1/3] Downloading latest server build...
curl -L -o %ZIP_FILE% %DOWNLOAD_URL%
if errorlevel 1 (
    echo FAILED: Could not download. Check internet connection.
    pause
    exit /b 1
)
echo      OK.

echo [2/3] Extracting...
if exist %EXTRACT_DIR% rmdir /s /q %EXTRACT_DIR%
powershell -Command "Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '.' -Force"
if errorlevel 1 (
    echo FAILED: Could not extract zip file.
    pause
    exit /b 1
)
echo      OK.

echo [3/3] Starting server...
if not exist "%EXE_PATH%" (
    echo FAILED: EXE file not found at %EXE_PATH%
    pause
    exit /b 1
)
start "" "%EXE_PATH%"
echo      Server started!

echo.
echo ==> Server is running. You can close this window.
echo.
timeout /t 3
