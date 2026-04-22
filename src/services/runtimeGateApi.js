import axios from 'axios'

const DEFAULT_USER_ID = 'frontend-operator'
const DEFAULT_ROLE_CODES = 'TENANT_ADMIN,OPERATOR'
const DEFAULT_AUTH_SOURCE = 'header'
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

let correlationSequence = 0

const sleep = delayMs => new Promise(resolve => window.setTimeout(resolve, delayMs))

const trimCorrelationId = value => String(value || '').slice(0, MAX_CORRELATION_ID_LENGTH)

const nextCorrelationId = prefix => {
  correlationSequence += 1
  const suffix = `${Date.now()}-${correlationSequence}`
  const prefixText = String(prefix || 'frontend-runtime')
  const maxPrefixLength = Math.max(1, MAX_CORRELATION_ID_LENGTH - suffix.length - 1)
  return `${prefixText.slice(0, maxPrefixLength)}-${suffix}`
}

const protectedHeaders = (tenantId, options = {}) => {
  const {
    requestPrefix = 'frontend-runtime',
    tracePrefix,
    requestId,
    traceId
  } = options
  const issuedAt = Date.now()
  const expiresAt = issuedAt + 10 * 60 * 1000
  const resolvedRequestId = trimCorrelationId(requestId || nextCorrelationId(requestPrefix))
  const resolvedTraceId = trimCorrelationId(
    traceId || (tracePrefix ? nextCorrelationId(tracePrefix) : resolvedRequestId)
  )

  return {
    'X-Tenant-Id': tenantId,
    'X-User-Id': DEFAULT_USER_ID,
    'X-Role-Codes': DEFAULT_ROLE_CODES,
    'X-Request-Id': resolvedRequestId,
    'X-Trace-Id': resolvedTraceId,
    'X-Auth-Source': DEFAULT_AUTH_SOURCE,
    'X-Issued-At': String(issuedAt),
    'X-Expires-At': String(expiresAt)
  }
}

const request = async ({ method, url, data, tenantId, requestOptions = {} }) => {
  const response = await httpClient.request({
    method,
    url,
    data,
    headers: {
      ...protectedHeaders(tenantId, requestOptions)
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
