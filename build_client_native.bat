@echo off
echo ===================================================
echo  BUILD CLIENT NATIVE - RemotePC (GraalVM)
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
if exist build\client rmdir /s /q build\client
mkdir build\client

REM --- 3. Copy Java files ---
copy client\*.java build\client\

REM --- 4. Compile ---
echo [1/5] Compiling Java source files...
javac -d build\client build\client\*.java
if errorlevel 1 (
    echo FAILED: Compile error.
    pause
    exit /b 1
)
echo      OK.

REM --- 5. Create JAR ---
echo [2/5] Creating client.jar...
echo Main-Class: ClientGUI> build\client\MANIFEST.MF
cd build\client
jar cfm client.jar MANIFEST.MF *.class
if errorlevel 1 (
    echo FAILED: Could not create JAR.
    cd ..\..
    pause
    exit /b 1
)
cd ..\..
echo      OK.

REM --- 6. Copy reflection config ---
copy client\reflect-config.json build\client\

REM --- 7. Build native image ---
echo [3/5] Building native executable (this may take 3-5 minutes)...
cd build\client
native-image ^
  -jar client.jar ^
  -H:Name=RemotePC-Client ^
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

REM --- 8. Create output directory ---
if not exist dist\client_native mkdir dist\client_native
move build\client\RemotePC-Client.exe dist\client_native\

REM --- 9. Add icon ---
echo [4/5] Adding icon...
if exist icons\client.ico (
    where rcedit >nul 2>nul
    if not errorlevel 1 (
        rcedit dist\client_native\RemotePC-Client.exe --set-icon icons\client.ico
        echo      Icon added!
    ) else (
        echo      rcedit not found. Run: npm install -g rcedit
    )
) else (
    echo      Converting PNG to ICO...
    where magick >nul 2>nul
    if not errorlevel 1 (
        magick convert icons\client_256.png -define icon:auto-resize=256,128,64,48,32,16 icons\client.ico
        where rcedit >nul 2>nul
        if not errorlevel 1 (
            rcedit dist\client_native\RemotePC-Client.exe --set-icon icons\client.ico
            echo      Icon added!
        )
    ) else (
        echo      ImageMagick not found. No icon added.
    )
)

echo [5/5] Done!
echo.
echo ==> Result: dist\client_native\RemotePC-Client.exe
echo     Single standalone executable
echo     No JRE required!
echo.
pause
