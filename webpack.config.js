const webpack = require('webpack');
const HtmlWebPackPlugin = require("html-webpack-plugin");

module.exports = {
    entry: {
        home_navbar: __dirname + "/src/main/javascript/home_navbar.js",
        create_account: __dirname + "/src/main/javascript/create_account.js",
        signed_in_navbar: __dirname + "/src/main/javascript/signed_in_navbar.js",
        manage_fleet: __dirname + "/src/main/javascript/manage_fleet.js",
        update_password: __dirname + "/src/main/javascript/update_password.js",
        update_profile: __dirname + "/src/main/javascript/update_profile.js"
        uploads: __dirname + "/src/main/javascript/uploads.js"
    },
    output: {
        path: __dirname + "/src/main/resources/public/js/",
        filename: "[name]-bundle.js"
    },
    module: {
        rules: [
            {
                test: /\.(js|jsx)$/,
                exclude: /node_modules/,
                use: {
                    loader: "babel-loader"
                }
            },
            {
                test: /\.html$/,
                use: [
                    {
                        loader: "html-loader"
                    }
                ]
            }
        ]
    },

    plugins: [
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery",
        })

        /*
        new HtmlWebPackPlugin({
            template: "./src/main/resources/public/js3/src/index.html",
            filename: "./index.html"
        })
        */
    ]
};
