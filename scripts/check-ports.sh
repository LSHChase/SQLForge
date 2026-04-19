#!/bin/bash

set -euo pipefail

for port in 3306 6379 9092 9000 8080 3000; do
    if lsof -Pi :"$port" -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo "[ERROR] 端口 $port 被占用"
        exit 1
    fi
done

echo "[OK] 所有端口可用"
