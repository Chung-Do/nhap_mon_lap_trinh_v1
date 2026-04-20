@echo off
echo ===================================================
echo  BUILD LAUNCHER EXE - RemotePC Server Installer
echo ===================================================

REM --- 1. Create build directory ---
if exist build\launcher rmdir /s /q build\launcher
mkdir build\launcher

REM --- 2. Copy Java file ---
copy launcher\*.java build\launcher\

REM --- 3. Compile ---
echo [1/4] Compiling Java source...
javac -d build\launcher build\launcher\*.java
if errorlevel 1 (
    echo FAILED: Compile error.
    pause
    exit /b 1
)
echo      OK.

REM --- 4. Create MANIFEST ---
echo Main-Class: ServerLauncher> build\launcher\MANIFEST.MF

REM --- 5. Create JAR ---
echo [2/4] Creating launcher.jar...
cd build\launcher
jar cfm launcher.jar MANIFEST.MF *.class
if errorlevel 1 (
    echo FAILED: Could not create JAR.
    cd ..\..
    pause
    exit /b 1
)
cd ..\..
echo      OK.

REM --- 6. Package to EXE with custom icon ---
echo [3/4] Packaging to EXE (this may take a minute)...
if exist dist\launcher rmdir /s /q dist\launcher

REM Check if custom icon exists
set ICON_ARG=
if exist icons\server.ico (
    set ICON_ARG=--icon icons\server.ico
    echo      Using custom icon: icons\server.ico
) else (
    echo      No custom icon found. Using default.
    echo      To add icon: place server.ico in icons\ folder
)

jpackage ^
  --type app-image ^
  --name RemotePC-Server-Installer ^
  --input build\launcher ^
  --main-jar launcher.jar ^
  --main-class ServerLauncher ^
  --dest dist\launcher ^
  %ICON_ARG% ^
  --win-console

if errorlevel 1 (
    echo FAILED: jpackage error.
    pause
    exit /b 1
)
echo      OK.

echo [4/4] Done!
echo.
echo ==> Result: dist\launcher\RemotePC-Server-Installer\RemotePC-Server-Installer.exe
echo     Double-click to download and run server.
echo.
if not exist icons\server.ico (
    echo TIP: Add icons\server.ico for custom icon, then rebuild.
)
echo.
pause
