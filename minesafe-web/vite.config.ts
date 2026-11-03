import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  // Load environment variables including secrets that shouldn't be exposed to the client
  const env = loadEnv(mode, process.cwd(), '');
  const aiBackendTarget = env.VITE_AI_ANALYSIS_API_URL || 'http://127.0.0.1:8000';
  const aiApiKey = env.MINESAFE_AI_API_KEY || '';

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        // Secure development proxy to avoid exposing private API key in the browser
        '/api/ai': {
          target: aiBackendTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api\/ai/, ''),
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq) => {
              if (aiApiKey) {
                proxyReq.setHeader('Authorization', `Bearer ${aiApiKey}`);
              }
            });
          },
        },
      },
    },
    test: {
      globals: true,
      environment: 'node',
    },
  };
});
