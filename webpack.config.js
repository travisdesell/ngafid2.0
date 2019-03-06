const HtmlWebPackPlugin = require("html-webpack-plugin");

module.exports = {
    entry: {
        //index: __dirname + "/src/main/resources/public/js3/src/index.js"
        index: __dirname + "/src/index.js"
    },
    output: {
        path: __dirname + "/src/main/resources/public/js3/dist",
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
        new HtmlWebPackPlugin({
            template: "./src/main/resources/public/js3/src/index.html",
            filename: "./index.html"
        })
    ]
};
