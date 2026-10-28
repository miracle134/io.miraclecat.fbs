/**

 * packageName    :
 * fileName       : webpack.dev.js
 * author         : MiracleCat
 * date           : 2025-08-12
 * description    :
 */
// webpack.dev.js
const {merge} = require('webpack-merge');
const common = require('./webpack.common.js');
const path = require('path');

module.exports = merge(common, {
    mode: 'development',
    devtool: 'cheap-module-source-map',

    // dev에선 해시 제거: 캐시 꼬임/리로드 지연 줄이기
    output: {
        filename: '[name].js',
        chunkFilename: '[name].js',
        clean: false, // watch 중 불필요한 삭제 IO 방지
    },

    stats: {
        errorDetails: true,   // 에러 상세(원인/stack)
        moduleTrace: true,    // 어떤 모듈 체인으로 유입됐는지
        reasons: true,        // 모듈 포함 이유
        children: true,       // child compiler(html-webpack-plugin 등) 로그
        // 너무 시끄러우면 위 4개만 두고 나머지는 끄면 됨
        // preset: 'verbose',  // 극단적으로 자세히 보고 싶을 때(권장X)
    },

    optimization: {
        minimize: false, // Terser 비용 제거
        runtimeChunk: 'single',
        // 초기 로딩 난립 방지: dev는 async 위주로 쪼개서 HMR/리로드 부담↓
        splitChunks: {
            chunks: 'async',
        },
        moduleIds: 'named',
        chunkIds: 'named',
    },

    watchOptions: {
        ignored: /node_modules/,
        aggregateTimeout: 200,
        // Windows/네트워크 드라이브라면 아래 poll을 300~800으로 조절
        // poll: 500,
    },

    devServer: {
        port: 8081,
        hot: true,
        headers: {
            'Cache-Control': 'no-store',
            'Access-Control-Allow-Origin': 'http://localhost:8080',
            'Access-Control-Allow-Methods': 'GET,HEAD,OPTIONS',
            'Access-Control-Allow-Headers': 'Origin, X-Requested-With, Content-Type, Accept',
        },
        proxy: [
            {
                context: ['/api'], // 매칭할 요청 경로
                target: 'http://localhost:8080',
                changeOrigin: true, // 필요 시 CORS 우회
                secure: false       // HTTPS 인증서 무시 (로컬 개발용)
            }
        ],
    },
});
