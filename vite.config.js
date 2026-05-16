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
  const hasDeliveryProgressFlag = Object.prototype.hasOwnProperty.call(
    env,
    'VITE_ENABLE_DELIVERY_PROGRESS'
  )
  const deliveryProgressFlag =
    isPortableBuild && !hasDeliveryProgressFlag ? 'false' : env.VITE_ENABLE_DELIVERY_PROGRESS

  return {
    plugins: [vue()],
    define: {
      'import.meta.env.VITE_ENABLE_DELIVERY_PROGRESS': JSON.stringify(deliveryProgressFlag)
    },
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
      // 路由懒加载并改为按组件注册 Element Plus 后，剩余共享 UI 运行时稳定在
      // 约 785 kB 压缩体积。这里使用仓库专属阈值，只在 bundle 明显退化时触发告警。
      chunkSizeWarningLimit: 800,
      rollupOptions: {
        output: {
          manualChunks: resolveManualChunk
        }
      }
    }
  }
})
