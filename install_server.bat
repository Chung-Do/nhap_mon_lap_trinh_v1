@echo off
REM ===================================================
REM  AUTO-INSTALLER for RemotePC Server
REM  Tự động tải và cài đặt server từ GitHub Releases
REM ===================================================

setlocal EnableDelayedExpansion

REM --- Configuration ---
set "GITHUB_REPO=Chung-Do/nhap_mon_lap_trinh_v1"
set "INSTALL_DIR=%USERPROFILE%\RemotePC"
set "TEMP_ZIP=%TEMP%\RemotePC-Server.zip"
REM Use direct download link (more reliable than tag-based URL)
set "DOWNLOAD_URL=https://github.com/%GITHUB_REPO%/releases/download/latest/RemotePC-Server.zip"

echo ===================================================
echo   RemotePC Server - Auto Installer
echo ===================================================
echo.

REM --- 1. Kiểm tra quyền Administrator (optional) ---
net session >nul 2>&1
if %errorlevel% == 0 (
    echo [INFO] Running with Administrator privileges
) else (
    echo [WARN] Not running as Administrator
    echo        Some features may not work properly
    echo.
)

REM --- 2. Tạo thư mục cài đặt ---
echo [1/5] Creating installation directory...
if not exist "%INSTALL_DIR%" (
    mkdir "%INSTALL_DIR%"
    echo      Created: %INSTALL_DIR%
) else (
    echo      Directory exists: %INSTALL_DIR%
)

REM --- 3. Tải phiên bản mới nhất từ GitHub ---
echo [2/5] Downloading latest version from GitHub...
echo      URL: %DOWNLOAD_URL%
echo      File size: ~50-60 MB
echo.
echo      Please wait, this may take 5-10 minutes depending on your network speed...
echo.

REM KHÔNG xóa file cũ - cho phép resume nếu download bị ngắt
if exist "%TEMP_ZIP%" (
    echo      Found partial download, resuming...
)

REM Download with curl (built-in on Windows 10)
REM Optimized for unreliable connections with resume capability
REM -L: follow redirects
REM -C -: resume from where it left off (safe with GitHub)
REM --connect-timeout 15: 15 seconds to establish connection
REM --max-time 900: 15 minute timeout per attempt
REM --retry 5: retry 5 times (important for unstable connections)
REM --retry-delay 3: wait 3 seconds between retries
REM --retry-max-time 0: no limit on total retry time
curl -L -C - -o "%TEMP_ZIP%" "%DOWNLOAD_URL%" --connect-timeout 15 --max-time 900 --retry 5 --retry-delay 3 --retry-max-time 0

if errorlevel 1 (
    echo.
    echo [ERROR] Download failed!
    echo.
    echo Possible reasons:
    echo   1. Unstable network connection (file is ~80-100MB)
    echo   2. GitHub release not found or file not uploaded properly
    echo   3. Firewall/Antivirus blocking download
    echo   4. Network timeout (slow connection)
    echo.
    echo Troubleshooting:
    echo   1. Test URL in browser: %DOWNLOAD_URL%
    echo   2. Check internet: ping github.com
    echo   3. Try from a different network
    echo   4. Download manually from: https://github.com/%GITHUB_REPO%/releases
    echo      Then extract to: %INSTALL_DIR%
    echo.
    pause
    exit /b 1
)

REM Kiểm tra file download có thành công không
if not exist "%TEMP_ZIP%" (
    echo.
    echo [ERROR] Download file not found!
    pause
    exit /b 1
)

REM Kiểm tra file size
for %%A in ("%TEMP_ZIP%") do set "FILESIZE=%%~zA"
if %FILESIZE% LSS 1000000 (
    echo.
    echo [ERROR] Downloaded file too small (%FILESIZE% bytes)
    echo         File may be corrupt or not the correct file.
    echo         Expected: ~80-100 MB
    echo.
    echo Common causes:
    echo   1. GitHub release tag 'latest' does not exist
    echo   2. File 'RemotePC-Server.zip' not found in release
    echo   3. GitHub returned a 404 error page instead of the file
    echo.
    echo Please check your GitHub release:
    echo   https://github.com/%GITHUB_REPO%/releases
    echo.
    echo The downloaded file will be kept for inspection at:
    echo   %TEMP_ZIP%
    echo.
    echo You can open it in Notepad to see what was actually downloaded.
    echo.
    pause
    exit /b 1
)

REM Test ZIP integrity trước khi extract
echo      Verifying ZIP file integrity...
tar -tzf "%TEMP_ZIP%" >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERROR] ZIP file is corrupted!
    echo         The downloaded file cannot be extracted.
    echo.
    echo Possible solutions:
    echo   1. Delete and run this script again
    echo   2. Check your network connection
    echo   3. Download manually from: %DOWNLOAD_URL%
    echo.
    del /f /q "%TEMP_ZIP%" 2>nul
    pause
    exit /b 1
)

echo.
echo      Download complete! (File size: %FILESIZE% bytes)

REM --- 4. Giải nén ---
echo [3/5] Extracting files...

REM Xóa folder cũ nếu có để tránh conflict
if exist "%INSTALL_DIR%\RemotePC-Server" (
    echo      Removing old installation...
    rmdir /s /q "%INSTALL_DIR%\RemotePC-Server" 2>nul
)

REM Extract bằng tar (có sẵn trên Windows 10+)
tar -xf "%TEMP_ZIP%" -C "%INSTALL_DIR%"

if errorlevel 1 (
    echo [ERROR] Extraction failed!
    pause
    exit /b 1
)

echo      Extracted successfully!

REM --- 5. Dọn dẹp file tạm ---
echo [4/5] Cleaning up...
if exist "%TEMP_ZIP%" del /q "%TEMP_ZIP%"
echo      Temporary files removed.

REM --- 6. Tạo shortcut ---
echo [5/5] Creating shortcuts...

REM Tạo VBS script để tạo shortcut (không cần PowerShell)
echo Set oWS = WScript.CreateObject("WScript.Shell") > "%TEMP%\CreateShortcut.vbs"
echo sLinkFile = "%USERPROFILE%\Desktop\RemotePC Server.lnk" >> "%TEMP%\CreateShortcut.vbs"
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> "%TEMP%\CreateShortcut.vbs"
echo oLink.TargetPath = "%INSTALL_DIR%\RemotePC-Server\RemotePC-Server.exe" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.WorkingDirectory = "%INSTALL_DIR%\RemotePC-Server" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.Description = "RemotePC Server" >> "%TEMP%\CreateShortcut.vbs"
echo oLink.Save >> "%TEMP%\CreateShortcut.vbs"

cscript //nologo "%TEMP%\CreateShortcut.vbs"
del "%TEMP%\CreateShortcut.vbs" 2>nul

echo      Desktop shortcut created!

echo.
echo ===================================================
echo   Installation Complete!
echo ===================================================
echo.
echo Install location: %INSTALL_DIR%\RemotePC-Server
echo.
echo [OPTION 1] Start now (will open in new window)
echo [OPTION 2] Exit and run later
echo.
choice /C 12 /N /M "Your choice [1 or 2]: "

if errorlevel 2 goto :end
if errorlevel 1 goto :start

:start
echo.
echo Starting RemotePC Server...
start "" "%INSTALL_DIR%\RemotePC-Server\RemotePC-Server.exe"
timeout /t 2 /nobreak >nul
goto :end

:end
echo.
echo You can start the server anytime from:
echo   - Desktop shortcut: "RemotePC Server"
echo   - Or directly: %INSTALL_DIR%\RemotePC-Server\RemotePC-Server.exe
echo.
echo To update, just run this install script again!
echo.
pause
