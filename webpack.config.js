const webpack = require('webpack');
const HtmlWebPackPlugin = require("html-webpack-plugin");
var path = require('path');

module.exports = {
    watch: true,

    node: {
          fs: 'empty'
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

    entry: {
        home_navbar: __dirname + "/src/main/javascript/home_navbar.js",
        create_account: __dirname + "/src/main/javascript/create_account.js",
        signed_in_navbar: __dirname + "/src/main/javascript/signed_in_navbar.js",
        manage_fleet: __dirname + "/src/main/javascript/manage_fleet.js",
        update_password: __dirname + "/src/main/javascript/update_password.js",
        reset_password: __dirname + "/src/main/javascript/reset_password.js",
        update_profile: __dirname + "/src/main/javascript/update_profile.js",
        uploads: __dirname + "/src/main/javascript/uploads.js",
        imports: __dirname + "/src/main/javascript/imports.js",
        flights: __dirname + "/src/main/javascript/flights.js",
        flight: __dirname + "/src/main/javascript/flight.js",
        event_statistics: __dirname + "/src/main/javascript/event_statistics.js",
        system_ids: __dirname + "/src/main/javascript/system_ids.js",
        welcome: __dirname + "/src/main/javascript/welcome.js",
        aggregate: __dirname + "/src/main/javascript/aggregate.js",
        trends: __dirname + "/src/main/javascript/trends.js",
        severities: __dirname + "/src/main/javascript/severities.js",
        create_event: __dirname + "/src/main/javascript/create_event.js",
        update_event: __dirname + "/src/main/javascript/update_event.js",
        event_definition: __dirname + "/src/main/javascript/event_definition.js",
        flight_display: __dirname + "/src/main/javascript/flight_display.js",
        time_zones: __dirname + "/src/main/javascript/time_zones.js",
        user_preferences : __dirname + "/src/main/javascript/preferences_page.js",
        // ngafid_cesium: __dirname + "/src/main/javascript/ngafid_cesium.js",

        ttf: __dirname + "/src/main/javascript/ttf.js"
    },

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
        })

        /*
        new HtmlWebPackPlugin({
            template: "./src/main/resources/public/js3/src/index.html",
            filename: "./index.html"
        })
        */
    ]
};
