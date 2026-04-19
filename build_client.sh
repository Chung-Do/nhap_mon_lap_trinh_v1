#!/bin/bash

echo "==================================================="
echo " BUILD CLIENT - RemotePC"
echo "==================================================="
echo ""

# --- 1. Tao thu muc build ---
echo "[1/4] Preparing build directory..."
rm -rf build/client
mkdir -p build/client
echo "      ✓ OK"

# --- 2. Copy tat ca file .java vao build/client ---
echo "[2/4] Copying Java source files..."
cp client/*.java build/client/
echo "      ✓ OK"

# --- 3. Compile ---
echo "[3/4] Compiling Java source files..."
javac -d build/client build/client/*.java
if [ $? -ne 0 ]; then
    echo "FAILED: Compile error."
    echo "Check if Java JDK is installed."
    exit 1
fi
echo "      ✓ OK"

# --- 4. Tao JAR ---
echo "[4/4] Creating client.jar..."
cat > build/client/MANIFEST.MF << EOF
Main-Class: Client
EOF

cd build/client
jar cfm client.jar MANIFEST.MF *.class
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
echo "==> Result: build/client/client.jar"
echo "    Run it with: java -jar build/client/client.jar"
echo ""
