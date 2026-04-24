@echo off
set SCRIPT_DIR=%~dp0
node "%SCRIPT_DIR%portable-server.mjs" --config "%SCRIPT_DIR%portable-config.json"
