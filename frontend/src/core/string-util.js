
/**
 * packageName    : static.js.core
 * fileName       : string-util
 * author         : MiracleCat
 * date           : 2025-07-03
 * description    : 문자열 처리
 */

/**
 * 공통 헬퍼: 셀렉터 또는 Element를 Element로 변환
 */
const getElement = (selectorOrElement) => {
    return typeof selectorOrElement === 'string'
        ? document.querySelector(selectorOrElement)
        : selectorOrElement;
};

class StringUtil {
	constructor() {
		this.patterns = {
			alphaNumericSpecial: /[^a-zA-Z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/g,
			alphaOnly: /[^a-zA-Z]/g,
			alphaNumericKorean: /[^a-zA-Z0-9가-힣]/g,
			numericOnly: /[^0-9]/g,
			phoneNumber: /[^0-9-]/g,
			email: /[^a-zA-Z0-9._%+\-@]/g,
			koreanOnly: /[^가-힣]/g,
			price: /[^0-9,]/g,
			businessNumber: /[^0-9-]/g,
			residentNumber: /[^0-9-]/g,
			filename: /[^a-zA-Z0-9가-힣._\-\s]/g
		};
	}

	/**
	 * 객체가 null 또는 blank 인지 확인
	 */
	isBlank(obj) {
		if (obj === null || obj === undefined) return true;
		if (Array.isArray(obj)) return obj.length === 0;
		if (typeof obj === "string") return obj.trim() === "";
		if (typeof obj === "number") return isNaN(obj);
		if (typeof obj === "object") return Object.keys(obj).length === 0;
		return false;
	}

	/**
	 * 객체가 비어 있으면 전달받은 문자열로 변환
	 */
	replaceNullToStr(obj, str = "") {
		return this.isBlank(obj) ? str : obj;
	}

	/**
	 * 객체가 비어 있으면 빈 문자열, 아니면 변환할 문자열을 반환
	 */
	replaceNotNullToStr(obj, str = "") {
		return this.isBlank(obj) ? "" : str;
	}

	/**
	 * 객체가 비어 있으면 빈 문자열, 아니면 XSS 처리를 통해 안전한 문자열 반환
	 */
	replaceNullToXss(obj) {
		return this.isBlank(obj) ? "" : this.unescapeHtml(obj);
	}

	/**
	 * 콤마 추가
	 */
    addComma(obj){
        if(!obj || obj === "") return "";

        // 숫자 검증
        const numValue = Number(String(obj).replace(/,/g, ''));
        if(isNaN(numValue)) return obj;  // 숫자 아니면 원본 반환

        const parts = String(obj).split('.');
        parts[0] = parts[0].replace(/,/g, '').replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        return parts.join('.');
    }

	/**
	 * 콤마 제거
	 */
    removeComma(obj) {
        return obj ? obj.replace(/,/g, "") : "";
    }

	/**
	 * 하이픈 제거
	 */
	replaceHyphen(obj) {
        return obj ? obj.replace(/-/g, "") : "";
	}

	/**
	 * HTML 이스케이프 처리 (XSS 방지)
	 */
	escapeHtml(unsafe) {
		return unsafe.replace(/<script>/g, "")
			.replace(/<\/script>/g, "")
			.replace(/eval/g, "")
			.replace(/\(/g, "&#40;")
			.replace(/\)/g, "&#41;")
			.replace(/</g, "&lt;")
			.replace(/>/g, "&gt;")
			.replace(/"/g, "&quot;")
			.replace(/"/g, "&#039;");
	}

	/**
	 * HTML 이스케이프 해제
	 */
	unescapeHtml(unsafe) {
		return unsafe.replace(/&#40;/g, "(")
			.replace(/&#41;/g, ")")
			.replace(/&lt;/g, "<")
			.replace(/&gt;/g, ">")
			.replace(/&quot;/g, "\"")
			.replace(/&#039;/g, "'");
	}

    /**
     * 대문자로 변환
     */
    toUpperCase(str) {
        return str.toUpperCase();
    }

    /**
     * 소문자로 변환
     */
    toLowerCase(str) {
        return str.toLowerCase();
    }

    /**
     * 첫 글자만 대문자
     */
    capitalize(str) {
        return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
    }

    /**
     * 스네이크 케이스 > 카멜 케이스
     * snake_case > camelCase
     */
    snakeToCamel(str) {
        return str.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase());
    }

    /**
     * 카멜 케이스 > 스네이크 케이스
     * camelCase > snake_case
     */
    camelToSnake(str) {
        return str.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`);
    }

    /**
     * 케밥 케이스 > 카멜 케이스
     * kebab-case > camelCase
     */
    kebabToCamel(str) {
        return str.replace(/-([a-z])/g, (_, letter) => letter.toUpperCase());
    }

    /**
     * 카멜 케이스 > 케밥 케이스
     * camelCase > kebab-case
     */
    camelToKebab(str) {
        return str.replace(/[A-Z]/g, letter => `-${letter.toLowerCase()}`);
    }

    /**
     * 카멜 케이스 > 파스칼 케이스
     * camelCase > PascalCase
     */
    camelToPascal(str) {
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    /**
     * 파스칼 케이스 > 카멜 케이스
     * PascalCase > camelCase
     */
    pascalToCamel(str) {
        return str.charAt(0).toLowerCase() + str.slice(1);
    }

	/**
	 * 왼쪽 정해진 문자열 체우기
	 */
	padLeft(str, length, padChar = " ") {
		return str.padStart(length, padChar);
	}

	/**
	 * 오른쪽 정해진 문자열 체우기
	 */
	padRight(str, length, padChar = " ") {
		return str.padEnd(length, padChar);
	}

    /**
     * 영문 + 숫자 + 특수문자만 가능 (한글 제외)
     */
    setInputAlphaNumericSpecial(selectorOrElement, maxLength = null) {
        const inputElement = getElement(selectorOrElement);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
           let inputValue = inputElement.value;
           inputValue = inputValue.replace(this.patterns.alphaNumericSpecial, '');
           if (maxLength && inputValue.length > maxLength) {
               inputValue = inputValue.slice(0, maxLength);
           }
           inputElement.value = inputValue;
        });
    }

    /**
     * 영문만 입력 가능
     */
    setInputAlphaOnly(selectorOrElement, maxLength = null) {
        const inputElement = getElement(selectorOrElement);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
           let inputValue = inputElement.value;
           inputValue = inputValue.replace(this.patterns.alphaOnly, '');
           if (maxLength && inputValue.length > maxLength) {
               inputValue = inputValue.slice(0, maxLength);
           }
           inputElement.value = inputValue;
        });
    }

    /**
     * 영문 + 숫자 + 한글만 입력 가능
     */
    setInputAlphaNumericKorean(selectorOrElement, maxLength = null) {
        const inputElement = getElement(selectorOrElement);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
           let inputValue = inputElement.value;
           inputValue = inputValue.replace(this.patterns.alphaNumericKorean, '');
           if (maxLength && inputValue.length > maxLength) {
               inputValue = inputValue.slice(0, maxLength);
           }
           inputElement.value = inputValue;
        });
    }

    /**
     * 전화번호 형식 입력 (국내/해외 번호 지원)
     */
    setInputPhoneNumber(selectorOrElement, autoFormat = true) {
        const inputElement = getElement(selectorOrElement);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
            inputElement.value = this.formatPhoneNumber(inputElement.value, autoFormat);
        });
    }

    /**
     * 전화번호 형식 변환 (setInputPhoneNumber / 이벤트 위임용)
     */
    formatPhoneNumber(value, autoFormat = true) {
            let inputValue = value ?? '';

            inputValue = inputValue.replace(/[^0-9+\-()]/g, '');

            if (autoFormat) {
                if (inputValue.startsWith('+')) {
                    const cleaned = inputValue.replace(/[^0-9+)]/g, '');

                    if (cleaned.includes(')')) {
                        const parts = cleaned.split(')');
                        const countryCode = parts[0];
                        const restNumber = parts.slice(1).join('').replace(/[^0-9]/g, '');

                        let formattedRest = '';
                        if (restNumber.length <= 2) {
                            formattedRest = restNumber;
                        } else if (restNumber.length <= 6) {
                            formattedRest = restNumber.replace(/(\d{2,3})(\d+)/, '$1-$2');
                        } else if (restNumber.length <= 10) {
                            formattedRest = restNumber.replace(/(\d{2,3})(\d{3,4})(\d+)/, '$1-$2-$3');
                        } else if (restNumber.length === 11) {
                            formattedRest = restNumber.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
                        } else {
                            formattedRest = restNumber;
                        }

                        inputValue = `${countryCode})${formattedRest}`;
                    } else {
                        inputValue = cleaned;
                    }
                }
                else {
                    const cleaned = inputValue.replace(/[^0-9]/g, '');

                    if (cleaned.startsWith('02')) {
                        if (cleaned.length <= 2) {
                            inputValue = cleaned;
                        } else if (cleaned.length <= 5) {
                            inputValue = cleaned.replace(/(\d{2})(\d+)/, '$1-$2');
                        } else if (cleaned.length <= 9) {
                            const middle = cleaned.substring(2, cleaned.length - 4);
                            const last = cleaned.substring(cleaned.length - 4);
                            if(middle) inputValue = `02-${middle}-${last}`;
                            else inputValue = `02-${last}`;
                        } else if (cleaned.length === 10) {
                            inputValue = cleaned.replace(/(\d{2})(\d{4})(\d{4})/, '$1-$2-$3');
                        } else {
                            inputValue = cleaned;
                        }
                    }
                    else {
                        if (cleaned.length <= 3) {
                            inputValue = cleaned;
                        } else if (cleaned.length <= 7) {
                            inputValue = cleaned.replace(/(\d{3})(\d+)/, '$1-$2');
                        } else if (cleaned.length <= 11) {
                            inputValue = cleaned.replace(/(\d{3})(\d{3,4})(\d{4})/, '$1-$2-$3');
                        } else {
                            inputValue = cleaned;
                        }
                    }
                }
            }

            return inputValue;
    }

    /**
     * 이메일 형식 입력 제어
     */
    setInputEmail(selectorOrElement, maxLength = null) {
        const inputElement = getElement(selectorOrElement);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
           inputElement.value = this.filterEmail(inputElement.value, maxLength);
        });
    }

    /**
     * 이메일 허용 문자만 남김 (setInputEmail / 이벤트 위임용)
     */
    filterEmail(value, maxLength = null) {
        let inputValue = (value ?? '').replace(this.patterns.email, '');
        if (maxLength && inputValue.length > maxLength) {
            inputValue = inputValue.slice(0, maxLength);
        }
        return inputValue;
    }

    /**
     * 한글만 입력 가능
     */
    setInputKoreanOnly(selectorOrElement, maxLength = null) {
        const inputElement = getElement(selectorOrElement);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
           let inputValue = inputElement.value;
           inputValue = inputValue.replace(this.patterns.koreanOnly, '');
           if (maxLength && inputValue.length > maxLength) {
               inputValue = inputValue.slice(0, maxLength);
           }
           inputElement.value = inputValue;
        });
    }

    /**
     * 가격 입력 (숫자와 쉼표만, 자동 천단위 구분)
     */
    setInputPrice(selectorOrElement, maxValue = null, maxLength = null) {
        const inputElement = getElement(selectorOrElement);
        if (!inputElement) return;

        inputElement.addEventListener('input', () => {
           let inputValue = inputElement.value;

           inputValue = inputValue.replace(this.patterns.price, '');

           let numericValue = inputValue.replace(/,/g, '');

           if (maxValue && parseInt(numericValue) > maxValue) {
              numericValue = maxValue.toString();
           }

           if (maxLength && numericValue.length > maxLength) {
              numericValue = numericValue.slice(0, maxLength);
           }

           if (numericValue) {
              inputValue = parseInt(numericValue).toLocaleString();
           }

           inputElement.value = inputValue;
        });
    }

	/**
	 * 사업자등록번호 형식 (000-00-00000)
	 */
	setInputBusinessNumber(selectorOrElement) {
		const inputElement = getElement(selectorOrElement);
		if (!inputElement) return;

		inputElement.addEventListener('input', () => {
			let inputValue = inputElement.value;
			inputValue = inputValue.replace(/[^0-9]/g, '');

			if (inputValue.length >= 3 && inputValue.length <= 5) {
				inputValue = inputValue.replace(/(\d{3})(\d+)/, '$1-$2');
			} else if (inputValue.length >= 6) {
				inputValue = inputValue.replace(/(\d{3})(\d{2})(\d+)/, '$1-$2-$3');
			}

			if (inputValue.length > 12) {
				inputValue = inputValue.substring(0, 12);
			}

			inputElement.value = inputValue;
		});
	}

	/**
	 * 주민등록번호 형식 (000000-0000000)
	 */
	setInputResidentNumber(selectorOrElement) {
		const inputElement = getElement(selectorOrElement);
		if (!inputElement) return;

		inputElement.addEventListener('input', () => {
			let inputValue = inputElement.value;
			inputValue = inputValue.replace(/[^0-9]/g, '');

			if (inputValue.length >= 7) {
				inputValue = inputValue.replace(/(\d{6})(\d+)/, '$1-$2');
			}

			if (inputValue.length > 14) {
				inputValue = inputValue.substring(0, 14);
			}

			inputElement.value = inputValue;
		});
	}

	/**
	 * 파일명 안전 문자만 허용
	 */
	setInputFilename(selectorOrElement) {
		const inputElement = getElement(selectorOrElement);
		if (!inputElement) return;

		inputElement.addEventListener('input', () => {
			let inputValue = inputElement.value;
			inputElement.value = inputValue.replace(this.patterns.filename, '');
		});
	}

	/**
	 * 최대 길이 제한 (한글 고려)
	 */
	setInputMaxLength(selectorOrElement, maxLength, showCounter = false) {
		const inputElement = getElement(selectorOrElement);
		if (!inputElement) return;

		let counterElement;
		if (showCounter) {
			counterElement = document.createElement('span');
			counterElement.className = 'char-counter';
			counterElement.style.fontSize = '12px';
			counterElement.style.color = '#666';
			inputElement.parentNode.appendChild(counterElement);
		}

		const updateCounter = () => {
			const currentLength = inputElement.value.length;
			if (counterElement) {
				counterElement.textContent = `${currentLength}/${maxLength}`;
				counterElement.style.color = currentLength > maxLength * 0.9 ? '#ff6b6b' : '#666';
			}
		};

		inputElement.addEventListener('input', () => {
			if (inputElement.value.length > maxLength) {
				inputElement.value = inputElement.value.substring(0, maxLength);
			}
			updateCounter();
		});

		if (showCounter) {
			updateCounter();
		}
	}

	/**
	 * URL 주소 입력 제어
	 */
	setInputUrl(selectorOrElement) {
		const inputElement = getElement(selectorOrElement);
		if (!inputElement) return;

		inputElement.addEventListener('input', () => {
			let inputValue = inputElement.value;
			inputValue = inputValue.replace(/[^a-zA-Z0-9._~:/?#\[\]@!$&'()*+,;=-]/g, '');
			inputElement.value = inputValue;
		});
	}

	/**
	 * 소수점 숫자 입력 (가격, 비율 등)
	 */
	setInputDecimal(selectorOrElement, decimalPlaces = 2) {
		const inputElement = getElement(selectorOrElement);
		if (!inputElement) return;

		inputElement.addEventListener('input', () => {
			let inputValue = inputElement.value;

			inputValue = inputValue.replace(/[^0-9.]/g, '');

			const parts = inputValue.split('.');
			if (parts.length > 2) {
				inputValue = parts[0] + '.' + parts.slice(1).join('');
			}

			if (parts[1] && parts[1].length > decimalPlaces) {
				inputValue = parts[0] + '.' + parts[1].substring(0, decimalPlaces);
			}

			inputElement.value = inputValue;
		});
	}

    /**
     * JSON safe 변환
     */
    safeJsonParse(str, defaultValue = {}) {
        if (!str) return defaultValue;
        try {
            return JSON.parse(str);
        } catch (e) {
            console.warn('JSON parse failed:', str);
            return defaultValue;
        }
    }

    /**
     * 분을 다음 형식으로 변환
     * - 70 입력 시 1시간 10분 반환
     * - 60 입력 시 1시간 반환
     * - 50 입력 시 50분 반환
     * - 잘못된 입력 시 '' 반환
     */
    formatMinutes(minutes) {
        if (!Number.isSafeInteger(minutes) || minutes < 0) {
            return '';
        }

        const hours = Math.floor(minutes / 60);
        const mins = minutes % 60;

        if (hours > 0 && mins > 0) {
            return `${hours}시간 ${mins}분`;
        }

        if (hours > 0) {
            return `${hours}시간`;
        }

        return `${mins}분`;
    }
}

const stringUtil = new StringUtil();
export default stringUtil;