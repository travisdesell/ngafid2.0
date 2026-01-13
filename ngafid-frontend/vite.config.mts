// ngafid-frontend/vite.config.mts
import tailwind from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path, { resolve } from 'path';
import externalGlobals from 'rollup-plugin-external-globals';
import { fileURLToPath } from 'url';
import { defineConfig, type ConfigEnv, type UserConfig } from 'vite';
import tsconfigPaths from 'vite-tsconfig-paths';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const outDir = path.resolve(__dirname, '../ngafid-static');
const assetsDir = 'js';

export default defineConfig((env: ConfigEnv): UserConfig => {

    const { mode } = env;

    console.log(`Vite mode: ${mode}`);

    return {
        plugins: [
            react(),
            tailwind(),
            tsconfigPaths(),

            // Map ESM imports to window.globals
            {
                ...externalGlobals({
                    cesium: 'Cesium',
                }),
                enforce: 'post',
                apply: 'build',
            },
        ],

        build: {
            outDir,
            assetsDir,
            emptyOutDir: false, // <-- Keep any existing static files
            sourcemap: true,
            rollupOptions: {
                input: {
                    app: resolve(__dirname, 'index.html'),
                },
                output: {
                    entryFileNames: 'js/[name]-bundle.js',
                    chunkFileNames: 'js/[name]-[hash].js',
                    assetFileNames: (asset) => {

                        const candidates = (asset.names && asset.names.length ? asset.names : asset.originalFileNames) ?? [];
                        const first = candidates[0] ?? '';
                        const extension = first ? path.extname(first).toLowerCase() : '';

                        // Got a CSS file, emit it without a hash:
                        if (extension === '.css') {
                            console.log('Emitting CSS asset:', asset.names[0]);
                            return 'css/[name].css';
                        }

                        // Otherwise, use the default pattern:
                        return 'assets/[name]-[hash][extname]';

                    },
                },
                // Keep any large libraries (e.g., Cesium) out of the bundle entirely:
                external: ['cesium'],
            },
            watch: {
                exclude: [
                    path.resolve(__dirname, '../ngafid-static') + '/**',
                    path.resolve(__dirname, '../ngafid-static/cesium') + '/**',
                ]
            },
            target: 'es2022',
        },

        define: {
            CESIUM_BASE_URL: JSON.stringify('/cesium'),
            // 'process.env.AZURE_MAPS_KEY': JSON.stringify(process.env.AZURE_MAPS_KEY || ''),
        },

        resolve: {
            alias: {
                'process/browser': 'process',
                '@': path.resolve(__dirname, 'src/app'),
            },
            extensions: ['.ts', '.tsx', '.js', '.jsx'],
            dedupe: ['react', 'react-dom'],
        },

        optimizeDeps: {
            include: ['react', 'react-dom'],
        },

        server: {
            host: '0.0.0.0',
            port: 4000,
            strictPort: true,
            proxy: { '/api': { target: 'http://localhost:8181', changeOrigin: true }}
        }

    }

});