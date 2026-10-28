/**
 
 * packageName    : static.js.core
 * fileName       : global
 * author         : MiracleCat 
 * date           : 2025-07-03
 * description    : 전역 변수 관리 및 전역 메서드
 */

/**
 * Global 클래스
 * - 전역 변수 관리 및 전역 유틸리티 메서드를 제공하는 클래스입니다.
 *
 * 주요 메서드:
 * - `setItem(key, value)` : 주어진 키와 값을 전역 변수로 저장합니다.
 * - `getItem(key)` : 주어진 키에 해당하는 값을 반환합니다.
 * - `enterInstance(id, func)` : 특정 입력 필드에서 "Enter" 키 입력 시 주어진 함수를 실행합니다.
 * - `selectInstance(id, func)` : 테이블 행 클릭 시 선택 상태로 설정하고 주어진 함수를 실행합니다.
 * - `genPaging(curPageNum, totalPageNum, pageSize, blockSize, func)` : 페이지 생성
 * - `isHandledError(error)` : 공통 에러 처리.
 *
 */
import 'bootstrap';

let lastSelectedRow = null; // 마지막 선택된 행
class Global {
	constructor() {
	}
	
	/**
	 * 변수 등록
	 * @param key
	 * @param value
	 */
	setItem(key, value) {
		globalVal[key] = value;
	}
	
	/**
	 * 변수 조회
	 * @returns key
	 */
	getItem(key) {
		return globalVal[key];
	}

	/**
	 * 특정 입력 필드에서 "Enter" 키 입력 시 함수 실행
	 * @param {string} id - 입력 필드의 DOM id
	 * @param {function} func - 엔터 키 입력 시 실행할 함수
	 * @param {boolean} isEmpty - 빈값 function 실행 여부
	 * @param {boolean} isBlur - 엔터시 포커스 해제 여부
	 */
	enterInstance(id, func, isEmpty = false, isBlur = false) {
		const inputElement = document.querySelector(id);
		
		// 입력 필드가 존재하는지 확인
		if (!inputElement) {
			console.warn(`Element with id ${id} not found`);
			return;
		}
		
		// "keydown" 이벤트 리스너 추가
		inputElement.addEventListener("keydown", event => {
			// 엔터 키인지 확인
			if (event.key === "Enter") {
				const query = inputElement.value.trim();
				
				if (isEmpty || query.length > 0) {
					if (isBlur) inputElement.blur(); // 포커스 해제
					func(); // 함수 실행
					event.preventDefault(); // 중복 입력 방지
				}
			}
		});
	}
	
	/**
	 * 테이블에서 행을 클릭하면 선택 상태로 설정
	 * @param {string} id - 테이블 요소의 DOM <table> id
	 * @param {function} func - 선택된 행에 대해 실행할 함수
	 */
	selectInstance(id, func = null) {
		// 해당 테이블의 tbody 선택
		const tableBody = document.getElementById(id);
		
		// 테이블이 존재하는지 확인
		if (!tableBody) {
			console.warn(`Element with id ${id} not found`);
			return;
		}
		
		// 클릭 이벤트를 테이블의 tbody에 위임
		tableBody.addEventListener("click", function (event) {
			// 클릭된 요소가 tr인지 확인
			const clickedRow = event.target.closest("tr");
			if (clickedRow) {
				// // 모든 행에서 "select" 클래스 제거
				// tableBody.querySelectorAll("tr").forEach(row => row.classList.remove("select"));
				// // 클릭된 행에 "select" 클래스 추가
				// clickedRow.classList.add("select");
				// if (func) func(clickedRow);
				
				if (lastSelectedRow && lastSelectedRow !== clickedRow) {
					lastSelectedRow.classList.remove("select"); // 선택 class 제거
				}
				clickedRow.classList.add("select"); // 선택 class 추가
				lastSelectedRow = clickedRow; // 마지막 선택 저장
				if (func) func(clickedRow); // 선택된 행에 대해 함수 실행
			}
		});
	}
	
	/**
	 * 안전 경로 조회: "a.b[0].c" / "a.b.c" 모두 지원. 함수도 허용.
	 * @param {object} obj
	 * @param {string|function} keyOrFn - 경로 문자열 또는 (item)=>any 함수
	 * @returns {*}
	 */
	getByPath(obj, keyOrFn) {
		if (typeof keyOrFn === 'function') return keyOrFn(obj);
		if (typeof keyOrFn !== 'string' || !keyOrFn) return undefined;
		// a.b[0].c -> a.b.0.c 로 통일
		const parts = keyOrFn
			.replace(/\[(\w+)\]/g, '.$1')
			.replace(/^\./, '')
			.split('.');
		let cur = obj;
		for (const p of parts) {
			if (cur == null) return undefined;
			cur = cur[p];
		}
		return cur;
	}
	
	/**
	 * 셀렉트 옵션 채우기 (공통)
	 *
	 * @param {HTMLSelectElement|string} select - <select> 요소 또는 CSS 선택자
	 * @param {Array} items - 리스트(객체 배열, [value,label] 튜플 배열, 원시값 배열 모두 가능)
	 * @param {Object} cfg
	 * @param {string} [cfg.valueKey] - 객체 배열일 때 value에 사용할 키
	 * @param {string} [cfg.labelKey] - 객체 배열일 때 label에 사용할 키
	 * @param {string|number|null} [cfg.selected] - 선택할 값
	 * @param {string|null} [cfg.placeholder] - 맨 위에 비활성 placeholder 텍스트 (e.g. "선택하세요")
	 * @param {boolean} [cfg.clear=true] - 기존 옵션 제거 여부
	 * @returns {HTMLSelectElement}
	 *
	 * ex)
	 * 1) 객체 배열
	 * const list1 = [
	 *   { code: 'A', name: '옵션 A' },
	 *   { code: 'B', name: '옵션 B' },
	 * ];
	 * populateSelect('#mySelect', list1, {
	 *   valueKey: 'code',
	 *   labelKey: 'name',
	 *   selected: 'B',
	 *   placeholder: '선택하세요',
	 * });
	 *
	 * 2) 튜플 배열
	 * const list2 = [
	 *   ['10', '열 번째'],
	 *   ['20', '스무 번째'],
	 * ];
	 * populateSelect('#mySelect2', list2, { selected: '20' });
	 *
	 * 3) 원시값 배열
	 * const list3 = [1, 2, 3];
	 * populateSelect('#mySelect3', list3, { placeholder: '숫자 선택' });
	 */
	populateSelect(select, items, {
		valueKey,
		labelKey,
		selected = null,
		placeholder = null,
		clear = true
	} = {}) {
		const el = typeof select === 'string' ? document.querySelector(select) : select;
		if (!el) throw new Error('select 요소를 찾을 수 없습니다.');
		if (!Array.isArray(items)) items = [];
		
		if (clear) el.replaceChildren(); // 기존 옵션 제거
		
		if (placeholder) {
			const opt = document.createElement('option');
			opt.value = '';
			opt.textContent = placeholder;
			opt.selected = selected == null; // 선택값 없을 때 placeholder 선택
			el.appendChild(opt);
		}
		
		const frag = document.createDocumentFragment();
		
		for (const item of items) {
			let value, label;
		    if (item && typeof item === 'object' && !Array.isArray(item)) {
			    // 객체 배열: 경로/함수 우선, 없으면 흔한 키 후보 사용
			    value = this.getByPath(item, valueKey);
			    if (value === undefined) value = item.value ?? item.id ?? item.key;
			    
			    label = this.getByPath(item, labelKey);
			    if (label === undefined) label = item.label ?? item.name ?? item.title ?? String(value);
		    } else if (Array.isArray(item)) {
			    // [value, label]
			    [value, label] = item;
		    } else {
			    // 원시값
			    value = item;
			    label = String(item);
		    }
			
			if (value == null) continue;
			
			const opt = document.createElement('option');
			opt.value = String(value);
			opt.textContent = label ?? String(value);
			if (selected != null && String(selected) === String(value)) {
				opt.selected = true;
			}
			frag.appendChild(opt);
		}
		
		el.appendChild(frag);
		return el;
	}
	
	/**
	 * 새 <select> 생성 + 옵션 채우기
	 * @param {Array} items
	 * @param {Object} cfg - populateSelect과 동일
	 * @returns {HTMLSelectElement}
	 *
	 * ex)
	 * 새 셀렉트 생성해서 특정 컨테이너에 붙이기
	 * const selectEl = createSelect(list1, { valueKey: 'code', labelKey: 'name' });
	 * document.querySelector('#selectContainer').appendChild(selectEl);
	 */
	createSelect(items, cfg = {}) {
		const select = document.createElement('select');
		return this.populateSelect(select, items, cfg);
	}

    /**
     * MultiPart 통신을 위한 Object to FormData 유틸     *
     * @param obj {object}
     * @returns {FormData}
     */
    objectToFormData(obj) {
        const formData = new FormData();
        Object.entries(obj).forEach(([key, value]) => {
            if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
                // 객체는 JSON 문자열로 변환
                formData.append(key, JSON.stringify(value));
            } else {
                formData.append(key, value);
            }
        });
        return formData;
    }

	/**
	 * 공통 Error 처리
	 * @param error
	 */
	isHandledError(error) {
		return error?.message?.includes("__ERROR__");
	}

    /**
     * 특정 메뉴의 권한 보유 여부 확인
     * @param {string} menu - 메뉴 키 (예: 'PARKING_MGMT')
     * @param {string} action - 권한 타입 (예: 'ACCESS', 'CREATE', 'UPDATE', 'DELETE', 'EXPORT')
     * @returns {boolean} 권한 보유 여부
     */
    hasPermission(menu, action) {
        return permissions[menu]?.includes(action) ?? false;
    }

    /**
     * 특정 메뉴의 여러 권한 중 하나라도 보유 여부 확인
     * @param {string} menu - 메뉴 키
     * @param {string[]} actions - 권한 타입 배열
     * @returns {boolean} 권한 중 하나라도 보유 시 true
     */
    hasAnyPermission(menu, actions) {
        return actions.some(action => permissions[menu]?.includes(action));
    }

    /**
     * 특정 메뉴의 모든 권한 보유 여부 확인
     * @param {string} menu - 메뉴 키
     * @param {string[]} actions - 권한 타입 배열
     * @returns {boolean} 모든 권한 보유 시 true
     */
    hasAllPermissions(menu, actions) {
        return actions.every(action => permissions[menu]?.includes(action));
    }

}

// 전역 변수
const globalVal = {}

const global = new Global();
export default global;