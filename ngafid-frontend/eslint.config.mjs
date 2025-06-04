// eslint.config.js
import { defineConfig, globalIgnores } from "eslint/config";

import globals from "globals";
import compat from "eslint-plugin-compat";

import reactPlugin from "eslint-plugin-react";
import reactHooksPlugin from "eslint-plugin-react-hooks";
import reactRecommended from "eslint-plugin-react/configs/recommended.js";

import tsPlugin from "@typescript-eslint/eslint-plugin";
import tsParser from "@typescript-eslint/parser";

export default defineConfig([

    //eslint-plgin-compat
    compat.configs["flat/recommended"],

    reactRecommended,

    {
        files: ["**/*.{js,jsx,ts,tsx,mjs}"],

        languageOptions: {

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
                ...globals.browser,
                window: "readonly",
                document: "readonly",
                $: "readonly",
                jQuery: "readonly",
            },
        },

        plugins: {
            '@typescript-eslint': tsPlugin,
            react: reactPlugin,
            "react-hooks": reactHooksPlugin,
        },

        settings: {
            react: { version: "detect" },
            polyfills: [
                "Promise",
                "fetch",
                "URL",
                "URLSearchParams",
                "AbortController",
            ]
        },

        rules: {

            ...tsPlugin.configs['recommended'].rules,
            
            "react/react-in-jsx-scope": "off",      //https://github.com/jsx-eslint/eslint-plugin-react/blob/master/docs/rules/react-in-jsx-scope.md
            "react/jsx-uses-react": "off",          //https://github.com/jsx-eslint/eslint-plugin-react/blob/master/docs/rules/jsx-uses-react.md

            "react/prop-types": "off",              //<-- PropTypes are deprecated, just use TypeScript instead 🤝

            //Rules for React Hooks (...which I don't think we actually use)
            //https://react.dev/reference/rules/rules-of-hooks
            "react-hooks/rules-of-hooks": "error",
            "react-hooks/exhaustive-deps": "warn",

            "no-console": "off",

            "prefer-const": [
                "error", {
                    "destructuring": "any",
                    "ignoreReadBeforeAssign": false
                }
            ],

            "no-var": "error", /* Note: This sometimes suggests to replace 'var' with 'const' when it should be 'let' */

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