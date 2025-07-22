/* webpack.config.js */
const webpack = require('webpack');
const HtmlPlugin = require("html-webpack-plugin");
const HtmlTagsPlugin = require("html-webpack-tags-plugin");
const CopyWebpackPlugin = require("copy-webpack-plugin");
const DeadCodePlugin = require('webpack-deadcode-plugin');
const dotenv = require('dotenv');
dotenv.config();


const path = require('path');


const isCI   = !!process.env.CI;
const isProd = (process.env.NODE_ENV === 'production');
const doWatch = (!isCI && !isProd);


const { join } = require('path');



class ShowChangedFilesPlugin {

    apply(compiler) {

        compiler.hooks.watchRun.tap('ShowChangedFiles', comp => {

            //No files changed, do nothing
            if (!comp.modifiedFiles || comp.modifiedFiles.length === 0)
                return;

            //Log the files that changed
            console.log(
                '\n\nChanged since last build:',
                [...comp.modifiedFiles].join(', '),
                "\n\n"
            );

        });
    }
    
}



const outputDir   = path.resolve(__dirname, '../ngafid-static/js');
const cesiumCache = path.resolve(outputDir, 'cesium');
const cacheDir    = path.resolve(__dirname, '.webpack_cache');


module.exports = {

    mode: process.env.NODE_ENV || 'development',
    watch: doWatch,

    optimization: {
        usedExports: true,
    },

    resolve: {
        extensions: ['.ts', '.tsx', '.js', '.jsx'],
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
            `${outputDir}/**`,
            `${cesiumCache}/**`,
            "**/\.webpack_cache/**",
        ],
    },

    cache: {
        type: 'filesystem',
        cacheDirectory: cacheDir,
    },

    entry: {
        aggregate_trends: "./src/aggregate_trends.js",
        aggregate: "./src/aggregate.js",
        airsync_imports: "./src/airsync_imports.js",
        airsync_uploads: "./src/airsync_uploads.js",
        bug_report: "./src/bug_report_page.tsx",
        create_account: "./src/create_account.js",
        create_event: "./src/create_event.js",
        event_definition: "./src/event_definition.js",
        event_definitions_display: "./src/event_definitions_display.js",
        event_statistics: "./src/event_statistics.js",
        fleet_trends: "./src/fleet_trends.js",
        flight_display: "./src/flight_display.js",
        flight: "./src/flight.js",
        flights: "./src/flights.js",
        forgot_password: "./src/forgot_password.js",
        home_navbar: "./src/home_navbar.js",
        imports: "./src/imports.js",
        manage_events: "./src/manage_events.js",
        manage_fleet: "./src/manage_fleet.js",
        ngafid_cesium: __dirname + "/src/ngafid_cesium.js",
        proximity_map: "./src/proximity_map.tsx",
        heat_map: "./src/heat_map.tsx",
        reset_password: "./src/reset_password.js",
        severities: "./src/severities.js",
        signed_in_navbar: "./src/signed_in_navbar.js",
        status: "./src/status_page.tsx",
        system_ids: "./src/system_ids.js",
        theme_preload: "./src/theme_preload.js",
        time_zones: "./src/time_zones.js",
        ttf: "./src/ttf.js",
        update_event: "./src/update_event.js",
        update_password: "./src/update_password.js",
        update_profile: "./src/update_profile.js",
        uploads: "./src/uploads.js",
        user_preferences: "./src/preferences_page.js",
        welcome: "./src/welcome.js",
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
                // test: /\.(js|jsx)$/,
                test: /\.[jt]sx?$/,
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
                use: [
                    'style-loader',      //<-- Injects styles into the DOM
                    'css-loader',        //<-- Resolves @import/URL
                    'postcss-loader',    //<-- Runs Tailwind & PostCSS
                ],
            },
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
            'process.env.AZURE_MAPS_KEY': JSON.stringify(process.env.AZURE_MAPS_KEY),
        }),
        new ShowChangedFilesPlugin(),
        new DeadCodePlugin({
            patterns: [
                'src/**/*.(js|jsx|css|ts|tsx)',
            ],
            exclude: [
                'node_modules/**',
                'src/cesium/**',
                'src/ngafid_cesium.js',
                'src/plotly.js',
                'src/plotly.min.js',
            ],
        }),
    ],

};