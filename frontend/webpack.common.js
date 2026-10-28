/**
 
 * packageName    :
 * fileName       : webpack.common.js
 * author         : MiracleCat 
 * date           : 2025-08-12
 * description    :
 */
// webpack.common.js
const glob = require('glob');
const path = require('path');
const {DefinePlugin} = require('webpack');
const {WebpackManifestPlugin} = require('webpack-manifest-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');

function getEntries() {
	const files = glob.sync('./src/page/**/*.js');
	const entries = {};
	files.forEach(file => {
		const relativePath = path.relative(path.resolve('src/page'), path.resolve(file)).replace(/\\/g, '/');
		const name = relativePath.replace(/\.js$/, '');
		entries[name] = path.resolve(file);
	});
	return entries;
}

const cssFilesToCopy = [
	{
		from: path.resolve(__dirname, 'node_modules/flatpickr/dist/flatpickr.min.css'),
		to: path.resolve(__dirname, '../src/main/resources/static/css/flatpickr.min.css')
	},
	{
		from: path.resolve(__dirname, 'node_modules/bootstrap/dist/css/bootstrap.min.css'),
		to: path.resolve(__dirname, '../src/main/resources/static/css/bootstrap.min.css')
	},
	{
		from: path.resolve(__dirname, 'node_modules/bootstrap-icons/font/bootstrap-icons.min.css'),
		to: path.resolve(__dirname, '../src/main/resources/static/css/bootstrap-icons.min.css')
	},
];

module.exports = {
	entry: getEntries(),
	output: {
		path: path.resolve(__dirname, '../src/main/resources/static/js'),
		publicPath: '/js/',
		module: true,
	},
	experiments: {
		outputModule: true,
	},
	resolve: {
		alias: {
			'@core': path.resolve(__dirname, 'src/core'),
			'@i18n': path.resolve(__dirname, 'src/i18n'),
			'@feature': path.resolve(__dirname, 'src/feature'),
		},
		extensions: ['.js'],
	},
	module: {
		rules: [
			// 필요 시 로더 추가 (babel, css 등)
		],
	},
	plugins: [
		// 서버 템플릿/전역에서 사용할 수 있는 빌드 스탬프
		new DefinePlugin({
			__BUILD_STAMP__: JSON.stringify(Date.now().toString()),
		}),
		
		// manifest.json 생성 (dev/prod 공용)
		new WebpackManifestPlugin({
			fileName: 'manifest.json',
			publicPath: '/js/',
			generate: (seed, files, entries) => {
				const fileMap = files.reduce((acc, f) => {
					acc[f.name] = f.path;   // publicPath 반영됨
					return acc;
				}, {});
				const entrypoints = {};
				Object.keys(entries).forEach((entryName) => {
					entrypoints[entryName] = entries[entryName]
						.filter(x => x.endsWith('.js'))
						.map(x => fileMap[x] || `/js/${x}`);
				});
				return {files: fileMap, entrypoints};
			},
		}),
		
		new CopyWebpackPlugin({
			patterns: cssFilesToCopy.map(file => ({
				...file,
				noErrorOnMissing: true,
				force: true
			}))
		}),
	
	],
	cache: {
		type: 'filesystem',
		buildDependencies: {config: [__filename]},
	},
	performance: {hints: false},
	infrastructureLogging: {level: 'warn'},
	stats: 'summary',
};
