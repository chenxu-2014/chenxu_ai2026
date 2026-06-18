import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8081',
      '/meter': 'http://localhost:8081',
      '/compare': 'http://localhost:8081',
      '/kafka': 'http://localhost:8081',
      '/sendOrdered': 'http://localhost:8081',
      '/redis': 'http://localhost:8081',
      '/sentinel': 'http://localhost:8081',
      '/order': 'http://localhost:8081'
    }
  }
})
