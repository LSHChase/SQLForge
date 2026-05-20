import fs from 'node:fs'
import http from 'node:http'
import path from 'node:path'
import { spawn } from 'node:child_process'
import { chromium } from 'playwright'
import { ROUTE_PATHS } from '../src/config/routePaths.mjs'

const defaultTimeoutMs = Number(process.env.FRONTEND_PRW012_SMOKE_TIMEOUT_MS || 20000)
const repoRoot = process.cwd()
const smokeTmpDir = path.join(repoRoot, 'target', 'prw012-browser-smoke-tmp')
fs.mkdirSync(smokeTmpDir, { recursive: true })
if (!process.env.TMPDIR || process.env.TMPDIR === '/tmp' || process.env.TMPDIR.startsWith('/tmp/')) {
  process.env.TMPDIR = smokeTmpDir
}
const tenantId = 'tenant-a'
const recommendationId = 'rec-prw-012'
const rewriteRecordId = 'rewrite-prw-012'
const validationRunId = 'validation-prw-012'
const runtimeBindingId = 'rwb-prw-012'
const runtimeRuleVersion = 'runtime-rewrite-v1'
const historyId = 'history-prw-012'
const longPredicateLiteral = `production_rewrite_scroll_marker_${'x'.repeat(180)}`
const longPredicates = Array.from(
  { length: 42 },
  (_, index) => `AND metric_${index} = '${longPredicateLiteral}_${index}'`
).join(' ')
const deepNestedPredicate = `query_date IN (
  SELECT query_date FROM mart.calendar_day WHERE calendar_key IN (
    SELECT calendar_key FROM mart.calendar_acl WHERE EXISTS (
      SELECT 1 FROM security.acl acl WHERE acl.calendar_key = mart.calendar_acl.calendar_key AND acl.tenant_id IN (
        SELECT tenant_id FROM security.tenant_scope WHERE scope_id IN (
          SELECT scope_id FROM security.scope_group WHERE group_id IN (
            SELECT group_id FROM security.group_owner WHERE owner_note = 'literal select from production smoke'
          )
        )
      )
    )
  )
)`
const originalSql = `-- report_code=RPT_PRW_012
/* owner: production rewrite smoke */
SELECT * FROM (
  SELECT order_id, id, query_date, metric_0 FROM orders WHERE ${deepNestedPredicate} ${longPredicates}
) d WHERE EXISTS (
  SELECT 1 FROM mart.order_quality q WHERE q.order_id = d.order_id AND q.status IN (
    SELECT status FROM mart.valid_status WHERE status_note = 'select/from literal'
  )
)`
const recommendedSql = `SELECT id FROM (
  SELECT order_id, id, query_date, metric_0 FROM orders WHERE ${deepNestedPredicate} ${longPredicates}
) d WHERE EXISTS (
  SELECT 1 FROM mart.order_quality q WHERE q.order_id = d.order_id AND q.status IN (
    SELECT status FROM mart.valid_status WHERE status_note = 'select/from literal'
  )
)`
const sqlFingerprint = 'fp_prw_012_orders'
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

const assertSqlCompareDualPane = async compareLocator => {
  const originalPane = compareLocator.getByTestId('sql-compare-pane-original')
  const recommendedPane = compareLocator.getByTestId('sql-compare-pane-recommended')
  const originalViewport = compareLocator.getByTestId('sql-compare-original-viewport')
  const recommendedViewport = compareLocator.getByTestId('sql-compare-recommended-viewport')

  await originalPane.waitFor({ timeout: defaultTimeoutMs })
  await recommendedPane.waitFor({ timeout: defaultTimeoutMs })
  await compareLocator.getByTestId('sql-compare-copy-original').waitFor({ timeout: defaultTimeoutMs })
  await compareLocator.getByTestId('sql-compare-format-original').waitFor({ timeout: defaultTimeoutMs })
  await compareLocator.getByTestId('sql-compare-copy-recommended').waitFor({ timeout: defaultTimeoutMs })
  await compareLocator.getByTestId('sql-compare-format-recommended').waitFor({ timeout: defaultTimeoutMs })

  const originalMetrics = await originalViewport.evaluate(element => ({
    maxLeft: element.scrollWidth - element.clientWidth,
    maxTop: element.scrollHeight - element.clientHeight
  }))
  assert(originalMetrics.maxLeft > 0, 'Recommendation SQL compare original pane must scroll horizontally.')
  assert(originalMetrics.maxTop > 0, 'Recommendation SQL compare original pane must scroll vertically.')

  await originalViewport.evaluate(element => {
    element.scrollLeft = Math.min(180, element.scrollWidth - element.clientWidth)
    element.scrollTop = Math.min(160, element.scrollHeight - element.clientHeight)
    element.dispatchEvent(new Event('scroll', { bubbles: true }))
  })
  await compareLocator.page().waitForTimeout(120)

  const originalScroll = await originalViewport.evaluate(element => ({
    left: element.scrollLeft,
    top: element.scrollTop
  }))
  const recommendedScroll = await recommendedViewport.evaluate(element => ({
    left: element.scrollLeft,
    top: element.scrollTop
  }))
  assert(Math.abs(recommendedScroll.left - originalScroll.left) <= 1, 'Recommendation SQL compare must sync right pane horizontal scroll.')
  assert(Math.abs(recommendedScroll.top - originalScroll.top) <= 1, 'Recommendation SQL compare must sync right pane vertical scroll.')
}

const recommendation = {
  recommendationId,
  tenantId,
  recommendationType: 'REWRITE',
  status: 'RECOMMENDED',
  benefitLevel: 'HIGH',
  riskLevel: 'MEDIUM',
  summary: 'PRW-012 production rewrite candidate',
  reason: 'projection pruning',
  expectedGain: 'lower scanned bytes',
  riskSummary: 'requires SQL rewrite activation evidence',
  requiresDispatch: false,
  sourceType: 'QUERY',
  sourceKind: 'QUERY_HISTORY',
  sourceId: historyId,
  historyId,
  evidenceLevel: 'RUNTIME_HISTORY',
  sqlFingerprint,
  sourceSqlText: originalSql,
  recommendedSqlText: recommendedSql,
  targetEngine: 'HETU',
  targetDatasource: 'hetu_main',
  reportCode: 'RPT_PRW_012',
  logicalObjectKey: 'TABLE:orders',
  validationStatus: 'EQUIVALENT',
  autoApplyAllowed: true,
  manualReviewRequired: true,
  ruleChain: [{ ruleCode: 'SELECT_STAR_PRUNE', status: 'APPLIED' }],
  preconditions: [{ code: 'READONLY_ONLY', passed: true }],
  semanticRisks: [{ code: 'PROJECTION_CHANGE', severity: 'MEDIUM' }],
  unappliedRules: []
}

let rewriteRecord = {
  rewriteRecordId,
  recommendationId,
  tenantId,
  sourceType: 'QUERY',
  sourceKind: 'QUERY_HISTORY',
  sourceId: historyId,
  evidenceLevel: 'RUNTIME_HISTORY',
  historyId,
  sqlFingerprint,
  datasourceCode: 'hetu_main',
  status: 'APPLIED',
  activationStatus: 'INACTIVE',
  validationStatus: 'EQUIVALENT',
  alertStatus: 'NONE',
  lastValidationRunId: 'validation-equivalent-prw-012',
  autoApplyAllowed: true,
  manualReviewRequired: true,
  originalSqlText: originalSql,
  recommendedSqlText: recommendedSql,
  executedSqlText: recommendedSql,
  runtimeBindingId: '',
  runtimeRuleVersion: '',
  runtimeBindingScope: '',
  traceRefs: {
    recommendationEvidence: 'PRW-012',
    alertRefs: []
  },
  diffSummary: {
    changeCount: 1,
    writesBackRecommendation: true,
    evidenceBoundary: 'production-rewrite-closed-loop'
  },
  ruleChain: [{ ruleCode: 'SELECT_STAR_PRUNE', status: 'APPLIED' }]
}

const clone = value => JSON.parse(JSON.stringify(value))

const activationEligibility = () => ({
  rewriteRecordId,
  tenantId,
  eligible:
    rewriteRecord.validationStatus === 'EQUIVALENT' &&
    ['INACTIVE', 'ACTIVATE_FAILED', 'PAUSED'].includes(rewriteRecord.activationStatus),
  policyId: 'DEFAULT_REWRITE_ACTIVATION_ELIGIBILITY',
  validationStatus: rewriteRecord.validationStatus,
  activationStatus: rewriteRecord.activationStatus,
  alertStatus: rewriteRecord.alertStatus,
  autoApplyAllowed: rewriteRecord.autoApplyAllowed,
  lastValidationRunId: rewriteRecord.lastValidationRunId,
  refusalReasons:
    ['INACTIVE', 'ACTIVATE_FAILED', 'PAUSED'].includes(rewriteRecord.activationStatus)
      ? []
      : [{ code: 'ACTIVATION_STATUS_NOT_READY', message: 'Rewrite record is not ready for activation', blocking: true }]
})

const historyRow = () => ({
  historyId,
  tenantId,
  traceId: 'trace-prw-012',
  reportCode: 'RPT_PRW_012',
  datasourceCode: 'hetu_main',
  stageCode: 'PROD',
  resultStatus: 'SUCCESS',
  accessChannel: 'API',
  targetEngine: 'HETU',
  queryDateStart: '2026-05-12',
  queryDateEnd: '2026-05-12',
  queryDateStatus: 'RESOLVED',
  parameterizedSqlFlag: false,
  bindingMode: 'LITERAL',
  rewriteApplied: false,
  hasRewriteRecord: true,
  rewriteRecordId,
  recommendationId,
  submittedBy: 'operator-001',
  submittedAt: '2026-05-12T01:12:00Z',
  auditEventCount: 3
})

const historyDetail = () => ({
  ...historyRow(),
  historyType: 'QUERY_EXECUTION',
  sqlText: originalSql,
  sqlTemplate: originalSql,
  boundSql: originalSql,
  actualSql: originalSql,
  returnedRowCount: 1,
  cacheHit: false,
  accelerationApplied: false,
  executionSummary: {
    rewriteApplied: false,
    cacheHit: false,
    accelerationApplied: false,
    returnedRowCount: 1
  },
  rewriteAudit: {
    rewriteApplied: false,
    rewriteRecordId,
    runtimeBindingId: '',
    ruleVersion: null,
    runtimeRuleVersion: '',
    runtimeRewriteStatus: 'MISSING',
    rewriteActivationStatusSnapshot: 'INACTIVE',
    originalSql,
    actualSql: originalSql
  },
  sqlState: {
    originalSql,
    actualSql: originalSql,
    sqlFingerprint
  },
  routeDecision: { selectedEngine: 'HETU' },
  cacheSummary: { cacheHit: false },
  traceDetail: {
    auditEventCount: 3,
    auditEvents: [
      { action: 'REWRITE_ACTIVATED', subjectId: rewriteRecordId },
      { action: 'REWRITE_PAUSED', subjectId: rewriteRecordId }
    ]
  }
})

const queryHistoryRewriteRecords = () => ({
  historyId,
  tenantId,
  rewriteRecordCount: 1,
  contractStage: 'LONG_TERM_BASELINE',
  implementationStage: 'PRW_012_BROWSER_SMOKE',
  items: [
    {
      ...clone(rewriteRecord),
      autoApplyPaused: true,
      alertRefs: [{ alertId: 'alert-prw-012', alertType: 'SQL_REWRITE_RESULT_DIVERGENCE' }],
      traceRefs: {
        ...clone(rewriteRecord.traceRefs),
        autoApplyPauseEvidence: {
          autoApplyPaused: true,
          reason: 'SQL_REWRITE_RESULT_DIVERGENCE'
        },
        divergenceAlert: {
          autoApplyPaused: true,
          alertType: 'SQL_REWRITE_RESULT_DIVERGENCE',
          linkages: [{ alertId: 'alert-prw-012' }]
        }
      }
    }
  ]
})

const assertFrontendHeaders = request => {
  const headers = request.headers()
  assert(headers['x-sqlforge-dev-tenant-id'] === tenantId, `Unexpected tenant header ${headers['x-sqlforge-dev-tenant-id']}`)
  const prefix = String(headers['x-sqlforge-dev-request-prefix'] || '')
  assert(
    prefix.startsWith('frontend-recommendation-') ||
      prefix.startsWith('frontend-sql-history-') ||
      prefix.startsWith('frontend-governance-') ||
      prefix.startsWith('frontend-query-history-'),
    `Unexpected request prefix ${prefix}`
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
    const { pathname, searchParams } = requestUrl
    const method = request.method()
    const key = `${method} ${pathname}`
    assertFrontendHeaders(request)

    if (method === 'GET' && pathname === '/api/sql-optimization/recommendations/page') {
      assert(searchParams.get('pageNo') === '1', 'Recommendation page must request pageNo=1 by default.')
      assert(searchParams.get('pageSize') === '8', 'Recommendation page must request pageSize=8 by default.')
      assert(searchParams.get('sortBy') === 'createdAt', 'Recommendation page must use the governed sort key.')
      seen.add(key)
      await fulfillJson(route, {
        items: [recommendation],
        pageNo: 1,
        pageSize: 8,
        totalCount: 1,
        pageCount: 1,
        hasMore: false
      })
      return
    }

    if (method === 'GET' && pathname === `/api/sql-optimization/recommendations/${recommendationId}`) {
      seen.add(key)
      await fulfillJson(route, recommendation)
      return
    }

    if (method === 'GET' && pathname === `/api/sql-optimization/recommendations/${recommendationId}/trace`) {
      seen.add(key)
      await fulfillJson(route, {
        recommendationId,
        historyId,
        sqlFingerprint,
        reportCode: 'RPT_PRW_012',
        logicalObjectKey: 'TABLE:orders',
        alertId: 'alert-prw-012',
        alertStatus: 'OPEN',
        traceRefs: {
          rewriteRecordId,
          auditRefs: ['rewrite-activated', 'rewrite-status-active']
        },
        alertRefs: [{ alertId: 'alert-prw-012', alertType: 'SQL_REWRITE_RESULT_DIVERGENCE' }]
      })
      return
    }

    if (method === 'GET' && pathname === `/api/sql-optimization/recommendations/${recommendationId}/diff`) {
      seen.add(key)
      await fulfillJson(route, {
        recommendationId,
        tenantId,
        sourceType: 'QUERY',
        sourceKind: 'QUERY_HISTORY',
        sourceId: historyId,
        evidenceLevel: 'RUNTIME_HISTORY',
        sqlFingerprint,
        originalSql,
        recommendedSql,
        textDiff: [
          {
            hunkId: 'hunk-1',
            type: 'REPLACE',
            granularity: 'TOKEN',
            originalStartLine: 1,
            recommendedStartLine: 1,
            originalStartUnit: 2,
            recommendedStartUnit: 2,
            originalText: '*',
            recommendedText: 'id',
            originalUnits: ['*'],
            recommendedUnits: ['id']
          }
        ],
        ruleDiff: [{ diffId: 'rule-1', ruleCode: 'SELECT_STAR_PRUNE', textHunkIds: ['hunk-1'] }],
        astSummaryDiff: { projectionChanged: true },
        diffSummary: {
          changeCount: 1,
          ruleDiffCount: 1,
          manualReviewRequired: true,
          autoApplyAllowed: true,
          writesBackRecommendation: true,
          evidenceBoundary: 'production-rewrite-closed-loop'
        },
        diffStatus: 'READY'
      })
      return
    }

    if (method === 'GET' && pathname === '/api/sql-optimization/dispatch-contract') {
      seen.add(key)
      await fulfillJson(route, {
        coordinationMode: 'PULL_ONLY',
        sqlExecutionAllowed: false,
        dataLoadingAllowed: false,
        activeExternalPushAllowed: false,
        externalPullRequired: true,
        allowedEventStatuses: ['READY'],
        allowedDispatchTypes: ['REWRITE_RECORD']
      })
      return
    }

    if (method === 'GET' && pathname === '/api/sql-optimization/dispatch-events') {
      seen.add(key)
      await fulfillJson(route, [])
      return
    }

    if (method === 'GET' && pathname === '/api/sql-optimization/rewrite-records') {
      assert(searchParams.get('recommendationId') === recommendationId, 'Recommendation smoke must load rewrite records by recommendationId.')
      seen.add(`${key}?recommendationId`)
      await fulfillJson(route, [rewriteRecord])
      return
    }

    if (method === 'GET' && pathname === `/api/sql-optimization/rewrite-records/${rewriteRecordId}`) {
      seen.add(key)
      await fulfillJson(route, rewriteRecord)
      return
    }

    if (method === 'GET' && pathname === `/api/sql-optimization/rewrite-records/${rewriteRecordId}/activation-eligibility`) {
      seen.add(key)
      await fulfillJson(route, activationEligibility())
      return
    }

    if (method === 'GET' && pathname === `/api/sql-optimization/rewrite-records/${rewriteRecordId}/validation-runs`) {
      seen.add(key)
      await fulfillJson(route, [
        {
          validationRunId: validationRunId,
          rewriteRecordId,
          recommendationId,
          status: 'FINISHED',
          comparisonStatus: rewriteRecord.validationStatus,
          differenceType: 'RESULT_SET',
          autoApplyPaused: true,
          startedAt: '2026-05-12T01:10:00Z',
          finishedAt: '2026-05-12T01:10:05Z'
        }
      ])
      return
    }

    if (method === 'POST' && pathname === `/api/sql-optimization/rewrite-records/${rewriteRecordId}/activate`) {
      const payload = parseJsonBody(request)
      assert(payload.tenantId === tenantId, `Unexpected activate tenant ${payload.tenantId}`)
      assert(String(payload.reason || '').trim(), 'Rewrite activate must submit an action reason.')
      rewriteRecord = {
        ...rewriteRecord,
        activationStatus: 'ACTIVE',
        runtimeBindingId,
        runtimeRuleVersion,
        runtimeBindingScope: 'TENANT_SQL_FINGERPRINT',
        runtimeBindingAt: '2026-05-12T01:12:20Z',
        runtimeBindingBy: 'operator-001',
        traceRefs: {
          ...rewriteRecord.traceRefs,
          activationEvidence: {
            action: 'ACTIVATE',
            activationStatus: 'ACTIVE',
            runtimeBindingId,
            runtimeRuleVersion
          }
        }
      }
      seen.add(key)
      await fulfillJson(route, rewriteRecord)
      return
    }

    if (method === 'POST' && pathname === `/api/sql-optimization/rewrite-records/${rewriteRecordId}/pause`) {
      const payload = parseJsonBody(request)
      assert(payload.tenantId === tenantId, `Unexpected pause tenant ${payload.tenantId}`)
      assert(String(payload.reason || '').trim(), 'Rewrite pause must submit an action reason.')
      rewriteRecord = {
        ...rewriteRecord,
        activationStatus: 'PAUSED',
        validationStatus: 'DIVERGED',
        alertStatus: 'OPEN',
        lastValidationRunId: 'validation-diverged-prw-012',
        autoApplyPaused: true,
        traceRefs: {
          ...rewriteRecord.traceRefs,
          autoApplyPauseEvidence: {
            autoApplyPaused: true,
            reason: 'SQL_REWRITE_RESULT_DIVERGENCE'
          },
          divergenceAlert: {
            autoApplyPaused: true,
            alertType: 'SQL_REWRITE_RESULT_DIVERGENCE',
            linkages: [{ alertId: 'alert-prw-012' }]
          },
          pauseEvidence: {
            action: 'PAUSE',
            activationStatus: 'PAUSED',
            statusOnly: true
          }
        }
      }
      seen.add(key)
      await fulfillJson(route, rewriteRecord)
      return
    }

    if (method === 'GET' && pathname === '/api/governance/datasources') {
      seen.add(key)
      await fulfillJson(route, [{ datasourceCode: 'hetu_main', datasourceType: 'HETU', tenantId }])
      return
    }

    if (method === 'GET' && pathname === '/api/governance/query-history') {
      assert(searchParams.get('historyType') === 'QUERY_EXECUTION', 'SQL history must request QUERY_EXECUTION history.')
      seen.add(key)
      await fulfillJson(route, {
        pageNo: Number(searchParams.get('pageNo') || '1'),
        pageSize: Number(searchParams.get('pageSize') || '10'),
        totalCount: 1,
        pageCount: 1,
        items: [historyRow()],
        classificationSummary: {
          statusCounts: { SUCCESS: 1 },
          accessChannelCounts: { API: 1 }
        }
      })
      return
    }

    if (method === 'GET' && pathname === `/api/governance/query-history/${historyId}`) {
      seen.add(key)
      await fulfillJson(route, historyDetail())
      return
    }

    if (method === 'GET' && pathname === `/api/governance/query-history/${historyId}/rewrite-records`) {
      seen.add(key)
      await fulfillJson(route, queryHistoryRewriteRecords())
      return
    }

    unexpectedApiRequests.push(key)
    await route.fulfill({
      status: 404,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify({ error: 'UNEXPECTED_PRW012_ENDPOINT', path: pathname })
    })
  })

  try {
    await page.goto(
      `${baseUrl}${ROUTE_PATHS.recommendationCenter}?tenantId=${tenantId}&recommendationId=${recommendationId}&rewriteRecordId=${rewriteRecordId}`,
      { waitUntil: 'domcontentloaded' }
    )
    await page.getByTestId('recommendation-page').waitFor({ timeout: defaultTimeoutMs })
    await page.getByRole('tab', { name: /SQL diff|SQL 差异/ }).click()
    const recommendationCompare = page.getByTestId('recommendation-sql-compare')
    await recommendationCompare.waitFor({ timeout: defaultTimeoutMs })
    const compareText = await recommendationCompare.textContent()
    const compareCodeText = await recommendationCompare
      .locator('.sql-compare-code')
      .evaluateAll(nodes => nodes.map(node => node.textContent || '').join('\n'))
    assert(compareText.includes('-- report_code=RPT_PRW_012'), 'Recommendation SQL compare must carry source leading comments.')
    assert(compareText.includes('*'), 'Recommendation SQL compare must expose the original SQL fragment.')
    assert(compareText.includes('id'), 'Recommendation SQL compare must expose the recommended SQL fragment.')
    assert(compareCodeText.includes('EXISTS'), 'Recommendation SQL compare must render nested EXISTS predicates.')
    assert(compareCodeText.includes('IN ('), 'Recommendation SQL compare must render nested IN subqueries.')
    assert(
      compareCodeText.includes("'literal select from production smoke'") &&
        compareCodeText.includes("'select/from literal'"),
      'Recommendation SQL compare must preserve SQL-like quoted literals inside nested SQL.'
    )
    assert(
      (compareCodeText.match(/\n\s{8,}SELECT\b/g) || []).length >= 2,
      'Recommendation SQL compare must show multi-level nested SELECT indentation.'
    )
    assert(
      (await recommendationCompare.locator('.sql-compare-token-mark--delete').count()) > 0,
      'Recommendation SQL compare must expose token-level delete marks.'
    )
    assert(
      (await recommendationCompare.locator('.sql-compare-token-mark--insert').count()) > 0,
      'Recommendation SQL compare must expose token-level insert marks.'
    )
    await assertSqlCompareDualPane(recommendationCompare)
    await page.getByRole('tab', { name: /SQL evidence|SQL 证据/ }).click()
    const recommendedSqlEvidenceText = await page.getByTestId('recommendation-recommended-sql').textContent()
    assert(
      recommendedSqlEvidenceText.includes('-- report_code=RPT_PRW_012'),
      'Recommendation SQL evidence must carry source leading comments on the recommended display SQL.'
    )

    await page.getByRole('tab', { name: /Rewrite activation and pause|改写激活与暂停/ }).click()
    await page.getByTestId('recommendation-rewrite-lifecycle').waitFor({ timeout: defaultTimeoutMs })
    await page.getByTestId('recommendation-rewrite-activate').click()
    await page.getByTestId('recommendation-rewrite-lifecycle-success').waitFor({ timeout: defaultTimeoutMs })
    let lifecycleText = await page.getByTestId('recommendation-rewrite-lifecycle').textContent()
    assert(lifecycleText.includes('ACTIVE'), 'Recommendation lifecycle must show active rewrite status.')
    assert(lifecycleText.includes(runtimeBindingId), 'Recommendation lifecycle activation must expose runtime binding id.')
    assert(lifecycleText.includes(runtimeRuleVersion), 'Recommendation lifecycle activation must expose runtime rule version.')

    await page.getByTestId('recommendation-rewrite-pause').click()
    await page.getByTestId('recommendation-rewrite-lifecycle-success').waitFor({ timeout: defaultTimeoutMs })
    lifecycleText = await page.getByTestId('recommendation-rewrite-lifecycle').textContent()
    assert(lifecycleText.includes('PAUSED'), 'Recommendation lifecycle must show paused rewrite status.')
    assert(lifecycleText.includes('DIVERGED'), 'Recommendation lifecycle must expose divergence validation status.')

    await page.goto(
      `${baseUrl}${ROUTE_PATHS.sqlHistory}?tenantId=${tenantId}&historyId=${historyId}`,
      { waitUntil: 'domcontentloaded' }
    )
    await page.getByTestId('sql-history-page').waitFor({ timeout: defaultTimeoutMs })
    await page.getByTestId('sql-history-detail-drawer').waitFor({ timeout: defaultTimeoutMs })
    await page.getByRole('tab', { name: /Rewrite records|改写记录/ }).click()
    await page.getByTestId('sql-history-rewrite-record-table').waitFor({ timeout: defaultTimeoutMs })
    await page.getByTestId('sql-history-rewrite-record-detail-link').first().waitFor({ timeout: defaultTimeoutMs })
    const historyRewriteText = await page.getByTestId('sql-history-rewrite-records-tab').textContent()
    assert(historyRewriteText.includes(rewriteRecordId), 'SQL history rewrite tab must expose rewrite record id.')
    assert(historyRewriteText.includes(recommendationId), 'SQL history rewrite tab must expose recommendation id.')
    assert(historyRewriteText.includes('SQL_REWRITE_RESULT_DIVERGENCE'), 'SQL history rewrite tab must expose divergence alert linkage.')
    assert(historyRewriteText.includes('true'), 'SQL history rewrite tab must expose autoApplyPaused evidence.')

    await page.getByTestId('sql-history-rewrite-record-detail-link').first().click()
    await page.waitForURL(
      url =>
        url.pathname === ROUTE_PATHS.recommendationCenter &&
        url.searchParams.get('recommendationId') === recommendationId &&
        url.searchParams.get('rewriteRecordId') === rewriteRecordId,
      { timeout: defaultTimeoutMs }
    )
    await page.getByTestId('recommendation-page').waitFor({ timeout: defaultTimeoutMs })

    const requiredSeen = [
      'GET /api/sql-optimization/recommendations/page',
      `GET /api/sql-optimization/recommendations/${recommendationId}`,
      `GET /api/sql-optimization/recommendations/${recommendationId}/diff`,
      `GET /api/sql-optimization/recommendations/${recommendationId}/trace`,
      'GET /api/sql-optimization/rewrite-records?recommendationId',
      `GET /api/sql-optimization/rewrite-records/${rewriteRecordId}/validation-runs`,
      `GET /api/sql-optimization/rewrite-records/${rewriteRecordId}/activation-eligibility`,
      `POST /api/sql-optimization/rewrite-records/${rewriteRecordId}/activate`,
      `POST /api/sql-optimization/rewrite-records/${rewriteRecordId}/pause`,
      'GET /api/governance/query-history',
      `GET /api/governance/query-history/${historyId}`,
      `GET /api/governance/query-history/${historyId}/rewrite-records`
    ]
    const missing = requiredSeen.filter(item => !seen.has(item))
    assert(missing.length === 0, `PRW-012 browser smoke missed endpoint(s): ${missing.join(', ')}`)
    assert(unexpectedApiRequests.length === 0, `Unexpected API request(s): ${unexpectedApiRequests.join(', ')}`)
    assert(pageErrors.length === 0, `Browser page error(s): ${pageErrors.join('; ')}`)
  } finally {
    await browser.close()
  }
}

const main = async () => {
  const port = await getAvailablePort()
  const baseUrl = `http://127.0.0.1:${port}`
  const viteBinPath = path.join(repoRoot, 'node_modules', 'vite', 'bin', 'vite.js')
  const child = spawn('node', [viteBinPath, '--host', '127.0.0.1', '--port', String(port)], {
    stdio: ['ignore', 'ignore', 'pipe'],
    env: {
      ...process.env,
      TMPDIR: process.env.TMPDIR || smokeTmpDir
    }
  })
  let stderr = ''
  child.stderr.on('data', chunk => {
    stderr += chunk.toString()
  })

  try {
    await waitForDevServer(baseUrl)
    await runBrowserSmoke(baseUrl)
    console.log('Production rewrite closed-loop browser smoke passed')
  } catch (error) {
    if (stderr.trim()) {
      console.error(stderr.trim())
    }
    throw error
  } finally {
    await stopChild(child)
  }
}

main().then(() => {
  process.exit(0)
}).catch(error => {
  console.error(error)
  process.exit(1)
})
