import fs from 'node:fs'
import { execFileSync } from 'node:child_process'
import { chromium } from 'playwright'

const frontendBaseUrl = process.env.FRONTEND_BASE_URL || 'http://127.0.0.1:3000'
const defaultTimeoutMs = Number(process.env.FRONTEND_RUNTIME_SMOKE_TIMEOUT_MS || 60000)
const browserCandidates = [
  process.env.FRONTEND_RUNTIME_BROWSER_BIN,
  '/usr/bin/google-chrome-stable',
  '/usr/bin/google-chrome',
  '/opt/google/chrome/chrome',
  '/usr/bin/chromium-browser',
  '/usr/bin/chromium'
].filter(Boolean)
const mysqlContainer = process.env.MYSQL_CONTAINER || 'sqlforge-mysql'
const mysqlDatabase = process.env.MYSQL_DATABASE || 'sqlforge'
const mysqlUser = process.env.MYSQL_USER || 'sqlforge'
const mysqlPassword = process.env.MYSQL_PASSWORD || 'sqlforge'

const assert = (condition, message) => {
  if (!condition) {
    throw new Error(message)
  }
}

const sqlEscape = value => String(value).replace(/\\/g, '\\\\').replace(/'/g, "\\'")

const mysqlExec = sql =>
  execFileSync(
    'docker',
    [
      'exec',
      mysqlContainer,
      'sh',
      '-lc',
      `mysql -N -B -u${mysqlUser} -p${mysqlPassword} ${mysqlDatabase} -e "${sql.replace(/"/g, '\\"')}"`
    ],
    { encoding: 'utf8' }
  ).trim()

const seedFailedGovernanceMessage = () => {
  const traceId = `frontend-system-smoke-${Date.now()}`
  const messageBody = sqlEscape(JSON.stringify({ event: 'frontend-system-smoke', traceId }))
  const headers = sqlEscape(JSON.stringify({ source: 'frontend-runtime-smoke', traceId }))
  const sql =
    `INSERT INTO kafka_message_queue (topic, partition_key, message_body, headers, status, retry_count, error_log) ` +
    `VALUES ('manual.smoke', 'tenant-manual', '${messageBody}', '${headers}', 'FAILED', 3, 'frontend runtime smoke setup'); ` +
    'SELECT LAST_INSERT_ID();'
  const seededId = mysqlExec(sql).split('\n').filter(Boolean).pop()
  assert(seededId, 'Failed to seed governance failed message')
  return { seededId, traceId }
}

const cleanupGovernanceMessage = seededId => {
  if (!seededId) {
    return
  }
  mysqlExec(`DELETE FROM kafka_message_queue WHERE id = ${Number(seededId)};`)
}

const resolveExecutablePath = () => browserCandidates.find(candidate => fs.existsSync(candidate))

const readText = async (page, testId) => {
  const locator = page.getByTestId(testId)
  await locator.waitFor({ timeout: defaultTimeoutMs })
  return (await locator.textContent())?.trim() || ''
}

const expectText = async (page, testId, expectedText) => {
  const text = await readText(page, testId)
  assert(text.includes(expectedText), `Expected ${testId} to include "${expectedText}", got "${text}"`)
  return text
}

const expectNumber = async (page, testId) => {
  const text = await readText(page, testId)
  const value = Number(text)
  assert(Number.isFinite(value), `Expected ${testId} to be numeric, got "${text}"`)
  return value
}

const expectNumberAtLeast = async (page, testId, minimum) => {
  const value = await expectNumber(page, testId)
  assert(value >= minimum, `Expected ${testId} to be >= ${minimum}, got ${value}`)
  return value
}

const waitForPost = (page, pathFragment) =>
  page.waitForResponse(
    response => response.url().includes(pathFragment) && response.request().method() === 'POST',
    { timeout: defaultTimeoutMs }
  )

const waitForGet = (page, pathFragment) =>
  page.waitForResponse(
    response => response.url().includes(pathFragment) && response.request().method() === 'GET',
    { timeout: defaultTimeoutMs }
  )

const runQueryFlow = async page => {
  await page.goto(`${frontendBaseUrl}/sql-query`, { waitUntil: 'networkidle' })
  await page.getByTestId('query-flow-page').waitFor({ timeout: defaultTimeoutMs })

  const successResponsePromise = waitForPost(page, '/api/query-execution/queries/execute')
  await page.getByTestId('query-flow-submit').click()
  const successResponse = await successResponsePromise
  const successPayload = await successResponse.json()

  assert(successResponse.status() === 200, `Query success flow returned HTTP ${successResponse.status()}`)
  assert(successPayload.status === 'SUCCESS', `Query success flow returned unexpected status ${successPayload.status}`)

  await expectText(page, 'query-flow-status', 'SUCCESS')
  await expectText(page, 'query-flow-engine', 'HETU')
  await expectText(page, 'query-flow-degraded', 'false')

  const recoveryResponsePromise = waitForPost(page, '/api/query-execution/queries/execute')
  await page.getByTestId('query-flow-submit-recovery').click()
  const recoveryResponse = await recoveryResponsePromise
  const recoveryPayload = await recoveryResponse.json()

  assert(recoveryResponse.status() === 200, `Query recovery flow returned HTTP ${recoveryResponse.status()}`)
  assert(recoveryPayload.status === 'PARTIAL', `Query recovery flow returned unexpected status ${recoveryPayload.status}`)
  assert(recoveryPayload.degraded === true, 'Query recovery flow did not mark degraded=true')
  assert(recoveryPayload.metadata?.targetEngine === 'HIVE', 'Query recovery flow did not fallback to HIVE')
  assert(
    Array.isArray(recoveryPayload.retryPath) && recoveryPayload.retryPath.length === 2,
    `Query recovery flow retryPath length mismatch: ${JSON.stringify(recoveryPayload.retryPath)}`
  )

  await expectText(page, 'query-flow-status', 'PARTIAL')
  await expectText(page, 'query-flow-engine', 'HIVE')
  await expectText(page, 'query-flow-degraded', 'true')
  await expectText(page, 'query-flow-retry-path-size', '2')
  await expectText(page, 'query-flow-compensation-status', 'COMPENSATED')
  await expectNumberAtLeast(page, 'query-flow-queue-total-delta', 1)
}

const runOptimizationFlow = async page => {
  await page.goto(`${frontendBaseUrl}/acceleration`, { waitUntil: 'networkidle' })
  await page.getByTestId('optimization-flow-page').waitFor({ timeout: defaultTimeoutMs })

  const successSubmitResponsePromise = waitForPost(page, '/api/sql-optimization/tasks')
  await page.getByTestId('optimization-flow-submit').click()
  const successSubmitResponse = await successSubmitResponsePromise
  const successSubmitPayload = await successSubmitResponse.json()

  assert(successSubmitResponse.status() === 200, `Optimization submit returned HTTP ${successSubmitResponse.status()}`)
  assert(
    successSubmitPayload.status === 'QUEUED',
    `Optimization submit returned unexpected status ${successSubmitPayload.status}`
  )

  await expectText(page, 'optimization-flow-status', 'SUCCEEDED')
  const summaryText = await readText(page, 'optimization-flow-summary')
  assert(summaryText.length > 0, 'Optimization summary should not be empty')

  const failureSubmitResponsePromise = waitForPost(page, '/api/sql-optimization/tasks')
  await page.getByTestId('optimization-flow-submit-failure').click()
  const failureSubmitResponse = await failureSubmitResponsePromise
  const failureSubmitPayload = await failureSubmitResponse.json()

  assert(
    failureSubmitResponse.status() === 200,
    `Optimization failure submit returned HTTP ${failureSubmitResponse.status()}`
  )
  assert(
    failureSubmitPayload.status === 'QUEUED',
    `Optimization failure submit returned unexpected status ${failureSubmitPayload.status}`
  )

  await expectText(page, 'optimization-flow-status', 'FAILED')
  await expectText(page, 'optimization-flow-failure-code', '13000')
  await expectText(page, 'optimization-flow-compensation-status', 'FAILED')
  await expectText(page, 'optimization-flow-compensation-indicator', 'COMPENSATED')
  await expectNumberAtLeast(page, 'optimization-flow-queue-total-delta', 1)
}

const runBenchmarkFlow = async page => {
  await page.goto(`${frontendBaseUrl}/benchmark`, { waitUntil: 'networkidle' })
  await page.getByTestId('benchmark-flow-page').waitFor({ timeout: defaultTimeoutMs })

  const successSubmitResponsePromise = waitForPost(page, '/api/benchmark-engine/tasks')
  const reportResponsePromise = waitForGet(page, '/api/benchmark-engine/reports/')

  await page.getByTestId('benchmark-flow-submit').click()
  const successSubmitResponse = await successSubmitResponsePromise
  const successSubmitPayload = await successSubmitResponse.json()

  assert(successSubmitResponse.status() === 200, `Benchmark submit returned HTTP ${successSubmitResponse.status()}`)
  assert(successSubmitPayload.status === 'QUEUED', `Benchmark submit returned unexpected status ${successSubmitPayload.status}`)

  await expectText(page, 'benchmark-flow-status', 'SUCCEEDED')
  const reportIdText = await expectText(page, 'benchmark-flow-report-id', 'report-')
  assert(reportIdText.startsWith('report-'), `Benchmark report id is invalid: ${reportIdText}`)

  const reportResponse = await reportResponsePromise
  const reportPayload = await reportResponse.json()

  assert(reportResponse.status() === 200, `Benchmark report returned HTTP ${reportResponse.status()}`)
  assert(reportPayload.requestedFormat === 'JSON', `Benchmark report format mismatch: ${reportPayload.requestedFormat}`)
  await page.getByTestId('benchmark-flow-report').waitFor({ timeout: defaultTimeoutMs })

  const failureSubmitResponsePromise = waitForPost(page, '/api/benchmark-engine/tasks')
  await page.getByTestId('benchmark-flow-submit-failure').click()
  const failureSubmitResponse = await failureSubmitResponsePromise
  const failureSubmitPayload = await failureSubmitResponse.json()

  assert(
    failureSubmitResponse.status() === 200,
    `Benchmark failure submit returned HTTP ${failureSubmitResponse.status()}`
  )
  assert(
    failureSubmitPayload.status === 'QUEUED',
    `Benchmark failure submit returned unexpected status ${failureSubmitPayload.status}`
  )

  await expectText(page, 'benchmark-flow-status', 'FAILED')
  await expectText(page, 'benchmark-flow-failure-code', '14000')
  await expectText(page, 'benchmark-flow-compensation-status', 'FAILED')
  await expectText(page, 'benchmark-flow-compensation-indicator', 'COMPENSATED')
  await expectNumberAtLeast(page, 'benchmark-flow-queue-total-delta', 1)
}

const runSystemFlow = async page => {
  const { seededId } = seedFailedGovernanceMessage()

  try {
    await page.goto(`${frontendBaseUrl}/system`, { waitUntil: 'networkidle' })
    await page.getByTestId('system-flow-page').waitFor({ timeout: defaultTimeoutMs })

    await expectText(page, 'system-flow-tenant-config-status', 'system')
    await expectText(page, 'system-flow-audit-level', 'NORMAL')
    await expectNumberAtLeast(page, 'system-flow-queue-failed', 1)

    const retryResponsePromise = waitForPost(page, '/api/governance/admin/messages/retry')
    await page.getByTestId('system-flow-retry').click()
    const retryResponse = await retryResponsePromise
    const retryPayload = await retryResponse.json()

    assert(retryResponse.status() === 200, `Governance retry returned HTTP ${retryResponse.status()}`)
    assert(retryPayload.status === 'ACCEPTED', `Governance retry returned unexpected status ${retryPayload.status}`)
    assert(retryPayload.retriedCount >= 1, `Expected retriedCount >= 1, got ${retryPayload.retriedCount}`)

    await expectText(page, 'system-flow-retry-status', 'ACCEPTED')
    await expectNumberAtLeast(page, 'system-flow-retry-count', 1)
    await expectText(page, 'system-flow-repair-outcome', 'REPAIRED')
  } finally {
    cleanupGovernanceMessage(seededId)
  }
}

const main = async () => {
  const executablePath = resolveExecutablePath()
  const browser = await chromium.launch({
    headless: true,
    ...(executablePath ? { executablePath } : {})
  })
  const page = await browser.newPage()
  const pageErrors = []

  page.on('pageerror', error => {
    pageErrors.push(`pageerror: ${error.message}`)
  })

  try {
    await runQueryFlow(page)
    await runOptimizationFlow(page)
    await runBenchmarkFlow(page)
    await runSystemFlow(page)
  } finally {
    await browser.close()
  }

  if (pageErrors.length > 0) {
    throw new Error(pageErrors.join('\n'))
  }

  process.stdout.write('Frontend runtime smoke passed\n')
}

main().catch(error => {
  process.stderr.write(`${error.stack || error.message}\n`)
  process.exit(1)
})
