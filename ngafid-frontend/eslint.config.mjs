// eslint.config.js
import { defineConfig } from "eslint/config";
import reactPlugin from "eslint-plugin-react";
import reactHooksPlugin from "eslint-plugin-react-hooks";
import globals from "globals";
import reactRecommended from "eslint-plugin-react/configs/recommended.js";

export default defineConfig([
    
    reactRecommended,

    {
        files: ["**/*.{js,jsx,ts,tsx,mjs}"],

        languageOptions: {

            ecmaVersion: "latest",
            sourceType: "module",

            parserOptions: {
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
            react: reactPlugin,
            "react-hooks": reactHooksPlugin,
        },

        settings: {
            react: { version: "detect" },
        },

        rules: {
            
            "react/react-in-jsx-scope": "off",      //https://github.com/jsx-eslint/eslint-plugin-react/blob/master/docs/rules/react-in-jsx-scope.md
            "react/jsx-uses-react": "off",          //https://github.com/jsx-eslint/eslint-plugin-react/blob/master/docs/rules/jsx-uses-react.md

            "react/prop-types": "off",              //<-- PropTypes are deprecated, just use TypeScript instead 🤝

            //Rules for React Hooks (...which I don't think we actually use)
            //https://react.dev/reference/rules/rules-of-hooks
            "react-hooks/rules-of-hooks": "error",
            "react-hooks/exhaustive-deps": "warn",

            "no-console": "off",

        },

        ignores: [
            "**/node_modules/**",
            "**/dist/**",
            "**/build/**",
            "**/Build/**",
        ],
        
    },
]);