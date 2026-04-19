#!/bin/bash

echo "==================================================="
echo " BUILD SERVER - RemotePC"
echo "==================================================="
echo ""

# --- 1. Tao thu muc build ---
echo "[1/4] Preparing build directory..."
rm -rf build/server
mkdir -p build/server
echo "      ✓ OK"

# --- 2. Copy tat ca file .java vao build/server ---
echo "[2/4] Copying Java source files..."
cp server/*.java build/server/
echo "      ✓ OK"

# --- 3. Compile ---
echo "[3/4] Compiling Java source files..."
javac -d build/server build/server/*.java
if [ $? -ne 0 ]; then
    echo "FAILED: Compile error. Check if Java JDK is installed."
    exit 1
fi
echo "      ✓ OK"

# --- 4. Tao JAR ---
echo "[4/4] Creating server.jar..."
cat > build/server/MANIFEST.MF << EOF
Main-Class: Server
EOF

cd build/server
jar cfm server.jar MANIFEST.MF *.class
if [ $? -ne 0 ]; then
    echo "FAILED: Could not create JAR."
    cd ../..
    exit 1
fi
cd ../..
echo "      ✓ OK"

echo ""
echo "==================================================="
echo " ✓ BUILD SUCCESSFUL!"
echo "==================================================="
echo ""
echo "==> Result: build/server/server.jar"
echo "    Run it with: java -jar build/server/server.jar"
echo ""
echo "NOTE: Webcam feature works out-of-the-box!"
echo "      - Windows: Auto-downloads ffmpeg on first run"
echo "      - macOS:   Run 'brew install ffmpeg' (one-time)"
echo "      - Linux:   Run 'sudo apt install ffmpeg' (one-time)"
echo ""
