#!/bin/bash
echo "==================================================="
echo " BUILD SERVER - RemotePC (Ubuntu/Linux)"
echo "==================================================="

# --- 1. Tao thu muc build ---
rm -rf build/server
mkdir -p build/server

# --- 2. Copy tat ca file .java vao build/server ---
cp server/*.java build/server/

# --- 3. Compile ---
echo "[1/4] Compiling Java source files..."
javac -d build/server build/server/*.java
if [ $? -ne 0 ]; then
    echo "FAILED: Compile error. Kiem tra Java JDK da cai chua."
    echo "  Ubuntu: sudo apt install openjdk-21-jdk"
    exit 1
fi
echo "      OK."

# --- 4. Tao MANIFEST.MF ---
echo "Main-Class: Server" > build/server/MANIFEST.MF

# --- 5. Tao JAR ---
echo "[2/4] Creating server.jar..."
cd build/server
jar cfm server.jar MANIFEST.MF *.class
if [ $? -ne 0 ]; then
    echo "FAILED: Could not create JAR."
    cd ../..
    exit 1
fi
cd ../..
echo "      OK."

# --- 6. Copy ffmpeg (neu co) ---
echo "[3/4] Checking ffmpeg..."
if [ -f "resources/ffmpeg/ffmpeg" ]; then
    mkdir -p build/server/ffmpeg_bundle
    cp resources/ffmpeg/ffmpeg build/server/ffmpeg_bundle/
    chmod +x build/server/ffmpeg_bundle/ffmpeg
    echo "      FFmpeg found and copied!"
elif command -v ffmpeg &> /dev/null; then
    echo "      FFmpeg found in system PATH. OK."
else
    echo "      FFmpeg not found."
    echo "      Server se tu dong download lan dau chay."
    echo "      Hoac cai bang: sudo apt install ffmpeg"
fi

# --- 7. Tao app bang jpackage (neu co) ---
echo "[4/4] Packaging..."
if command -v jpackage &> /dev/null; then
    echo "      jpackage found. Creating native app..."
    rm -rf dist/server
    jpackage \
      --type app-image \
      --name RemotePC-Server \
      --input build/server \
      --main-jar server.jar \
      --main-class Server \
      --dest dist/server
    if [ $? -eq 0 ]; then
        echo "      OK."
        echo ""
        echo "==> App:  dist/server/RemotePC-Server/bin/RemotePC-Server"
    else
        echo "      jpackage failed. Fallback to JAR."
    fi
else
    echo "      jpackage not found. Skipping native packaging."
    echo "      (Install JDK 14+ for jpackage support)"
fi

echo ""
echo "==> JAR:  build/server/server.jar"
echo "    Chay: java -jar build/server/server.jar"
echo ""
echo "LUU Y: App nay duoc thiet ke cho Windows."
echo "  Mot so tinh nang se KHONG hoat dong tren Linux:"
echo "  - Keylogger (dung PowerShell + GetAsyncKeyState)"
echo "  - App Manager (dung PowerShell Get-Process)"
echo "  - Shutdown/Restart (dung 'shutdown /s /t 5')"
echo "  - System Tray icon (co the loi tren mot so Linux DE)"
echo ""
echo "  Cac tinh nang VAN hoat dong tren Linux:"
echo "  - Ket noi TCP/IP"
echo "  - Screenshot (java.awt.Robot)"
echo "  - File Transfer"
echo "  - Remote Desktop"
echo "  - Network Diagnostics"
echo "  - Freeze Screen"
echo ""
