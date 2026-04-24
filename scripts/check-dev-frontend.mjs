import fs from 'node:fs'
import http from 'node:http'
import path from 'node:path'
import { spawn } from 'node:child_process'
import { chromium } from 'playwright'
import { ROUTE_PATHS } from '../src/config/routePaths.mjs'

const defaultTimeoutMs = Number(process.env.FRONTEND_DEV_SMOKE_TIMEOUT_MS || 20000)
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

const httpGetText = async url => {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`Request failed for ${url}: ${response.status} ${response.statusText}`)
  }
  return response.text()
}

const waitForDevServer = async baseUrl => {
  const startedAt = Date.now()

  while (Date.now() - startedAt < defaultTimeoutMs) {
    try {
      const viteClient = await httpGetText(`${baseUrl}/@vite/client`)
      const indexBody = await httpGetText(`${baseUrl}/`)
      assert(viteClient.includes('createHotContext'), 'Vite client is missing expected HMR runtime.')
      assert(indexBody.includes('/@vite/client'), 'Dev index.html is missing the Vite client entry.')
      assert(indexBody.includes('/src/main.js'), 'Dev index.html is missing the source entrypoint.')
      return
    } catch (error) {
      if (Date.now() - startedAt >= defaultTimeoutMs) {
        throw error
      }
    }
    await sleep(250)
  }

  throw new Error(`Frontend dev server did not become ready at ${baseUrl}`)
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

const parseJsonBody = request => {
  const text = request.postData() || ''
  return text ? JSON.parse(text) : {}
}

const assertDevHeaders = (request, expectedTenantId, expectedPrefixes) => {
  const headers = request.headers()
  assert(
    headers['x-sqlforge-dev-tenant-id'] === expectedTenantId,
    `Unexpected dev tenant header ${headers['x-sqlforge-dev-tenant-id']}`
  )
  assert(
    expectedPrefixes.includes(headers['x-sqlforge-dev-request-prefix']),
    `Unexpected dev request prefix ${headers['x-sqlforge-dev-request-prefix']}`
  )
}

const waitForChildExit = child =>
  new Promise(resolve => {
    child.once('exit', () => resolve())
  })

const stopChild = async child => {
  if (child.exitCode !== null || child.signalCode !== null) {
    return
  }
  const exitPromise = waitForChildExit(child)
  child.kill('SIGINT')
  let exited = await Promise.race([exitPromise.then(() => true), sleep(2000).then(() => false)])
  if (exited) {
    return
  }
  child.kill('SIGTERM')
  exited = await Promise.race([exitPromise.then(() => true), sleep(2000).then(() => false)])
  if (exited) {
    return
  }
  child.kill('SIGKILL')
  await exitPromise
}

const runBrowserSmoke = async baseUrl => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: resolveExecutablePath()
  })
  const page = await browser.newPage()
  const pageErrors = []
  const unexpectedApiRequests = []
  let governanceStatsCalls = 0

  page.on('pageerror', error => {
    pageErrors.push(error.message)
  })

  await page.route('**/api/**', async route => {
    const request = route.request()
    const { pathname } = new URL(request.url())

    if (pathname === '/api/query-execution/queries/execute') {
      assertDevHeaders(request, 'tenant-a', ['frontend-query'])
      const payload = parseJsonBody(request)
      const isRecoveryFlow = Boolean(payload.queryContext?.timeoutMs)
      await route.fulfill({
        status: 200,
        contentType: 'application/json; charset=utf-8',
        body: JSON.stringify(
          isRecoveryFlow
            ? {
                status: 'PARTIAL',
                degraded: true,
                implementationStage: 'DEV_BROWSER_SMOKE',
                degradeReason: 'Mock timeout fallback',
                rows: [{ orderId: 'dev-recovery-order-1', totalAmount: 18.4 }],
                retryPath: [
                  { engine: 'HETU', resultStatus: 'TIMEOUT', elapsedMs: 30 },
                  { engine: 'HIVE', resultStatus: 'SUCCESS', elapsedMs: 18 }
                ],
                sqlFingerprint: 'query-recovery-fingerprint',
                metadata: {
                  targetEngine: 'HIVE'
                }
              }
            : {
                status: 'SUCCESS',
                degraded: false,
                implementationStage: 'DEV_BROWSER_SMOKE',
                degradeReason: '',
                rows: [{ orderId: 'dev-success-order-1', totalAmount: 42.8 }],
                retryPath: [],
                sqlFingerprint: 'query-success-fingerprint',
                metadata: {
                  targetEngine: 'HETU'
                }
              }
        )
      })
      return
    }

    if (pathname === '/api/governance/admin/messages/stats') {
      assertDevHeaders(request, 'tenant-a', [
        'frontend-query-governance-stats-before',
        'frontend-query-governance-stats-after'
      ])
      governanceStatsCalls += 1
      await route.fulfill({
        status: 200,
        contentType: 'application/json; charset=utf-8',
        body: JSON.stringify(
          governanceStatsCalls === 1
            ? { pending: 0, total: 0, failed: 0 }
            : { pending: 1, total: 1, failed: 1 }
        )
      })
      return
    }

    unexpectedApiRequests.push(`${request.method()} ${pathname}`)
    await route.fulfill({
      status: 404,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify({
        error: 'UNEXPECTED_DEV_SMOKE_ENDPOINT',
        path: pathname
      })
    })
  })

  try {
    await page.goto(`${baseUrl}${ROUTE_PATHS.dashboard}`, { waitUntil: 'domcontentloaded' })
    await page.locator('.dashboard-page').waitFor({ timeout: defaultTimeoutMs })
    assert(
      page.url().includes(ROUTE_PATHS.dashboard) && !page.url().includes('#'),
      `Expected history-mode dashboard route, got ${page.url()}`
    )

    await page.goto(`${baseUrl}${ROUTE_PATHS.runtimeGates}`, { waitUntil: 'domcontentloaded' })
    await page.getByTestId('runtime-gates-page').waitFor({ timeout: defaultTimeoutMs })
    assert(
      page.url().includes(ROUTE_PATHS.runtimeGates) && !page.url().includes('#'),
      `Expected history-mode runtime-gates route, got ${page.url()}`
    )

    await page.goto(`${baseUrl}${ROUTE_PATHS.recoveryDrill}`, { waitUntil: 'domcontentloaded' })
    await page.getByTestId('recovery-drill-page').waitFor({ timeout: defaultTimeoutMs })
    assert(
      page.url().includes(ROUTE_PATHS.recoveryDrill) && !page.url().includes('#'),
      `Expected history-mode recovery-drill route, got ${page.url()}`
    )

    await page.goto(`${baseUrl}${ROUTE_PATHS.sqlQuery}`, { waitUntil: 'domcontentloaded' })
    await page.getByTestId('query-flow-page').waitFor({ timeout: defaultTimeoutMs })

    await page.getByTestId('query-flow-submit').click()
    await expectText(page, 'query-flow-status', 'SUCCESS')
    await expectText(page, 'query-flow-engine', 'HETU')
    await expectText(page, 'query-flow-degraded', 'false')

    await page.getByTestId('query-flow-submit-recovery').click()
    await expectText(page, 'query-flow-status', 'PARTIAL')
    await expectText(page, 'query-flow-engine', 'HIVE')
    await expectText(page, 'query-flow-degraded', 'true')
    await expectText(page, 'query-flow-retry-path-size', '2')
    await expectText(page, 'query-flow-compensation-status', 'COMPENSATED')
    await expectText(page, 'query-flow-queue-total-delta', '1')

    assert(governanceStatsCalls === 2, `Expected 2 governance stats calls, got ${governanceStatsCalls}`)
    assert(unexpectedApiRequests.length === 0, `Unexpected API requests: ${unexpectedApiRequests.join(', ')}`)
    assert(pageErrors.length === 0, `Frontend dev smoke saw page errors: ${pageErrors.join(' | ')}`)
  } finally {
    await page.close()
    await browser.close()
  }
}

const main = async () => {
  const devPort = await getAvailablePort()
  const baseUrl = `http://127.0.0.1:${devPort}`
  const viteBinPath = path.join(process.cwd(), 'node_modules', 'vite', 'bin', 'vite.js')
  const child = spawn('node', [viteBinPath, '--host', '127.0.0.1', '--port', String(devPort)], {
    stdio: ['ignore', 'pipe', 'pipe']
  })

  let stdout = ''
  let stderr = ''
  child.stdout.on('data', chunk => {
    stdout += chunk.toString()
  })
  child.stderr.on('data', chunk => {
    stderr += chunk.toString()
  })

  try {
    await waitForDevServer(baseUrl)
    await runBrowserSmoke(baseUrl)
  } catch (error) {
    const processOutput = [stdout.trim(), stderr.trim()].filter(Boolean).join('\n')
    if (processOutput) {
      console.error(processOutput)
    }
    throw error
  } finally {
    await stopChild(child)
  }

  process.stdout.write('Frontend dev browser smoke passed\n')
}

main().catch(error => {
  console.error(error.message)
  process.exit(1)
})
