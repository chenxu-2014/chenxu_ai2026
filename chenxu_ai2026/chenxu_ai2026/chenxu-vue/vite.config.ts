import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8080',
      '/meter': 'http://localhost:8080',
      '/compare': 'http://localhost:8080',
      '/kafka': 'http://localhost:8080',
      '/sendOrdered': 'http://localhost:8080',
      '/redis': 'http://localhost:8080',
      '/sentinel': 'http://localhost:8080',
      '/order': 'http://localhost:8080'
    }
  }
})
