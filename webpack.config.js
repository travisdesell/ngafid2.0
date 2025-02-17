const webpack = require('webpack');
const HtmlWebPackPlugin = require("html-webpack-plugin");
var path = require('path');
const HtmlPlugin = require("html-webpack-plugin");
const HtmlTagsPlugin = require("html-webpack-tags-plugin");
const CopyWebpackPlugin = require("copy-webpack-plugin");


module.exports = {

    watch: true,


    resolve: {
        fallback: {
            fs: false,
            path: false,
            stream: require.resolve('stream-browserify'),
        },
    },
    externals: {
        cesium: "Cesium"
    },
    /*
    node: {
        fs: 'empty'
    },
    */


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


    entry: {
        aggregate: __dirname + "/src/main/javascript/aggregate.js",
        aggregate_trends : __dirname + "/src/main/javascript/aggregate_trends.js",
        airsync_imports: __dirname + "/src/main/javascript/airsync_imports.js",
        airsync_uploads: __dirname + "/src/main/javascript/airsync_uploads.js",
        create_account: __dirname + "/src/main/javascript/create_account.js",
        create_event: __dirname + "/src/main/javascript/create_event.js",
        event_definition: __dirname + "/src/main/javascript/event_definition.js",
        event_definitions_display: __dirname + "/src/main/javascript/event_definitions_display.js",
        event_statistics: __dirname + "/src/main/javascript/event_statistics.js",
        fleet_trends: __dirname+ "/src/main/javascript/fleet_trends.js",
        flight: __dirname + "/src/main/javascript/flight.js",
        flight_display: __dirname + "/src/main/javascript/flight_display.js",
        flights: __dirname + "/src/main/javascript/flights.js",
        forgot_password: __dirname + "/src/main/javascript/forgot_password.js",
        signed_in_navbar: __dirname + "/src/main/javascript/signed_in_navbar.js",
        home_navbar: __dirname + "/src/main/javascript/home_navbar.js",
        imports: __dirname + "/src/main/javascript/imports.js",
        manage_events: __dirname + "/src/main/javascript/manage_events.js",
        manage_fleet: __dirname + "/src/main/javascript/manage_fleet.js",
        ngafid_cesium: __dirname + "/src/main/javascript/ngafid_cesium.js",
        reset_password: __dirname + "/src/main/javascript/reset_password.js",
        severities: __dirname + "/src/main/javascript/severities.js",
        system_ids: __dirname + "/src/main/javascript/system_ids.js",
        // trends: __dirname + "/src/main/javascript/trends.js",
        time_zones: __dirname + "/src/main/javascript/time_zones.js",
        ttf: __dirname + "/src/main/javascript/ttf.js",
        user_preferences: __dirname + "/src/main/javascript/preferences_page.js",
        update_event: __dirname + "/src/main/javascript/update_event.js",
        update_password: __dirname + "/src/main/javascript/update_password.js",
        update_profile: __dirname + "/src/main/javascript/update_profile.js",
        uploads: __dirname + "/src/main/javascript/uploads.js",
        welcome: __dirname + "/src/main/javascript/welcome.js",
        theme_preload: __dirname + "/src/main/javascript/theme_preload.js",
    },


    devtool: "source-map",


    output: {
        // Farhad: for webpackCesium
        sourcePrefix: "",
        //
        path: __dirname + "/src/main/resources/public/js/",
        filename: "[name]-bundle.js"
    },


    module: {
        rules: [
            {
                test: /\.(js|jsx)$/,
                exclude: /node_modules/,
                use: [
                    'cache-loader',
                    {
                        loader: 'babel-loader',
                        options: {
                            cacheDirectory: true,
                        }
                    }
                ],
                include: path.resolve('src')
            },
            {
                test: /\.css$/,
                use: [
                    'css-loader',
                ]
            },


            {
                test: /\.html$/,
                use: [
                    {
                        loader: "html-loader"
                    }
                ]
            },
            {
                test: /\.mjs$/,
                include: /node_modules/,
                type: "javascript/auto",
            }
        ]
    },


    plugins: [
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery",
            'window.jQuery': 'jquery',
            Popper: ['popper.js', 'default']
        }),
        new CopyWebpackPlugin({
            patterns: [
                {
                    from: "node_modules/cesium/Build/Cesium",
                    to: "cesium",
                },
            ],
        }),
        new HtmlPlugin({
            template: "src/main/resources/public/templates/flights.html",
        }),
        new HtmlTagsPlugin({
            append: false,
            tags: ["cesium/Widgets/widgets.css", "cesium/Cesium.js"],
        }),
        new webpack.DefinePlugin({
            CESIUM_BASE_URL: JSON.stringify("/cesium"),
        }),


        /*
        new HtmlWebPackPlugin({
            template: "./src/main/resources/public/js3/src/index.html",
            filename: "./index.html"
        })
        */
    ]
};
