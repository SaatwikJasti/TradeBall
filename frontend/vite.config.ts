import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';

export default defineConfig({
  root: fileURLToPath(new URL('.', import.meta.url)),
  base: './',
  plugins: [react()],
  server: { host: '127.0.0.1', port: 5173, strictPort: true,
    proxy: { '/api': { target: 'http://localhost:18080', changeOrigin: true } } },
  build: { outDir: '../www/compare', emptyOutDir: true },
  test: { environment: 'jsdom', setupFiles: ['./src/test-setup.ts'], clearMocks: true }
});
