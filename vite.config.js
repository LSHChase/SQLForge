import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

const MAX_CORRELATION_ID_LENGTH = 64
const DEV_PROXY_HEADER_NAMES = [
  'x-sqlforge-dev-tenant-id',
  'x-sqlforge-dev-request-prefix',
  'x-sqlforge-dev-trace-prefix',
  'x-sqlforge-dev-request-id',
  'x-sqlforge-dev-trace-id'
]

let correlationSequence = 0

const trimCorrelationId = value => String(value || '').slice(0, MAX_CORRELATION_ID_LENGTH)

const nextCorrelationId = prefix => {
  correlationSequence += 1
  const suffix = `${Date.now()}-${correlationSequence}`
  const prefixText = trimCorrelationId(prefix || 'vite-dev-proxy')
  const maxPrefixLength = Math.max(1, MAX_CORRELATION_ID_LENGTH - suffix.length - 1)
  return `${prefixText.slice(0, maxPrefixLength)}-${suffix}`
}

const removeProxyHeader = (proxyReq, headerName) => {
  if (typeof proxyReq.removeHeader === 'function') {
    proxyReq.removeHeader(headerName)
  }
}

const resolveProxyDefaults = env => ({
  userId: env.SQLFORGE_DEV_PROXY_USER_ID || 'frontend-operator',
  roleCodes: env.SQLFORGE_DEV_PROXY_ROLE_CODES || 'TENANT_ADMIN,OPERATOR',
  authSource: env.SQLFORGE_DEV_PROXY_AUTH_SOURCE || 'vite-dev-proxy',
  ttlMs: Number(env.SQLFORGE_DEV_PROXY_TTL_MS || 600000)
})

const createProtectedApiProxy = (target, proxyDefaults, defaultTenantId, defaultRequestPrefix) => ({
  target,
  changeOrigin: true,
  configure(proxy) {
    proxy.on('proxyReq', (proxyReq, req) => {
      const tenantId = String(req.headers['x-sqlforge-dev-tenant-id'] || defaultTenantId)
      const requestPrefix = String(req.headers['x-sqlforge-dev-request-prefix'] || defaultRequestPrefix)
      const tracePrefix = String(req.headers['x-sqlforge-dev-trace-prefix'] || '')
      const issuedAt = Date.now()
      const expiresAt = issuedAt + proxyDefaults.ttlMs
      const requestId = trimCorrelationId(
        req.headers['x-sqlforge-dev-request-id'] || nextCorrelationId(requestPrefix)
      )
      const traceId = trimCorrelationId(
        req.headers['x-sqlforge-dev-trace-id'] ||
          (tracePrefix ? nextCorrelationId(tracePrefix) : requestId)
      )

      proxyReq.setHeader('X-Tenant-Id', tenantId)
      proxyReq.setHeader('X-User-Id', proxyDefaults.userId)
      proxyReq.setHeader('X-Role-Codes', proxyDefaults.roleCodes)
      proxyReq.setHeader('X-Request-Id', requestId)
      proxyReq.setHeader('X-Trace-Id', traceId)
      proxyReq.setHeader('X-Auth-Source', proxyDefaults.authSource)
      proxyReq.setHeader('X-Issued-At', String(issuedAt))
      proxyReq.setHeader('X-Expires-At', String(expiresAt))

      DEV_PROXY_HEADER_NAMES.forEach(headerName => removeProxyHeader(proxyReq, headerName))
    })
  }
})

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyDefaults = resolveProxyDefaults(env)

  return {
    plugins: [
      vue(),
      AutoImport({
        imports: ['vue', 'vue-router', 'pinia'],
        resolvers: [ElementPlusResolver()],
        eslintrc: {
          enabled: false
        }
      }),
      Components({
        resolvers: [
          ElementPlusResolver({
            importStyle: 'css'
          })
        ]
      })
    ],
    server: {
      host: '0.0.0.0',
      port: 3000,
      proxy: {
        '/api/query-execution': createProtectedApiProxy(
          'http://localhost:8081',
          proxyDefaults,
          'tenant-a',
          'frontend-query'
        ),
        '/api/sql-optimization': createProtectedApiProxy(
          'http://localhost:8082',
          proxyDefaults,
          'tenant-a',
          'frontend-sql-optimization'
        ),
        '/api/benchmark-engine': createProtectedApiProxy(
          'http://localhost:8083',
          proxyDefaults,
          'tenant-a',
          'frontend-benchmark'
        ),
        '/api/governance': createProtectedApiProxy(
          'http://localhost:8080',
          proxyDefaults,
          'system',
          'frontend-governance'
        )
      }
    },
    build: {
      target: 'es2015'
    }
  }
})
