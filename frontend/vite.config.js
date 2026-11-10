import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// El backend (SecurityConfig.corsConfigurationSource) solo acepta el origen http://localhost:5173.
// Por eso el servidor de desarrollo se fija en ese puerto (strictPort) y en el host "localhost".
export default defineConfig({
  plugins: [react()],
  server: {
    host: 'localhost',
    port: 5173,
    strictPort: true,
  },
  preview: {
    host: 'localhost',
    port: 5173,
    strictPort: true,
  },
  build: {
    sourcemap: false,
    chunkSizeWarningLimit: 900,
  },
});
