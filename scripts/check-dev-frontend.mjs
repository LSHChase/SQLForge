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

const expectTextInLocator = async (locator, expectedText) => {
  await locator.waitFor({ timeout: defaultTimeoutMs })
  const startedAt = Date.now()

  while (Date.now() - startedAt < defaultTimeoutMs) {
    const currentText = (await locator.textContent())?.trim() || ''
    if (currentText.includes(expectedText)) {
      return
    }
    await locator.page().waitForTimeout(200)
  }

  throw new Error(`Expected locator to include "${expectedText}"`)
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
  let queryExecuteCalls = 0
  let sqlHistoryPageCalls = 0
  let sqlHistoryRewriteRecordCalls = 0
  const sqlHistoryPageRequests = []
  let parseHistoryPageCalls = 0

  page.on('pageerror', error => {
    pageErrors.push(error.message)
  })

  await page.route('**/api/**', async route => {
    const request = route.request()
    const requestUrl = new URL(request.url())
    const { pathname } = requestUrl
    const requestPrefix = request.headers()['x-sqlforge-dev-request-prefix'] || ''

    if (requestPrefix.startsWith('frontend-dashboard-')) {
      if (pathname === '/api/sql-optimization/parse-statistics/overview') {
        await fulfillJson(route, { totalSqlCount: 0, issueSqlCount: 0, issueSceneCount: 0 })
        return
      }
      if (pathname === '/api/sql-optimization/parse-statistics/by-issue-scene') {
        await fulfillJson(route, [])
        return
      }
      if (pathname === '/api/sql-optimization/parse-statistics/important-urgent') {
        await fulfillJson(route, [])
        return
      }
      if (pathname === '/api/governance/admin/messages/stats') {
        await fulfillJson(route, { pending: 0, total: 0, failed: 0 })
        return
      }
      if (pathname === '/api/sql-optimization/dispatch-contract') {
        await fulfillJson(route, { coordinationMode: 'PULL_ONLY', externalPullRequired: false })
        return
      }
      if (pathname === '/api/governance/query-history') {
        await fulfillJson(route, {
          items: [],
          pageNo: 1,
          pageSize: 8,
          totalCount: 0,
          pageCount: 0,
          classificationSummary: {
            totalItems: 0,
            accessChannelCounts: {}
          }
        })
        return
      }
      if (pathname === '/api/sql-optimization/parse-history') {
        await fulfillJson(route, {
          items: [],
          pageNo: 1,
          pageSize: 8,
          totalCount: 0,
          pageCount: 0,
          classificationSummary: {
            totalItems: 0,
            accessChannelCounts: {},
            sourceTypeCounts: {}
          }
        })
        return
      }
      if (pathname === '/api/sql-optimization/dispatch-events') {
        await fulfillJson(route, [])
        return
      }
      if (pathname === '/api/sql-optimization/recommendations') {
        await fulfillJson(route, [])
        return
      }
      if (pathname === '/api/sql-optimization/rewrite-records') {
        await fulfillJson(route, [])
        return
      }
    }

    if (pathname === '/api/governance/query-history' && requestPrefix === 'frontend-sql-history-page') {
      assert(
        requestUrl.searchParams.get('historyType') === 'QUERY_EXECUTION',
        `SQL history page must request QUERY_EXECUTION, got ${requestUrl.searchParams.get('historyType')}`
      )
      sqlHistoryPageCalls += 1
      const pageNo = Number(requestUrl.searchParams.get('pageNo') || 1)
      const pageSize = Number(requestUrl.searchParams.get('pageSize') || 10)
      const totalCount = 42
      sqlHistoryPageRequests.push({
        pageNo,
        pageSize,
        reportCode: requestUrl.searchParams.get('reportCode') || ''
      })
      const remainingCount = Math.max(0, totalCount - ((pageNo - 1) * pageSize))
      const itemCount = Math.min(pageSize, remainingCount)
      await fulfillJson(route, {
        items: Array.from({ length: itemCount }, (_, index) => ({
          historyId: `dev-history-${pageNo}-${index + 1}`,
          traceId: `dev-trace-${pageNo}-${index + 1}`,
          reportCode: `DEV_RPT_${pageNo}_${index + 1}`,
          datasourceCode: 'hetu_main',
          resultStatus: 'SUCCESS',
          accessChannel: 'PAGE',
          targetEngine: 'HETU',
          cacheHit: index % 2 === 0,
          rewriteApplied: false,
          accelerationApplied: true,
          submittedBy: 'dev-smoke',
          submittedAt: '2026-05-08T22:30:00',
          auditEventCount: 1
        })),
        pageNo,
        pageSize,
        totalCount,
        pageCount: Math.ceil(totalCount / pageSize),
        classificationSummary: {
          totalItems: totalCount,
          statusCounts: { SUCCESS: totalCount },
          accessChannelCounts: { PAGE: totalCount }
        }
      })
      return
    }

    if (
      pathname.startsWith('/api/governance/query-history/') &&
      !pathname.endsWith('/rewrite-records') &&
      requestPrefix === 'frontend-sql-history-detail'
    ) {
      const historyId = decodeURIComponent(pathname.split('/')[4] || '')
      await fulfillJson(route, {
        historyId,
        tenantId: 'tenant-a',
        historyType: 'QUERY_EXECUTION',
        traceId: `trace-${historyId}`,
        reportCode: `REPORT_${historyId}`,
        datasourceCode: 'hetu_main',
        resultStatus: 'SUCCESS',
        accessChannel: 'PAGE',
        targetEngine: 'HETU',
        submittedBy: 'dev-smoke',
        submittedAt: '2026-05-08T22:30:00',
        sqlFingerprint: `fingerprint-${historyId}`,
        sqlText: 'SELECT * FROM sales.orders WHERE dt = ?',
        sqlTemplateText: 'SELECT * FROM sales.orders WHERE dt = :bizDate',
        boundSqlText: "SELECT * FROM sales.orders WHERE dt = '2026-05-08'",
        executionSummary: {
          cacheHit: false,
          rewriteApplied: true,
          accelerationApplied: false,
          returnedRowCount: 12
        },
        queryDateSummary: {
          queryDateStatus: 'RESOLVED'
        },
        recommendationRefs: [],
        benchmarkRefs: [],
        auditRefs: [],
        alertRefs: [],
        traceDetail: {
          auditEventCount: 1,
          auditEvents: [
            {
              serviceCode: 'QUERY_EXECUTION',
              operationType: 'QUERY_EXECUTE',
              status: 'SUCCESS',
              createTime: '2026-05-08T22:30:01'
            }
          ]
        }
      })
      return
    }

    if (
      pathname.startsWith('/api/governance/query-history/') &&
      pathname.endsWith('/rewrite-records') &&
      requestPrefix === 'frontend-sql-history-rewrite-records'
    ) {
      sqlHistoryRewriteRecordCalls += 1
      const historyId = decodeURIComponent(pathname.split('/')[4] || '')
      await fulfillJson(route, {
        tenantId: 'tenant-a',
        historyId,
        rewriteRecordCount: 1,
        contractStage: 'LONG_TERM_BASELINE',
        implementationStage: 'QUERY_HISTORY_REWRITE_RECORD_AGGREGATION',
        items: [
          {
            rewriteRecordId: 'rewrite-dev-1',
            tenantId: 'tenant-a',
            recommendationId: 'rec-dev-1',
            sourceType: 'QUERY',
            sourceKind: 'QUERY_HISTORY',
            sourceId: historyId,
            evidenceLevel: 'RUNTIME_HISTORY',
            historyId,
            parseHistoryId: 'parse-dev-1',
            sqlFingerprint: `fingerprint-${historyId}`,
            validationStatus: 'DIVERGED',
            lastValidationRunId: 'validation-dev-1',
            lastComparedAt: '2026-05-08T22:31:00',
            alertStatus: 'OPEN',
            originalSqlText: 'SELECT * FROM sales.orders WHERE dt = ?',
            recommendedSqlText: 'SELECT id FROM sales.orders WHERE dt = ?',
            executedSqlText: 'SELECT id FROM sales.orders WHERE dt = ?',
            ruleChain: [{ ruleCode: 'SELECT_STAR', action: 'PROJECT_COLUMNS' }],
            diffSummary: { changed: 1, manualReviewRequired: true },
            traceRefs: { alertRefs: [{ alertId: 'alert-dev-1', alertStatus: 'OPEN' }] }
          }
        ]
      })
      return
    }

    if (pathname === '/api/sql-optimization/parse-history' && requestPrefix === 'frontend-parse-record-parse-history-page') {
      assert(
        !requestUrl.searchParams.has('historyType'),
        'Parse record page must not request governance query-history historyType'
      )
      parseHistoryPageCalls += 1
      await fulfillJson(route, {
        items: [],
        pageNo: Number(requestUrl.searchParams.get('pageNo') || 1),
        pageSize: Number(requestUrl.searchParams.get('pageSize') || 10),
        totalCount: 0,
        pageCount: 0,
        classificationSummary: {
          totalItems: 0,
          statusCounts: {},
          accessChannelCounts: {},
          sourceTypeCounts: {}
        }
      })
      return
    }

    if (pathname === '/api/governance/datasources') {
      assertDevHeaders(request, 'tenant-a', [
        'frontend-parse-record-datasource-options',
        'frontend-sql-history-datasource-options'
      ])
      await fulfillJson(route, [
        { datasourceCode: 'hetu_main', datasourceName: 'Hetu main' },
        { datasourceCode: 'hive_archive', datasourceName: 'Hive archive' }
      ])
      return
    }

    if (pathname === '/api/sql-optimization/parse-batches') {
      await fulfillJson(route, { items: [], pageNo: 1, pageSize: 10, totalCount: 0, pageCount: 0 })
      return
    }

    if (pathname === '/api/sql-optimization/report-batches') {
      await fulfillJson(route, { items: [], pageNo: 1, pageSize: 10, totalCount: 0, pageCount: 0 })
      return
    }

    if (pathname === '/api/query-execution/queries/execute') {
      assertDevHeaders(request, 'tenant-a', ['frontend-query'])
      queryExecuteCalls += 1
      const payload = parseJsonBody(request)
      const isRecoveryFlow = Boolean(payload.queryContext?.timeoutMs)
      await fulfillJson(
        route,
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
      return
    }

    if (pathname === '/api/governance/admin/messages/stats') {
      assertDevHeaders(request, 'tenant-a', [
        'frontend-query-governance-stats-before',
        'frontend-query-governance-stats-after'
      ])
      governanceStatsCalls += 1
      await fulfillJson(
        route,
        governanceStatsCalls === 1
          ? { pending: 0, total: 0, failed: 0 }
          : { pending: 1, total: 1, failed: 1 }
      )
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
    await expectTextInLocator(page.locator('.result-rail'), 'SUCCESS')
    await expectTextInLocator(page.locator('.result-rail'), 'HETU')
    await expectTextInLocator(page.locator('.results-stage'), 'dev-success-order-1')

    await page.getByTestId('query-flow-submit-recovery').click()
    await expectTextInLocator(page.locator('.result-rail'), 'PARTIAL')
    await expectTextInLocator(page.locator('.result-rail'), 'HIVE')
    await expectTextInLocator(page.locator('.results-stage'), 'dev-recovery-order-1')

    await page.goto(`${baseUrl}${ROUTE_PATHS.sqlHistory}`, { waitUntil: 'domcontentloaded' })
    await page.getByTestId('sql-history-page').waitFor({ timeout: defaultTimeoutMs })
    await expectTextInLocator(page.getByTestId('sql-history-pagination'), '42')
    await Promise.all([
      page.waitForResponse(response => {
        if (!response.url().includes('/api/governance/query-history')) {
          return false
        }
        const responseUrl = new URL(response.url())
        return responseUrl.searchParams.get('pageNo') === '2'
      }),
      page.getByTestId('sql-history-pagination').locator('.btn-next').click()
    ])
    const pageSizeResponse = page.waitForResponse(response => {
      if (!response.url().includes('/api/governance/query-history')) {
        return false
      }
      const responseUrl = new URL(response.url())
      return responseUrl.searchParams.get('pageNo') === '1' && responseUrl.searchParams.get('pageSize') === '25'
    })
    await page.getByTestId('sql-history-pagination').locator('.el-select').click()
    await page.getByRole('option', { name: /25/ }).click()
    await pageSizeResponse
    await page.getByTestId('sql-history-report-filter').click()
    await page.keyboard.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
    await page.keyboard.type('DEV_RPT_FILTER')
    await Promise.all([
      page.waitForResponse(response => {
        if (!response.url().includes('/api/governance/query-history')) {
          return false
        }
        const responseUrl = new URL(response.url())
        return responseUrl.searchParams.get('pageNo') === '1' && responseUrl.searchParams.get('reportCode') === 'DEV_RPT_FILTER'
      }),
      page.keyboard.press('Enter')
    ])
    await page.getByTestId('sql-history-has-rewrite-record-filter').click()
    await page.getByRole('option', { name: /true/ }).click()
    await page.getByTestId('sql-history-rewrite-validation-status-filter').click()
    await page.getByRole('option', { name: /DIVERGED/ }).click()
    await Promise.all([
      page.waitForResponse(response => {
        if (!response.url().includes('/api/governance/query-history')) {
          return false
        }
        const responseUrl = new URL(response.url())
        return responseUrl.searchParams.get('hasRewriteRecord') === 'true' &&
          responseUrl.searchParams.get('rewriteValidationStatus') === 'DIVERGED'
      }),
      page.getByTestId('sql-history-refresh').click()
    ])
    await page.getByTestId('sql-history-trace-item').first().click()
    await page.getByTestId('sql-history-detail-drawer').waitFor({ timeout: defaultTimeoutMs })
    await page.getByTestId('sql-history-detail-tabs').getByRole('tab', { name: /改写记录|Rewrite records/ }).click()
    await expectTextInLocator(page.getByTestId('sql-history-rewrite-record-table'), 'rewrite-dev-1')
    await expectTextInLocator(page.getByTestId('sql-history-rewrite-records-tab'), 'DIVERGED')
    await page.goto(`${baseUrl}${ROUTE_PATHS.parseRecord}`, { waitUntil: 'domcontentloaded' })
    await page.getByTestId('parse-record-page').waitFor({ timeout: defaultTimeoutMs })

    assert(queryExecuteCalls === 2, `Expected 2 query execution calls, got ${queryExecuteCalls}`)
    assert(governanceStatsCalls === 2, `Expected 2 governance stats calls, got ${governanceStatsCalls}`)
    assert(sqlHistoryPageCalls >= 1, `Expected SQL history page calls, got ${sqlHistoryPageCalls}`)
    assert(
      sqlHistoryRewriteRecordCalls === 1,
      `Expected one SQL history rewrite-record call, got ${sqlHistoryRewriteRecordCalls}`
    )
    assert(
      sqlHistoryPageRequests.some(item => item.pageNo === 2),
      `Expected SQL history next-page request, got ${JSON.stringify(sqlHistoryPageRequests)}`
    )
    assert(
      sqlHistoryPageRequests.some(item => item.pageNo === 1 && item.pageSize === 25),
      `Expected SQL history page-size reset request, got ${JSON.stringify(sqlHistoryPageRequests)}`
    )
    assert(
      sqlHistoryPageRequests.some(item => item.pageNo === 1 && item.reportCode === 'DEV_RPT_FILTER'),
      `Expected SQL history submit-search request, got ${JSON.stringify(sqlHistoryPageRequests)}`
    )
    assert(parseHistoryPageCalls >= 1, `Expected parse history page calls, got ${parseHistoryPageCalls}`)
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
