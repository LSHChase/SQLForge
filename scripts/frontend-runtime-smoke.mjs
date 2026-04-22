import fs from 'node:fs'
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

const assert = (condition, message) => {
  if (!condition) {
    throw new Error(message)
  }
}

const resolveExecutablePath = () => browserCandidates.find(candidate => fs.existsSync(candidate))

const expectText = async (page, testId, expectedText) => {
  const locator = page.getByTestId(testId)
  await locator.waitFor({ timeout: defaultTimeoutMs })
  const text = (await locator.textContent())?.trim() || ''
  assert(text.includes(expectedText), `Expected ${testId} to include "${expectedText}", got "${text}"`)
  return text
}

const runQueryFlow = async page => {
  await page.goto(`${frontendBaseUrl}/sql-query`, { waitUntil: 'networkidle' })
  await page.getByTestId('query-flow-page').waitFor({ timeout: defaultTimeoutMs })

  const queryResponsePromise = page.waitForResponse(
    response =>
      response.url().includes('/api/query-execution/queries/execute') && response.request().method() === 'POST',
    { timeout: defaultTimeoutMs }
  )

  await page.getByTestId('query-flow-submit').click()
  const queryResponse = await queryResponsePromise
  const queryPayload = await queryResponse.json()

  assert(queryResponse.status() === 200, `Query flow returned HTTP ${queryResponse.status()}`)
  assert(queryPayload.status === 'SUCCESS', `Query flow returned unexpected status ${queryPayload.status}`)

  await expectText(page, 'query-flow-status', 'SUCCESS')
  await expectText(page, 'query-flow-engine', 'HETU')
}

const runOptimizationFlow = async page => {
  await page.goto(`${frontendBaseUrl}/acceleration`, { waitUntil: 'networkidle' })
  await page.getByTestId('optimization-flow-page').waitFor({ timeout: defaultTimeoutMs })

  const submitResponsePromise = page.waitForResponse(
    response => response.url().includes('/api/sql-optimization/tasks') && response.request().method() === 'POST',
    { timeout: defaultTimeoutMs }
  )

  await page.getByTestId('optimization-flow-submit').click()
  const submitResponse = await submitResponsePromise
  const submitPayload = await submitResponse.json()

  assert(submitResponse.status() === 200, `Optimization submit returned HTTP ${submitResponse.status()}`)
  assert(submitPayload.status === 'QUEUED', `Optimization submit returned unexpected status ${submitPayload.status}`)

  await expectText(page, 'optimization-flow-status', 'SUCCEEDED')
  const summaryText = await expectText(page, 'optimization-flow-summary', 'placeholder')
  assert(summaryText.length > 0, 'Optimization summary should not be empty')
}

const runBenchmarkFlow = async page => {
  await page.goto(`${frontendBaseUrl}/benchmark`, { waitUntil: 'networkidle' })
  await page.getByTestId('benchmark-flow-page').waitFor({ timeout: defaultTimeoutMs })

  const submitResponsePromise = page.waitForResponse(
    response => response.url().includes('/api/benchmark-engine/tasks') && response.request().method() === 'POST',
    { timeout: defaultTimeoutMs }
  )
  const reportResponsePromise = page.waitForResponse(
    response =>
      response.url().includes('/api/benchmark-engine/reports/') && response.request().method() === 'GET',
    { timeout: defaultTimeoutMs }
  )

  await page.getByTestId('benchmark-flow-submit').click()
  const submitResponse = await submitResponsePromise
  const submitPayload = await submitResponse.json()

  assert(submitResponse.status() === 200, `Benchmark submit returned HTTP ${submitResponse.status()}`)
  assert(submitPayload.status === 'QUEUED', `Benchmark submit returned unexpected status ${submitPayload.status}`)

  await expectText(page, 'benchmark-flow-status', 'SUCCEEDED')
  const reportIdText = await expectText(page, 'benchmark-flow-report-id', 'report-')
  assert(reportIdText.startsWith('report-'), `Benchmark report id is invalid: ${reportIdText}`)

  const reportResponse = await reportResponsePromise
  const reportPayload = await reportResponse.json()

  assert(reportResponse.status() === 200, `Benchmark report returned HTTP ${reportResponse.status()}`)
  assert(reportPayload.requestedFormat === 'JSON', `Benchmark report format mismatch: ${reportPayload.requestedFormat}`)
  await page.getByTestId('benchmark-flow-report').waitFor({ timeout: defaultTimeoutMs })
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
