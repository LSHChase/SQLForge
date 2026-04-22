import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
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
      '/api/query-execution': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/sql-optimization': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      '/api/benchmark-engine': {
        target: 'http://localhost:8083',
        changeOrigin: true
      },
      '/api/governance': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    target: 'es2015'
  }
})
