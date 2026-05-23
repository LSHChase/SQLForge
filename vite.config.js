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
    // 配置项 plugins：说明该配置的用途、默认值或运行影响。
    plugins: [vue()],
    // 配置项 define：说明该配置的用途、默认值或运行影响。
    define: {
      // 配置项 import.meta.env.VITE_ENABLE_DELIVERY_PROGRESS：说明该配置的用途、默认值或运行影响。
      'import.meta.env.VITE_ENABLE_DELIVERY_PROGRESS': JSON.stringify(deliveryProgressFlag)
    },
    // 配置项 base：说明该配置的用途、默认值或运行影响。
    base: isPortableBuild ? './' : '/',
    // 配置项 server：说明该配置的用途、默认值或运行影响。
    server: {
      // 配置项 host：说明该配置的用途、默认值或运行影响。
      host: '0.0.0.0',
      // 配置项 port：说明该配置的用途、默认值或运行影响。
      port: 3000,
      // 配置项 proxy：说明该配置的用途、默认值或运行影响。
      proxy: {
        // 配置项 /api/query-execution：说明该配置的用途、默认值或运行影响。
        '/api/query-execution': createProtectedApiProxy(
          'http://localhost:8081',
          proxyDefaults,
          'tenant-a',
          'frontend-query'
        ),
        // 配置项 /api/sql-optimization：说明该配置的用途、默认值或运行影响。
        '/api/sql-optimization': createProtectedApiProxy(
          'http://localhost:8082',
          proxyDefaults,
          'tenant-a',
          'frontend-sql-optimization'
        ),
        // 配置项 /api/benchmark-engine：说明该配置的用途、默认值或运行影响。
        '/api/benchmark-engine': createProtectedApiProxy(
          'http://localhost:8083',
          proxyDefaults,
          'tenant-a',
          'frontend-benchmark'
        ),
        // 配置项 /api/governance：说明该配置的用途、默认值或运行影响。
        '/api/governance': createProtectedApiProxy(
          'http://localhost:8080',
          proxyDefaults,
          'system',
          'frontend-governance'
        )
      }
    },
    // 配置项 build：说明该配置的用途、默认值或运行影响。
    build: {
      // 配置项 outDir：说明该配置的用途、默认值或运行影响。
      outDir: isPortableBuild ? 'dist-portable' : 'dist',
      // 配置项 target：说明该配置的用途、默认值或运行影响。
      target: 'es2015',
      // 路由懒加载并改为按组件注册 Element Plus 后，剩余共享 UI 运行时稳定在
      // 约 785 kB 压缩体积。这里使用仓库专属阈值，只在 bundle 明显退化时触发告警。
      // 配置项 chunkSizeWarningLimit：说明该配置的用途、默认值或运行影响。
      chunkSizeWarningLimit: 800,
      // 配置项 rollupOptions：说明该配置的用途、默认值或运行影响。
      rollupOptions: {
        // 配置项 output：说明该配置的用途、默认值或运行影响。
        output: {
          // 配置项 manualChunks：说明该配置的用途、默认值或运行影响。
          manualChunks: resolveManualChunk
        }
      }
    }
  }
})
