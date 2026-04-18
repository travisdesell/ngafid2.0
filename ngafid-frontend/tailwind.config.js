// ngafid-frontend/tailwind.config.js
/** @type {import('tailwindcss').Config} */
export default {
    darkMode: 'class', // Use class-based dark mode
    safelist: 'dark',
    content: [
        'index.html',
        "./src/**/*.{html,js,jsx,ts,tsx}",
    ],
};