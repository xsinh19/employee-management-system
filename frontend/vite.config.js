import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The browser only ever talks to the Vite dev server (localhost:5173).
// Requests to /api are forwarded server-side to Spring Boot (localhost:8080),
// so from the browser's point of view everything is same-origin and CORS never applies.
// changeOrigin: false keeps the Host header as localhost:5173, so Spring builds Location
// headers the browser can follow. (Vite's string shorthand silently sets changeOrigin: true.)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: false,
      },
    },
  },
});
