/**
 * packageName    : static.js.core
 * fileName       : number-util
 * author         : MiracleCat 
 * date           : 2025-07-03
 * description    : 숫자 처리
 */
import stringUtil from '@core/string-util.js'

/**
 * NumberUtil 클래스
 * - 숫자 조작 및 입력 유효성 검사에 대한 유틸리티 클래스.
 *
 * 주요 메서드:
 * - setInputNumber(selector, defaultValue): 입력 필드에서 음수와 0을 허용하는 숫자 입력 처리.
 * - setInputNumberWithZero(selector, defaultValue): 입력 필드에서 0을 허용하는 숫자 입력 처리.
 * - setVariableInputNumber(selector, subId, defaultValue): 가변 입력 필드에서 숫자 입력 처리.
 * - setThousandComma(number): 숫자를 천자리 콤마 형식으로 변환.
 * - isNumeric(value): 주어진 값이 숫자인지 여부를 체크.
 * - roundToDecimalPlaces(number, decimals): 주어진 숫자를 소수점 자리수로 반올림.
 * - clamp(number, min, max): 숫자를 특정 범위 내로 제한.
 * - randomInt(min, max): 주어진 범위 내에서 난수 생성.
 * - percentage(part, total): 부분과 전체에 대한 비율 계산.
 *
 */
class NumberUtil {
    constructor() {
    }

    /**
     * Input 숫자만 입력 가능 (일반 입력용)
     * @param {string} selector - 입력 필드의 선택자
     * @param {number} defaultValue - 기본값
     * @param {Object} options - 옵션 객체
     * @param {number} options.min - 최소값
     * @param {number} options.max - 최대값
     * @param {boolean} options.allowDecimal - 소수점 허용 여부 (기본: false)
     * @param {boolean} options.allowLeadingZero - 앞에 0 허용 여부 (기본: false)
     * @param {boolean} options.allowNegative - 음수 허용 여부 (기본: true)
     * @param {boolean} options.formatComma - 천자리 콤마 포맷 여부 (기본: false)
     * @param {number} options.maxLength - 최대 입력 길이
     */
    setInputNumber(selector, defaultValue = 0, options = {}) {
        const inputElement = document.querySelector(selector);
        if (!inputElement) return;

        const {
            min = null,
            max = null,
            allowDecimal = false,
            allowLeadingZero = false,
            allowNegative = true,
            formatComma = false,
            maxLength = null
        } = options;

        // 콤마 추가 함수
        const addComma = (value) => {
            const parts = value.split('.');
            parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
            return parts.join('.');
        };

        // 콤마 제거 함수
        const removeComma = (value) => {
            return value.replace(/,/g, '');
        };

        // 포커스 시 콤마 제거 (편집 모드)
        if (formatComma) {
            inputElement.addEventListener('focus', () => {
                const value = inputElement.value;
                if (value) {
                    inputElement.value = removeComma(value);
                }
            });
        }

        inputElement.addEventListener('input', () => {
            let inputValue = inputElement.value;

            if (stringUtil.isBlank(inputValue)) {
                inputElement.dataset.rawValue = '';
                return;
            }

            // 콤마 제거 (입력 처리용)
            inputValue = removeComma(inputValue);

            // 소수점, 음수 허용 여부에 따른 정규식
            let pattern;
            if (allowLeadingZero) {
                // 앞 0 허용 모드: 음수 불가, 소수점만 체크
                pattern = /[^0-9]/g;
            } else {
                // 일반 모드
                if (allowNegative && allowDecimal) {
                    pattern = /[^0-9.-]/g;
                } else if (allowNegative) {
                    pattern = /[^0-9-]/g;
                } else if (allowDecimal) {
                    pattern = /[^0-9.]/g;
                } else {
                    pattern = /[^0-9]/g;
                }
            }
            inputValue = inputValue.replace(pattern, '');

            // 음수 기호는 맨 앞에만 허용
            if (allowNegative && inputValue.indexOf('-') > 0) {
                inputValue = inputValue.replace(/-/g, '');
            }

            // 소수점은 하나만 허용
            if (allowDecimal) {
                const parts = inputValue.split('.');
                if (parts.length > 2) {
                    inputValue = parts[0] + '.' + parts.slice(1).join('');
                }
            }

            // 앞의 0 제거 (allowLeadingZero가 false일 때)
            if (!allowLeadingZero && /^-?0+[0-9]/.test(inputValue)) {
                inputValue = inputValue.replace(/^(-?)0+/, '$1');
            }

            // 최대 길이 체크
            if (maxLength !== null) {
                const numOnly = inputValue.replace(/[-.]/g, '');
                if (numOnly.length > maxLength) {
                    const extra = (inputValue.includes('-') ? 1 : 0) + (inputValue.includes('.') ? 1 : 0);
                    inputValue = inputValue.slice(0, maxLength + extra);
                }
            }

            // 범위 체크
            const numValue = allowLeadingZero ? parseInt(inputValue) : parseFloat(inputValue);
            if (!isNaN(numValue)) {
                if (min !== null && numValue < min) {
                    inputValue = min.toString();
                }
                if (max !== null && numValue > max) {
                    inputValue = max.toString();
                }
            }

            // 순수 숫자값 저장
            inputElement.dataset.rawValue = inputValue;
            inputElement.value = inputValue;
        });

        inputElement.addEventListener('blur', () => {
            let inputValue = inputElement.value;

            if (stringUtil.isBlank(inputValue) || inputValue === '-' || inputValue === '.') {
                inputElement.value = formatComma ? addComma(defaultValue.toString()) : defaultValue;
                inputElement.dataset.rawValue = defaultValue.toString();
                return;
            }

            // 블러 시 콤마 포맷 적용
            if (formatComma) {
                inputValue = removeComma(inputValue);
                inputElement.value = addComma(inputValue);
                inputElement.dataset.rawValue = inputValue;
            }
        });
    }

    /**
     * Input 숫자만 입력 가능 (다중 처리)
     * @param selector
     * @param defaultValue
     * @param options
     */
    setInputNumberAll(selector, defaultValue = 0, options = {}) {
        const inputElements = document.querySelectorAll(selector);
        if (inputElements.length === 0) return;

        inputElements.forEach(inputElement => {
            this.setupInputNumber(inputElement, defaultValue, options);
        });
    }

    /**
     * Input 숫자만 입력 가능 (이벤트 위임 방식)
     * @param defaultValue
     * @param options
     * @param parentSelector
     * @param childSelector
     */
    setInputNumberDelegate(parentSelector, childSelector, defaultValue = 0, options = {}) {
        const parentElement = typeof parentSelector === 'string'
            ? document.querySelector(parentSelector)
            : parentSelector;

        if (!parentElement) return;

        const {
            min = null,
            max = null,
            allowDecimal = false,
            allowLeadingZero = false,
            allowNegative = true,
            formatComma = false,
            maxLength = null
        } = options;

        const addComma = (value) => {
            const parts = value.split('.');
            parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
            return parts.join('.');
        };

        const removeComma = (value) => {
            return value.replace(/,/g, '');
        };

        const processInput = (inputValue) => {
            inputValue = removeComma(inputValue);

            let pattern;
            if (allowLeadingZero) {
                pattern = /[^0-9]/g;
            } else {
                if (allowNegative && allowDecimal) {
                    pattern = /[^0-9.-]/g;
                } else if (allowNegative) {
                    pattern = /[^0-9-]/g;
                } else if (allowDecimal) {
                    pattern = /[^0-9.]/g;
                } else {
                    pattern = /[^0-9]/g;
                }
            }
            inputValue = inputValue.replace(pattern, '');

            if (allowNegative && inputValue.indexOf('-') > 0) {
                inputValue = inputValue.replace(/-/g, '');
            }

            if (allowDecimal) {
                const parts = inputValue.split('.');
                if (parts.length > 2) {
                    inputValue = parts[0] + '.' + parts.slice(1).join('');
                }
            }

            if (!allowLeadingZero && /^-?0+[0-9]/.test(inputValue)) {
                inputValue = inputValue.replace(/^(-?)0+/, '$1');
            }

            if (maxLength !== null) {
                const numOnly = inputValue.replace(/[-.]/g, '');
                if (numOnly.length > maxLength) {
                    const extra = (inputValue.includes('-') ? 1 : 0) + (inputValue.includes('.') ? 1 : 0);
                    inputValue = inputValue.slice(0, maxLength + extra);
                }
            }

            const numValue = allowLeadingZero ? parseInt(inputValue) : parseFloat(inputValue);
            if (!isNaN(numValue)) {
                if (min !== null && numValue < min) {
                    inputValue = min.toString();
                }
                if (max !== null && numValue > max) {
                    inputValue = max.toString();
                }
            }

            return inputValue;
        };

        // 이벤트 위임
        parentElement.addEventListener('focus', (e) => {
            if (!e.target.matches(childSelector)) return;

            if (formatComma) {
                const value = e.target.value;
                if (value) {
                    e.target.value = removeComma(value);
                }
            }
        }, true);

        parentElement.addEventListener('input', (e) => {
            if (!e.target.matches(childSelector)) return;

            let inputValue = e.target.value;

            if (stringUtil.isBlank(inputValue)) {
                e.target.dataset.rawValue = '';
                return;
            }

            inputValue = processInput(inputValue);

            e.target.dataset.rawValue = inputValue;
            e.target.value = inputValue;
        });

        parentElement.addEventListener('blur', (e) => {
            if (!e.target.matches(childSelector)) return;

            let inputValue = e.target.value;

            if (stringUtil.isBlank(inputValue) || inputValue === '-' || inputValue === '.') {
                e.target.value = formatComma ? addComma(defaultValue.toString()) : defaultValue;
                e.target.dataset.rawValue = defaultValue.toString();
                return;
            }

            if (formatComma) {
                inputValue = removeComma(inputValue);
                e.target.value = addComma(inputValue);
                e.target.dataset.rawValue = inputValue;
            }
        }, true);
    }

    setupInputNumber(inputElement, defaultValue = 0, options = {}) {
        const {
            min = null,
            max = null,
            allowDecimal = false,
            allowLeadingZero = false,
            allowNegative = true,
            formatComma = false,
            maxLength = null
        } = options;

        const addComma = (value) => {
            const parts = value.split('.');
            parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
            return parts.join('.');
        };

        const removeComma = (value) => {
            return value.replace(/,/g, '');
        };

        if (formatComma) {
            inputElement.addEventListener('focus', () => {
                const value = inputElement.value;
                if (value) {
                    inputElement.value = removeComma(value);
                }
            });
        }

        inputElement.addEventListener('input', () => {
            let inputValue = inputElement.value;

            if (stringUtil.isBlank(inputValue)) {
                inputElement.dataset.rawValue = '';
                return;
            }

            inputValue = removeComma(inputValue);

            let pattern;
            if (allowLeadingZero) {
                pattern = /[^0-9]/g;
            } else {
                if (allowNegative && allowDecimal) {
                    pattern = /[^0-9.-]/g;
                } else if (allowNegative) {
                    pattern = /[^0-9-]/g;
                } else if (allowDecimal) {
                    pattern = /[^0-9.]/g;
                } else {
                    pattern = /[^0-9]/g;
                }
            }
            inputValue = inputValue.replace(pattern, '');

            if (allowNegative && inputValue.indexOf('-') > 0) {
                inputValue = inputValue.replace(/-/g, '');
            }

            if (allowDecimal) {
                const parts = inputValue.split('.');
                if (parts.length > 2) {
                    inputValue = parts[0] + '.' + parts.slice(1).join('');
                }
            }

            if (!allowLeadingZero && /^-?0+[0-9]/.test(inputValue)) {
                inputValue = inputValue.replace(/^(-?)0+/, '$1');
            }

            if (maxLength !== null) {
                const numOnly = inputValue.replace(/[-.]/g, '');
                if (numOnly.length > maxLength) {
                    const extra = (inputValue.includes('-') ? 1 : 0) + (inputValue.includes('.') ? 1 : 0);
                    inputValue = inputValue.slice(0, maxLength + extra);
                }
            }

            const numValue = allowLeadingZero ? parseInt(inputValue) : parseFloat(inputValue);
            if (!isNaN(numValue)) {
                if (min !== null && numValue < min) {
                    inputValue = min.toString();
                }
                if (max !== null && numValue > max) {
                    inputValue = max.toString();
                }
            }

            inputElement.dataset.rawValue = inputValue;
            inputElement.value = inputValue;
        });

        inputElement.addEventListener('blur', () => {
            let inputValue = inputElement.value;

            if (stringUtil.isBlank(inputValue) || inputValue === '-' || inputValue === '.') {
                inputElement.value = formatComma ? addComma(defaultValue.toString()) : defaultValue;
                inputElement.dataset.rawValue = defaultValue.toString();
                return;
            }

            if (formatComma) {
                inputValue = removeComma(inputValue);
                inputElement.value = addComma(inputValue);
                inputElement.dataset.rawValue = inputValue;
            }
        });
    }

    /**
     * 가변 Input 숫자만 입력 가능 (동적 요소용 - 이벤트 위임)
     *
     * 설명: 동적으로 생성되는 입력 필드(예: 테이블 행 추가)에서 사용
     * - selector: 부모 컨테이너 (이벤트 위임용)
     * - subId: 실제 입력받을 하위 요소 (동적으로 추가되는 input)
     *
     * 사용 예시:
     * <div id="table-container">  // selector
     *   <input id="row-1-input">  // subId
     *   <input id="row-2-input">  // 동적 추가
     * </div>
     *
     * @param {string} selector - 부모 컨테이너 선택자
     * @param {string} subId - 하위 입력 필드의 선택자
     * @param {number} defaultValue - 기본값
     * @param {Object} options - 옵션 객체
     * @param {number} options.min - 최소값
     * @param {number} options.max - 최대값
     * @param {boolean} options.allowDecimal - 소수점 허용 여부 (기본: false)
     * @param {boolean} options.allowLeadingZero - 앞에 0 허용 여부 (기본: false)
     * @param {boolean} options.allowNegative - 음수 허용 여부 (기본: true)
     * @param {boolean} options.formatComma - 천자리 콤마 포맷 여부 (기본: false)
     * @param {number} options.maxLength - 최대 입력 길이
     */
    setVariableInputNumber(selector, subId, defaultValue = 0, options = {}) {
        const inputElement = document.querySelector(selector);
        if (!inputElement) return;

        const {
            min = null,
            max = null,
            allowDecimal = false,
            allowLeadingZero = false,
            allowNegative = true,
            formatComma = false,
            maxLength = null
        } = options;

        // 콤마 추가 함수
        const addComma = (value) => {
            const parts = value.split('.');
            parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
            return parts.join('.');
        };

        // 콤마 제거 함수
        const removeComma = (value) => {
            return value.replace(/,/g, '');
        };

        // 포커스 시 콤마 제거 (편집 모드)
        if (formatComma) {
            inputElement.addEventListener('focus', (event) => {
                const subInputElement = document.querySelector(subId);
                if (subInputElement) {
                    const value = subInputElement.value;
                    if (value) {
                        subInputElement.value = removeComma(value);
                    }
                }
            }, true);
        }

        inputElement.addEventListener('input', (event) => {
            const subInputElement = document.querySelector(subId);
            if (!subInputElement) return;

            let inputValue = subInputElement.value;

            if (stringUtil.isBlank(inputValue)) {
                subInputElement.dataset.rawValue = '';
                return;
            }

            // 콤마 제거 (입력 처리용)
            inputValue = removeComma(inputValue);

            // 소수점, 음수 허용 여부에 따른 정규식
            let pattern;
            if (allowLeadingZero) {
                pattern = /[^0-9]/g;
            } else {
                if (allowNegative && allowDecimal) {
                    pattern = /[^0-9.-]/g;
                } else if (allowNegative) {
                    pattern = /[^0-9-]/g;
                } else if (allowDecimal) {
                    pattern = /[^0-9.]/g;
                } else {
                    pattern = /[^0-9]/g;
                }
            }
            inputValue = inputValue.replace(pattern, '');

            // 음수 기호는 맨 앞에만 허용
            if (allowNegative && inputValue.indexOf('-') > 0) {
                inputValue = inputValue.replace(/-/g, '');
            }

            // 소수점은 하나만 허용
            if (allowDecimal) {
                const parts = inputValue.split('.');
                if (parts.length > 2) {
                    inputValue = parts[0] + '.' + parts.slice(1).join('');
                }
            }

            // 앞의 0 제거 (allowLeadingZero가 false일 때)
            if (!allowLeadingZero && /^-?0+[0-9]/.test(inputValue)) {
                inputValue = inputValue.replace(/^(-?)0+/, '$1');
            }

            // 최대 길이 체크
            if (maxLength !== null) {
                const numOnly = inputValue.replace(/[-.]/g, '');
                if (numOnly.length > maxLength) {
                    const extra = (inputValue.includes('-') ? 1 : 0) + (inputValue.includes('.') ? 1 : 0);
                    inputValue = inputValue.slice(0, maxLength + extra);
                }
            }

            // 범위 체크
            const numValue = allowLeadingZero ? parseInt(inputValue) : parseFloat(inputValue);
            if (!isNaN(numValue)) {
                if (min !== null && numValue < min) {
                    inputValue = min.toString();
                }
                if (max !== null && numValue > max) {
                    inputValue = max.toString();
                }
            }

            // 순수 숫자값 저장
            subInputElement.dataset.rawValue = inputValue;
            subInputElement.value = inputValue;
        });

        inputElement.addEventListener('blur', (event) => {
            const subInputElement = document.querySelector(subId);
            if (!subInputElement) return;

            let inputValue = subInputElement.value;

            if (stringUtil.isBlank(inputValue) || inputValue === '-' || inputValue === '.') {
                subInputElement.value = formatComma ? addComma(defaultValue.toString()) : defaultValue;
                subInputElement.dataset.rawValue = defaultValue.toString();
                return;
            }

            // 블러 시 콤마 포맷 적용
            if (formatComma) {
                inputValue = removeComma(inputValue);
                subInputElement.value = addComma(inputValue);
                subInputElement.dataset.rawValue = inputValue;
            }
        }, true);
    }

    /**
     * 천자리 콤마 형식으로 변환, 입력값이 null일 경우 "-"를 반환
     * @param {number|string} number - 변환할 숫자
     * @param {number} decimals - 소수점 자리수 (선택적)
     * @returns {string} - 천자리 콤마 형식의 문자열 또는 "-"
     */
    setThousandComma(number, decimals = null) {
        if (stringUtil.isBlank(number) || isNaN(number)) {
            return "-";
        }

        const num = parseFloat(number);
        if (decimals !== null) {
            return num.toFixed(decimals).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        }

        return num.toLocaleString();
    }

    /**
     * 주어진 값이 숫자인지 체크
     * @param {any} value - 체크할 값
     * @returns {boolean} - 숫자 여부
     */
    isNumeric(value) {
        return !isNaN(parseFloat(value)) && isFinite(value);
    }

    /**
     * 소수점 자리수로 반올림
     * @param {Object} number - 반올림할 숫자
     * @param {number} [decimals=2] - 소수점 자리수
     * @returns {number} - 반올림된 숫자
     */
    roundToDecimalPlaces(number, decimals = 2) {
        return parseFloat(number).toFixed(decimals);
    }

    /**
     * 숫자를 특정 범위 내로 제한
     * @param {number} number - 제한할 숫자
     * @param {number} min - 최소값
     * @param {number} max - 최대값
     * @returns {number} - 제한된 숫자
     */
    clamp(number, min, max) {
        return Math.min(Math.max(number, min), max);
    }

    /**
     * 주어진 범위 내의 난수 생성
     * @param {number} min - 최소값
     * @param {number} max - 최대값
     * @returns {number} - 생성된 난수
     */
    randomInt(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    }

    /**
     * 부분과 전체에 대한 비율 계산
     * @param {number} part - 부분
     * @param {number} total - 전체
     * @returns {number} - 비율
     */
    percentage(part, total) {
        return (part / total) * 100;
    }

    /**
     * 퍼센트 입력 제어 (0~100 범위)
     * @param {string} selector - 입력 필드의 선택자
     * @param {number} defaultValue - 기본값
     * @param {number} maxPercent - 최대 퍼센트 (기본: 100)
     */
    setInputPercentage(selector, defaultValue = 0, maxPercent = 100) {
        const inputElement = document.querySelector(selector);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
            let inputValue = inputElement.value;

            if (stringUtil.isBlank(inputValue)) {
                return;
            }

            // 숫자와 소수점만 허용
            inputValue = inputValue.replace(/[^0-9.]/g, '');

            // 소수점은 하나만
            const parts = inputValue.split('.');
            if (parts.length > 2) {
                inputValue = parts[0] + '.' + parts[1];
            }

            // 소수점 둘째 자리까지만
            if (parts[1] && parts[1].length > 2) {
                inputValue = parts[0] + '.' + parts[1].substring(0, 2);
            }

            // 최대값 체크
            const numValue = parseFloat(inputValue);
            if (!isNaN(numValue) && numValue > maxPercent) {
                inputValue = maxPercent.toString();
            }

            inputElement.value = inputValue;
        });

        inputElement.addEventListener('blur', () => {
            const inputValue = inputElement.value;
            if (stringUtil.isBlank(inputValue) || inputValue === '.') {
                inputElement.value = defaultValue;
            }
        });
    }

    /**
     * 점수 입력 제어 (예: 0~100점)
     * @param {string} selector - 입력 필드의 선택자
     * @param {number} minScore - 최소 점수
     * @param {number} maxScore - 최대 점수
     * @param {boolean} allowDecimal - 소수점 허용 여부
     */
    setInputScore(selector, minScore = 0, maxScore = 100, allowDecimal = false) {
        const inputElement = document.querySelector(selector);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
            let inputValue = inputElement.value;

            if (stringUtil.isBlank(inputValue)) {
                return;
            }

            const pattern = allowDecimal ? /[^0-9.]/g : /[^0-9]/g;
            inputValue = inputValue.replace(pattern, '');

            if (allowDecimal) {
                const parts = inputValue.split('.');
                if (parts.length > 2) {
                    inputValue = parts[0] + '.' + parts[1];
                }
                if (parts[1] && parts[1].length > 1) {
                    inputValue = parts[0] + '.' + parts[1].substring(0, 1);
                }
            }

            const numValue = parseFloat(inputValue);
            if (!isNaN(numValue)) {
                if (numValue < minScore) {
                    inputValue = minScore.toString();
                }
                if (numValue > maxScore) {
                    inputValue = maxScore.toString();
                }
            }

            inputElement.value = inputValue;
        });

        inputElement.addEventListener('blur', () => {
            const inputValue = inputElement.value;
            if (stringUtil.isBlank(inputValue) || inputValue === '.') {
                inputElement.value = minScore.toString();
            }
        });
    }

    /**
     * 나이 입력 제어 (1~150)
     * @param {string} selector - 입력 필드의 선택자
     */
    setInputAge(selector) {
        this.setInputScore(selector, 1, 150, false);
    }

    /**
     * 연도 입력 제어 (1900~현재년도+10)
     * @param {string} selector - 입력 필드의 선택자
     * @param {number} minYear - 최소 연도 (기본: 1900)
     * @param {number} maxYear - 최대 연도 (기본: 현재년도+10)
     */
    setInputYear(selector, minYear = 1900, maxYear = new Date().getFullYear() + 10) {
        const inputElement = document.querySelector(selector);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
            let inputValue = inputElement.value;

            if (stringUtil.isBlank(inputValue)) {
                return;
            }

            inputValue = inputValue.replace(/[^0-9]/g, '');

            // 4자리 제한
            if (inputValue.length > 4) {
                inputValue = inputValue.substring(0, 4);
            }

            const numValue = parseInt(inputValue);
            if (!isNaN(numValue) && inputValue.length === 4) {
                if (numValue < minYear) {
                    inputValue = minYear.toString();
                }
                if (numValue > maxYear) {
                    inputValue = maxYear.toString();
                }
            }

            inputElement.value = inputValue;
        });
    }

    /**
     * 월 입력 제어 (1~12)
     * @param {string} selector - 입력 필드의 선택자
     */
    setInputMonth(selector) {
        const inputElement = document.querySelector(selector);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
            let inputValue = inputElement.value;

            if (stringUtil.isBlank(inputValue)) {
                return;
            }

            inputValue = inputValue.replace(/[^0-9]/g, '');

            const numValue = parseInt(inputValue);
            if (!isNaN(numValue)) {
                if (numValue > 12) {
                    inputValue = '12';
                }
                if (numValue < 1 && inputValue.length >= 2) {
                    inputValue = '1';
                }
            }

            inputElement.value = inputValue;
        });
    }

    /**
     * 일 입력 제어 (1~31)
     * @param {string} selector - 입력 필드의 선택자
     */
    setInputDay(selector) {
        const inputElement = document.querySelector(selector);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
            let inputValue = inputElement.value;

            if (stringUtil.isBlank(inputValue)) {
                return;
            }

            inputValue = inputValue.replace(/[^0-9]/g, '');

            const numValue = parseInt(inputValue);
            if (!isNaN(numValue)) {
                if (numValue > 31) {
                    inputValue = '31';
                }
                if (numValue < 1 && inputValue.length >= 2) {
                    inputValue = '1';
                }
            }

            inputElement.value = inputValue;
        });
    }

    /**
     * 시간 입력 제어 (0~23)
     * @param {string} selector - 입력 필드의 선택자
     */
    setInputHour(selector) {
        const inputElement = document.querySelector(selector);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
            let inputValue = inputElement.value;

            if (stringUtil.isBlank(inputValue)) {
                return;
            }

            inputValue = inputValue.replace(/[^0-9]/g, '');

            const numValue = parseInt(inputValue);
            if (!isNaN(numValue)) {
                if (numValue > 23) {
                    inputValue = '23';
                }
            }

            inputElement.value = inputValue;
        });
    }

    /**
     * 분/초 입력 제어 (0~59)
     * @param {string} selector - 입력 필드의 선택자
     */
    setInputMinuteSecond(selector) {
        const inputElement = document.querySelector(selector);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
            let inputValue = inputElement.value;

            if (stringUtil.isBlank(inputValue)) {
                return;
            }

            inputValue = inputValue.replace(/[^0-9]/g, '');

            const numValue = parseInt(inputValue);
            if (!isNaN(numValue)) {
                if (numValue > 59) {
                    inputValue = '59';
                }
            }

            inputElement.value = inputValue;
        });
    }

    /**
     * 소수점 자릿수 지정 입력 제어
     * @param {string} selector - 입력 필드의 선택자
     * @param {number} decimalPlaces - 소수점 자릿수
     * @param {number} min - 최소값 (선택적)
     * @param {number} max - 최대값 (선택적)
     */
    setInputDecimalPlaces(selector, decimalPlaces = 2, min = null, max = null) {
        const inputElement = document.querySelector(selector);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
            let inputValue = inputElement.value;

            if (stringUtil.isBlank(inputValue)) {
                return;
            }

            // 숫자, 소수점, 음수 기호만 허용
            inputValue = inputValue.replace(/[^0-9.-]/g, '');

            // 음수 기호는 맨 앞에만
            if (inputValue.indexOf('-') > 0) {
                inputValue = inputValue.replace(/-/g, '');
            }

            // 소수점은 하나만
            const parts = inputValue.split('.');
            if (parts.length > 2) {
                inputValue = parts[0] + '.' + parts[1];
            }

            // 소수점 자릿수 제한
            if (parts[1] && parts[1].length > decimalPlaces) {
                inputValue = parts[0] + '.' + parts[1].substring(0, decimalPlaces);
            }

            // 범위 체크
            const numValue = parseFloat(inputValue);
            if (!isNaN(numValue)) {
                if (min !== null && numValue < min) {
                    inputValue = min.toString();
                }
                if (max !== null && numValue > max) {
                    inputValue = max.toString();
                }
            }

            inputElement.value = inputValue;
        });

        inputElement.addEventListener('blur', () => {
            const inputValue = inputElement.value;
            if (stringUtil.isBlank(inputValue) || inputValue === '-' || inputValue === '.') {
                inputElement.value = min !== null ? min : 0;
            }
        });
    }

    /**
     * 숫자를 한글로 변환 (예: 1234 -> 천이백삼십사)
     * @param {number} num - 변환할 숫자
     * @returns {string} - 한글 숫자
     */
    toKoreanNumber(num) {
        const units = ['', '십', '백', '천', '만', '십만', '백만', '천만', '억'];
        const numbers = ['', '일', '이', '삼', '사', '오', '육', '칠', '팔', '구'];

        if (num === 0) return '영';

        let result = '';
        let unitIndex = 0;

        while (num > 0) {
            const digit = num % 10;
            if (digit > 0) {
                result = numbers[digit] + units[unitIndex] + result;
            }
            num = Math.floor(num / 10);
            unitIndex++;
        }

        return result;
    }

    /**
     * 바이트 크기를 읽기 쉬운 형태로 변환
     * @param {number} bytes - 바이트 크기
     * @param {number} decimals - 소수점 자릿수
     * @returns {string} - 변환된 문자열 (예: "1.23 KB")
     */
    formatBytes(bytes, decimals = 2) {
        if (bytes === 0) return '0 Bytes';

        const k = 1024;
        const dm = decimals < 0 ? 0 : decimals;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];

        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    }

    /**
     * 숫자 범위 배열 생성
     * @param {number} start - 시작 숫자
     * @param {number} end - 끝 숫자
     * @param {number} step - 증가 단위
     * @returns {Array} - 숫자 배열
     */
    range(start, end, step = 1) {
        const result = [];
        for (let i = start; i <= end; i += step) {
            result.push(i);
        }
        return result;
    }

    /**
     * 평균 계산
     * @param {Array} numbers - 숫자 배열
     * @returns {number} - 평균값
     */
    average(numbers) {
        if (!numbers || numbers.length === 0) return 0;
        return numbers.reduce((sum, num) => sum + num, 0) / numbers.length;
    }


}

const numberUtil = new NumberUtil();
export default numberUtil;
