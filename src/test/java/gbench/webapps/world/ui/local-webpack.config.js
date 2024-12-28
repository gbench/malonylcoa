const WORLD_SERVER_HOST= "http://localhost:8010";
const path = require("path");
const webpack = require("webpack");
const ESLintPlugin = require("eslint-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");

/**
 * 
 */
module.exports = {
	entry: path.resolve(__dirname, "src/index.js"),
	output: {
		path: path.resolve(__dirname, "public"),
		filename: "bundle.js"
	},

	target: ['web', 'es5'],
	devtool: "source-map",
	mode: "development",

	// SNOWWHITE-开发服务器的设置
	devServer: {
		hot: true,
		proxy: {
			'/h5/api/': { // 转换 h5:world的服务端口是8010
				target: WORLD_SERVER_HOST,
				secure: true,
				logLevel: 'debug',
				changeOrigin: true, // 必须加入否则会导致webpack奔溃
				pathRewrite: { '^/h5/api': '/world/api' }, // 转换接口标记
			}, // world服务器
		},// proxy
	},// devServer

	/**
	 * 模塊增加 处理的文件的类型（输入资料的种类）
	 */
	module: {
		rules: [{
			test: /\.js$/,
			exclude: /node_modules/,
			use: ["babel-loader", "eslint-loader"]
		}],
		rules: [{
			test: /\.css$/,
			exclude: /node_modules/,
			use: ["style-loader", "css-loader"]
		}]
	},

	/**
	 * 
	 */
	plugins: [
		new webpack.DefinePlugin({
			__VUE_OPTIONS_API__: true,
			__VUE_PROD_DEVTOOLS__: false
		}),
		new ESLintPlugin(),
		new HtmlWebpackPlugin({
			scriptLoading: "blocking",
			favicon: path.resolve(__dirname, "src/images/favicon.png"),
			template: path.resolve(__dirname, "src/html/index.html")
		})
	],

	/**
	 * 模块名别名的创建与处理
	 */
	resolve: {
		alias: { // 别名
			vue$: "vue/dist/vue.esm-bundler.js", // 启动runtime 编译
		},
	},

}
