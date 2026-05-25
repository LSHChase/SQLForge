import fs from 'node:fs'
import http from 'node:http'
import path from 'node:path'
import { spawn } from 'node:child_process'
import { chromium } from 'playwright'
import { ROUTE_PATHS } from '../src/config/routePaths.mjs'

const defaultTimeoutMs = Number(process.env.FRONTEND_DEV_SMOKE_TIMEOUT_MS || 20000)
const rewriteValidationScreenshotPath = process.env.SQLFORGE_DEV_SMOKE_REWRITE_VALIDATION_SCREENSHOT || ''
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

const recommendationLongLiteral = `recommendation_scroll_marker_${'x'.repeat(180)}`
const recommendationLongPredicates = Array.from(
  { length: 42 },
  (_, index) => `AND metric_${index} = '${recommendationLongLiteral}_${index}'`
).join(' ')
const recommendationDeepNestedPredicate = `dt IN (
  SELECT dt FROM mart.calendar_day WHERE calendar_key IN (
    SELECT calendar_key FROM mart.calendar_acl WHERE EXISTS (
      SELECT 1 FROM security.acl acl WHERE acl.calendar_key = mart.calendar_acl.calendar_key AND acl.tenant_id IN (
        SELECT tenant_id FROM security.tenant_scope WHERE scope_id IN (
          SELECT scope_id FROM security.scope_group WHERE group_id IN (
            SELECT group_id FROM security.group_owner WHERE owner_note = 'literal select from dev smoke'
          )
        )
      )
    )
  )
)`
const recommendationSourceSql = `-- report_code=DEV_RPT_REWRITE
/* owner: recommendation smoke */
SELECT * FROM (
  SELECT order_id, id, dt, metric_0 FROM sales.orders WHERE ${recommendationDeepNestedPredicate} ${recommendationLongPredicates}
) d WHERE EXISTS (
  SELECT 1 FROM mart.order_quality q WHERE q.order_id = d.order_id AND q.status IN (
    SELECT status FROM mart.valid_status WHERE status_note = 'select/from literal'
  )
)`
const recommendationDiffOriginalSql = `SELECT * FROM (
  SELECT order_id, id, dt, metric_0 FROM sales.orders WHERE ${recommendationDeepNestedPredicate} ${recommendationLongPredicates}
) d WHERE EXISTS (
  SELECT 1 FROM mart.order_quality q WHERE q.order_id = d.order_id AND q.status IN (
    SELECT status FROM mart.valid_status WHERE status_note = 'select/from literal'
  )
)`
const recommendationRecommendedSql = `SELECT id FROM (
  SELECT order_id, id, dt, metric_0 FROM sales.orders WHERE ${recommendationDeepNestedPredicate} ${recommendationLongPredicates}
) d WHERE EXISTS (
  SELECT 1 FROM mart.order_quality q WHERE q.order_id = d.order_id AND q.status IN (
    SELECT status FROM mart.valid_status WHERE status_note = 'select/from literal'
  )
)`

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
  assert(originalMetrics.maxLeft > 0, '推荐 SQL compare 左侧 pane 必须能独立横向滚动。')
  assert(originalMetrics.maxTop > 0, '推荐 SQL compare 左侧 pane 必须能独立纵向滚动。')

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
  assert(Math.abs(recommendedScroll.left - originalScroll.left) <= 1, '推荐 SQL compare 右侧 pane 必须同步左侧横向滚动。')
  assert(Math.abs(recommendedScroll.top - originalScroll.top) <= 1, '推荐 SQL compare 右侧 pane 必须同步左侧纵向滚动。')

  await recommendedViewport.evaluate(element => {
    element.scrollLeft = Math.min(90, element.scrollWidth - element.clientWidth)
    element.scrollTop = Math.min(80, element.scrollHeight - element.clientHeight)
    element.dispatchEvent(new Event('scroll', { bubbles: true }))
  })
  await compareLocator.page().waitForTimeout(120)

  const originalSyncedBack = await originalViewport.evaluate(element => ({
    left: element.scrollLeft,
    top: element.scrollTop
  }))
  const recommendedSyncedBack = await recommendedViewport.evaluate(element => ({
    left: element.scrollLeft,
    top: element.scrollTop
  }))
  assert(
    Math.abs(originalSyncedBack.left - recommendedSyncedBack.left) <= 1,
    '推荐 SQL compare 左侧 pane 必须同步右侧横向滚动。'
  )
  assert(
    Math.abs(originalSyncedBack.top - recommendedSyncedBack.top) <= 1,
    '推荐 SQL compare 左侧 pane 必须同步右侧纵向滚动。'
  )
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
  let recommendationRewriteValidationRunCalls = 0
  let rewriteValidationTaskPolls = 0
  let rewriteValidationRecordCreateCalls = 0
  let rewriteValidationRunCreateCalls = 0
  const sqlHistoryPageRequests = []
  const recommendationPageRequests = []
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
      if (pathname === '/api/sql-optimization/recommendations/page') {
        await fulfillJson(route, {
          items: [],
          pageNo: 1,
          pageSize: 8,
          totalCount: 0,
          pageCount: 0,
          hasMore: false
        })
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

    if (pathname === '/api/sql-optimization/tasks' && requestPrefix === 'frontend-rewrite-validation-task-submit') {
      assertDevHeaders(request, 'tenant-a', ['frontend-rewrite-validation-task-submit'])
      const payload = parseJsonBody(request)
      assert(payload.taskType === 'REWRITE', 'SQL 改写验证必须提交 REWRITE 优化任务。')
      assert(payload.sqlText, 'SQL 改写验证任务必须携带原 SQL。')
      assert(
        payload.taskContext?.sourceType === 'COMBINED_PARSE',
        `人工改写验证任务需要映射到 COMBINED_PARSE 来源，实际为 ${payload.taskContext?.sourceType}`
      )
      assert(
        Array.isArray(payload.taskContext?.issueScenes) && payload.taskContext.issueScenes.includes('SELECT_STAR'),
        'SQL 改写验证任务必须携带问题场景以便后端落推荐。'
      )
      await fulfillJson(route, {
        taskId: 'rewrite-task-dev-1',
        status: 'QUEUED',
        currentPhase: 'SUBMITTED',
        estimatedReadyAt: '2026-05-17T10:00:00',
        statusQueryPath: '/api/sql-optimization/tasks/rewrite-task-dev-1',
        contractStage: 'LONG_TERM_BASELINE',
        implementationStage: 'ACCELERATION_PLAN_GOVERNANCE_BASELINE'
      })
      return
    }

    if (
      pathname === '/api/sql-optimization/tasks/rewrite-task-dev-1' &&
      (requestPrefix.startsWith('frontend-rewrite-validation-task-poll-') ||
        requestPrefix.startsWith('frontend-sql-optimization-poll-') ||
        requestPrefix === 'frontend-sql-optimization-status')
    ) {
      assertDevHeaders(request, 'tenant-a', [requestPrefix])
      rewriteValidationTaskPolls += 1
      await fulfillJson(route, {
        taskId: 'rewrite-task-dev-1',
        taskType: 'REWRITE',
        status: 'SUCCEEDED',
        currentPhase: 'FINISHED',
        priority: 'NORMAL',
        progressPercent: 100,
        requestedSuggestionTypes: [],
        suggestion: {
          summary: '已生成候选改写 SQL，包含 1 条安全 AST 规则。',
          primaryRecommendation: '请先将候选改写结果与原始语句做校验，再把批准后的 SQL 带入下一步治理。',
          confidenceScore: 68,
          artifacts: [
            {
              category: 'REWRITTEN_SQL',
              name: 'candidateSql',
              content: "SELECT id FROM orders WHERE dt = '2026-04-01' ORDER BY id"
            },
            {
              category: 'REWRITE_RULE_TRACE',
              name: 'appliedRules',
              content: JSON.stringify(['REMOVE_DUPLICATE_PREDICATE'])
            },
            {
              category: 'AST_PROFILE',
              name: 'astProfile',
              content: JSON.stringify({
                statementType: 'SELECT',
                parserEngine: 'JSQLPARSER',
                tables: ['orders'],
                projectionCount: 1,
                predicateCount: 1,
                joinCount: 0,
                duplicateOrderByKeyCount: 1,
                duplicateGroupByKeyCount: 0,
                selectStar: false,
                repeatedSubqueryCount: 0
              })
            }
          ],
          benefits: [{ category: 'PLAN_SIMPLIFICATION', score: 48, description: '逻辑计划更小。' }],
          costs: [{ category: 'VALIDATION', level: 'MEDIUM', description: '批准前需要做结果集差异校验。' }],
          risks: [
            {
              category: 'SEMANTIC_VALIDATION_REQUIRED',
              level: 'MEDIUM',
              description: '批准前仍需要对比结果集。',
              suggestion: '在代表性样本上执行只读摘要校验。'
            }
          ]
        },
        failure: null,
        statusHistory: [
          { status: 'QUEUED', note: 'TASK_SUBMITTED', occurredAt: '2026-05-17T10:00:00' },
          { status: 'SUCCEEDED', note: 'WORKER_REWRITE_ARTIFACTS_READY', occurredAt: '2026-05-17T10:00:01' }
        ],
        contractStage: 'LONG_TERM_BASELINE',
        implementationStage: 'ACCELERATION_PLAN_GOVERNANCE_BASELINE'
      })
      return
    }

    if (pathname === '/api/sql-optimization/recommendations/page' && requestPrefix === 'frontend-rewrite-validation-recommendation-page') {
      assert(requestUrl.searchParams.get('recommendationType') === 'REWRITE', '改写验证推荐读取必须限定 REWRITE。')
      assert(requestUrl.searchParams.get('sourceType') === 'PARSE', '改写验证推荐读取必须携带 PARSE 来源类型。')
      assert(requestUrl.searchParams.get('sourceKind') === 'COMBINED_PARSE', '人工改写验证推荐读取必须携带 COMBINED_PARSE 来源。')
      assert(requestUrl.searchParams.get('sourceId') === 'manual-rewrite-validation', '改写验证推荐读取必须携带来源 ID。')
      await fulfillJson(route, {
        items: [
          {
            recommendationId: 'rec-dev-1',
            tenantId: 'tenant-a',
            recommendationType: 'REWRITE',
            validationStatus: 'NOT_VALIDATED',
            benefitLevel: 'MEDIUM',
            riskLevel: 'MEDIUM',
            sourceType: 'PARSE',
            sourceKind: 'COMBINED_PARSE',
            sourceId: 'manual-rewrite-validation',
            sourceSqlText: recommendationSourceSql,
            recommendedSqlText: recommendationRecommendedSql
          }
        ],
        pageNo: 1,
        pageSize: 5,
        totalCount: 1,
        pageCount: 1,
        hasMore: false
      })
      return
    }

    if (pathname === '/api/sql-optimization/rewrite-records' && requestPrefix === 'frontend-rewrite-validation-record-create') {
      const payload = parseJsonBody(request)
      rewriteValidationRecordCreateCalls += 1
      assert(payload.sourceKind === 'MANUAL', '改写验证创建改写记录时必须保留 MANUAL 来源场景。')
      assert(payload.originalSqlText && payload.recommendedSqlText, '改写验证创建改写记录必须携带原 SQL 和推荐 SQL。')
      await fulfillJson(route, {
        rewriteRecordId: 'rewrite-validation-dev-1',
        tenantId: 'tenant-a',
        recommendationId: payload.recommendationId || 'rec-dev-1',
        optimizationTaskId: 'rewrite-task-dev-1',
        sourceType: 'PARSE',
        sourceKind: 'MANUAL',
        sourceId: 'manual-rewrite-validation',
        evidenceLevel: 'STATIC_PARSE',
        validationStatus: 'NOT_VALIDATED',
        activationStatus: 'INACTIVE',
        alertStatus: 'NONE',
        autoApplyAllowed: false,
        manualReviewRequired: true,
        originalSqlText: payload.originalSqlText,
        recommendedSqlText: payload.recommendedSqlText
      })
      return
    }

    if (
      pathname === '/api/sql-optimization/rewrite-records/rewrite-validation-dev-1/validation-runs' &&
      requestPrefix === 'frontend-rewrite-validation-run-list'
    ) {
      await fulfillJson(route, [])
      return
    }

    if (
      pathname === '/api/sql-optimization/rewrite-records/rewrite-validation-dev-1/validation-runs' &&
      requestPrefix === 'frontend-rewrite-validation-run-create'
    ) {
      const payload = parseJsonBody(request)
      rewriteValidationRunCreateCalls += 1
      assert(payload.comparisonStatus === 'NOT_COMPARED', '前端静态改写验证不得声明结果已等价。')
      await fulfillJson(route, {
        validationRunId: 'validation-rewrite-page-dev-1',
        tenantId: 'tenant-a',
        rewriteRecordId: 'rewrite-validation-dev-1',
        recommendationId: 'rec-dev-1',
        status: 'SUCCEEDED',
        comparisonStatus: 'NOT_COMPARED',
        differenceType: 'UNKNOWN',
        autoApplyPaused: false,
        startedAt: '2026-05-17T10:00:02',
        finishedAt: '2026-05-17T10:00:03'
      })
      return
    }

    if (pathname === '/api/sql-optimization/parse/structure' && requestPrefix === 'frontend-parse-workbench-structure') {
      assertDevHeaders(request, 'tenant-a', ['frontend-parse-workbench-structure'])
      const payload = parseJsonBody(request)
      assert(payload.sqlText, 'SQL 改写验证结构解析请求必须携带 SQL 文本。')
      await fulfillJson(route, {
        parseTaskId: 'dev-parse-structure-1',
        historyId: 'dev-parse-history-structure-1',
        sqlFingerprint: 'dev-parse-fingerprint-1',
        analysisStatus: 'SUCCEEDED',
        structureAnalysisStatus: 'SUCCEEDED',
        syntaxStatus: 'VALID',
        complexityLevel: 'LOW',
        sqlType: 'SELECT',
        priorityLevel: 'P2',
        priorityScore: 42,
        important: false,
        urgent: false,
        featureSummary: {
          parserEngine: 'JSQLPARSER',
          scanMode: 'TABLE_SCAN',
          tableCount: 1,
          duplicateOrderByKeyCount: 1,
          duplicateGroupByKeyCount: 0,
          repeatedSubqueryCount: 0
        },
        logicalObjectHits: [{ objectType: 'TABLE', objectKey: 'sales.orders' }],
        riskTags: ['DUPLICATE_GROUP_OR_ORDER_KEY_RISK'],
        rewriteCandidates: ['DUPLICATE_GROUP_ORDER_KEY'],
        riskChecklist: [],
        issues: []
      })
      return
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
        reportCode: requestUrl.searchParams.get('reportCode') || '',
        hasRewriteRecord: requestUrl.searchParams.get('hasRewriteRecord') || '',
        rewriteValidationStatus: requestUrl.searchParams.get('rewriteValidationStatus') || ''
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
            originalSqlText: recommendationSourceSql,
            recommendedSqlText: recommendationRecommendedSql,
            executedSqlText: recommendationRecommendedSql,
            ruleChain: [{ ruleCode: 'SELECT_STAR', action: 'PROJECT_COLUMNS' }],
            diffSummary: { changed: 1, manualReviewRequired: true },
            traceRefs: { alertRefs: [{ alertId: 'alert-dev-1', alertStatus: 'OPEN' }] }
          }
        ]
      })
      return
    }

    if (pathname === '/api/sql-optimization/recommendations/page' && requestPrefix === 'frontend-recommendation-center-page') {
      assert(requestUrl.searchParams.get('pageNo') === '1', '推荐页必须从第一页加载远程分页结果。')
      assert(requestUrl.searchParams.get('pageSize') === '8', '推荐页默认分页大小必须为 8。')
      assert(requestUrl.searchParams.get('sortBy') === 'createdAt', '推荐页默认排序字段必须为 createdAt。')
      recommendationPageRequests.push({
        sourceType: requestUrl.searchParams.get('sourceType') || '',
        sourceKind: requestUrl.searchParams.get('sourceKind') || '',
        historyId: requestUrl.searchParams.get('historyId') || '',
        parseTaskId: requestUrl.searchParams.get('parseTaskId') || '',
        sourceId: requestUrl.searchParams.get('sourceId') || ''
      })
      await fulfillJson(route, {
        items: [
          {
            recommendationId: 'rec-dev-1',
            recommendationType: 'REWRITE',
            status: 'DISPATCH_READY',
            benefitLevel: 'HIGH',
            riskLevel: 'MEDIUM',
            validationStatus: 'DIVERGED',
            alertStatus: 'OPEN',
            sourceSqlText: recommendationSourceSql,
            recommendedSqlText: recommendationRecommendedSql,
            logicalObjectKey: 'sales.orders',
            targetEngine: 'HETU',
            targetDatasource: 'hetu_main',
            requiresDispatch: true,
            manualReviewRequired: true,
            autoApplyAllowed: false,
            ruleChain: [{ ruleCode: 'SELECT_STAR', action: 'PROJECT_COLUMNS' }],
            preconditions: [],
            semanticRisks: [{ type: 'COLUMN_PROJECTION', severity: 'MEDIUM' }],
            unappliedRules: []
          }
        ],
        pageNo: 1,
        pageSize: 8,
        totalCount: 1,
        pageCount: 1,
        hasMore: false
      })
      return
    }

    if (pathname === '/api/sql-optimization/dispatch-contract' && requestPrefix === 'frontend-recommendation-center-contract') {
      await fulfillJson(route, {
        coordinationMode: 'PULL_ONLY',
        externalPullRequired: true,
        allowedEventStatuses: ['PENDING', 'ACKED'],
        allowedDispatchTypes: ['REWRITE_RECORD']
      })
      return
    }

    if (pathname === '/api/sql-optimization/dispatch-events' && requestPrefix === 'frontend-recommendation-center-events') {
      await fulfillJson(route, [])
      return
    }

    if (
      pathname === '/api/sql-optimization/recommendations/rec-dev-1/trace' &&
      requestPrefix === 'frontend-recommendation-center-trace'
    ) {
      await fulfillJson(route, {
        recommendationId: 'rec-dev-1',
        historyId: 'dev-history-nav-1',
        reportCode: 'DEV_RPT_REWRITE',
        sqlFingerprint: 'fingerprint-dev-history-nav-1',
        traceRefs: { alertRefs: [{ alertId: 'alert-dev-1' }] },
        alertId: 'alert-dev-1',
        alertStatus: 'OPEN'
      })
      return
    }

    if (
      pathname === '/api/sql-optimization/recommendations/rec-dev-1/diff' &&
      requestPrefix === 'frontend-recommendation-center-diff'
    ) {
      await fulfillJson(route, {
        recommendationId: 'rec-dev-1',
        diffStatus: 'AVAILABLE',
        sourceType: 'QUERY',
        sourceKind: 'QUERY_HISTORY',
        sourceId: 'dev-history-nav-1',
        evidenceLevel: 'RUNTIME_HISTORY',
        sqlFingerprint: 'fingerprint-dev-history-nav-1',
        originalSql: recommendationDiffOriginalSql,
        recommendedSql: recommendationRecommendedSql,
        diffSummary: {
          changeCount: 1,
          ruleDiffCount: 1,
          manualReviewRequired: true,
          autoApplyAllowed: false,
          writesBackRecommendation: true,
          evidenceBoundary: 'repo-closed smoke'
        },
        textDiff: [
          {
            hunkId: 'hunk-dev-1',
            type: 'REPLACE',
            originalStartLine: 1,
            recommendedStartLine: 1,
            originalText: recommendationDiffOriginalSql,
            recommendedText: recommendationRecommendedSql
          }
        ],
        ruleDiff: [{ diffId: 'rule-dev-1', rule: 'SELECT_STAR', action: 'PROJECT_COLUMNS' }],
        astSummaryDiff: { parseStatus: 'SUCCESS' }
      })
      return
    }

    if (pathname === '/api/sql-optimization/recommendations/rec-dev-1' && requestPrefix === 'frontend-recommendation-center-detail') {
      await fulfillJson(route, {
        recommendationId: 'rec-dev-1',
        recommendationType: 'REWRITE',
        status: 'DISPATCH_READY',
        benefitLevel: 'HIGH',
        riskLevel: 'MEDIUM',
        validationMethod: 'SQL_COMPARE',
        validationStatus: 'DIVERGED',
        alertStatus: 'OPEN',
        sourceSqlText: recommendationSourceSql,
        recommendedSqlText: recommendationRecommendedSql,
        logicalObjectKey: 'sales.orders',
        targetEngine: 'HETU',
        targetDatasource: 'hetu_main',
        requiresDispatch: true,
        manualReviewRequired: true,
        autoApplyAllowed: false,
        ruleChain: [{ ruleCode: 'SELECT_STAR', action: 'PROJECT_COLUMNS' }],
        preconditions: [],
        semanticRisks: [{ type: 'COLUMN_PROJECTION', severity: 'MEDIUM' }],
        unappliedRules: []
      })
      return
    }

    if (pathname === '/api/sql-optimization/rewrite-records' && requestPrefix === 'frontend-recommendation-rewrite-record-list') {
      assert(
        requestUrl.searchParams.get('recommendationId') === 'rec-dev-1',
        '推荐页改写记录列表必须携带 recommendationId。'
      )
      await fulfillJson(route, [
        {
          rewriteRecordId: 'rewrite-dev-1',
          tenantId: 'tenant-a',
          recommendationId: 'rec-dev-1',
          activationStatus: 'ACTIVE',
          validationStatus: 'DIVERGED',
          alertStatus: 'OPEN',
          lastValidationRunId: 'validation-rec-dev-1',
          autoApplyAllowed: false,
          runtimeBindingId: 'runtime-dev-1'
        }
      ])
      return
    }

    if (
      pathname === '/api/sql-optimization/rewrite-records/rewrite-dev-1/validation-runs' &&
      requestPrefix === 'frontend-recommendation-rewrite-validation-runs'
    ) {
      recommendationRewriteValidationRunCalls += 1
      await fulfillJson(route, [
        {
          validationRunId: 'validation-rec-dev-1',
          rewriteRecordId: 'rewrite-dev-1',
          status: 'FINISHED',
          comparisonStatus: 'DIVERGED',
          differenceType: 'RESULT_SET',
          autoApplyPaused: true,
          startedAt: '2026-05-16T10:00:00',
          finishedAt: '2026-05-16T10:00:05'
        }
      ])
      return
    }

    if (
      pathname === '/api/sql-optimization/rewrite-records/rewrite-dev-1/activation-eligibility' &&
      requestPrefix === 'frontend-recommendation-rewrite-activation-eligibility'
    ) {
      await fulfillJson(route, {
        eligible: false,
        policyId: 'policy-dev-1',
        validationStatus: 'DIVERGED',
        activationStatus: 'ACTIVE',
        alertStatus: 'OPEN',
        autoApplyAllowed: false,
        lastValidationRunId: 'validation-rec-dev-1',
        refusalReasons: [{ code: 'VALIDATION_DIVERGED', message: '验证存在差异', blocking: true }]
      })
      return
    }

    if (pathname === '/api/sql-optimization/rewrite-records/rewrite-dev-1' && requestPrefix === 'frontend-recommendation-rewrite-record-detail') {
      await fulfillJson(route, {
        rewriteRecordId: 'rewrite-dev-1',
        tenantId: 'tenant-a',
        recommendationId: 'rec-dev-1',
        activationStatus: 'ACTIVE',
        validationStatus: 'DIVERGED',
        alertStatus: 'OPEN',
        lastValidationRunId: 'validation-rec-dev-1',
        autoApplyAllowed: false,
        runtimeBindingId: 'runtime-dev-1',
        runtimeRuleVersion: 'v1',
        runtimeBindingScope: 'tenant-a',
        runtimeBindingAt: '2026-05-16T09:58:00',
        runtimeBindingBy: 'dev-smoke'
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
        'frontend-rewrite-validation-governance-datasources',
        'frontend-query-datasource-inventory',
        'frontend-parse-workbench-governance-datasources',
        'frontend-parse-record-datasource-options',
        'frontend-sql-history-datasource-options'
      ])
      await fulfillJson(route, [
        { datasourceCode: 'hetu_main', datasourceName: 'Hetu main', engineType: 'HETU' },
        { datasourceCode: 'hive_archive', datasourceName: 'Hive archive', engineType: 'HIVE' }
      ])
      return
    }

    if (pathname === '/api/governance/tenant-config') {
      if (request.method() === 'PUT') {
        const payload = parseJsonBody(request)
        await fulfillJson(route, {
          tenantId: payload.tenantId || 'tenant-a',
          quotaConcurrent: 20,
          quotaStorage: 2048,
          defaultEngine: payload.defaultEngine || 'HETU',
          backupEngine: payload.backupEngine || 'HIVE',
          auditLevel: 'NORMAL',
          retentionDays: 180,
          accelerationQuota: 50
        })
      } else {
        const targetTenantId = requestUrl.searchParams.get('tenantId') || 'tenant-a'
        await fulfillJson(route, {
          tenantId: targetTenantId,
          quotaConcurrent: 20,
          quotaStorage: 2048,
          defaultEngine: 'HETU',
          backupEngine: 'HIVE',
          auditLevel: 'NORMAL',
          retentionDays: 180,
          accelerationQuota: 50
        })
      }
      return
    }

    if (pathname === '/api/governance/tenant-config/options') {
      await fulfillJson(route, [
        {
          tenantId: 'system',
          label: 'System Admin',
          defaultEngine: 'HETU',
          backupEngine: 'HIVE'
        },
        {
          tenantId: 'tenant-a',
          label: 'Tenant A (Development)',
          defaultEngine: 'HETU',
          backupEngine: 'HIVE'
        },
        {
          tenantId: 'tenant-b',
          label: 'Tenant B (Development)',
          defaultEngine: 'TRINO',
          backupEngine: 'HIVE'
        }
      ])
      return
    }

    if (pathname === '/api/governance/metadata/schemas') {
      await fulfillJson(route, [
        { schemaName: 'sales', catalogName: 'lakehouse' },
        { schemaName: 'analytics', catalogName: 'lakehouse' }
      ])
      return
    }

    if (pathname === '/api/governance/metadata/tables') {
      await fulfillJson(route, [
        { tableName: 'orders', schemaName: 'sales', objectKey: 'TABLE:sales.orders', columnCount: 12 },
        { tableName: 'order_items', schemaName: 'sales', objectKey: 'TABLE:sales.order_items', columnCount: 6 }
      ])
      return
    }

    if (pathname.startsWith('/api/governance/metadata/tables/')) {
      const tableName = decodeURIComponent(pathname.split('/')[5] || '')
      await fulfillJson(route, {
        tableName,
        schemaName: 'sales',
        objectKey: `TABLE:sales.${tableName}`,
        columnCount: tableName === 'orders' ? 12 : 6,
        rowCount: 1280000,
        storageBytes: 536870912,
        freshnessStatus: 'FRESH',
        slaStatus: 'ON_TRACK',
        queryabilityStatus: 'QUERYABLE',
        evidenceStatus: 'CAPTURED',
        upstreamCount: 0,
        downstreamCount: 1
      })
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
              rows: [
                {
                  records: [
                    { orderId: 'dev-recovery-order-1', totalAmount: 18.4 },
                    { orderId: 'dev-recovery-order-2', totalAmount: 28.4 }
                  ],
                  current: 1,
                  size: 2,
                  total: 2,
                  pages: 1
                }
              ],
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
              rows: {
                records: [
                  { orderId: 'dev-success-order-1', totalAmount: 42.8 },
                  { orderId: 'dev-success-order-2', totalAmount: 52.8 }
                ],
                pageNo: 1,
                pageSize: 10,
                totalCount: 25,
                pageCount: 3
              },
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
    await page.getByText('改写治理', { exact: true }).click()
    await page.locator('.app-menu').getByText('SQL 改写验证', { exact: true }).click()
    await page.getByTestId('rewrite-validation-page').waitFor({ timeout: defaultTimeoutMs })
    const rewriteValidationUrl = new URL(page.url())
    assert(rewriteValidationUrl.pathname === ROUTE_PATHS.acceleration, 'SQL 改写验证导航必须复用单条 SQL 解析路由。')
    assert(rewriteValidationUrl.searchParams.get('mode') === 'rewriteValidation', 'SQL 改写验证导航必须带 mode=rewriteValidation。')
    await expectTextInLocator(page.getByTestId('rewrite-validation-title'), 'SQL 改写验证')
    await expectTextInLocator(page.getByTestId('rewrite-validation-boundary'), '不会标记生产已自动改写')
    await page.getByTestId('rewrite-validation-submit').click()
    try {
      await expectTextInLocator(page.getByTestId('rewrite-validation-status'), 'SUCCEEDED')
    } catch (error) {
      const statusText = await page.getByTestId('rewrite-validation-status').textContent().catch(() => '')
      const errorText = await page.getByTestId('rewrite-validation-error').textContent().catch(() => '')
      throw new Error(`改写验证任务未到达成功状态，当前状态=${statusText}，页面错误=${errorText}，未预期请求=${unexpectedApiRequests.join(';')}`)
    }
    await expectTextInLocator(page.getByTestId('rewrite-validation-rule-chain'), 'REMOVE_DUPLICATE_PREDICATE')
    await expectTextInLocator(page.getByTestId('rewrite-validation-recommendation-table'), 'rec-dev-1')
    await page.getByTestId('rewrite-validation-create-record').click()
    await expectTextInLocator(page.getByTestId('rewrite-validation-success'), '改写记录草稿已创建')
    await page.getByTestId('rewrite-validation-create-run').click()
    await expectTextInLocator(page.getByTestId('rewrite-validation-run-table'), 'validation-rewrite-page-dev-1')
    if (rewriteValidationScreenshotPath) {
      fs.mkdirSync(path.dirname(rewriteValidationScreenshotPath), { recursive: true })
      await page.screenshot({ path: rewriteValidationScreenshotPath, fullPage: true })
    }
    await page.getByTestId('rewrite-validation-open-recommendations').click()
    await page.getByTestId('recommendation-page').waitFor({ timeout: defaultTimeoutMs })
    const recommendationDeepLinkUrl = new URL(page.url())
    assert(recommendationDeepLinkUrl.pathname === ROUTE_PATHS.recommendationCenter, '改写验证推荐入口必须复用推荐结果路由。')
    assert(recommendationDeepLinkUrl.searchParams.get('sourceCategory') === 'SQL_PARSE', '改写验证推荐入口必须带 sourceCategory=SQL_PARSE。')
    assert(recommendationDeepLinkUrl.searchParams.get('sourceId') === 'manual-rewrite-validation', '改写验证推荐入口必须带来源 ID。')
    assert(
      recommendationPageRequests.some(item =>
        item.sourceType === 'PARSE' &&
        item.sourceKind === 'COMBINED_PARSE' &&
        item.sourceId === 'manual-rewrite-validation'
      ),
      `推荐结果分页请求必须消费改写验证来源深链，实际为 ${JSON.stringify(recommendationPageRequests)}`
    )
    assert(rewriteValidationTaskPolls >= 1, 'SQL 改写验证必须轮询 REWRITE 任务终态。')
    assert(rewriteValidationRecordCreateCalls === 1, 'SQL 改写验证必须能创建改写记录草稿。')
    assert(rewriteValidationRunCreateCalls === 1, 'SQL 改写验证必须能创建 validation run。')

    await page.locator('.app-menu').getByText('改写记录', { exact: true }).click()
    await page.getByTestId('recommendation-page').waitFor({ timeout: defaultTimeoutMs })
    const rewriteRecordUrl = new URL(page.url())
    assert(rewriteRecordUrl.pathname === ROUTE_PATHS.recommendationCenter, '改写记录导航必须复用推荐结果路由。')
    assert(rewriteRecordUrl.searchParams.get('tab') === 'rewriteLifecycle', '改写记录导航必须进入 rewriteLifecycle tab。')
    await page.getByTestId('recommendation-item').locator('tr').filter({ hasText: 'rec-dev-1' }).first().click()
    await page.getByTestId('recommendation-detail-drawer').waitFor({ timeout: defaultTimeoutMs })
    await page.getByTestId('recommendation-rewrite-lifecycle').waitFor({ timeout: defaultTimeoutMs })
    await page.getByRole('tab', { name: /SQL 差异|SQL diff/ }).click()
    const recommendationCompare = page.getByTestId('recommendation-sql-compare')
    await recommendationCompare.waitFor({ timeout: defaultTimeoutMs })
    await expectTextInLocator(recommendationCompare, 'SELECT')
    await expectTextInLocator(recommendationCompare, '-- report_code=DEV_RPT_REWRITE')
    const recommendationCompareCodeText = await recommendationCompare
      .locator('.sql-compare-code')
      .evaluateAll(nodes => nodes.map(node => node.textContent || '').join('\n'))
    assert(recommendationCompareCodeText.includes('EXISTS'), '推荐 SQL compare 必须渲染嵌套 EXISTS 条件。')
    assert(recommendationCompareCodeText.includes('IN ('), '推荐 SQL compare 必须渲染嵌套 IN 子查询。')
    assert(
      recommendationCompareCodeText.includes("'literal select from dev smoke'") &&
        recommendationCompareCodeText.includes("'select/from literal'"),
      '推荐 SQL compare 必须保留嵌套 SQL 中类似 SQL 的字符串字面量。'
    )
    assert(
      (recommendationCompareCodeText.match(/\n\s{8,}SELECT\b/g) || []).length >= 2,
      '推荐 SQL compare 必须展示多层嵌套 SELECT 缩进。'
    )
    assert(
      (await recommendationCompare.locator('.sql-compare-token-mark--delete').count()) > 0,
      '推荐 SQL compare 必须标记删除侧 token 差异。'
    )
    assert(
      (await recommendationCompare.locator('.sql-compare-token-mark--insert').count()) > 0,
      '推荐 SQL compare 必须标记插入侧 token 差异。'
    )
    await assertSqlCompareDualPane(recommendationCompare)
    await page.getByRole('tab', { name: /SQL 证据|SQL evidence/ }).click()
    await expectTextInLocator(page.getByTestId('recommendation-recommended-sql'), '-- report_code=DEV_RPT_REWRITE')
    await expectTextInLocator(page.getByTestId('recommendation-recommended-sql'), 'SELECT')
    await page.getByRole('tab', { name: /改写激活与暂停|Rewrite activation and pause/ }).click()
    await expectTextInLocator(page.getByTestId('recommendation-rewrite-validation-run-table'), 'validation-rec-dev-1')
    await page.keyboard.press('Escape')
    await page.getByTestId('recommendation-detail-drawer').waitFor({ state: 'hidden', timeout: defaultTimeoutMs })

    await page.locator('.app-menu').getByText('改写历史', { exact: true }).click()
    await page.getByTestId('sql-history-page').waitFor({ timeout: defaultTimeoutMs })
    const rewriteHistoryUrl = new URL(page.url())
    assert(rewriteHistoryUrl.pathname === ROUTE_PATHS.sqlHistory, '改写历史导航必须复用 SQL 历史查询路由。')
    assert(rewriteHistoryUrl.searchParams.get('hasRewriteRecord') === 'true', '改写历史导航必须带 hasRewriteRecord=true。')
    assert(rewriteHistoryUrl.searchParams.get('detailTab') === 'rewriteRecords', '改写历史导航必须带 detailTab=rewriteRecords。')

    await page.goto(`${baseUrl}${ROUTE_PATHS.sqlHistory}?historyId=dev-history-deep-link&detailTab=rewriteRecords&hasRewriteRecord=true`, {
      waitUntil: 'domcontentloaded'
    })
    await page.getByTestId('sql-history-detail-drawer').waitFor({ timeout: defaultTimeoutMs })
    await expectTextInLocator(page.getByTestId('sql-history-rewrite-record-table'), 'rewrite-dev-1')

    await page.goto(`${baseUrl}${ROUTE_PATHS.sqlQuery}`, { waitUntil: 'domcontentloaded' })
    await page.getByTestId('query-flow-page').waitFor({ timeout: defaultTimeoutMs })

    await page.getByTestId('query-flow-submit').click()
    await expectTextInLocator(page.locator('.result-rail'), 'SUCCESS')
    await expectTextInLocator(page.locator('.result-rail'), 'HETU')
    await expectTextInLocator(page.locator('.results-stage'), 'dev-success-order-1')
    await expectTextInLocator(page.locator('.results-stage'), 'dev-success-order-2')
    await expectTextInLocator(page.getByTestId('query-result-pagination'), '25')

    await page.getByTestId('query-flow-submit-recovery').click()
    await expectTextInLocator(page.locator('.result-rail'), 'PARTIAL')
    await expectTextInLocator(page.locator('.result-rail'), 'HIVE')
    await expectTextInLocator(page.locator('.results-stage'), 'dev-recovery-order-1')
    await expectTextInLocator(page.locator('.results-stage'), 'dev-recovery-order-2')

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
      sqlHistoryRewriteRecordCalls >= 2,
      `SQL 历史改写记录接口至少应调用两次，实际为 ${sqlHistoryRewriteRecordCalls}`
    )
    assert(
      recommendationRewriteValidationRunCalls >= 1,
      `推荐页改写验证运行接口至少应调用一次，实际为 ${recommendationRewriteValidationRunCalls}`
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
    assert(
      sqlHistoryPageRequests.some(item => item.hasRewriteRecord === 'true'),
      `SQL 历史应收到改写记录路由筛选请求，实际为 ${JSON.stringify(sqlHistoryPageRequests)}`
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
