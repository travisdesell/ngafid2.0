const webpack = require('webpack');
const HtmlPlugin = require("html-webpack-plugin");
const HtmlTagsPlugin = require("html-webpack-tags-plugin");
const CopyWebpackPlugin = require("copy-webpack-plugin");


const path = require('path');


const isCI   = !!process.env.CI;
const isProd = (process.env.NODE_ENV === 'production');
const doWatch = (!isCI && !isProd);

module.exports = {

    mode: process.env.NODE_ENV || 'development',
    watch: doWatch,

    resolve: {
        fallback: {
            fs: false,
            path: false,
            stream: require.resolve('stream-browserify'),
            events: require.resolve('events/'),
        },
    },
    externals: {
        cesium: "Cesium",
        plotly: "Plotly",
    },
    watchOptions: {
        ignored: [
            "/node_modules/",
            path.resolve(__dirname, "src/main/resources/public/dist/"), //<-- Ignore built files so they aren't recompiled
        ],
    },

    cache: {
        type: 'filesystem',
        cacheDirectory: path.resolve(__dirname, '.webpack_cache'),
    },

    entry: {
        aggregate: "./src/aggregate.js",
        aggregate_trends: "./src/aggregate_trends.js",
        airsync_imports: "./src/airsync_imports.js",
        airsync_uploads: "./src/airsync_uploads.js",
        create_account: "./src/create_account.js",
        create_event: "./src/create_event.js",
        event_definition: "./src/event_definition.js",
        event_definitions_display: "./src/event_definitions_display.js",
        event_statistics: "./src/event_statistics.js",
        fleet_trends: "./src/fleet_trends.js",
        flight: "./src/flight.js",
        flight_display: "./src/flight_display.js",
        flights: "./src/flights.js",
        forgot_password: "./src/forgot_password.js",
        signed_in_navbar: "./src/signed_in_navbar.js",
        home_navbar: "./src/home_navbar.js",
        imports: "./src/imports.js",
        manage_events: "./src/manage_events.js",
        manage_fleet: "./src/manage_fleet.js",
        ngafid_cesium: __dirname + "/src/ngafid_cesium.js",
        reset_password: "./src/reset_password.js",
        severities: "./src/severities.js",
        system_ids: "./src/system_ids.js",
        time_zones: "./src/time_zones.js",
        ttf: "./src/ttf.js",
        user_preferences: "./src/preferences_page.js",
        update_event: "./src/update_event.js",
        update_password: "./src/update_password.js",
        update_profile: "./src/update_profile.js",
        uploads: "./src/uploads.js",
        welcome: "./src/welcome.js",
        theme_preload: "./src/theme_preload.js",
    },

    devtool: (process.env.CI ? false : 'source-map'),

    output: {
        path: path.resolve(__dirname, "../ngafid-static/js/"),
        filename: "[name]-bundle.js",
        publicPath: "/js/",
    },

    module: {
        rules: [
            {
                test: /\.(js|jsx)$/,
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        cacheDirectory: true,
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
        new CopyWebpackPlugin({
            patterns: [
                {
                    from: "node_modules/cesium/Build/Cesium",
                    to: "cesium",
                },
            ],
        }),
        new webpack.DefinePlugin({
            CESIUM_BASE_URL: JSON.stringify("/cesium"),
        }),
    ]

};