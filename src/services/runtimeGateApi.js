import axios from 'axios'

const DEFAULT_TIMEOUT_MS = 30000
const DEFAULT_POLL_INTERVAL_MS = 1000
const DEFAULT_POLL_ATTEMPTS = 30
const MAX_CORRELATION_ID_LENGTH = 64
const DEFAULT_COMPENSATION_TRACE_PREFIX =
  import.meta.env.VITE_SQLFORGE_GOVERNANCE_SMOKE_FORCE_TRACE_PREFIX || 'SMOKE-FORCE-AUDIT-FALLBACK'

const optimizationTerminalStates = new Set(['SUCCEEDED', 'FAILED'])
const benchmarkTerminalStates = new Set(['SUCCEEDED', 'FAILED'])

const httpClient = axios.create({
  timeout: DEFAULT_TIMEOUT_MS
})

const sleep = delayMs => new Promise(resolve => window.setTimeout(resolve, delayMs))

const trimCorrelationId = value => String(value || '').slice(0, MAX_CORRELATION_ID_LENGTH)

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

export const getGovernanceTraceSummaries = (tenantId, limit = 12, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/governance/history/traces?tenantId=${encodeURIComponent(tenantId)}&limit=${encodeURIComponent(limit)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-trace-summaries',
      ...requestOptions
    }
  })

export const lookupGovernanceTraces = (tenantId, filters = {}, limit = 12, requestOptions = {}) => {
  const params = new URLSearchParams()
  params.set('tenantId', tenantId)
  params.set('limit', String(limit))

  ;['traceId', 'taskId', 'reportId'].forEach(key => {
    const value = String(filters?.[key] || '').trim()
    if (value) {
      params.set(key, value)
    }
  })
  ;['windowStart', 'windowEnd'].forEach(key => {
    const value = String(filters?.[key] || '').trim()
    if (value) {
      params.set(key, value)
    }
  })
  const cursor = String(filters?.cursor || '').trim()
  if (cursor) {
    params.set('cursor', cursor)
  }

  return request({
    method: 'get',
    url: `/api/governance/history/lookups?${params.toString()}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-trace-lookups',
      ...requestOptions
    }
  })
}

export const getGovernanceTraceDetail = (tenantId, traceId, limit = 20, requestOptions = {}) =>
  request({
    method: 'get',
    url:
        `/api/governance/history/traces/${encodeURIComponent(traceId)}` +
      `?tenantId=${encodeURIComponent(tenantId)}&limit=${encodeURIComponent(limit)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-trace-detail',
      ...requestOptions
    }
  })

export const getGovernanceQueryHistoryDetail = (tenantId, historyId, requestOptions = {}) =>
  request({
    method: 'get',
    url: `/api/governance/query-history/${encodeURIComponent(historyId)}?tenantId=${encodeURIComponent(tenantId)}`,
    tenantId,
    requestOptions: {
      requestPrefix: 'frontend-governance-query-history-detail',
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
