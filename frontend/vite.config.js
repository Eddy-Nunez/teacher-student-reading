import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Allow tunnel/custom hosts (trycloudflare, LAN, etc.) during local demos.
    allowedHosts: true,
    // In dev, forward API calls to the local Spring Boot backend.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
