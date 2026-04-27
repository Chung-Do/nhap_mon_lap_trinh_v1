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
echo      Please wait...

REM Lấy URL của release mới nhất
powershell -Command ^
    "$repo = '%GITHUB_REPO%'; " ^
    "$apiUrl = 'https://api.github.com/repos/' + $repo + '/releases/latest'; " ^
    "try { " ^
    "    $release = Invoke-RestMethod -Uri $apiUrl; " ^
    "    $asset = $release.assets | Where-Object { $_.name -eq 'RemotePC-Server.zip' }; " ^
    "    if ($asset) { " ^
    "        Write-Host '      Downloading:' $asset.name '(' ([math]::Round($asset.size/1MB, 2)) 'MB)'; " ^
    "        Invoke-WebRequest -Uri $asset.browser_download_url -OutFile '%TEMP_ZIP%'; " ^
    "        Write-Host '      Download complete!' -ForegroundColor Green; " ^
    "    } else { " ^
    "        Write-Host '      ERROR: RemotePC-Server.zip not found in latest release' -ForegroundColor Red; " ^
    "        exit 1; " ^
    "    } " ^
    "} catch { " ^
    "    Write-Host '      ERROR: Failed to download. Check internet connection.' -ForegroundColor Red; " ^
    "    Write-Host '      Details:' $_.Exception.Message; " ^
    "    exit 1; " ^
    "}"

if errorlevel 1 (
    echo.
    echo [ERROR] Download failed!
    echo         - Check your internet connection
    echo         - Make sure GitHub repository is public
    echo         - Verify repository name: %GITHUB_REPO%
    echo.
    pause
    exit /b 1
)

REM --- 4. Giải nén ---
echo [3/5] Extracting files...
powershell -Command ^
    "try { " ^
    "    Expand-Archive -Path '%TEMP_ZIP%' -DestinationPath '%INSTALL_DIR%' -Force; " ^
    "    Write-Host '      Extracted successfully!' -ForegroundColor Green; " ^
    "} catch { " ^
    "    Write-Host '      ERROR: Failed to extract ZIP' -ForegroundColor Red; " ^
    "    exit 1; " ^
    "}"

if errorlevel 1 (
    echo [ERROR] Extraction failed!
    pause
    exit /b 1
)

REM --- 5. Dọn dẹp file tạm ---
echo [4/5] Cleaning up...
if exist "%TEMP_ZIP%" del /q "%TEMP_ZIP%"
echo      Temporary files removed.

REM --- 6. Tạo shortcut (optional) ---
echo [5/5] Creating shortcuts...
powershell -Command ^
    "$WshShell = New-Object -ComObject WScript.Shell; " ^
    "$Shortcut = $WshShell.CreateShortcut('%USERPROFILE%\Desktop\RemotePC Server.lnk'); " ^
    "$Shortcut.TargetPath = '%INSTALL_DIR%\RemotePC-Server\RemotePC-Server.exe'; " ^
    "$Shortcut.WorkingDirectory = '%INSTALL_DIR%\RemotePC-Server'; " ^
    "$Shortcut.Description = 'RemotePC Server'; " ^
    "$Shortcut.Save();"
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
