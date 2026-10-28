/**
 * packageName    : static.js.core
 * fileName       : core
 * author         : MiracleCat
 * date           : 2025-07-03
 * description    : Core Script
 */

// 필수 유틸
import global from '@core/global.js';
import ajaxUtil from '@core/ajax-util.js';
import dialogUtil from '@core/dialog-util.js';
import * as dom from '@core/dom-util.js';
import * as message from '@i18n/i18n.js';

/// 초기화
message.bindI18nAutoUpdate();

// 필요할 때만 로딩할 유틸
const dynamicMap = {
	stringUtil: () => import(/* webpackChunkName: "string-util" */ '@core/string-util.js'),
	numberUtil: () => import(/* webpackChunkName: "number-util" */ '@core/number-util.js'),
	storageUtil: () => import(/* webpackChunkName: "storage-util" */ '@core/storage-util.js'),
	regexUtil: () => import(/* webpackChunkName: "regex-util" */ '@core/regex-util.js'),
	dateUtil: () => import(/* webpackChunkName: "date-util" */ '@core/date-util.js'),
	fileUtil: () => import(/* webpackChunkName: "file-util" */ '@core/file-util.js'),
	validUtil: () => import(/* webpackChunkName: "validation-util" */ '@core/validation-util.js'),
	pagingUtil: () => import(/* webpackChunkName: "paging-util" */ '@core/paging-util.js'),
};

// 유틸 캐시
const utilCache = {};

async function loadUtil(name) {
	if (utilCache[name]) return utilCache[name];
	const loader = dynamicMap[name];
	if (!loader) throw new Error(`Unknown util: ${name}`);
	const promise = loader().then(mod => mod.default || mod);
	utilCache[name] = promise;
	return promise;
}

// 유틸 로드
export async function loadUtils(names = []) {
	const modules = await Promise.all(names.map(name => loadUtil(name)));
	return Object.fromEntries(names.map((name, i) => [name, modules[i]]));
}

// 필요할 때만 로딩할 API (API 추가시 추가)
const apiMap = {
    adminApi: () => import(/* webpackChunkName: "admin-api" */ '../domain/admin-api.js'),
    bookingApi: () => import(/* webpackChunkName: "booking-api" */ '../domain/booking-api.js'),
    codeApi: () => import(/* webpackChunkName: "code-api" */ '../domain/code-api.js'),
    flightApi: () => import(/* webpackChunkName: "flight-api" */ '../domain/flight-api.js'),
    memberApi: () => import(/* webpackChunkName: "member-api" */ '../domain/member-api.js'),
    noticeApi: () => import(/* webpackChunkName: "notice-api" */ '../domain/notice-api.js'),
    paymentApi: () => import(/* webpackChunkName: "payment-api" */ '../domain/payment-api.js'),
};

// API 캐시
const apiCache = {};

async function loadApi(name) {
	if (apiCache[name]) return apiCache[name];
	const loader = apiMap[name];
	if (!loader) throw new Error(`Unknown API: ${name}`);
	const promise = loader().then(mod => mod.default || mod);
	apiCache[name] = promise;
	return promise;
}

// API 로드
export async function loadApis(names = []) {
	const modules = await Promise.all(names.map(name => loadApi(name)));
	return Object.fromEntries(names.map((name, i) => [name, modules[i]]));
}

export {
	global,
	ajaxUtil as ajax,
	dialogUtil as dialog,
	dom,
	message
};