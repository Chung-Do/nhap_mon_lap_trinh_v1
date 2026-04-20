@echo off
echo ===================================================
echo  BUILD SERVER NATIVE - RemotePC (GraalVM)
echo ===================================================

REM --- 1. Check GraalVM native-image ---
where native-image >nul 2>nul
if errorlevel 1 (
    echo FAILED: GraalVM native-image not found.
    echo Please install GraalVM and run: gu install native-image
    pause
    exit /b 1
)

REM --- 2. Create build directory ---
if exist build\server rmdir /s /q build\server
mkdir build\server

REM --- 3. Copy Java files ---
copy server\*.java build\server\

REM --- 4. Compile ---
echo [1/6] Compiling Java source files...
javac -d build\server build\server\*.java
if errorlevel 1 (
    echo FAILED: Compile error.
    pause
    exit /b 1
)
echo      OK.

REM --- 5. Create JAR ---
echo [2/6] Creating server.jar...
echo Main-Class: Server> build\server\MANIFEST.MF
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

REM --- 6. Copy reflection config ---
copy server\reflect-config.json build\server\

REM --- 7. Copy ffmpeg (if available) ---
echo [3/6] Copying ffmpeg (if available)...
if exist resources\ffmpeg\ffmpeg.exe (
    if not exist build\server\ffmpeg_bundle mkdir build\server\ffmpeg_bundle
    copy resources\ffmpeg\ffmpeg.exe build\server\ffmpeg_bundle\
    echo      FFmpeg found and copied!
) else (
    echo      FFmpeg not found. Server will auto-download on first run.
)

REM --- 8. Build native image ---
echo [4/6] Building native executable (this may take 3-5 minutes)...
cd build\server
native-image ^
  -jar server.jar ^
  -H:Name=RemotePC-Server ^
  -H:ReflectionConfigurationFiles=reflect-config.json ^
  -H:+ReportExceptionStackTraces ^
  --no-fallback ^
  --verbose

if errorlevel 1 (
    echo FAILED: native-image build error.
    cd ..\..
    pause
    exit /b 1
)
cd ..\..
echo      OK.

REM --- 9. Create output directory ---
if not exist dist\server_native mkdir dist\server_native
move build\server\RemotePC-Server.exe dist\server_native\

REM --- 10. Copy ffmpeg bundle to dist ---
if exist build\server\ffmpeg_bundle (
    xcopy /E /I build\server\ffmpeg_bundle dist\server_native\ffmpeg_bundle
)

REM --- 11. Add icon ---
echo [5/6] Adding icon...
if exist icons\server.ico (
    where rcedit >nul 2>nul
    if not errorlevel 1 (
        rcedit dist\server_native\RemotePC-Server.exe --set-icon icons\server.ico
        echo      Icon added!
    ) else (
        echo      rcedit not found. Run: npm install -g rcedit
    )
) else (
    echo      Converting PNG to ICO...
    where magick >nul 2>nul
    if not errorlevel 1 (
        magick convert icons\server_256.png -define icon:auto-resize=256,128,64,48,32,16 icons\server.ico
        where rcedit >nul 2>nul
        if not errorlevel 1 (
            rcedit dist\server_native\RemotePC-Server.exe --set-icon icons\server.ico
            echo      Icon added!
        )
    ) else (
        echo      ImageMagick not found. No icon added.
    )
)

echo [6/6] Done!
echo.
echo ==> Result: dist\server_native\RemotePC-Server.exe
echo     Single standalone executable
echo     No JRE required!
echo.
if exist dist\server_native\ffmpeg_bundle (
    echo LUU Y: FFmpeg da duoc bundle!
) else (
    echo LUU Y: Server will auto-download FFmpeg on first run.
)
echo.
pause
