import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '..')
const distDir = path.join(root, 'dist-portable')
const serverSourcePath = path.join(__dirname, 'portable-frontend-server.mjs')
const proxyHelperSourcePath = path.join(__dirname, 'frontend-proxy-shared.mjs')
const serverTargetPath = path.join(distDir, 'portable-server.mjs')
const proxyHelperTargetPath = path.join(distDir, 'frontend-proxy-shared.mjs')
const configPath = path.join(distDir, 'portable-config.json')
const readmePath = path.join(distDir, 'README-portable.md')
const startShPath = path.join(distDir, 'start-portable.sh')
const startCmdPath = path.join(distDir, 'start-portable.cmd')

const ensureDir = targetPath => {
  fs.mkdirSync(targetPath, { recursive: true })
}

const writeExecutable = (filePath, content) => {
  fs.writeFileSync(filePath, content, 'utf8')
  fs.chmodSync(filePath, 0o755)
}

ensureDir(distDir)
fs.copyFileSync(serverSourcePath, serverTargetPath)
fs.copyFileSync(proxyHelperSourcePath, proxyHelperTargetPath)

const defaultConfig = {
  host: '127.0.0.1',
  port: 3001,
  userId: 'frontend-user',
  authSource: 'portable-frontend-proxy',
  ttlMs: 600000,
  queryExecutionBaseUrl: 'http://127.0.0.1:8081',
  sqlOptimizationBaseUrl: 'http://127.0.0.1:8082',
  benchmarkEngineBaseUrl: 'http://127.0.0.1:8083',
  governanceBaseUrl: 'http://127.0.0.1:8080'
}

fs.writeFileSync(configPath, `${JSON.stringify(defaultConfig, null, 2)}\n`, 'utf8')
fs.writeFileSync(
  readmePath,
  [
    '# Portable Frontend Package',
    '',
    '1. Edit `portable-config.json` if the backend services are not on localhost ports 8080-8083.',
    '2. Start the package with `./start-portable.sh` on Linux/macOS or `start-portable.cmd` on Windows.',
    '3. Open the printed local URL in a browser. No recompilation is required on the target host.',
    '',
    'This package serves the prebuilt frontend assets and proxies `/api/*` calls to the configured backend services.'
  ].join('\n'),
  'utf8'
)
writeExecutable(
  startShPath,
  `#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
node "$SCRIPT_DIR/portable-server.mjs" --config "$SCRIPT_DIR/portable-config.json"
`
)
fs.writeFileSync(
  startCmdPath,
  '@echo off\r\nset SCRIPT_DIR=%~dp0\r\nnode "%SCRIPT_DIR%portable-server.mjs" --config "%SCRIPT_DIR%portable-config.json"\r\n',
  'utf8'
)
