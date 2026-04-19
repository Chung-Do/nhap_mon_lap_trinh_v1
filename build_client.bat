@echo off
echo ===================================================
echo  BUILD CLIENT - RemotePC
echo ===================================================

REM --- 1. Tao thu muc build ---
if exist build\client rmdir /s /q build\client
mkdir build\client

REM --- 2. Copy tat ca file .java vao build\client ---
copy client\*.java build\client\

REM --- 3. Compile ---
echo [1/4] Compiling Java source files...
javac -d build\client build\client\*.java
if errorlevel 1 (
    echo FAILED: Compile error. Kiem tra Java JDK da cai chua.
    pause
    exit /b 1
)
echo      OK.

REM --- 4. Tao MANIFEST.MF ---
echo Main-Class: ClientGUI> build\client\MANIFEST.MF

REM --- 5. Tao JAR (phai o TRONG build\client\) ---
echo [2/4] Creating client.jar...
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

REM --- 6. Tao EXE bang jpackage (GUI app - khong co --win-console) ---
echo [3/4] Packaging to EXE (this may take a minute)...
if exist dist\client rmdir /s /q dist\client
jpackage ^
  --type app-image ^
  --name RemotePC-Client ^
  --input build\client ^
  --main-jar client.jar ^
  --main-class ClientGUI ^
  --dest dist\client
if errorlevel 1 (
    echo FAILED: jpackage error. Kiem tra JDK 14+ da cai chua.
    pause
    exit /b 1
)
echo      OK.

echo [4/4] Done!
echo.
echo ==> Ket qua: dist\client\RemotePC-Client\RemotePC-Client.exe
echo     Chay file do de khoi dong client GUI.
echo.
pause
