import axios from 'axios'

const DEFAULT_USER_ID = 'frontend-operator'
const DEFAULT_ROLE_CODES = 'TENANT_ADMIN,OPERATOR'
const DEFAULT_AUTH_SOURCE = 'header'
const DEFAULT_TIMEOUT_MS = 30000
const DEFAULT_POLL_INTERVAL_MS = 1000
const DEFAULT_POLL_ATTEMPTS = 30

const optimizationTerminalStates = new Set(['SUCCEEDED', 'FAILED'])
const benchmarkTerminalStates = new Set(['SUCCEEDED', 'FAILED'])

const httpClient = axios.create({
  timeout: DEFAULT_TIMEOUT_MS
})

let correlationSequence = 0

const sleep = delayMs => new Promise(resolve => window.setTimeout(resolve, delayMs))

const nextCorrelationId = prefix => {
  correlationSequence += 1
  return `${prefix}-${Date.now()}-${correlationSequence}`
}

const protectedHeaders = (tenantId, prefix) => {
  const issuedAt = Date.now()
  const expiresAt = issuedAt + 10 * 60 * 1000
  const correlationId = nextCorrelationId(prefix)

  return {
    'X-Tenant-Id': tenantId,
    'X-User-Id': DEFAULT_USER_ID,
    'X-Role-Codes': DEFAULT_ROLE_CODES,
    'X-Request-Id': correlationId,
    'X-Trace-Id': correlationId,
    'X-Auth-Source': DEFAULT_AUTH_SOURCE,
    'X-Issued-At': String(issuedAt),
    'X-Expires-At': String(expiresAt)
  }
}

const request = async ({ method, url, data, tenantId, requestPrefix }) => {
  const response = await httpClient.request({
    method,
    url,
    data,
    headers: {
      ...protectedHeaders(tenantId, requestPrefix)
    }
  })

  return response.data
}

const pollUntilTerminal = async ({ fetcher, taskId, tenantId, requestPrefix, terminalStates }) => {
  let latestResponse = null

  for (let attempt = 0; attempt < DEFAULT_POLL_ATTEMPTS; attempt += 1) {
    latestResponse = await fetcher(taskId, tenantId, `${requestPrefix}-poll`)
    if (terminalStates.has(latestResponse.status)) {
      return latestResponse
    }
    await sleep(DEFAULT_POLL_INTERVAL_MS)
  }

  throw new Error(`Task ${taskId} did not reach a terminal state in time`)
}

export const executeQuery = payload =>
  request({
    method: 'post',
    url: '/api/query-execution/queries/execute',
    data: payload,
    tenantId: payload.tenantId,
    requestPrefix: 'frontend-query'
  })

export const submitOptimizationTask = payload =>
  request({
    method: 'post',
    url: '/api/sql-optimization/tasks',
    data: payload,
    tenantId: payload.tenantId,
    requestPrefix: 'frontend-sql-optimization-submit'
  })

export const getOptimizationTaskStatus = (taskId, tenantId, requestPrefix = 'frontend-sql-optimization-status') =>
  request({
    method: 'get',
    url: `/api/sql-optimization/tasks/${taskId}`,
    tenantId,
    requestPrefix
  })

export const waitForOptimizationTask = (taskId, tenantId) =>
  pollUntilTerminal({
    fetcher: getOptimizationTaskStatus,
    taskId,
    tenantId,
    requestPrefix: 'frontend-sql-optimization',
    terminalStates: optimizationTerminalStates
  })

export const submitBenchmarkTask = payload =>
  request({
    method: 'post',
    url: '/api/benchmark-engine/tasks',
    data: payload,
    tenantId: payload.tenantId,
    requestPrefix: 'frontend-benchmark-submit'
  })

export const getBenchmarkTaskStatus = (taskId, tenantId, requestPrefix = 'frontend-benchmark-status') =>
  request({
    method: 'get',
    url: `/api/benchmark-engine/tasks/${taskId}`,
    tenantId,
    requestPrefix
  })

export const waitForBenchmarkTask = (taskId, tenantId) =>
  pollUntilTerminal({
    fetcher: getBenchmarkTaskStatus,
    taskId,
    tenantId,
    requestPrefix: 'frontend-benchmark',
    terminalStates: benchmarkTerminalStates
  })

export const getBenchmarkReport = (reportId, tenantId) =>
  request({
    method: 'get',
    url: `/api/benchmark-engine/reports/${reportId}`,
    tenantId,
    requestPrefix: 'frontend-benchmark-report'
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
