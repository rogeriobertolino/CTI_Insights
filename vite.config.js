import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        configure(proxy) {
          // The browser calls Vite on the same origin; this is a server-to-server hop.
          proxy.on('proxyReq', request => request.removeHeader('origin'))
        },
      },
    },
  },
})
