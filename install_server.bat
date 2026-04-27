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
echo.

REM Xóa file cũ nếu có
if exist "%TEMP_ZIP%" del /q "%TEMP_ZIP%" 2>nul

REM Download với progress bar chi tiết
REM -L: follow redirects
REM -o: output file
REM --fail: fail silently on HTTP errors
REM --connect-timeout 10: timeout nếu không kết nối được trong 10s
REM --max-time 600: timeout nếu tổng thời gian > 10 phút
REM -#: progress bar

echo      Connecting to GitHub...
curl -L -o "%TEMP_ZIP%" "%DOWNLOAD_URL%" --fail --connect-timeout 10 --max-time 600 -#

if errorlevel 1 (
    echo.
    echo [ERROR] Download failed!
    echo.
    echo Possible reasons:
    echo   1. Network connection issue (check your internet)
    echo   2. GitHub release not found (check: https://github.com/%GITHUB_REPO%/releases/tag/latest)
    echo   3. Repository is private (make it public)
    echo   4. Connection timeout (slow network or server issue)
    echo.
    echo Troubleshooting:
    echo   - Try opening the URL in browser to verify it exists
    echo   - Check if you can access github.com
    echo   - Wait a few minutes and try again
    echo.
    pause
    exit /b 1
)

REM Kiểm tra file size
for %%A in ("%TEMP_ZIP%") do set "FILESIZE=%%~zA"
if %FILESIZE% LSS 1000000 (
    echo.
    echo [WARNING] Downloaded file is suspiciously small (^<%FILESIZE% bytes^)
    echo           This might not be the correct file.
    echo.
    echo Press any key to continue anyway, or Ctrl+C to cancel...
    pause >nul
)

echo.
echo      Download complete!

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
