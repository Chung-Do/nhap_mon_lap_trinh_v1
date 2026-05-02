#!/bin/bash
echo "==================================================="
echo " BUILD CLIENT - RemotePC (Ubuntu/Linux)"
echo "==================================================="

# --- 1. Tao thu muc build ---
rm -rf build/client
mkdir -p build/client

# --- 2. Copy tat ca file .java vao build/client ---
cp client/*.java build/client/

# --- 3. Compile ---
echo "[1/3] Compiling Java source files..."
javac -d build/client build/client/*.java
if [ $? -ne 0 ]; then
    echo "FAILED: Compile error. Kiem tra Java JDK da cai chua."
    echo "  Ubuntu: sudo apt install openjdk-21-jdk"
    exit 1
fi
echo "      OK."

# --- 4. Tao MANIFEST.MF ---
echo "Main-Class: ClientGUI" > build/client/MANIFEST.MF

# --- 5. Tao JAR ---
echo "[2/3] Creating client.jar..."
cd build/client
jar cfm client.jar MANIFEST.MF *.class
if [ $? -ne 0 ]; then
    echo "FAILED: Could not create JAR."
    cd ../..
    exit 1
fi
cd ../..
echo "      OK."

# --- 6. Tao app bang jpackage (neu co) ---
echo "[3/3] Packaging..."
if command -v jpackage &> /dev/null; then
    echo "      jpackage found. Creating native app..."
    rm -rf dist/client
    jpackage \
      --type app-image \
      --name RemotePC-Client \
      --input build/client \
      --main-jar client.jar \
      --main-class ClientGUI \
      --dest dist/client
    if [ $? -eq 0 ]; then
        echo "      OK."
        echo ""
        echo "==> App:  dist/client/RemotePC-Client/bin/RemotePC-Client"
    else
        echo "      jpackage failed. Fallback to JAR."
    fi
else
    echo "      jpackage not found. Skipping native packaging."
    echo "      (Install JDK 14+ for jpackage support)"
fi

echo ""
echo "==> JAR:  build/client/client.jar"
echo "    Chay: java -jar build/client/client.jar"
echo ""
