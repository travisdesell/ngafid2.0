// eslint.config.js
import { defineConfig, globalIgnores } from "eslint/config";

import globals from "globals";
import compat from "eslint-plugin-compat";

import reactPlugin from "eslint-plugin-react";
import reactHooksPlugin from "eslint-plugin-react-hooks";
import reactRecommended from "eslint-plugin-react/configs/recommended.js";

import tsPlugin from "@typescript-eslint/eslint-plugin";
import tsParser from "@typescript-eslint/parser";

import importPlugin from 'eslint-plugin-import';
import js from '@eslint/js';
import WebpackDeadcodePlugin from "webpack-deadcode-plugin";

export default defineConfig([

    //eslint-plgin-compat
    compat.configs["flat/recommended"],

    reactRecommended,

    js.configs.recommended,

    importPlugin.flatConfigs.recommended,

    {
        ...reactRecommended,

        files: ["**/*.{js,jsx,ts,tsx,mjs}"],

        languageOptions: {

            ...reactRecommended.languageOptions,

            parser: tsParser,

            ecmaVersion: "latest",
            sourceType: "module",

            parserOptions: {
                project: './tsconfig.json',
                sourceType: 'module',
                ecmaVersion: 'latest',
                ecmaFeatures: { jsx: true },
            },

            globals: {

                //Browser globals
                ...globals.browser,
                window: "readonly",
                document: "readonly",
                $: "readonly",
                jQuery: "readonly",
                Cesium: "readonly",

                //Injected globals
                admin: 'readonly',
                aggregateView: 'readonly',
                airframeMap: 'readonly',
                airframes: 'readonly',
                airports: 'readonly',
                airSyncEnabled: 'readonly',
                airsyncTimeout: 'readonly',
                currentPage: 'readonly',
                doubleTimeSeriesNames: 'readonly',
                eventDefinitions: 'readonly',
                eventNames: 'readonly',
                fleetManager: 'readonly',
                fleetNames: 'readonly',
                hasStatusView: 'readonly',
                imports: 'readonly',
                isAdmin: 'readonly',    //NOTE: 'isAdmin' seems to be a duplicate of 'admin' ⚠
                isUploader: 'readonly',
                lastUpdateTime: 'readonly',
                modifyTailsAccess: 'readonly',
                numberPages: 'readonly',
                pendingUploads: 'readonly',
                plotMapHidden: 'readonly',
                runways: 'readonly',
                systemIds: 'readonly',
                tagNames: 'readonly',
                tailNumbers: 'readonly',
                unconfirmedTailsCount: 'readonly',
                uploads: 'readonly',
                user: 'readonly',
                userName: 'readonly',
                userPreferences: 'readonly',
                visitedAirports: 'readonly',
                visitedRunways: 'readonly',
                waitingUserCount: 'readonly',
            },

        },

        plugins: {
            '@typescript-eslint': tsPlugin,
            react: reactPlugin,
            "react-hooks": reactHooksPlugin,
        },

        settings: {
            react: { version: "detect" },   /* Still getting a 'Warning: React version not specified in eslint-plugin-react settings.' message even with this set, gonna just ignore it 🤷‍♂️ */
            polyfills: [
                "Promise",
                "fetch",
                "URL",
                "URLSearchParams",
                "AbortController",
            ],
            'import/resolver': {
                webpack: {},
                "node": {
                    "extensions": [".js", ".jsx", ".ts", ".tsx", ".mjs"],
                    "moduleDirectory": ["node_modules", "src/"],
                }
            },
        },

        rules: {

            ...tsPlugin.configs['recommended'].rules,

            
            /* JSX Rules */
            //https://github.com/jsx-eslint/eslint-plugin-react/blob/master/docs/rules/react-in-jsx-scope.md
            "react/react-in-jsx-scope": "off",

            //https://github.com/jsx-eslint/eslint-plugin-react/blob/master/docs/rules/jsx-uses-react.md
            "react/jsx-uses-react": "off",



            "react/prop-types": "off",              /* <-- PropTypes are deprecated, just use TypeScript instead 🤝 */



            /* Rules for React Hooks (...which I don't think we actually use) */
            //https://react.dev/reference/rules/rules-of-hooks
            "react-hooks/rules-of-hooks": "error",
            "react-hooks/exhaustive-deps": "warn",



            //https://eslint.org/docs/latest/rules/no-console
            "no-console": "off",



            //https://eslint.org/docs/latest/rules/prefer-const
            "prefer-const": [
                "error", {
                    "destructuring": "any",
                    "ignoreReadBeforeAssign": false
                }
            ],



            //https://eslint.org/docs/latest/rules/no-var 
            "no-var": "error",   /* Note: This sometimes suggests to replace 'var' with 'const' when it should be 'let' */



            /* String Formatting Rules */
            //https://eslint.org/docs/latest/rules/no-useless-concat
            "no-useless-concat": "error",
            //https://eslint.org/docs/latest/rules/prefer-template
            "prefer-template": "error",     



            //https://www.npmjs.com/package/eslint-plugin-import
            "import/no-unresolved": ["error", { "commonjs": true, "amd": true }],
            "import/named": "error",
            "import/namespace": "error",
            "import/default": "error",
            "import/export": "error",
            


            /* Semicolon Rules */
            //https://eslint.org/docs/latest/rules/semi
            "semi": ["error", "always"],
            "semi-spacing": ["error", { "before": false, "after": true }],



            /* Unreached Code Rules */
            //https://eslint.org/docs/latest/rules/no-unreachable
            "no-unreachable": "error",

        },

        ignores: [
            "**/node_modules/**",
            "**/dist/**",
            "**/build/**",
            "**/Build/**",
            "**/webpack.config.js",
            "**/tailwind.config.js",
            "**/eslint.config.mjs",
        ],

    },

    globalIgnores([
        "**/node_modules/**",
        "**/dist/**",
        "**/build/**",
        "**/Build/**",
        "**/webpack.config.js",
        "**/tailwind.config.js",
        "**/eslint.config.mjs",
    ]),
    
]);