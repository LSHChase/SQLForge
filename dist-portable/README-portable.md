# Portable Frontend Package

1. Edit `portable-config.json` if the backend services are not on localhost ports 8080-8083.
2. Start the package with `./start-portable.sh` on Linux/macOS or `start-portable.cmd` on Windows.
3. Open the printed local URL in a browser. No recompilation is required on the target host.

This package serves the prebuilt frontend assets and proxies `/api/*` calls to the configured backend services.