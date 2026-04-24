import fs from 'node:fs'
import path from 'node:path'
import http from 'node:http'
import https from 'node:https'
import { fileURLToPath } from 'node:url'

import { buildProtectedProxyHeaders, resolveProxyDefaults } from './frontend-proxy-shared.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const DEFAULT_ROOT = path.resolve(__dirname, '..', 'dist-portable')
const MIME_TYPES = {
  '.css': 'text/css; charset=utf-8',
  '.html': 'text/html; charset=utf-8',
  '.ico': 'image/x-icon',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml; charset=utf-8',
  '.txt': 'text/plain; charset=utf-8'
}

const API_ROUTES = [
  {
    prefix: '/api/query-execution',
    configKey: 'queryExecutionBaseUrl',
    tenantId: 'tenant-a',
    requestPrefix: 'portable-query'
  },
  {
    prefix: '/api/sql-optimization',
    configKey: 'sqlOptimizationBaseUrl',
    tenantId: 'tenant-a',
    requestPrefix: 'portable-sql-optimization'
  },
  {
    prefix: '/api/benchmark-engine',
    configKey: 'benchmarkEngineBaseUrl',
    tenantId: 'tenant-a',
    requestPrefix: 'portable-benchmark'
  },
  {
    prefix: '/api/governance',
    configKey: 'governanceBaseUrl',
    tenantId: 'system',
    requestPrefix: 'portable-governance'
  }
]

const parseArgs = argv => {
  const result = {
    configPath: '',
    rootDir: DEFAULT_ROOT
  }

  for (let index = 2; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--config') {
      result.configPath = argv[index + 1] || ''
      index += 1
    } else if (arg === '--root') {
      result.rootDir = path.resolve(argv[index + 1] || DEFAULT_ROOT)
      index += 1
    }
  }

  return result
}

const loadJson = filePath => JSON.parse(fs.readFileSync(filePath, 'utf8'))

const loadConfig = ({ configPath, rootDir }) => {
  const resolvedConfigPath = configPath
    ? path.resolve(configPath)
    : path.join(rootDir, 'portable-config.json')
  const config = loadJson(resolvedConfigPath)
  return {
    ...config,
    configPath: resolvedConfigPath,
    rootDir
  }
}

const writeJson = (response, statusCode, payload) => {
  response.writeHead(statusCode, { 'Content-Type': 'application/json; charset=utf-8' })
  response.end(JSON.stringify(payload, null, 2))
}

const sendFile = (response, filePath) => {
  const ext = path.extname(filePath)
  response.writeHead(200, {
    'Content-Type': MIME_TYPES[ext] || 'application/octet-stream'
  })
  fs.createReadStream(filePath).pipe(response)
}

const safeJoin = (rootDir, requestPath) => {
  const normalizedPath = path.normalize(requestPath).replace(/^(\.\.[/\\])+/, '')
  return path.join(rootDir, normalizedPath)
}

const collectRequestBody = request =>
  new Promise((resolve, reject) => {
    const chunks = []
    request.on('data', chunk => chunks.push(chunk))
    request.on('end', () => resolve(Buffer.concat(chunks)))
    request.on('error', reject)
  })

const proxyRequest = async (request, response, routeConfig, config, proxyDefaults) => {
  const targetUrl = new URL(request.url, config[routeConfig.configKey])
  const protectedHeaders = buildProtectedProxyHeaders(
    request.headers,
    proxyDefaults,
    routeConfig.tenantId,
    routeConfig.requestPrefix
  )
  const headers = { ...request.headers }

  protectedHeaders.cleanupHeaderNames.forEach(headerName => {
    delete headers[headerName]
    delete headers[headerName.toLowerCase()]
  })
  Object.entries(protectedHeaders.headers).forEach(([headerName, headerValue]) => {
    headers[headerName] = headerValue
  })
  headers.host = targetUrl.host

  const body = await collectRequestBody(request)
  if (!body.length) {
    delete headers['content-length']
  } else {
    headers['content-length'] = String(body.length)
  }

  const transport = targetUrl.protocol === 'https:' ? https : http
  const proxy = transport.request(
    targetUrl,
    {
      method: request.method,
      headers
    },
    upstream => {
      response.writeHead(upstream.statusCode || 502, upstream.headers)
      upstream.pipe(response)
    }
  )

  proxy.on('error', error => {
    writeJson(response, 502, {
      error: 'PORTABLE_PROXY_FAILED',
      message: error.message,
      target: targetUrl.toString()
    })
  })

  if (body.length) {
    proxy.write(body)
  }
  proxy.end()
}

const createServer = config => {
  const proxyDefaults = resolveProxyDefaults(config)

  return http.createServer(async (request, response) => {
    const url = new URL(request.url, `http://${request.headers.host || '127.0.0.1'}`)
    const routeConfig = API_ROUTES.find(route => url.pathname.startsWith(route.prefix))

    if (url.pathname === '/__portable/health') {
      writeJson(response, 200, {
        status: 'UP',
        rootDir: config.rootDir,
        configPath: config.configPath
      })
      return
    }

    if (routeConfig) {
      await proxyRequest(request, response, routeConfig, config, proxyDefaults)
      return
    }

    const requestedPath = url.pathname === '/' ? '/index.html' : url.pathname
    const filePath = safeJoin(config.rootDir, requestedPath)
    if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
      sendFile(response, filePath)
      return
    }

    const indexPath = path.join(config.rootDir, 'index.html')
    if (fs.existsSync(indexPath)) {
      sendFile(response, indexPath)
      return
    }

    writeJson(response, 404, {
      error: 'PORTABLE_ASSET_NOT_FOUND',
      path: url.pathname
    })
  })
}

const main = () => {
  const args = parseArgs(process.argv)
  const config = loadConfig(args)
  const server = createServer(config)
  const host = config.host || '127.0.0.1'
  const port = Number(config.port || 3001)

  server.listen(port, host, () => {
    console.log(`Portable frontend available at http://${host}:${port}`)
  })
}

main()
