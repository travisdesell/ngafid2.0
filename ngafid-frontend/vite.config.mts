// ngafid-frontend/vite.config.mts
import { defineConfig, type ConfigEnv, type UserConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwind from '@tailwindcss/vite'
import tsconfigPaths from 'vite-tsconfig-paths'
import path, { resolve } from 'path';
import fs from 'fs';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import externalGlobals from 'rollup-plugin-external-globals';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const outDir = path.resolve(__dirname, '../ngafid-static');
const assetsDir = 'js';
const cesiumFrom = 'node_modules/cesium/Build/Cesium';
const cesiumTo = 'cesium';


function pickEntry(base: string) {

    const EXTENSIONS = ['.tsx', '.ts', '.jsx', '.js'];
    for (const extension of EXTENSIONS) {
        const p = path.resolve(__dirname, `src/${base}${extension}`);

        //Found a match, return it
        if (fs.existsSync(p))
            return p;
    }

    throw new Error(`Entry not found: src/${base}.{tsx,ts,jsx,js}`);
}



const DEV_HOST = process.env.VITE_DEV_HOST || '192.168.1.8';    //[EX]
const DEV_PORT = Number(process.env.VITE_DEV_PORT || 5173);


export default defineConfig((env: ConfigEnv): UserConfig => {

    const { mode } = env;

    console.log(`Vite mode: ${mode}`);

    return {
        plugins: [
            react(),
            tailwind(),
            tsconfigPaths(),
            viteStaticCopy({
                targets: [
                    {
                        src: cesiumFrom,
                        dest: cesiumTo
                    },
                ],
            }),

            //Map ESM imports to window.globals
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
            emptyOutDir: false, //<-- Keep any other static files we already have
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

                        //Got a CSS file, emit it without a hash:
                        if (extension === '.css') {
                            console.log('Emitting CSS asset:', asset.names[0]);
                            return 'css/[name].css';
                        }

                        //Otherwise, use the default pattern:
                        return 'assets/[name]-[hash][extname]';

                    },
                },
                //Keep any large libraries (e.g., Cesium) out of the bundle entirely:
                external: ['cesium'],
            },
            watch: {
                exclude: [
                    path.resolve(__dirname, '../ngafid-static') + '/**',
                    path.resolve(__dirname, '../ngafid-static/cesium') + '/**',
                ]
            },
            target: 'es2025',
        },

        define: {
            //Keep existing code as-is (uses process.env.*):
            CESIUM_BASE_URL: JSON.stringify('/cesium'),
            'process.env.AZURE_MAPS_KEY': JSON.stringify(process.env.AZURE_MAPS_KEY || ''),
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

        //Optional dev server (good for testing stuff over LAN)
        server: {
            host: '0.0.0.0',
            port: DEV_PORT,
            strictPort: true,
            proxy: {
                '/api': { target: 'http://localhost:8181', changeOrigin: true }
            },
            origin: `http://${DEV_HOST}:${DEV_PORT}`,
            hmr: {
                protocol: 'ws',
                host: DEV_HOST, //<-- Windows LAN IP
                port: DEV_PORT,
            },
        },
    }
});