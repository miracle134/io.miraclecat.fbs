/**
 
 * packageName    :
 * fileName       : webpack.prod.js
 * author         : MiracleCat 
 * date           : 2025-08-12
 * description    :
 */
// webpack.prod.js
const {merge} = require('webpack-merge');
const common = require('./webpack.common.js');
const {CleanWebpackPlugin} = require('clean-webpack-plugin');

module.exports = merge(common, {
	mode: 'production',
	devtool: false,
	
	output: {
		filename: '[name].[contenthash].js',
		chunkFilename: '[name].[contenthash].js',
		clean: true,
	},

    stats: {
        errorDetails: true,   // 에러 상세(원인/stack)
        moduleTrace: true,    // 어떤 모듈 체인으로 유입됐는지
        reasons: true,        // 모듈 포함 이유
        children: true,       // child compiler(html-webpack-plugin 등) 로그
        // preset: 'verbose',  // 극단적으로 자세히 보고 싶을 때(권장X)
    },

	plugins: [
		// 배포 시 산출물 폴더 깨끗하게
		new CleanWebpackPlugin({
			cleanOnceBeforeBuildPatterns: ['**/*'],
		}),
	],
	
	optimization: {
		minimize: true,
		runtimeChunk: 'single',
		// prod는 공통 청크를 적극 분리: 캐시 효율↑, 초기 부하 균형
		splitChunks: {
			chunks: 'all',
			cacheGroups: {
				vendor: {
					test: /[\\/]node_modules[\\/]/,
					name: 'vendor',
					priority: 10,
					enforce: true,
				},
				core: {
					test: /[\\/]src[\\/]core[\\/]core\.js$/,
					name: 'core',
					priority: 20,
					enforce: true,
				},
				global: {
					test: /[\\/]src[\\/]core[\\/]global\.js$/,
					name: 'global',
					priority: 20,
					enforce: true,
				},
				'ajax-util': {
					test: /[\\/]src[\\/]core[\\/]ajax-util\.js$/,
					name: 'ajax-util',
					priority: 20,
					enforce: true,
				},
				'dialog-util': {
					test: /[\\/]src[\\/]core[\\/]dialog-util\.js$/,
					name: 'dialog-util',
					priority: 20,
					enforce: true,
				},
				'dom-util': {
					test: /[\\/]src[\\/]core[\\/]dom-util\.js$/,
					name: 'dom-util',
					priority: 20,
					enforce: true,
				},
				i18n: {
					test: /[\\/]src[\\/]i18n[\\/]i18n\.js$/,
					name: 'i18n',
					priority: 20,
					enforce: true,
				},
			},
		},
		moduleIds: 'deterministic',
		chunkIds: 'deterministic',
	},
});
