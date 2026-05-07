import fs from 'node:fs'
import http from 'node:http'
import os from 'node:os'
import path from 'node:path'
import { spawn } from 'node:child_process'
import { chromium } from 'playwright'

const defaultTimeoutMs = 20000
const mockState = {
  persistedHistoryDetailCalls: 0,
  fabricatedHistoryDetailCalls: 0
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

const reportBatchSummary = {
  batchId: 'report-batch-smoke',
  tenantId: 'tenant-a',
  batchName: 'portable report smoke',
  fileType: 'CSV',
  status: 'COMPLETED',
  totalReports: 1,
  resolvedReports: 1,
  failedReports: 0,
  totalSqls: 2,
  resolvedSqls: 1,
  failedSqls: 1,
  itemTotalCount: 2,
  createdAt: '2026-05-07T08:00:00Z',
  updatedAt: '2026-05-07T08:01:00Z'
}

const reportBatchParseStatistics = {
  overview: {
    totalSqlCount: 2,
    issueSqlCount: 2,
    importantSqlCount: 1,
    urgentSqlCount: 0
  },
  issueSceneStatistics: [
    {
      issueScene: 'SELECT_STAR',
      affectedSqlCount: 1,
      issueCount: 1,
      reportCount: 1,
      logicalObjectCount: 1,
      severity: 'P2',
      sqlRatio: 0.5
    },
    {
      issueScene: 'SQL_SYNTAX_INVALID',
      affectedSqlCount: 1,
      issueCount: 1,
      reportCount: 1,
      logicalObjectCount: 0,
      severity: 'P1',
      sqlRatio: 0.5
    }
  ],
  importanceStatistics: [],
  reportStatistics: [
    {
      reportCode: 'RPT_PORTABLE',
      sqlCount: 2,
      issueCount: 2,
      issueSqlRatio: 1
    }
  ],
  sqlStatistics: [
    {
      itemId: 'report-item-saved',
      reportCode: 'RPT_PORTABLE',
      sqlColumnName: 'sql_1',
      highestPriorityLevel: 'P2',
      issueCount: 1,
      issueScenes: ['SELECT_STAR'],
      logicalObjectKeys: ['TABLE:orders']
    },
    {
      itemId: 'report-item-local',
      reportCode: 'RPT_PORTABLE',
      sqlColumnName: 'sql_2',
      highestPriorityLevel: 'P1',
      issueCount: 1,
      issueScenes: ['SQL_SYNTAX_INVALID'],
      logicalObjectKeys: []
    }
  ],
  priorityMatrix: [],
  logicalObjectStatistics: [
    {
      objectKey: 'TABLE:orders',
      sqlCount: 1,
      hitCount: 1,
      reportCodes: ['RPT_PORTABLE']
    }
  ]
}

const reportBatchDetail = {
  ...reportBatchSummary,
  parseStatistics: reportBatchParseStatistics,
  reportItems: [
    {
      itemId: 'report-item-saved',
      sequenceNumber: 1,
      reportCode: 'RPT_PORTABLE',
      reportName: 'Portable Report',
      datasourceCode: 'hetu_main',
      stage: 'PROD',
      priority: 'P2',
      sourceFileLine: '2',
      sqlColumnName: 'sql_1',
      sqlOrdinalInReport: 1,
      sqlText: 'SELECT * FROM orders',
      parseTaskId: 'parse-smoke-1',
      structureSyntaxStatus: 'VALID',
      accessServiceStatus: 'AVAILABLE',
      accessConnectionStatus: 'CONNECTED',
      historyId: 'history-parse-smoke-1',
      historyPersisted: true,
      historyPersistenceStatus: 'SAVED',
      status: 'RESOLVED',
      issueScenes: ['SELECT_STAR'],
      issueLocations: [{ issueScene: 'SELECT_STAR', locationSnippet: 'SELECT *' }],
      logicalObjectKeys: ['TABLE:orders']
    },
    {
      itemId: 'report-item-local',
      sequenceNumber: 2,
      reportCode: 'RPT_PORTABLE',
      reportName: 'Portable Report',
      datasourceCode: 'hetu_main',
      stage: 'PROD',
      priority: 'P1',
      sourceFileLine: '2',
      sqlColumnName: 'sql_2',
      sqlOrdinalInReport: 2,
      sqlText: 'SELECT * FROM missing_table',
      parseTaskId: 'parse-missing',
      structureSyntaxStatus: 'FAILED',
      accessServiceStatus: 'SKIPPED',
      accessConnectionStatus: 'SKIPPED',
      failureReason: 'SQL_SYNTAX_INVALID line 1 column 15',
      historyId: '',
      historyPersisted: false,
      historyPersistenceStatus: 'WRITE_FAILED',
      failureLine: 1,
      failureColumn: 15,
      failureToken: 'missing_table',
      failureSnippet: 'missing_table',
      diagnosticSummary: 'SQL_SYNTAX_INVALID at line 1, column 15',
      status: 'FAILED',
      issueScenes: ['SQL_SYNTAX_INVALID'],
      issueLocations: [{
        issueScene: 'SQL_SYNTAX_INVALID',
        failureLine: 1,
        failureColumn: 15,
        failureToken: 'missing_table',
        locationSnippet: 'missing_table'
      }],
      logicalObjectKeys: []
    }
  ],
  statusHistory: []
}

const queryHistoryDetail = {
  historyId: 'history-parse-smoke-1',
  tenantId: 'tenant-a',
  reportCode: 'RPT_PORTABLE',
  historyType: 'SQL_PARSE',
  resultStatus: 'SUCCESS',
  targetEngine: 'HETU',
  accessChannel: 'REPORT_BATCH',
  sqlText: 'SELECT * FROM orders',
  structureParseSummary: {
    parseTaskId: 'parse-smoke-1',
    syntaxStatus: 'VALID',
    sqlFingerprint: 'fp-smoke',
    sqlType: 'SELECT',
    priorityLevel: 'P2',
    priorityScore: 45,
    important: true,
    urgent: false,
    featureSummary: {
      tableCount: 1,
      predicateCount: 0
    },
    issues: [{
      issueCode: 'SELECT_STAR',
      severity: 'P2',
      summary: 'SELECT star usage',
      detail: 'SELECT * keeps the output shape implicit.',
      suggestedAction: 'Select explicit columns.'
    }],
    logicalObjectHits: [{ objectType: 'TABLE', objectKey: 'TABLE:orders' }]
  },
  accessParseSummary: {
    serviceStatus: 'AVAILABLE',
    connectionStatus: 'CONNECTED'
  },
  executionSummary: {
    resultSummary: {
      overallStatus: 'SUCCESS',
      accessAvailable: true
    }
  }
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

      if (request.url.startsWith('/api/governance/datasources')) {
        requireProxyHeaders(request)
        writeJson(response, 200, [])
        return
      }

      if (request.url.startsWith('/api/governance/query-history/history-parse-smoke-1')) {
        requireProxyHeaders(request)
        mockState.persistedHistoryDetailCalls += 1
        writeJson(response, 200, queryHistoryDetail)
        return
      }

      if (request.url.startsWith('/api/governance/query-history/history-parse-parse-missing')) {
        requireProxyHeaders(request)
        mockState.fabricatedHistoryDetailCalls += 1
        writeJson(response, 404, {
          code: 'QUERY_HISTORY_NOT_FOUND',
          message: 'Fabricated parse history id must not be requested by the portable frontend.'
        })
        return
      }

      if (request.url.startsWith('/api/governance/query-history?')) {
        requireProxyHeaders(request)
        writeJson(response, 200, {
          items: [],
          pageNo: 1,
          pageSize: 10,
          totalCount: 0,
          pageCount: 0,
          classificationSummary: {}
        })
        return
      }

      if (request.url.startsWith('/api/sql-optimization/parse-batches')) {
        requireProxyHeaders(request)
        writeJson(response, 200, {
          items: [],
          pageNo: 1,
          pageSize: 10,
          totalCount: 0,
          pageCount: 0,
          hasMore: false
        })
        return
      }

      if (request.url.startsWith('/api/sql-optimization/report-batches/report-batch-smoke/parse-statistics')) {
        requireProxyHeaders(request)
        writeJson(response, 200, reportBatchParseStatistics)
        return
      }

      if (request.url.startsWith('/api/sql-optimization/report-batches/report-batch-smoke')) {
        requireProxyHeaders(request)
        writeJson(response, 200, reportBatchDetail)
        return
      }

      if (request.url.startsWith('/api/sql-optimization/report-batches')) {
        requireProxyHeaders(request)
        writeJson(response, 200, {
          items: [reportBatchSummary],
          pageNo: 1,
          pageSize: 10,
          totalCount: 1,
          pageCount: 1,
          hasMore: false
        })
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
    await expectTextInLocator(page.locator('.query-workbench'), 'success-order-1')
    await expectTextInLocator(page.locator('.result-rail'), 'HETU')

    await page.getByTestId('query-flow-submit-recovery').click()
    await expectTextInLocator(page.locator('.query-workbench'), 'degraded-order-1')
    await expectTextInLocator(page.locator('.result-rail'), 'HIVE')

    await page.goto(`${baseUrl}/#/governance/history/parse-record`, { waitUntil: 'networkidle' })
    await page.getByTestId('parse-record-page').waitFor({ timeout: defaultTimeoutMs })
    await page.getByRole('tab', { name: /批量解析与报表导入历史|Batch parse and report-import history/ }).click()
    const batchHistoryPanel = page.getByTestId('parse-record-batch-report-history-tab')
    await batchHistoryPanel.waitFor({ state: 'visible', timeout: defaultTimeoutMs })
    await batchHistoryPanel.getByRole('tab', { name: /报表导入历史|Report import history/ }).click()
    await batchHistoryPanel.locator('[data-testid="parse-record-batch-history-report"]:visible').first().click()
    const reportDrawer = page.getByTestId('parse-record-report-batch-detail')
    await reportDrawer.waitFor({ state: 'visible', timeout: defaultTimeoutMs })
    await reportDrawer.getByRole('tab', { name: /SQL 清单|SQL list/ }).click()
    await reportDrawer.getByTestId('parse-record-report-sql-detail').first().waitFor({ timeout: defaultTimeoutMs })
    await expectTextInLocator(reportDrawer, 'missing_table')
    await expectTextInLocator(reportDrawer, 'WRITE_FAILED')
    await reportDrawer.getByRole('button', { name: /加载解析详情|Load parse detail/ }).click()
    const sqlDetailDialog = page.getByTestId('parse-record-report-sql-parse-detail-dialog')
    await sqlDetailDialog.waitFor({ state: 'visible', timeout: defaultTimeoutMs })
    await expectTextInLocator(sqlDetailDialog, 'history-parse-smoke-1')
    await expectTextInLocator(sqlDetailDialog, 'orders')
    assert(
      mockState.persistedHistoryDetailCalls === 1,
      `Expected one persisted history detail request, got ${mockState.persistedHistoryDetailCalls}`
    )
    assert(
      mockState.fabricatedHistoryDetailCalls === 0,
      `Portable frontend requested fabricated parse history detail ${mockState.fabricatedHistoryDetailCalls} time(s)`
    )

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

const expectTextInLocator = async (locator, expectedText) => {
  await locator.waitFor({ timeout: defaultTimeoutMs })
  const startedAt = Date.now()

  while (Date.now() - startedAt < defaultTimeoutMs) {
    const currentText = (await locator.textContent())?.trim() || ''
    if (currentText.includes(expectedText)) {
      return
    }
    await sleep(200)
  }

  throw new Error(`Expected locator to include "${expectedText}"`)
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
