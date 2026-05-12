import fs from 'node:fs'
import http from 'node:http'
import path from 'node:path'
import { spawn } from 'node:child_process'
import { chromium } from 'playwright'
import { ROUTE_PATHS } from '../src/config/routePaths.mjs'

const defaultTimeoutMs = Number(process.env.FRONTEND_ACCELERATION_WORKBENCH_SMOKE_TIMEOUT_MS || 20000)
const repoRoot = process.cwd()
const smokeTmpDir = path.join(repoRoot, 'target', 'acceleration-workbench-browser-smoke-tmp')
fs.mkdirSync(smokeTmpDir, { recursive: true })
if (!process.env.TMPDIR || process.env.TMPDIR === '/tmp' || process.env.TMPDIR.startsWith('/tmp/')) {
  process.env.TMPDIR = smokeTmpDir
}
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

const launchChromium = async () => {
  const executablePath = resolveExecutablePath()
  if (!executablePath) {
    return chromium.launch({ headless: true })
  }

  try {
    return await chromium.launch({
      headless: true,
      executablePath
    })
  } catch (error) {
    if (process.env.FRONTEND_RUNTIME_BROWSER_BIN) {
      throw error
    }
    return chromium.launch({ headless: true })
  }
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

const waitForChildExit = child =>
  new Promise(resolve => {
    if (child.exitCode !== null || child.signalCode !== null) {
      resolve()
      return
    }
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

const parseJsonBody = request => {
  const text = request.postData() || ''
  return text ? JSON.parse(text) : {}
}

const fulfillJson = (route, body) =>
  route.fulfill({
    status: 200,
    contentType: 'application/json; charset=utf-8',
    body: JSON.stringify(body)
  })

const assertWorkbenchHeaders = request => {
  const headers = request.headers()
  assert(headers['x-sqlforge-dev-tenant-id'] === 'tenant-a', `Unexpected tenant header ${headers['x-sqlforge-dev-tenant-id']}`)
  assert(
    String(headers['x-sqlforge-dev-request-prefix'] || '').startsWith('frontend-acceleration-workbench-'),
    `Unexpected request prefix ${headers['x-sqlforge-dev-request-prefix']}`
  )
}

const runBrowserSmoke = async baseUrl => {
  const browser = await launchChromium()
  const page = await browser.newPage({ viewport: { width: 1440, height: 1200 } })
  const pageErrors = []
  const seen = new Set()
  const unexpectedApiRequests = []

  page.on('pageerror', error => {
    pageErrors.push(error.message)
  })

  await page.route('**/api/**', async route => {
    const request = route.request()
    const requestUrl = new URL(request.url())
    const { pathname } = requestUrl
    const method = request.method()
    const key = `${method} ${pathname}`
    assertWorkbenchHeaders(request)

    if (method === 'POST' && pathname === '/api/sql-optimization/acceleration-candidates') {
      const payload = parseJsonBody(request)
      assert(payload.sourceType === 'PARSE', `Candidate sourceType drifted: ${payload.sourceType}`)
      assert(payload.evidenceLevel === 'STATIC_PARSE', `Candidate evidenceLevel drifted: ${payload.evidenceLevel}`)
      seen.add(key)
      await fulfillJson(route, {
        ...payload,
        candidateId: 'candidate-138',
        status: 'DRAFT',
        implementationStage: 'BROWSER_SMOKE'
      })
      return
    }

    if (method === 'GET' && pathname === '/api/sql-optimization/acceleration-candidates') {
      seen.add(key)
      await fulfillJson(route, [
        {
          candidateId: 'candidate-138',
          status: 'DRAFT',
          candidateType: 'ACCELERATION_AND_REWRITE',
          evidenceLevel: 'STATIC_PARSE',
          sourceKind: 'COMBINED_PARSE'
        }
      ])
      return
    }

    if (method === 'POST' && pathname === '/api/sql-optimization/tasks') {
      const payload = parseJsonBody(request)
      assert(payload.taskType === 'ACCELERATION_SUGGESTION', `Unexpected taskType ${payload.taskType}`)
      assert(!payload.taskContext.requestedSuggestionTypes.includes('ALL'), 'Plan smoke must not submit ALL suggestion type.')
      seen.add(key)
      await fulfillJson(route, {
        taskId: 'task-138',
        status: 'SUBMITTED',
        currentPhase: 'QUEUED',
        statusQueryPath: '/api/sql-optimization/tasks/task-138'
      })
      return
    }

    if (method === 'GET' && pathname === '/api/sql-optimization/tasks/task-138') {
      seen.add(key)
      await fulfillJson(route, {
        taskId: 'task-138',
        taskType: 'ACCELERATION_SUGGESTION',
        status: 'SUCCEEDED',
        currentPhase: 'COMPLETED',
        progressPercent: 100,
        requestedSuggestionTypes: ['PRECOMPUTE', 'PARTITION']
      })
      return
    }

    if (method === 'GET' && pathname === '/api/sql-optimization/recommendations/rec-138/diff') {
      seen.add(key)
      await fulfillJson(route, {
        recommendationId: 'rec-138',
        tenantId: 'tenant-a',
        sourceType: 'PARSE',
        sourceKind: 'COMBINED_PARSE',
        sourceId: 'parse-history-001',
        evidenceLevel: 'STATIC_PARSE',
        sqlFingerprint: 'fp_sales_daily_20260401',
        originalSql: "SELECT * FROM vw_sales_daily WHERE dt = '2026-04-01'",
        recommendedSql: "SELECT id FROM vw_sales_daily WHERE dt = '2026-04-01'",
        textDiff: [{ type: 'replace', before: '*', after: 'id' }],
        ruleDiff: [{ ruleCode: 'SELECT_STAR_METADATA' }],
        astSummaryDiff: { projectionChanged: true },
        diffSummary: { changed: 1 },
        diffStatus: 'READY'
      })
      return
    }

    if (method === 'POST' && pathname === '/api/sql-optimization/rewrite-records') {
      const payload = parseJsonBody(request)
      assert(payload.recommendationId === 'rec-138', `Unexpected rewrite recommendation ${payload.recommendationId}`)
      assert(payload.autoApplyAllowed === false, 'Workbench rewrite record must not allow auto apply by default.')
      seen.add(key)
      await fulfillJson(route, {
        ...payload,
        rewriteRecordId: 'rewrite-138',
        validationStatus: 'NOT_VALIDATED',
        alertStatus: 'NONE'
      })
      return
    }

    if (method === 'POST' && pathname === '/api/sql-optimization/acceleration-plans') {
      const payload = parseJsonBody(request)
      assert(payload.sourceTaskId === 'task-138', `Unexpected sourceTaskId ${payload.sourceTaskId}`)
      assert(!payload.selectedSuggestionTypes.includes('ALL'), 'Plan submit must not contain ALL suggestion type.')
      seen.add(key)
      await fulfillJson(route, {
        planId: 'plan-138',
        status: 'PENDING_APPROVAL',
        statusQueryPath: '/api/sql-optimization/acceleration-plans/plan-138'
      })
      return
    }

    if (method === 'POST' && pathname === '/api/sql-optimization/acceleration-plans/plan-138/approval') {
      const payload = parseJsonBody(request)
      assert(payload.approve === true, `Expected approval approve=true, got ${payload.approve}`)
      seen.add(key)
      await fulfillJson(route, {
        planId: 'plan-138',
        status: 'APPROVED',
        reviewNote: payload.reviewNote,
        statusHistory: [{ status: 'APPROVED', reason: payload.reviewNote, createdAt: '2026-05-11T00:00:00Z' }]
      })
      return
    }

    if (method === 'POST' && pathname === '/api/sql-optimization/acceleration-plans/plan-138/apply') {
      seen.add(key)
      await fulfillJson(route, {
        planId: 'plan-138',
        status: 'APPLIED',
        runtimeBindingJson: '{"binding":"created"}',
        configSnapshotId: 'config-138'
      })
      return
    }

    if (method === 'POST' && pathname === '/api/sql-optimization/acceleration-plans/plan-138/verify') {
      seen.add(key)
      await fulfillJson(route, {
        planId: 'plan-138',
        status: 'VERIFIED',
        verificationEvidenceJson: '{"equivalent":true}',
        resultId: 'result-138'
      })
      return
    }

    if (method === 'POST' && pathname === '/api/sql-optimization/acceleration-plans/plan-138/rollback') {
      seen.add(key)
      await fulfillJson(route, {
        planId: 'plan-138',
        status: 'ROLLED_BACK',
        rollbackEvidenceJson: '{"rolledBack":true}'
      })
      return
    }

    if (method === 'POST' && pathname === '/api/query-execution/queries/execute') {
      const payload = parseJsonBody(request)
      assert(['NONE', 'PREFER_ACCELERATED'].includes(payload.accelerationPreference), `Unexpected accelerationPreference ${payload.accelerationPreference}`)
      seen.add(`${key} ${payload.accelerationPreference}`)
      await fulfillJson(route, {
        status: 'SUCCESS',
        sqlFingerprint: 'fp-sales-smoke',
        metadata: {
          accelerationApplied: payload.accelerationPreference === 'PREFER_ACCELERATED',
          targetEngine: 'HETU'
        },
        rows: []
      })
      return
    }

    if (method === 'GET' && pathname === '/api/sql-optimization/rewrite-records') {
      seen.add(key)
      await fulfillJson(route, [
        {
          rewriteRecordId: 'rewrite-138',
          validationStatus: 'DIVERGED',
          alertStatus: 'OPEN',
          autoApplyAllowed: false,
          manualReviewRequired: true,
          traceRefs: {
            divergenceAlert: {
              alertType: 'SQL_REWRITE_RESULT_DIVERGENCE',
              autoApplyPaused: true,
              linkages: [{ alertId: 'alert-138', alertStatus: 'OPEN' }]
            }
          }
        }
      ])
      return
    }

    if (method === 'GET' && pathname === '/api/governance/query-history/history-138/rewrite-records') {
      seen.add(key)
      await fulfillJson(route, {
        historyId: 'history-138',
        rewriteRecordCount: 1,
        items: [
          {
            rewriteRecordId: 'rewrite-138',
            validationStatus: 'DIVERGED',
            alertStatus: 'OPEN',
            traceRefs: {
              divergenceAlert: {
                alertType: 'SQL_REWRITE_RESULT_DIVERGENCE',
                autoApplyPaused: true
              }
            }
          }
        ]
      })
      return
    }

    if (method === 'GET' && pathname === '/api/sql-optimization/rewrite-records/rewrite-138/validation-runs') {
      seen.add(key)
      await fulfillJson(route, [
        {
          validationRunId: 'validation-138-existing',
          status: 'PENDING',
          comparisonStatus: 'DIVERGED',
          differenceType: 'ROW_HASH_MISMATCH',
          autoApplyPaused: true
        }
      ])
      return
    }

    if (method === 'POST' && pathname === '/api/sql-optimization/rewrite-records/rewrite-138/validation-runs') {
      const payload = parseJsonBody(request)
      assert(payload.status === 'PENDING', `Validation run must start pending, got ${payload.status}`)
      seen.add(key)
      await fulfillJson(route, {
        validationRunId: 'validation-138',
        status: 'PENDING',
        comparisonStatus: 'DIVERGED',
        differenceType: 'ROW_HASH_MISMATCH',
        autoApplyPaused: true
      })
      return
    }

    unexpectedApiRequests.push(key)
    await route.fulfill({
      status: 404,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify({ error: 'UNEXPECTED_ACCELERATION_WORKBENCH_ENDPOINT', path: pathname })
    })
  })

  try {
    await page.goto(`${baseUrl}${ROUTE_PATHS.accelerationGovernanceWorkbench}`, { waitUntil: 'domcontentloaded' })
    await page.getByTestId('acceleration-governance-workbench-page').waitFor({ timeout: defaultTimeoutMs })

    await page.getByTestId('acceleration-workbench-create-candidate').click()
    await page.getByTestId('acceleration-workbench-list-candidates').click()
    await page.getByTestId('acceleration-workbench-submit-suggestion').click()
    await page.getByTestId('acceleration-workbench-refresh-suggestion').click()

    await page.getByRole('tab', { name: /SQL 差异|SQL diff/ }).click()
    await page.getByTestId('acceleration-workbench-recommendation-id').fill('rec-138')
    await page.getByTestId('acceleration-workbench-load-diff').click()
    await page.getByTestId('acceleration-workbench-create-rewrite-record').click()

    await page.getByRole('tab', { name: /计划审批|Plan approval/ }).click()
    await page.getByTestId('acceleration-workbench-submit-plan').click()
    await page.getByTestId('acceleration-workbench-approve-plan').click()

    await page.getByRole('tab', { name: /应用验证|Apply validation/ }).click()
    await page.getByTestId('acceleration-workbench-apply-plan').click()
    await page.getByTestId('acceleration-workbench-verify-plan').click()
    await page.getByTestId('acceleration-workbench-baseline-query').click()
    await page.getByTestId('acceleration-workbench-accelerated-query').click()
    await page.getByTestId('acceleration-workbench-rollback-plan').click()

    await page.getByRole('tab', { name: /监控与告警|Monitoring and alerts/ }).click()
    await page.getByTestId('acceleration-workbench-monitor-history-id').fill('history-138')
    await page.getByTestId('acceleration-workbench-list-rewrite-records').click()
    await page.getByTestId('acceleration-workbench-history-rewrite-records').click()
    await page.getByTestId('acceleration-workbench-list-validation-runs').click()
    await page.getByTestId('acceleration-workbench-create-validation-run').click()
    await page.getByTestId('acceleration-workbench-auto-pause-evidence').waitFor({ timeout: defaultTimeoutMs })
    const pauseEvidenceText = await page.getByTestId('acceleration-workbench-auto-pause-evidence').textContent()
    assert(pauseEvidenceText.includes('autoApplyPaused'), 'Workbench monitoring tab must expose autoApplyPaused evidence.')
    assert(pauseEvidenceText.includes('SQL_REWRITE_RESULT_DIVERGENCE'), 'Workbench monitoring tab must expose divergence alert evidence.')

    await page.getByRole('tab', { name: /接口证据|Interface evidence/ }).click()
    await page.getByTestId('acceleration-workbench-last-evidence').waitFor({ timeout: defaultTimeoutMs })
    const evidenceText = await page.getByTestId('acceleration-workbench-last-evidence').textContent()
    assert(evidenceText.includes('createRewriteValidationRun'), 'Evidence tab must show the latest validation run action.')

    const requiredSeen = [
      'POST /api/sql-optimization/acceleration-candidates',
      'GET /api/sql-optimization/acceleration-candidates',
      'POST /api/sql-optimization/tasks',
      'GET /api/sql-optimization/tasks/task-138',
      'GET /api/sql-optimization/recommendations/rec-138/diff',
      'POST /api/sql-optimization/rewrite-records',
      'POST /api/sql-optimization/acceleration-plans',
      'POST /api/sql-optimization/acceleration-plans/plan-138/approval',
      'POST /api/sql-optimization/acceleration-plans/plan-138/apply',
      'POST /api/sql-optimization/acceleration-plans/plan-138/verify',
      'POST /api/query-execution/queries/execute NONE',
      'POST /api/query-execution/queries/execute PREFER_ACCELERATED',
      'POST /api/sql-optimization/acceleration-plans/plan-138/rollback',
      'GET /api/sql-optimization/rewrite-records',
      'GET /api/governance/query-history/history-138/rewrite-records',
      'GET /api/sql-optimization/rewrite-records/rewrite-138/validation-runs',
      'POST /api/sql-optimization/rewrite-records/rewrite-138/validation-runs'
    ]
    for (const item of requiredSeen) {
      assert(seen.has(item), `Workbench smoke did not call expected endpoint: ${item}`)
    }
    assert(unexpectedApiRequests.length === 0, `Unexpected API requests: ${unexpectedApiRequests.join(', ')}`)
    assert(pageErrors.length === 0, `Workbench smoke saw page errors: ${pageErrors.join(' | ')}`)
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

  process.stdout.write('Acceleration workbench browser smoke passed\n')
}

main().catch(error => {
  console.error(error.message)
  process.exit(1)
})
