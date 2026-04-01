#!/bin/bash
echo "============================================"
echo "  Metamodel Coevolution Tool v1.0.0"
echo "============================================"

# Start Flask API
echo "[1/2] Starting Flask API..."
cd "$(dirname "$0")/../python/api"
python app.py &
API_PID=$!
sleep 3

# Start GUI
echo "[2/2] Starting GUI..."
cd "$(dirname "$0")/.."
java -Dapi.url=http://localhost:5000 -jar release/coevolution-gui.jar

# Cleanup
kill $API_PID 2>/dev/null
echo "[OK] Done."