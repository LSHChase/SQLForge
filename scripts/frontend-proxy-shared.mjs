const MAX_CORRELATION_ID_LENGTH = 64
const DEV_PROXY_HEADER_NAMES = [
  'x-sqlforge-dev-tenant-id',
  'x-sqlforge-dev-request-prefix',
  'x-sqlforge-dev-trace-prefix',
  'x-sqlforge-dev-request-id',
  'x-sqlforge-dev-trace-id'
]

let correlationSequence = 0

const normalizeHeaders = headers =>
  Object.fromEntries(
    Object.entries(headers || {}).map(([key, value]) => [String(key).toLowerCase(), value])
  )

const trimCorrelationId = value => String(value || '').slice(0, MAX_CORRELATION_ID_LENGTH)

const nextCorrelationId = prefix => {
  correlationSequence += 1
  const suffix = `${Date.now()}-${correlationSequence}`
  const prefixText = trimCorrelationId(prefix || 'frontend-proxy')
  const maxPrefixLength = Math.max(1, MAX_CORRELATION_ID_LENGTH - suffix.length - 1)
  return `${prefixText.slice(0, maxPrefixLength)}-${suffix}`
}

export const removeProxyHeader = (proxyReq, headerName) => {
  if (typeof proxyReq.removeHeader === 'function') {
    proxyReq.removeHeader(headerName)
  }
}

export const resolveProxyDefaults = env => ({
  userId: env.SQLFORGE_DEV_PROXY_USER_ID || env.userId || 'frontend-operator',
  roleCodes: env.SQLFORGE_DEV_PROXY_ROLE_CODES || env.roleCodes || 'TENANT_ADMIN,OPERATOR',
  authSource: env.SQLFORGE_DEV_PROXY_AUTH_SOURCE || env.authSource || 'frontend-portable-proxy',
  ttlMs: Number(env.SQLFORGE_DEV_PROXY_TTL_MS || env.ttlMs || 600000)
})

export const buildProtectedProxyHeaders = (
  requestHeaders,
  proxyDefaults,
  defaultTenantId,
  defaultRequestPrefix
) => {
  const normalizedHeaders = normalizeHeaders(requestHeaders)
  const tenantId = String(normalizedHeaders['x-sqlforge-dev-tenant-id'] || defaultTenantId)
  const requestPrefix = String(
    normalizedHeaders['x-sqlforge-dev-request-prefix'] || defaultRequestPrefix
  )
  const tracePrefix = String(normalizedHeaders['x-sqlforge-dev-trace-prefix'] || '')
  const issuedAt = Date.now()
  const expiresAt = issuedAt + proxyDefaults.ttlMs
  const requestId = trimCorrelationId(
    normalizedHeaders['x-sqlforge-dev-request-id'] || nextCorrelationId(requestPrefix)
  )
  const traceId = trimCorrelationId(
    normalizedHeaders['x-sqlforge-dev-trace-id'] ||
      (tracePrefix ? nextCorrelationId(tracePrefix) : requestId)
  )

  return {
    cleanupHeaderNames: DEV_PROXY_HEADER_NAMES,
    headers: {
      'X-Tenant-Id': tenantId,
      'X-User-Id': proxyDefaults.userId,
      'X-Role-Codes': proxyDefaults.roleCodes,
      'X-Request-Id': requestId,
      'X-Trace-Id': traceId,
      'X-Auth-Source': proxyDefaults.authSource,
      'X-Issued-At': String(issuedAt),
      'X-Expires-At': String(expiresAt)
    }
  }
}

export const createProtectedApiProxy = (
  target,
  proxyDefaults,
  defaultTenantId,
  defaultRequestPrefix
) => ({
  target,
  changeOrigin: true,
  configure(proxy) {
    proxy.on('proxyReq', (proxyReq, req) => {
      const protectedHeaders = buildProtectedProxyHeaders(
        req.headers,
        proxyDefaults,
        defaultTenantId,
        defaultRequestPrefix
      )

      Object.entries(protectedHeaders.headers).forEach(([headerName, headerValue]) => {
        proxyReq.setHeader(headerName, headerValue)
      })
      protectedHeaders.cleanupHeaderNames.forEach(headerName => removeProxyHeader(proxyReq, headerName))
    })
  }
})
