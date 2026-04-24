import { defineConfig, loadEnv } from 'vite'
import { createProtectedApiProxy, resolveProxyDefaults } from './scripts/frontend-proxy-shared.mjs'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyDefaults = resolveProxyDefaults(env)
  const isPortableBuild = mode === 'portable'

  return {
    base: isPortableBuild ? './' : '/',
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
      outDir: isPortableBuild ? 'dist-portable' : 'dist',
      target: 'es2015'
    }
  }
})
