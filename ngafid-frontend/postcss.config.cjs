/* eslint-disable no-undef */
//postcss.config.js

module.exports = {

    plugins: {

        "@tailwindcss/postcss": {
            content: [
                "./src/**/*.{js,jsx,ts,tsx}",
                "./public/**/*.html",
            ],
            optimize: {
                strip: (process.env.NODE_ENV === "production")
            },
        },

        autoprefixer: {},
    },

};
