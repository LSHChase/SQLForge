import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createProtectedApiProxy, resolveProxyDefaults } from './scripts/frontend-proxy-shared.mjs'

const resolveManualChunk = id => {
  if (!id.includes('node_modules')) {
    return undefined
  }
  if (id.includes('element-plus')) {
    return 'vendor-element-plus'
  }
  if (id.includes('vue-i18n')) {
    return 'vendor-vue-i18n'
  }
  if (id.includes('vue-router')) {
    return 'vendor-router'
  }
  if (id.includes('pinia')) {
    return 'vendor-pinia'
  }
  if (id.includes('@vue') || id.includes('/vue/')) {
    return 'vendor-vue'
  }
  return 'vendor-misc'
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyDefaults = resolveProxyDefaults(env)
  const isPortableBuild = mode === 'portable'

  return {
    plugins: [vue()],
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
      target: 'es2015',
      // After lazy-loading routes and replacing full-library Element Plus install with
      // explicit component registration, the remaining large shared UI runtime stabilizes
      // around ~785 kB minified. Use a repo-specific threshold so build warnings only fire
      // when the bundle regresses materially beyond the current measured baseline.
      chunkSizeWarningLimit: 800,
      rollupOptions: {
        output: {
          manualChunks: resolveManualChunk
        }
      }
    }
  }
})
