@echo off
echo ===================================================
echo  BUILD SERVER - RemotePC
echo ===================================================

REM --- 1. Tao thu muc build ---
if exist build\server rmdir /s /q build\server
mkdir build\server

REM --- 2. Copy tat ca file .java vao build\server ---
copy server\*.java build\server\

REM --- 3. Compile tat ca file Java ---
echo [1/4] Compiling Java source files...
javac -d build\server build\server\*.java
if errorlevel 1 (
    echo FAILED: Compile error. Kiem tra Java JDK da cai chua.
    pause
    exit /b 1
)
echo      OK.

REM --- 4. Tao MANIFEST.MF ---
echo Main-Class: Server> build\server\MANIFEST.MF

REM --- 5. Tao JAR (phai o TRONG build\server\) ---
echo [2/4] Creating server.jar...
cd build\server
jar cfm server.jar MANIFEST.MF *.class
if errorlevel 1 (
    echo FAILED: Could not create JAR.
    cd ..\..
    pause
    exit /b 1
)
cd ..\..
echo      OK.

REM --- 6. Copy ffmpeg (neu co) ---
echo [3/5] Copying ffmpeg (if available)...
if exist resources\ffmpeg\ffmpeg.exe (
    if not exist build\server\ffmpeg_bundle mkdir build\server\ffmpeg_bundle
    copy resources\ffmpeg\ffmpeg.exe build\server\ffmpeg_bundle\
    echo      FFmpeg found and copied!
) else (
    echo      FFmpeg not found in resources\ffmpeg\
    echo      Server will auto-download on first run.
)

REM --- 7. Tao EXE bang jpackage ---
echo [4/5] Packaging to EXE (this may take a minute)...
if exist dist\server rmdir /s /q dist\server
jpackage ^
  --type app-image ^
  --name RemotePC-Server ^
  --input build\server ^
  --main-jar server.jar ^
  --main-class Server ^
  --dest dist\server
if errorlevel 1 (
    echo FAILED: jpackage error. Kiem tra JDK 14+ da cai chua.
    pause
    exit /b 1
)
echo      OK.

echo [5/5] Done!
echo.
echo ==> Ket qua: dist\server\RemotePC-Server\RemotePC-Server.exe
echo     Chay file do de khoi dong server.
echo.
if exist resources\ffmpeg\ffmpeg.exe (
    echo LUU Y: FFmpeg da duoc bundle! Webcam hoat dong ngay!
) else (
    echo LUU Y: Chuc nang webcam:
    echo        - Lan dau chay: App tu dong download ffmpeg (~70MB)
    echo        - Hoac: Dat ffmpeg.exe vao resources\ffmpeg\ roi build lai
)
echo.
pause
