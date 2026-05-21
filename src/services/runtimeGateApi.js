import axios from 'axios'

const DEFAULT_TIMEOUT_MS = 30000
const DEFAULT_POLL_INTERVAL_MS = 1000
const DEFAULT_POLL_ATTEMPTS = 30
const MAX_CORRELATION_ID_LENGTH = 64
const DEFAULT_COMPENSATION_TRACE_PREFIX =
  import.meta.env.VITE_SQLFORGE_GOVERNANCE_SMOKE_FORCE_TRACE_PREFIX || 'SMOKE-FORCE-AUDIT-FALLBACK'

const optimizationTerminalStates = new Set(['SUCCEEDED', 'FAILED'])
const benchmarkTerminalStates = new Set(['SUCCEEDED', 'FAILED'])
const combinedParseTerminalStates = new Set(['ACCESS_SUCCEEDED', 'PARTIAL_SUCCEEDED', 'FAILED'])

const httpClient = axios.create({
  timeout: DEFAULT_TIMEOUT_MS
})

const sleep = delayMs => new Promise(resolve => window.setTimeout(resolve, delayMs))

const trimCorrelationId = value => String(value || '').slice(0, MAX_CORRELATION_ID_LENGTH)
const normalizeTenantId = tenantId => String(tenantId || '').trim()

const buildTenantQuerySuffix = tenantId => {
  const normalizedTenantId = normalizeTenantId(tenantId)
  return normalizedTenantId ? `?tenantId=${encodeURIComponent(normalizedTenantId)}` : ''
}

const buildTenantPagedQuery = (tenantId, filters = {}) => {
  const params = new URLSearchParams()
  const normalizedTenantId = normalizeTenantId(tenantId)
  if (normalizedTenantId) {
    params.set('tenantId', normalizedTenantId)
  }
  const pageNo = Number(filters?.pageNo)
  const pageSize = Number(filters?.pageSize)
  if (Number.isFinite(pageNo) && pageNo > 0) {
    params.set('pageNo', String(pageNo))
  }
  if (Number.isFinite(pageSize) && pageSize > 0) {
    params.set('pageSize', String(pageSize))
  }
  const query = params.toString()
  return query ? `?${query}` : ''
}

const buildReportBatchDetailQuery = filters => {
  const params = new URLSearchParams()
  const pageNumber = Number(filters?.pageNumber)
  const pageSize = Number(filters?.pageSize)
  if (Number.isFinite(pageNumber) && pageNumber > 0) {
    params.set('pageNumber', String(pageNumber))
  }
  if (Number.isFinite(pageSize) && pageSize > 0) {
    params.set('pageSize', String(pageSize))
  }
  const reportDetailPageNumber = Number(filters?.reportDetailPageNumber)
  const reportDetailPageSize = Number(filters?.reportDetailPageSize)
  if (Number.isFinite(reportDetailPageNumber) && reportDetailPageNumber > 0) {
    params.set('reportDetailPageNumber', String(reportDetailPageNumber))
  }
  if (Number.isFinite(reportDetailPageSize) && reportDetailPageSize > 0) {
    params.set('reportDetailPageSize', String(reportDetailPageSize))
  }
  const logicalObjectDetailPageNumber = Number(filters?.logicalObjectDetailPageNumber)
  const logicalObjectDetailPageSize = Number(filters?.logicalObjectDetailPageSize)
  if (Number.isFinite(logicalObjectDetailPageNumber) && logicalObjectDetailPageNumber > 0) {
    params.set('logicalObjectDetailPageNumber', String(logicalObjectDetailPageNumber))
  }
  if (Number.isFinite(logicalObjectDetailPageSize) && logicalObjectDetailPageSize > 0) {
    params.set('logicalObjectDetailPageSize', String(logicalObjectDetailPageSize))
  }
  const reportCode = String(filters?.reportCode || '').trim()
  if (reportCode) {
    params.set('reportCode', reportCode)
  }
  const logicalObjectKey = String(filters?.logicalObjectKey || '').trim()
  if (logicalObjectKey) {
    params.set('logicalObjectKey', logicalObjectKey)
  }
  const query = params.toString()
  return query ? `?${query}` : ''
}

const buildRewriteRecordsQuery = filters => {
  const params = new URLSearchParams()
  const historyId = String(filters?.historyId || '').trim()
  const recommendationId = String(filters?.recommendationId || '').trim()
  const validationStatus = String(filters?.validationStatus || '').trim()
  const sourceType = String(filters?.sourceType || '').trim()
  if (historyId) {
    params.set('historyId', historyId)
  }
  if (recommendationId) {
    params.set('recommendationId', recommendationId)
  }
  if (validationStatus) {
    params.set('validationStatus', validationStatus)
  }
  if (sourceType) {
    params.set('sourceType', sourceType)
  }
  const query = params.toString()
  return query ? `?${query}` : ''
}

const devProxyHeaders = (tenantId, options = {}) => {
  const {
    requestPrefix = 'frontend-runtime',
    tracePrefix,
    requestId = '',
    traceId = ''
  } = options

  const headers = {
    'X-SQLForge-Dev-Tenant-Id': String(tenantId || '')
  }

  const normalizedRequestPrefix = trimCorrelationId(requestPrefix)
  if (normalizedRequestPrefix) {
    headers['X-SQLForge-Dev-Request-Prefix'] = normalizedRequestPrefix
  }
  const normalizedTracePrefix = trimCorrelationId(tracePrefix)
  if (normalizedTracePrefix) {
    headers['X-SQLForge-Dev-Trace-Prefix'] = normalizedTracePrefix
  }
  const normalizedRequestId = trimCorrelationId(requestId)
  if (normalizedRequestId) {
    headers['X-SQLForge-Dev-Request-Id'] = normalizedRequestId
  }
  const normalizedTraceId = trimCorrelationId(traceId)
  if (normalizedTraceId) {
    headers['X-SQLForge-Dev-Trace-Id'] = normalizedTraceId
  }

  return headers
}

const request = async ({ method, url, data, tenantId, requestOptions = {} }) => {
  const response = await httpClient.request({
    method,
    url,
    data,
    headers: {
      ...devProxyHeaders(tenantId, requestOptions)
    }
  })

  return response.data
}

const pollUntilTerminal = async ({ fetcher, taskId, tenantId, requestPrefix, terminalStates, requestOptions = {} }) => {
  let latestResponse = null

  for (let attempt = 0; attempt < DEFAULT_POLL_ATTEMPTS; attempt += 1) {
    latestResponse = await fetcher(taskId, tenantId, {
      ...requestOptions,
      requestPrefix: `${requestPrefix}-poll-${attempt + 1}`
    })
    if (terminalStates.has(latestResponse.status)) {
      return latestResponse
    }
    await sleep(DEFAULT_POLL_INTERVAL_MS)
  }

  throw new Error(`Task ${taskId} did not reach a terminal state in time`)
}

export const GOVERNANCE_COMPENSATION_TRACE_PREFIX = DEFAULT_COMPENSATION_TRACE_PREFIX

export const executeQuery = (payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/query-execution/queries/execute',
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-query',
      ...requestOptions
    }
  })

export const submitOptimizationTask = (payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/sql-optimization/tasks',
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-sql-optimization-submit',
      ...requestOptions
    }
  })

export const getOptimizationTaskStatus = (taskId, tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/tasks/${taskId}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-sql-optimization-status',
      ...requestOptions
    }
  })

export const waitForOptimizationTask = (taskId, tenantId, requestOptions = {}) =>
  pollUntilTerminal({
    fetcher: getOptimizationTaskStatus,
    taskId,
    tenantId,
    requestPrefix: 'frontend-sql-optimization',
    terminalStates: optimizationTerminalStates,
    requestOptions
  })

export const submitBenchmarkTask = (payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/benchmark-engine/tasks',
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-benchmark-submit',
      ...requestOptions
    }
  })

export const getBenchmarkTaskStatus = (taskId, tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/benchmark-engine/tasks/${taskId}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-benchmark-status',
      ...requestOptions
    }
  })

export const waitForBenchmarkTask = (taskId, tenantId, requestOptions = {}) =>
  pollUntilTerminal({
    fetcher: getBenchmarkTaskStatus,
    taskId,
    tenantId,
    requestPrefix: 'frontend-benchmark',
    terminalStates: benchmarkTerminalStates,
    requestOptions
  })

export const getBenchmarkReport = (reportId, tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/benchmark-engine/reports/${reportId}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-benchmark-report',
      ...requestOptions
    }
  })

export const parseStructureSql = (payload, requestOptions = {}) => {
  const { tenantId, ...data } = payload
  delete data.connectionRequired
  return request({
    method: 'post',
    url: '/api/sql-optimization/parse/structure',
    data,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-structure-submit',
      ...requestOptions
    }
  })
}

export const submitCombinedParse = (payload, requestOptions = {}) => {
  const { tenantId, ...data } = payload
  return request({
    method: 'post',
    url: '/api/sql-optimization/parse/combined',
    data,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-combined-submit',
      ...requestOptions
    }
  })
}

export const getCombinedParseStatus = (parseTaskId, tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/parse/${encodeURIComponent(parseTaskId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-combined-status',
      ...requestOptions
    }
  })

export const waitForCombinedParse = (parseTaskId, tenantId, requestOptions = {}) =>
  pollUntilTerminal({
    fetcher: getCombinedParseStatus,
    taskId: parseTaskId,
    tenantId,
    requestPrefix: 'frontend-parse-combined',
    terminalStates: combinedParseTerminalStates,
    requestOptions
  })

export const getGovernanceMessageStats = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: '/api/governance/admin/messages/stats',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-message-stats',
      ...requestOptions
    }
  })

export const retryGovernanceFailedMessages = (tenantId, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/governance/admin/messages/retry',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-message-retry',
      ...requestOptions
    }
  })

export const getGovernanceTenantConfig = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/governance/tenant-config?tenantId=${encodeURIComponent(tenantId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-tenant-config',
      ...requestOptions
    }
  })

export const getGovernanceQueryHistoryDetail = (tenantId, historyId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/governance/query-history/${encodeURIComponent(historyId)}${buildTenantQuerySuffix(tenantId)}`,
    tenantId: normalizeTenantId(tenantId),
    requestOptions: {
      requestPrefix: 'frontend-governance-query-history-detail',
      ...requestOptions
    }
  })

export const exportGovernanceQueryHistory = (tenantId, payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: `/api/governance/query-history/export${buildTenantQuerySuffix(tenantId)}`,
    data: payload,
    tenantId: normalizeTenantId(tenantId),
    requestOptions: {
      requestPrefix: 'frontend-governance-query-history-export',
      ...requestOptions
    }
  })

export const getGovernanceQueryHistoryPage = (filters = {}, requestOptions = {}) => {
  const params = new URLSearchParams()
  const filterTenantId = normalizeTenantId(filters.tenantId)
  const requestTenantId = normalizeTenantId(filters.requestTenantId) || filterTenantId
  const pageNo = Number(filters.pageNo || 1)
  const pageSize = Number(filters.pageSize || 8)
  if (filterTenantId) {
    params.set('tenantId', filterTenantId)
  }
  params.set('pageNo', String(pageNo))
  params.set('pageSize', String(pageSize))
  ;[
    'historyType',
    'reportCode',
    'datasourceCode',
    'stage',
    'bizDate',
    'queryDateStart',
    'queryDateEnd',
    'status',
    'logicalObjectType',
    'accessChannel',
    'engine',
    'submittedBy',
    'submittedStart',
    'submittedEnd',
    'rewriteValidationStatus',
    'rewriteSourceType',
    'recommendationId',
    'sortBy',
    'sortOrder'
  ].forEach(key => {
    const value = String(filters?.[key] || '').trim()
    if (value) {
      params.set(key, value)
    }
  })
  ;['cacheHit', 'rewriteApplied', 'accelerationApplied', 'parameterizedSql', 'hasRewriteRecord'].forEach(key => {
    if (typeof filters?.[key] === 'boolean') {
      params.set(key, String(filters[key]))
    }
  })

  return request({
    method: 'get',
    url: `/api/governance/query-history?${params.toString()}`,
    tenantId: requestTenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-query-history-page',
      ...requestOptions
    }
  })
}

export const getSqlParseHistoryDetail = (tenantId, parseHistoryId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/parse-history/${encodeURIComponent(parseHistoryId)}${buildTenantQuerySuffix(tenantId)}`,
    tenantId: normalizeTenantId(tenantId),
    requestOptions: {
      requestPrefix: 'frontend-sql-parse-history-detail',
      ...requestOptions
    }
  })

export const exportSqlParseHistory = (tenantId, payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: `/api/sql-optimization/parse-history/export${buildTenantQuerySuffix(tenantId)}`,
    data: payload,
    tenantId: normalizeTenantId(tenantId),
    requestOptions: {
      requestPrefix: 'frontend-sql-parse-history-export',
      ...requestOptions
    }
  })

export const getSqlParseHistoryPage = (filters = {}, requestOptions = {}) => {
  const params = new URLSearchParams()
  const filterTenantId = normalizeTenantId(filters.tenantId)
  const requestTenantId = normalizeTenantId(filters.requestTenantId) || filterTenantId
  const pageNo = Number(filters.pageNo || 1)
  const pageSize = Number(filters.pageSize || 8)
  if (filterTenantId) {
    params.set('tenantId', filterTenantId)
  }
  params.set('pageNo', String(pageNo))
  params.set('pageSize', String(pageSize))
  ;[
    'sourceType',
    'reportCode',
    'datasourceCode',
    'stage',
    'bizDate',
    'queryDateStart',
    'queryDateEnd',
    'status',
    'logicalObjectType',
    'accessChannel',
    'engine',
    'submittedBy',
    'traceId',
    'parseTaskId',
    'submittedStart',
    'submittedEnd',
    'sortBy',
    'sortOrder'
  ].forEach(key => {
    const value = String(filters?.[key] || '').trim()
    if (value) {
      params.set(key, value)
    }
  })

  return request({
    method: 'get',
    url: `/api/sql-optimization/parse-history?${params.toString()}`,
    tenantId: requestTenantId,
    requestOptions: {
      requestPrefix: 'frontend-sql-parse-history-page',
      ...requestOptions
    }
  })
}

export const getHetuRouteCalibration = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: '/api/query-execution/internal/hetu/route-calibration',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-route-calibration',
      ...requestOptions
    }
  })

export const getRecommendations = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: '/api/sql-optimization/recommendations',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-recommendations-list',
      ...requestOptions
    }
  })

export const getRecommendationPage = (tenantId, filters = {}, requestOptions = {}) => {
  const params = new URLSearchParams()
  const queryKeys = [
    'pageNo',
    'pageSize',
    'sortBy',
    'sortOrder',
    'recommendationType',
    'status',
    'benefitLevel',
    'riskLevel',
    'validationStatus',
    'requiresDispatch',
    'manualReviewRequired',
    'sourceType',
    'sourceKind',
    'sourceId',
    'historyId',
    'parseTaskId',
    'batchId',
    'reportCode'
  ]
  queryKeys.forEach(key => {
    const value = filters?.[key]
    if (value !== null && value !== undefined && String(value).trim() !== '') {
      params.set(key, String(value))
    }
  })

  return request({
    method: 'get',
    url: `/api/sql-optimization/recommendations/page?${params.toString()}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-recommendation-page',
      ...requestOptions
    }
  })
}

export const getRecommendationDetail = (tenantId, recommendationId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/recommendations/${encodeURIComponent(recommendationId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-recommendation-detail',
      ...requestOptions
    }
  })

export const getRecommendationTrace = (tenantId, recommendationId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/recommendations/${encodeURIComponent(recommendationId)}/trace`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-recommendation-trace',
      ...requestOptions
    }
  })

export const createAccelerationCandidate = (payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/sql-optimization/acceleration-candidates',
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-acceleration-candidate-create',
      ...requestOptions
    }
  })

export const getAccelerationCandidates = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: '/api/sql-optimization/acceleration-candidates',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-acceleration-candidate-list',
      ...requestOptions
    }
  })

export const getAccelerationCandidate = (tenantId, candidateId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/acceleration-candidates/${encodeURIComponent(candidateId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-acceleration-candidate-detail',
      ...requestOptions
    }
  })

export const getRecommendationDiff = (tenantId, recommendationId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/recommendations/${encodeURIComponent(recommendationId)}/diff`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-recommendation-diff',
      ...requestOptions
    }
  })

export const createRewriteTrial = (payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/sql-optimization/rewrite-trials',
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-rewrite-trial-create',
      ...requestOptions
    }
  })

export const getRewriteTrial = (tenantId, runId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/rewrite-trials/${encodeURIComponent(runId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-rewrite-trial-detail',
      ...requestOptions
    }
  })

export const submitAccelerationPlan = (payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/sql-optimization/acceleration-plans',
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-acceleration-plan-submit',
      ...requestOptions
    }
  })

export const getAccelerationPlan = (tenantId, planId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/acceleration-plans/${encodeURIComponent(planId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-acceleration-plan-detail',
      ...requestOptions
    }
  })

export const activateAccelerationPlan = (tenantId, planId, payload = {}, requestOptions = {}) =>
  request({
    method: 'post',
    url: `/api/sql-optimization/acceleration-plans/${encodeURIComponent(planId)}/activate`,
    data: payload,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-acceleration-plan-activate',
      ...requestOptions
    }
  })

export const pauseAccelerationPlan = (tenantId, planId, payload = {}, requestOptions = {}) =>
  request({
    method: 'post',
    url: `/api/sql-optimization/acceleration-plans/${encodeURIComponent(planId)}/pause`,
    data: payload,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-acceleration-plan-pause',
      ...requestOptions
    }
  })

export const createSqlRewriteRecord = (payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/sql-optimization/rewrite-records',
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-rewrite-record-create',
      ...requestOptions
    }
  })

export const getSqlRewriteRecords = (tenantId, filters = {}, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/rewrite-records${buildRewriteRecordsQuery(filters)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-rewrite-record-list',
      ...requestOptions
    }
  })

export const getSqlRewriteRecord = (tenantId, rewriteRecordId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-rewrite-record-detail',
      ...requestOptions
    }
  })

export const getRewriteActivationEligibility = (tenantId, rewriteRecordId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/activation-eligibility`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-rewrite-record-activation-eligibility',
      ...requestOptions
    }
  })

export const activateSqlRewriteRecord = (tenantId, rewriteRecordId, payload = {}, requestOptions = {}) =>
  request({
    method: 'post',
    url: `/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/activate`,
    data: payload,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-rewrite-record-activate',
      ...requestOptions
    }
  })

export const pauseSqlRewriteRecord = (tenantId, rewriteRecordId, payload = {}, requestOptions = {}) =>
  request({
    method: 'post',
    url: `/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/pause`,
    data: payload,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-rewrite-record-pause',
      ...requestOptions
    }
  })

export const reviewSqlRewriteRecord = (tenantId, rewriteRecordId, payload = {}, requestOptions = {}) =>
  request({
    method: 'post',
    url: `/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/review`,
    data: payload,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-rewrite-record-review',
      ...requestOptions
    }
  })

export const createRewriteValidationRun = (tenantId, rewriteRecordId, payload = {}, requestOptions = {}) =>
  request({
    method: 'post',
    url: `/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/validation-runs`,
    data: payload,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-rewrite-validation-run-create',
      ...requestOptions
    }
  })

export const getRewriteValidationRuns = (tenantId, rewriteRecordId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/validation-runs`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-rewrite-validation-run-list',
      ...requestOptions
    }
  })

export const getQueryHistoryRewriteRecords = (tenantId, historyId, requestOptions = {}) =>
  request({
    method: 'get',
    url:
      `/api/governance/query-history/${encodeURIComponent(historyId)}/rewrite-records` +
      buildTenantQuerySuffix(tenantId),
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-query-history-rewrite-records',
      ...requestOptions
    }
  })

export const getDispatchEvents = (tenantId, status = '', requestOptions = {}) => {
  const query = String(status || '').trim() ? `?status=${encodeURIComponent(status)}` : ''
  return request({
    method: 'get',
    url: `/api/sql-optimization/dispatch-events${query}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-dispatch-events',
      ...requestOptions
    }
  })
}

export const getDispatchEventDetail = (tenantId, dispatchEventId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/dispatch-events/${encodeURIComponent(dispatchEventId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-dispatch-event-detail',
      ...requestOptions
    }
  })

export const getDispatchContract = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: '/api/sql-optimization/dispatch-contract',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-dispatch-contract',
      ...requestOptions
    }
  })

export const createParseBatch = (payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/sql-optimization/parse-batches',
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-batch-create',
      ...requestOptions
    }
  })

export const listParseBatches = (tenantId, filters = {}, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/parse-batches${buildTenantPagedQuery(tenantId, filters)}`,
    tenantId: normalizeTenantId(tenantId),
    requestOptions: {
      requestPrefix: 'frontend-parse-batch-list',
      ...requestOptions
    }
  })

export const ingestParseBatch = (batchId, tenantId, payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: `/api/sql-optimization/parse-batches/${encodeURIComponent(batchId)}/ingest`,
    data: payload,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-batch-ingest',
      ...requestOptions
    }
  })

export const getParseBatch = (batchId, tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/parse-batches/${encodeURIComponent(batchId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-batch-detail',
      ...requestOptions
    }
  })

export const retryParseBatchAccess = (batchId, tenantId, payload = {}, requestOptions = {}) =>
  request({
    method: 'post',
    url: `/api/sql-optimization/parse-batches/${encodeURIComponent(batchId)}/retry-access`,
    data: payload,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-batch-retry-access',
      ...requestOptions
    }
  })

export const createParseBatchRewriteTrials = (batchId, tenantId, payload = {}, requestOptions = {}) =>
  request({
    method: 'post',
    url: `/api/sql-optimization/parse-batches/${encodeURIComponent(batchId)}/rewrite-trials`,
    data: {
      tenantId,
      ...payload
    },
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-batch-rewrite-trial-create',
      ...requestOptions
    }
  })

export const getLatestParseBatchRewriteTrial = (batchId, tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/parse-batches/${encodeURIComponent(batchId)}/rewrite-trials/latest`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-batch-rewrite-trial-latest',
      ...requestOptions
    }
  })

export const listReportBatches = (tenantId, filters = {}, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/report-batches${buildTenantPagedQuery(tenantId, filters)}`,
    tenantId: normalizeTenantId(tenantId),
    requestOptions: {
      requestPrefix: 'frontend-report-batch-list',
      ...requestOptions
    }
  })

export const importReportBatch = (payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/sql-optimization/report-batches/import',
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-report-batch-import',
      ...requestOptions
    }
  })

export const resolveReportBatchSqls = (batchId, tenantId, requestOptions = {}) =>
  request({
    method: 'post',
    url: `/api/sql-optimization/report-batches/${encodeURIComponent(batchId)}/resolve-sqls`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-report-batch-resolve',
      ...requestOptions
    }
  })

export const getReportBatch = (batchId, tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/report-batches/${encodeURIComponent(batchId)}${buildReportBatchDetailQuery(requestOptions)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-report-batch-detail',
      ...requestOptions
    }
  })

export const getReportBatchParseStatistics = (batchId, tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/report-batches/${encodeURIComponent(batchId)}/parse-statistics${buildReportBatchDetailQuery(requestOptions)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-report-batch-parse-statistics',
      ...requestOptions
    }
  })

export const getReportBatchIssueSceneDetail = (batchId, issueScene, tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/sql-optimization/report-batches/${encodeURIComponent(batchId)}/parse-statistics/issue-scenes/${encodeURIComponent(issueScene)}${buildReportBatchDetailQuery(requestOptions)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-report-batch-issue-scene-detail',
      ...requestOptions
    }
  })

export const getParseStatisticsOverview = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: '/api/sql-optimization/parse-statistics/overview',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-statistics-overview',
      ...requestOptions
    }
  })

export const getParseStatisticsByIssueScene = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: '/api/sql-optimization/parse-statistics/by-issue-scene',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-statistics-issue-scene',
      ...requestOptions
    }
  })

export const getParseStatisticsBySql = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: '/api/sql-optimization/parse-statistics/by-sql',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-statistics-by-sql',
      ...requestOptions
    }
  })

export const getParseStatisticsByReport = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: '/api/sql-optimization/parse-statistics/by-report',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-statistics-by-report',
      ...requestOptions
    }
  })

export const getParseStatisticsPriorityMatrix = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: '/api/sql-optimization/parse-statistics/priority-matrix',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-statistics-priority-matrix',
      ...requestOptions
    }
  })

export const getParseStatisticsImportantUrgent = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: '/api/sql-optimization/parse-statistics/important-urgent',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-statistics-important-urgent',
      ...requestOptions
    }
  })

export const getParseStatisticsRewriteTrialOverview = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: '/api/sql-optimization/parse-statistics/rewrite-trials/overview',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-statistics-rewrite-trial-overview',
      ...requestOptions
    }
  })

export const getParseStatisticsRewriteTrialsBySourceIssue = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: '/api/sql-optimization/parse-statistics/rewrite-trials/by-source-issue',
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-parse-statistics-rewrite-trial-by-source-issue',
      ...requestOptions
    }
  })

export const getGovernanceDatasources = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/governance/datasources?tenantId=${encodeURIComponent(tenantId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-datasources',
      ...requestOptions
    }
  })

export const createGovernanceDatasource = (payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/governance/datasources',
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-datasource-create',
      ...requestOptions
    }
  })

export const updateGovernanceDatasource = (datasourceId, payload, requestOptions = {}) =>
  request({
    method: 'put',
    url: `/api/governance/datasources/${encodeURIComponent(datasourceId)}`,
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-datasource-update',
      ...requestOptions
    }
  })

export const getGovernanceDatasourceDetail = (tenantId, datasourceId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/governance/datasources/${encodeURIComponent(datasourceId)}?tenantId=${encodeURIComponent(tenantId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-datasource-detail',
      ...requestOptions
    }
  })

export const testGovernanceDatasourceConnection = (datasourceId, tenantId, payload = {}, requestOptions = {}) =>
  request({
    method: 'post',
    url: `/api/governance/datasources/${encodeURIComponent(datasourceId)}/test-connection`,
    data: {
      tenantId,
      ...payload
    },
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-datasource-test-connection',
      ...requestOptions
    }
  })

export const getGovernanceReportInterfaces = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/governance/report-interfaces?tenantId=${encodeURIComponent(tenantId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-report-interfaces',
      ...requestOptions
    }
  })

export const createGovernanceReportInterface = (payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/governance/report-interfaces',
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-report-interface-create',
      ...requestOptions
    }
  })

export const updateGovernanceReportInterface = (interfaceId, payload, requestOptions = {}) =>
  request({
    method: 'put',
    url: `/api/governance/report-interfaces/${encodeURIComponent(interfaceId)}`,
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-report-interface-update',
      ...requestOptions
    }
  })

export const getGovernanceRedisRuleSources = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/governance/redis-rule-sources?tenantId=${encodeURIComponent(tenantId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-redis-rule-sources',
      ...requestOptions
    }
  })

export const createGovernanceRedisRuleSource = (payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/governance/redis-rule-sources',
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-redis-rule-source-create',
      ...requestOptions
    }
  })

export const updateGovernanceRedisRuleSource = (sourceId, payload, requestOptions = {}) =>
  request({
    method: 'put',
    url: `/api/governance/redis-rule-sources/${encodeURIComponent(sourceId)}`,
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-redis-rule-source-update',
      ...requestOptions
    }
  })

export const getGovernanceDispatchPolicies = (tenantId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/governance/dispatch-policies?tenantId=${encodeURIComponent(tenantId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-dispatch-policies',
      ...requestOptions
    }
  })

export const createGovernanceDispatchPolicy = (payload, requestOptions = {}) =>
  request({
    method: 'post',
    url: '/api/governance/dispatch-policies',
    data: payload,
    tenantId: payload.tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-dispatch-policy-create',
      ...requestOptions
    }
  })

export const getMetadataSnapshots = (filters = {}, requestOptions = {}) => {
  const params = new URLSearchParams()
  const tenantId = String(filters.tenantId || '').trim()
  ;['tenantId', 'datasourceCode', 'objectType', 'objectKey', 'freshnessStatus', 'slaStatus', 'queryabilityStatus', 'evidenceStatus']
    .forEach(key => {
      const value = String(filters?.[key] || '').trim()
      if (value) {
        params.set(key, value)
      }
    })
  const query = params.toString()
  return request({
    method: 'get',
    url: `/api/governance/metadata/snapshots${query ? `?${query}` : ''}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-metadata-snapshots',
      ...requestOptions
    }
  })
}

export const getMetadataSchemas = (tenantId, datasourceCode, requestOptions = {}) => {
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  if (String(datasourceCode || '').trim()) {
    params.set('datasourceCode', datasourceCode)
  }
  return request({
    method: 'get',
    url: `/api/governance/metadata/schemas?${params.toString()}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-metadata-schemas',
      ...requestOptions
    }
  })
}

export const getMetadataSchemaDetail = (tenantId, datasourceCode, schemaName, requestOptions = {}) =>
  request({
    method: 'get',
    url:
      `/api/governance/metadata/schemas/${encodeURIComponent(schemaName)}` +
      `?tenantId=${encodeURIComponent(tenantId)}&datasourceCode=${encodeURIComponent(datasourceCode)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-metadata-schema-detail',
      ...requestOptions
    }
  })

export const getMetadataTables = (tenantId, datasourceCode, schemaName = '', requestOptions = {}) => {
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  if (String(datasourceCode || '').trim()) {
    params.set('datasourceCode', datasourceCode)
  }
  if (String(schemaName || '').trim()) {
    params.set('schemaName', schemaName)
  }
  return request({
    method: 'get',
    url: `/api/governance/metadata/tables?${params.toString()}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-metadata-tables',
      ...requestOptions
    }
  })
}

export const getMetadataTableDetail = (tenantId, datasourceCode, schemaName, tableName, requestOptions = {}) =>
  request({
    method: 'get',
    url:
      `/api/governance/metadata/tables/${encodeURIComponent(tableName)}` +
      `?tenantId=${encodeURIComponent(tenantId)}&datasourceCode=${encodeURIComponent(datasourceCode)}` +
      `&schemaName=${encodeURIComponent(schemaName)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-metadata-table-detail',
      ...requestOptions
    }
  })

export const getLogicalViews = (tenantId, datasourceCode = '', requestOptions = {}) => {
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  if (String(datasourceCode || '').trim()) {
    params.set('datasourceCode', datasourceCode)
  }
  return request({
    method: 'get',
    url: `/api/governance/logical-views?${params.toString()}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-logical-views',
      ...requestOptions
    }
  })
}

export const getLogicalViewDetail = (tenantId, viewCode, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/governance/logical-views/${encodeURIComponent(viewCode)}?tenantId=${encodeURIComponent(tenantId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-logical-view-detail',
      ...requestOptions
    }
  })

export const getDatabaseViews = (tenantId, datasourceCode = '', requestOptions = {}) => {
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  if (String(datasourceCode || '').trim()) {
    params.set('datasourceCode', datasourceCode)
  }
  return request({
    method: 'get',
    url: `/api/governance/db-views?${params.toString()}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-db-views',
      ...requestOptions
    }
  })
}

export const getDatabaseViewDetail = (tenantId, datasourceCode, viewName, requestOptions = {}) =>
  request({
    method: 'get',
    url:
      `/api/governance/db-views/${encodeURIComponent(viewName)}` +
      `?tenantId=${encodeURIComponent(tenantId)}&datasourceCode=${encodeURIComponent(datasourceCode)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-db-view-detail',
      ...requestOptions
    }
  })

export const formatRuntimeError = error => {
  if (error?.response?.data) {
    const { code, message } = error.response.data
    if (code && message) {
      return `[${code}] ${message}`
    }
    if (message) {
      return message
    }
  }

  return error?.message || 'Unknown runtime error'
}
