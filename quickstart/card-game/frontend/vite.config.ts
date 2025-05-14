import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import ViteYaml from '@modyfi/vite-plugin-yaml'

export default defineConfig({
    plugins: [
        react(),
        ViteYaml(),
    ],
    server: {
        host: true,
        proxy: {
            '/api': {
                target: 'http://localhost:3000/',
                changeOrigin: true,
            },
            '/login/oauth2': {
                target: 'http://localhost:3000/',
                changeOrigin: true,
            },
            '/oauth2': {
                target: 'http://localhost:3000/',
                changeOrigin: true,
            }
        }
    },
})

