/**
 *
 * packageName    : static.js.core
 * fileName       : domUtil.js
 * author         : MiracleCat
 * date           : 2025-07-04
 * description    : Element 직접 전달 지원 리팩토링
 */

/**
 * 공통 헬퍼: 셀렉터 또는 Element를 Element로 변환
 */
const getElement = (selectorOrElement) => {
    return typeof selectorOrElement === 'string'
        ? document.querySelector(selectorOrElement)
        : selectorOrElement;
};

/**
 * 공통 헬퍼: 셀렉터 또는 Element를 NodeList로 변환
 */
const getElements = (selectorOrElements) => {
    if (typeof selectorOrElements === 'string') {
        return document.querySelectorAll(selectorOrElements);
    }
    // NodeList 또는 Array인 경우
    if (selectorOrElements instanceof NodeList || Array.isArray(selectorOrElements)) {
        return selectorOrElements;
    }
    // 단일 Element인 경우 배열로 감싸기
    return [selectorOrElements];
};

/**
 * 단일 요소 선택
 */
export const find = (selector) => document.querySelector(selector);

/**
 * 여러 요소 선택
 */
export const findAll = (selector) => document.querySelectorAll(selector);

/**
 * 여러 요소 선택 (인덱스)
 */
export const findAllSelect = (selector, index = 0) => document.querySelectorAll(selector)[index];

/**
 * 입력 요소의 value 속성 가져오기
 */
export function getValue(selectorOrElement, shouldTrim = true) {
    const el = getElement(selectorOrElement);
    if (!el) return null;
    return shouldTrim ? el.value.trim() : el.value;
}

/**
 * 요소의 텍스트 내용 가져오기
 */
export function getText(selectorOrElement, shouldTrim = true) {
    const el = getElement(selectorOrElement);
    if (!el) return null;
    return shouldTrim ? el.textContent.trim() : el.textContent;
}

/**
 * 입력 요소의 value 속성 설정
 */
export function setValue(selectorOrElement, value) {
    const el = getElement(selectorOrElement);
    if (el) {
        el.value = value;
    }
}

/**
 * 요소의 textContent 설정
 */
export function setText(selectorOrElement, text) {
    const el = getElement(selectorOrElement);
    if (el) {
        el.textContent = text;
    }
}

/**
 * 체크박스 체크 여부 반환
 */
export function getChecked(selectorOrElement) {
    const el = getElement(selectorOrElement);
    if (!el) return null;
    return el.checked;
}

/**
 * 체크박스 상태 및 값 반환
 */
export function getCheckbox(selectorOrElement, returnValue = false) {
    const el = getElement(selectorOrElement);
    if (!el) return null;
    return returnValue ? el.value : el.checked;
}

/**
 * 체크 여부 설정
 */
export function setChecked(selectorOrElement, checked) {
    const el = getElement(selectorOrElement);
    if (!el) return false;
    el.checked = !!checked;
    return true;
}

/**
 * 전체 체크 여부 설정
 */
export function setCheckedAll(selectorOrElements, checked) {
    const elements = getElements(selectorOrElements);
    if (!elements || elements.length === 0) return false;
    elements.forEach(e => e.checked = !!checked);
    return true;
}

/**
 * 라디오 버튼 체크 여부 반환
 */
export function getRadioChecked(selectorOrElement) {
    const el = getElement(selectorOrElement);
    if (!el) return null;
    return el.checked;
}

/**
 * 라디오 버튼 선택된 값 반환
 * @param {string} name - 라디오 버튼 name 속성
 * @returns {string|null} 선택된 라디오의 value 또는 null
 */
export function getRadioValue(name) {
    const checked = document.querySelector(`input[name="${name}"]:checked`);
    return checked ? checked.value : null;
}

/**
 * 라디오 버튼 체크 설정 (value로)
 * @param {string} name - 라디오 버튼 name 속성
 * @param {string} value - 체크할 라디오의 value
 */
export function setRadioValue(name, value) {
    const radio = document.querySelector(`input[name="${name}"][value="${value}"]`);
    if (radio) {
        radio.checked = true;
        return true;
    }
    return false;
}

/**
 * 라디오 버튼 체크 설정 (직접)
 */
export function setRadioChecked(selectorOrElement, checked) {
    const el = getElement(selectorOrElement);
    if (!el) return false;
    el.checked = !!checked;
    return true;
}

/**
 * children 단일 요소 선택
 */
export const nodeFind = (node, selector) => node.querySelector(selector);

/**
 * children 여러 요소 선택
 */
export const nodeFindAll = (node, selector) => node.querySelectorAll(selector);

/**
 * children 여러 요소 중 선택 (인덱스)
 */
export const nodeFindAllSelect = (node, selector, index = 0) => node.querySelectorAll(selector)[index];

/**
 * children 안의 value 속성 설정
 */
export function setNodeValue(node, selector, value) {
    const el = node.querySelector(selector);
    if (el) {
        el.value = value;
    }
}

/**
 * children 안의 textContent 설정
 */
export function setNodeText(node, selector, text) {
    const el = node.querySelector(selector);
    if (el) {
        el.textContent = text;
    }
}

/**
 * children 안의 html 설정
 */
export function setNodeHtml(node, selector, html) {
    const el = node.querySelector(selector);
    if (el) {
        el.innerHTML = html;
    }
}

/**
 * children 안의 dataset 설정
 */
export function setNodeDataset(node, selector, key, value) {
    const el = node.querySelector(selector);
    if (el) {
        el.dataset[key] = value;
    }
}

/**
 * children 안의 dataset 설정
 */
export function setNodeChecked(node, selector, checked) {
    const el = node.querySelector(selector);
    if (!el) return false;
    el.checked = !!checked;
    return true;
}

/**
 * children 요소 밖, 맨 앞
 */
export function setNodeBeforePrependHtml(node, selector, html) {
    if (!html) return;
    node.querySelector(selector).insertAdjacentHTML('beforebegin', html);
}

/**
 * children 요소 안, 맨 앞
 */
export function setNodePrependHtml(node, selector, html) {
    if (!html) return;
    node.querySelector(selector).insertAdjacentHTML('afterbegin', html);
}

/**
 * children 요소 안, 맨 뒤
 */
export function setNodeAppendHtml(node, selector, html) {
    if (!html) return;
    node.querySelector(selector).insertAdjacentHTML('beforeend', html);
}

/**
 * children 요소 밖, 맨 뒤
 */
export function setNodeAfterAppendHtml(node, selector, html) {
    if (!html) return;
    node.querySelector(selector).insertAdjacentHTML('afterend', html);
}

/**
 * children 내부 요소 비우기
 */
export function setNodeEmpty(node, selector) {
    node.querySelector(selector).innerHTML = "";
}

/**
 * children 요소 안, 맨 뒤
 */
export function setNodeAppend(node, selector, el) {
    if (!el) return;
    node.querySelector(selector).appendChild(el);
}

/**
 * children 요소 안, 맨 앞
 */
export function setNodePrepend(node, selector, el) {
    if (!el) return;
    node.querySelector(selector).prepend(el);
}

/**
 * 이벤트 등록
 */
export function on(selectorOrElement, event, handler) {
    const el = getElement(selectorOrElement);
    if (el) {
        el.addEventListener(event, handler);
    }
}

/**
 * 이벤트 다중 등록
 */
export function onMultiple(selectorOrElements, event, handler) {
    const elements = getElements(selectorOrElements);
    elements.forEach(el => {
        el.addEventListener(event, handler);
    });
}

/**
 * 이벤트 제거
 */
export function off(selectorOrElement, event, handler) {
    const el = getElement(selectorOrElement);
    if (el) {
        el.removeEventListener(event, handler);
    }
}

/**
 * template 이벤트 등록
 */
export function nodeOn(node, selector, event, handler) {
    const el = node.querySelector(selector);
    if (el) {
        el.addEventListener(event, handler);
    }
}

/**
 * 이벤트 위임(delegate)
 */
export function delegate(parentSelectorOrElement, eventType, childSelector, handler) {
    const parent = getElement(parentSelectorOrElement);
    if (!parent) return;

    parent.addEventListener(eventType, (e) => {
        const target = e.target.closest(childSelector);
        if (target) {
            handler.call(target, e);
        }
    });
}

/**
 * 다중 이벤트 위임(delegate)
 */
export function multipleDelegate(parentSelectorOrElements, eventType, childSelector, handler) {
    const parents = getElements(parentSelectorOrElements);
    if (!parents) return;

    parents.forEach(el => {
        el.addEventListener(eventType, (e) => {
            const target = e.target.closest(childSelector);
            if (target) {
                handler.call(target, e);
            }
        });
    });
}

/**
 * 클래스 추가
 */
export function addClass(selectorOrElement, className) {
    const el = getElement(selectorOrElement);
    if (el) {
        el.classList.add(className);
    }
}

/**
 * 클래스 제거
 */
export function removeClass(selectorOrElement, className) {
    const el = getElement(selectorOrElement);
    if (el) {
        el.classList.remove(className);
    }
}

/**
 * 클래스 토글
 */
export function toggleClass(selectorOrElement, className) {
    const el = getElement(selectorOrElement);
    if (el) {
        el.classList.toggle(className);
    }
}

/**
 * 클래스 유무
 */
export function containsClass(selectorOrElement, className) {
    const el = getElement(selectorOrElement);
    if (el) {
        return el.classList.contains(className);
    }
    return false;
}

/**
 * 요소의 표시/숨김 전환
 * @param {string|Element} selectorOrElement - CSS 선택자 또는 요소
 * @param {boolean} show - true면 표시, false면 숨김
 */
export function toggle(selectorOrElement, show) {
    const el = getElement(selectorOrElement);
    if (el) {
        el.style.display = show ? "" : "none";
    }
}

/**
 * dataset 값 읽기
 */
export function getDataset(selectorOrElement, key) {
    const el = getElement(selectorOrElement);
    if (el) {
        return el.dataset[key];
    }
    return null;
}

/**
 * dataset 값 쓰기
 */
export function setDataset(selectorOrElement, key, value) {
    const el = getElement(selectorOrElement);
    if (el) {
        el.dataset[key] = value;
    }
}

/**
 * 반복문
 */
export function forEach(selectorOrElements, callback) {
    const elements = getElements(selectorOrElements);
    elements.forEach(callback);
}

/**
 * 선택자에 해당하는 요소가 존재하는지 확인
 * @param {string} selectorOrElements - CSS 선택자
 * @returns {boolean} 요소 존재 여부
 */
export function exists(selectorOrElements) {
    const elements = getElements(selectorOrElements);
    return elements.length > 0;  // 길이로 체크
}

/**
 * 자식 요소로 html 추가
 */
export function html(selectorOrElement, html) {
    if (!html) return;
    const el = getElement(selectorOrElement);
    if (el) {
        el.innerHTML = html;
    }
}

/**
 * 요소 밖, 맨 앞
 */
export function beforePrepend(selectorOrElement, html) {
    if (!html) return;
    const el = getElement(selectorOrElement);
    if (el) {
        el.insertAdjacentHTML('beforebegin', html);
    }
}

/**
 * 요소 안, 맨 앞
 */
export function prepend(selectorOrElement, html) {
    if (!html) return;
    const el = getElement(selectorOrElement);
    if (el) {
        el.insertAdjacentHTML('afterbegin', html);
    }
}

/**
 * 요소 안, 맨 뒤
 */
export function append(selectorOrElement, html) {
    if (!html) return;
    const el = getElement(selectorOrElement);
    if (el) {
        el.insertAdjacentHTML('beforeend', html);
    }
}

/**
 * 요소 밖, 맨 뒤
 */
export function afterAppend(selectorOrElement, html) {
    if (!html) return;
    const el = getElement(selectorOrElement);
    if (el) {
        el.insertAdjacentHTML('afterend', html);
    }
}

/**
 * 내부 요소 비우기
 */
export function empty(selectorOrElement) {
    const el = getElement(selectorOrElement);
    if (el) {
        el.innerHTML = "";
    }
}

/**
 * 전체 삭제
 */
export function remove(selectorOrElement) {
    const el = getElement(selectorOrElement);
    if (el) {
        el.remove();
    }
}