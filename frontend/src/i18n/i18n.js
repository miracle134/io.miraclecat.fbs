/**

 * packageName    : static.js.i18n
 * fileName       : i18n.js
 * author         : MiracleCat
 * date           : 2025-07-08
 * description    :
 */
// Utils
import storageUtil from "@core/storage-util.js";
import ko from './message/ko';
import en from './message/en';

const resources = {ko, en};
let currentLang = getInitialLanguage();
const observers = [];

function getInitialLanguage() {
	return storageUtil.getLocal('language') || 'ko';
}

export function setLanguage(lang) {
	if (resources[lang]) {
		currentLang = lang;
		storageUtil.setLocal('language', lang);
		// location.reload();
		observers.forEach(fn => fn(currentLang));
		updateI18nText();
	}
}

export function getLanguage() {
	return currentLang;
}

export function getMessage(key) {
	const result = key.split('.').reduce((obj, part) => obj?.[part], resources[currentLang]);
	if (!result) {
		console.warn(`[i18n] Missing translation for key: "${key}"`);
		return '';
	}
	return result;
}

export function watchLanguageChange(fn) {
	observers.push(fn);
}

export function bindI18nAutoUpdate({ observeDynamic = false } = {}) {
	watchLanguageChange(() => updateI18nText()); // 언어 변경 시 자동 갱신
	updateI18nText(); // 최초 1회 실행
	if (observeDynamic) enableI18nAutoObserve();
}

function enableI18nAutoObserve() {
	if (enableI18nAutoObserve._enabled) return;
	const mo = new MutationObserver(muts => {
		for (const m of muts) {
			m.addedNodes.forEach(node => {
				if (node.nodeType === 1) applyI18n(node); // ELEMENT_NODE
			});
		}
	});
	mo.observe(document.body, {childList: true, subtree: true});
	enableI18nAutoObserve._enabled = true;
}

/** 엘리먼트 단위 번역 */
function translateElement(el) {
	// TEMPLATE 자신은 제외 (content에서 처리)
	if (el.tagName === 'TEMPLATE') return;
	
	const key = el.dataset.i18n;
	if (!key) return;
	
	const value = getMessage(key);
	if (!value) return;
	
	const target = (el.dataset.i18nTarget || 'text').toLowerCase();
	switch (target) {
		case 'html':
			el.innerHTML = value;
			break;
		case 'text':
		case 'div':
		case 'label':
		case 'button':
			el.textContent = value;
			break;
		case 'placeholder':
		case 'title':
		case 'alt':
		case 'aria-label':
			el.setAttribute(target, value);
			break;
		default:
			// 알 수 없는 타깃은 속성으로 간주
			el.setAttribute(target, value);
	}
	
	// data-i18n-attrs="placeholder,title" 같이 여러 속성 일괄 적용
	const multi = el.dataset.i18nAttrs;
	if (multi) {
		multi.split(',').map(s => s.trim()).forEach(attr => {
			if (attr) el.setAttribute(attr, value);
		});
	}
}

/** 루트 기준 재귀 번역 (template.content 포함) */
function applyI18n(root = document) {
	// 1) <template> 내부 content 먼저 처리 (재귀)
	root.querySelectorAll?.('template').forEach(tpl => {
		applyI18n(tpl.content);
	});
	
	// 2) 일반 엘리먼트 처리
	root.querySelectorAll?.('[data-i18n]').forEach(translateElement);
}

/** 외부 호출 함수 (기존 이름 유지) */
export function updateI18nText(root = document) {
	applyI18n(root);
}