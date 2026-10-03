/**
 * Vitest configuration for the NGAFID frontend.
 *
 * Runs the unit/component tests (`*.test.*` / `*.spec.*` under `src/`) in a jsdom
 * environment with the React plugin for JSX/TSX, loading `vitest.setup.ts` for the
 * `@testing-library/jest-dom` matchers. The vendored Cesium build under
 * `src/cesium/` and the usual build output directories are excluded.
 *
 * The `.mts` extension makes this an ESM config, which Vitest/Vite loads without
 * the CommonJS-interop deprecation warning.
 */
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    include: ["src/**/*.{test,spec}.{js,jsx,ts,tsx}"],
    exclude: ["node_modules/**", "dist/**", "build/**", "src/cesium/**"],
  },
});
