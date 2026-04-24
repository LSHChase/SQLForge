import fs from 'node:fs'
import http from 'node:http'
import os from 'node:os'
import path from 'node:path'
import { spawn } from 'node:child_process'
import { chromium } from 'playwright'

const defaultTimeoutMs = 20000
const browserCandidates = [
  process.env.FRONTEND_RUNTIME_BROWSER_BIN,
  '/usr/bin/google-chrome-stable',
  '/usr/bin/google-chrome',
  '/opt/google/chrome/chrome',
  '/usr/bin/chromium-browser',
  '/usr/bin/chromium'
].filter(Boolean)

const sleep = delayMs => new Promise(resolve => setTimeout(resolve, delayMs))

const assert = (condition, message) => {
  if (!condition) {
    throw new Error(message)
  }
}

const resolveExecutablePath = () => browserCandidates.find(candidate => fs.existsSync(candidate))

const httpGetText = async url => {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`Request failed for ${url}: ${response.status} ${response.statusText}`)
  }
  return response.text()
}

const waitForHealth = async url => {
  const startedAt = Date.now()
  while (Date.now() - startedAt < defaultTimeoutMs) {
    try {
      const body = await httpGetText(url)
      if (body.includes('"status": "UP"')) {
        return
      }
    } catch (error) {
      if (Date.now() - startedAt >= defaultTimeoutMs) {
        throw error
      }
    }
    await sleep(250)
  }
  throw new Error(`Portable frontend did not become healthy at ${url}`)
}

const getAvailablePort = () =>
  new Promise((resolve, reject) => {
    const server = http.createServer()
    server.listen(0, '127.0.0.1', () => {
      const address = server.address()
      server.close(error => {
        if (error) {
          reject(error)
          return
        }
        resolve(address.port)
      })
    })
    server.on('error', reject)
  })

const readJsonBody = request =>
  new Promise((resolve, reject) => {
    const chunks = []
    request.on('data', chunk => chunks.push(chunk))
    request.on('end', () => {
      const text = Buffer.concat(chunks).toString('utf8').trim()
      resolve(text ? JSON.parse(text) : {})
    })
    request.on('error', reject)
  })

const writeJson = (response, statusCode, payload) => {
  response.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8'
  })
  response.end(JSON.stringify(payload))
}

const requireProxyHeaders = request => {
  const requiredHeaders = [
    'x-tenant-id',
    'x-user-id',
    'x-role-codes',
    'x-request-id',
    'x-trace-id',
    'x-auth-source'
  ]

  requiredHeaders.forEach(headerName => {
    assert(request.headers[headerName], `Portable proxy did not forward required header ${headerName}`)
  })
  assert(
    request.headers['x-auth-source'] === 'portable-frontend-proxy',
    `Unexpected auth source ${request.headers['x-auth-source']}`
  )
}

const startMockBackend = async port => {
  let messageStatsCalls = 0
  const server = http.createServer(async (request, response) => {
    try {
      if (request.url.startsWith('/api/query-execution/queries/execute')) {
        requireProxyHeaders(request)
        const payload = await readJsonBody(request)
        if (payload.queryContext?.timeoutMs) {
          writeJson(response, 200, {
            status: 'PARTIAL',
            degraded: true,
            rows: [{ orderId: 'degraded-order-1', totalAmount: 18.4 }],
            retryPath: ['HETU', 'HIVE'],
            metadata: {
              targetEngine: 'HIVE'
            },
            errorMessage: 'Timeout recovered through fallback'
          })
          return
        }
        writeJson(response, 200, {
          status: 'SUCCESS',
          degraded: false,
          rows: [{ orderId: 'success-order-1', totalAmount: 42.8 }],
          retryPath: [],
          metadata: {
            targetEngine: 'HETU'
          }
        })
        return
      }

      if (request.url.startsWith('/api/governance/admin/messages/stats')) {
        requireProxyHeaders(request)
        messageStatsCalls += 1
        if (messageStatsCalls === 1) {
          writeJson(response, 200, { pending: 0, total: 0, failed: 0 })
          return
        }
        writeJson(response, 200, { pending: 1, total: 1, failed: 1 })
        return
      }

      writeJson(response, 404, {
        error: 'MOCK_ENDPOINT_NOT_FOUND',
        path: request.url
      })
    } catch (error) {
      writeJson(response, 500, {
        error: 'MOCK_BACKEND_FAILED',
        message: error.message
      })
    }
  })

  await new Promise((resolve, reject) => {
    server.listen(port, '127.0.0.1', resolve)
    server.on('error', reject)
  })

  return server
}

const createPortableConfig = (portablePort, backendPort) => {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'sqlforge-portable-smoke-'))
  const configPath = path.join(tempDir, 'portable-config.json')
  const backendBaseUrl = `http://127.0.0.1:${backendPort}`
  fs.writeFileSync(
    configPath,
    `${JSON.stringify(
      {
        host: '127.0.0.1',
        port: portablePort,
        userId: 'frontend-operator',
        roleCodes: 'TENANT_ADMIN,OPERATOR',
        authSource: 'portable-frontend-proxy',
        ttlMs: 600000,
        queryExecutionBaseUrl: backendBaseUrl,
        sqlOptimizationBaseUrl: backendBaseUrl,
        benchmarkEngineBaseUrl: backendBaseUrl,
        governanceBaseUrl: backendBaseUrl
      },
      null,
      2
    )}\n`,
    'utf8'
  )
  return configPath
}

const waitForPortableServer = async baseUrl => {
  await waitForHealth(`${baseUrl}/__portable/health`)
  const indexBody = await httpGetText(`${baseUrl}/`)
  if (!indexBody.includes('./assets/')) {
    throw new Error('Portable index.html does not use relative asset paths.')
  }
}

const runBrowserSmoke = async baseUrl => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: resolveExecutablePath()
  })
  const page = await browser.newPage()

  try {
    await page.goto(`${baseUrl}/#/dashboard`, { waitUntil: 'networkidle' })
    await page.locator('.dashboard-page').waitFor({ timeout: defaultTimeoutMs })

    await page.goto(`${baseUrl}/#/sql-query`, { waitUntil: 'networkidle' })
    await page.getByTestId('query-flow-page').waitFor({ timeout: defaultTimeoutMs })
    await page.getByTestId('query-flow-submit').click()
    await page.getByTestId('query-flow-status').waitFor({ timeout: defaultTimeoutMs })
    await page.getByTestId('query-flow-status').waitFor({ state: 'visible', timeout: defaultTimeoutMs })
    await expectText(page, 'query-flow-status', 'SUCCESS')
    await expectText(page, 'query-flow-engine', 'HETU')

    await page.getByTestId('query-flow-submit-recovery').click()
    await expectText(page, 'query-flow-status', 'PARTIAL')
    await expectText(page, 'query-flow-engine', 'HIVE')
    await expectText(page, 'query-flow-compensation-status', 'COMPENSATED')
    await expectText(page, 'query-flow-queue-total-delta', '1')

    await page.goto(`${baseUrl}/#/governance/ops/runtime-gates`, { waitUntil: 'networkidle' })
    await page.getByTestId('runtime-gates-page').waitFor({ timeout: defaultTimeoutMs })
    assert(
      page.url().includes('#/governance/ops/runtime-gates'),
      `Expected portable runtime-gates hash route, got ${page.url()}`
    )

    await page.goto(`${baseUrl}/#/governance/ops/recovery-drill`, { waitUntil: 'networkidle' })
    await page.getByTestId('recovery-drill-page').waitFor({ timeout: defaultTimeoutMs })
    assert(
      page.url().includes('#/governance/ops/recovery-drill'),
      `Expected portable recovery-drill hash route, got ${page.url()}`
    )
  } finally {
    await page.close()
    await browser.close()
  }
}

const expectText = async (page, testId, expectedText) => {
  const locator = page.getByTestId(testId)
  await locator.waitFor({ timeout: defaultTimeoutMs })
  const startedAt = Date.now()

  while (Date.now() - startedAt < defaultTimeoutMs) {
    const currentText = (await locator.textContent())?.trim() || ''
    if (currentText.includes(expectedText)) {
      return
    }
    await page.waitForTimeout(200)
  }

  throw new Error(`Expected ${testId} to include "${expectedText}"`)
}

const stopChild = async child => {
  if (child.exitCode !== null || child.signalCode !== null) {
    return
  }
  child.kill('SIGINT')
  await new Promise(resolve => child.once('exit', resolve))
}

const main = async () => {
  const portablePort = await getAvailablePort()
  const backendPort = await getAvailablePort()
  const configPath = createPortableConfig(portablePort, backendPort)
  const mockBackend = await startMockBackend(backendPort)
  const child = spawn(
    'node',
    ['dist-portable/portable-server.mjs', '--config', configPath],
    {
      stdio: ['ignore', 'pipe', 'pipe']
    }
  )

  let stderr = ''
  child.stderr.on('data', chunk => {
    stderr += chunk.toString()
  })

  try {
    const baseUrl = `http://127.0.0.1:${portablePort}`
    await waitForPortableServer(baseUrl)
    await runBrowserSmoke(baseUrl)
  } finally {
    await stopChild(child)
    await new Promise(resolve => mockBackend.close(resolve))
  }

  if (stderr.trim()) {
    console.error(stderr.trim())
  }

  process.stdout.write('Portable frontend browser smoke passed\n')
}

main().catch(error => {
  console.error(error.message)
  process.exit(1)
})
