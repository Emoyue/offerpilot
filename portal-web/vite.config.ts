import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * 学员端构建配置。
 *
 * 开发期通过代理把 /api 转发到后端 8080，避免 CORS（生产由 Nginx 同域分区承接，D20）。
 * 产物输出到 dist/，由 deploy/scripts/build.sh 汇总到 deploy/dist/portal。
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    chunkSizeWarningLimit: 1200
  }
})
