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

REM --- 6. Build native image with GraalVM ---
echo [3/4] Building native executable (this may take 2-3 minutes)...
if not exist dist\launcher mkdir dist\launcher

REM Check if native-image is available
where native-image >nul 2>nul
if errorlevel 1 (
    echo FAILED: GraalVM native-image not found.
    echo Please install GraalVM and run: gu install native-image
    pause
    exit /b 1
)

REM Build native image
cd build\launcher
native-image ^
  -jar launcher.jar ^
  -H:Name=RemotePC-Server-Installer ^
  -H:+ReportExceptionStackTraces ^
  --no-fallback ^
  --verbose

if errorlevel 1 (
    echo FAILED: native-image build error.
    cd ..\..
    pause
    exit /b 1
)

REM Move exe to dist
move RemotePC-Server-Installer.exe ..\..\dist\launcher\
cd ..\..
echo      OK.

REM --- 7. Add icon to EXE (optional) ---
echo [4/4] Adding icon...
if exist icons\server.ico (
    REM Use ResourceHacker if available
    where rcedit >nul 2>nul
    if not errorlevel 1 (
        rcedit dist\launcher\RemotePC-Server-Installer.exe --set-icon icons\server.ico
        echo      Icon added!
    ) else (
        echo      rcedit not found. Install via: npm install -g rcedit
        echo      EXE created without custom icon.
    )
) else (
    echo      No icon file found at icons\server.ico
)

echo.
echo Done!
echo.
echo ==> Result: dist\launcher\RemotePC-Server-Installer.exe
echo     Single standalone executable (~15MB)
echo     No JRE required!
echo.
pause
