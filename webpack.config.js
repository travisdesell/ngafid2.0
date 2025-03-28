const webpack = require('webpack');
const path = require('path');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;


module.exports = {
    mode: process.env.NODE_ENV || 'development',
    watch: true,

    resolve: {
        fallback: {
            fs: false,
            path: false,
            stream: require.resolve('stream-browserify'),
            events: require.resolve('events/'),
        },
        extensions: ['.js', '.jsx', '.ts', '.tsx']
    },

    watchOptions: {
        aggregateTimeout: 300,
        poll: 1000,
        ignored: /node_modules/
    },

    optimization: {
        removeAvailableModules: false,
        removeEmptyChunks: false,
        splitChunks: false,
    },

    cache: {
        type: 'filesystem',
        cacheDirectory: path.resolve(__dirname, '.webpack_cache'),
    },

    entry: {

        /*
            * ❌ - Not converted to TypeScript
            * 🧪 - Conversion in progress...
            * ✔️ - Converted to TypeScript
            
            (Non-exhaustive list of JS files needed for conversion,
            only includes the entry points for the webpack build)
        */

        /* ❌ */ aggregate: "./src/main/javascript/aggregate.js",
        /* ❌ */ aggregate_trends: "./src/main/javascript/aggregate_trends.js",
        /* ❌ */ airsync_imports: "./src/main/javascript/airsync_imports.js",
        /* ❌ */ airsync_uploads: "./src/main/javascript/airsync_uploads.js",
        /* ❌ */ create_account: "./src/main/javascript/create_account.js",
        /* ❌ */ create_event: "./src/main/javascript/create_event.js",
        /* ❌ */ event_definition: "./src/main/javascript/event_definition.js",
        /* ❌ */ event_definitions_display: "./src/main/javascript/event_definitions_display.js",
        /* ❌ */ event_statistics: "./src/main/javascript/event_statistics.js",
        /* ❌ */ fleet_trends: "./src/main/javascript/fleet_trends.js",
        /* ❌ */ flight: "./src/main/javascript/flight.js",
        /* ❌ */ flight_display: "./src/main/javascript/flight_display.js",
        /* ❌ */ flights: "./src/main/javascript/flights.js",
        /* ✔️ */ forgot_password: "./src/main/javascript/forgot_password.tsx",
        /* ❌ */ signed_in_navbar: "./src/main/javascript/signed_in_navbar.js",
        /* ❌ */ home_navbar: "./src/main/javascript/home_navbar.js",
        /* ❌ */ imports: "./src/main/javascript/imports.js",
        /* ❌ */ manage_events: "./src/main/javascript/manage_events.js",
        /* ❌ */ manage_fleet: "./src/main/javascript/manage_fleet.js",
        /* ✔️ */ reset_password: "./src/main/javascript/reset_password.tsx",
        /* ❌ */ severities: "./src/main/javascript/severities.js",
        /* ❌ */ system_ids: "./src/main/javascript/system_ids.js",
        /* ❌ */ time_zones: "./src/main/javascript/time_zones.js",
        /* ❌ */ ttf: "./src/main/javascript/ttf.js",
        /* ❌ */ user_preferences: "./src/main/javascript/preferences_page.js",
        /* ❌ */ update_event: "./src/main/javascript/update_event.js",
        /* ✔️ */ update_password: "./src/main/javascript/update_password.tsx",
        /* ❌ */ update_profile: "./src/main/javascript/update_profile.js",
        /* ❌ */ uploads: "./src/main/javascript/uploads.js",
        /* ❌ */ welcome: "./src/main/javascript/welcome.js",
        /* ❌ */ theme_preload: "./src/main/javascript/theme_preload.js",
        
    },

    devtool: "source-map",

    output: {
        path: path.resolve(__dirname, "src/main/resources/public/js/"),
        filename: "[name]-bundle.js"
    },

    module: {
        rules: [
            {
                test: /\.(js|jsx|ts|tsx)$/,
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        cacheDirectory: true,
                        presets: [
                            '@babel/preset-env',
                            '@babel/preset-react',
                            '@babel/preset-typescript'
                        ]
                    }
                },
                include: path.resolve('src')
            },
            {
                test: /\.html$/,
                use: ["html-loader"]
            },
            {
                test: /\.mjs$/,
                include: /node_modules/,
                type: "javascript/auto",
            },
            {
                test: /\.css$/,
                use: ["style-loader", "css-loader"]
            }
        ]
    },

    plugins: [
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery",
            'window.jQuery': 'jquery',
            Popper: ['@popperjs/core', 'default']
        }),
        new webpack.ProvidePlugin({
            process: 'process/browser',
        }),

        /* Uncomment to enable bundle analyzer (defaults to port 8888) */
        //  new BundleAnalyzerPlugin(),
    ],
};
