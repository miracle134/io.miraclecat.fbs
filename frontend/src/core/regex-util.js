
/**
 * packageName    : static.js.core
 * fileName       : regex-util
 * author         : MiracleCat
 * date           : 2025-07-03
 * description    : 정규 표현식 처리
 */

/**
 * RegexUtil 클래스
 * - 정규 표현식을 사용하여 문자열을 검증하는 유틸리티 클래스.
 *
 * 주요 메서드:
 * - isNumeric(value): 숫자만 포함하는 문자열인지 체크합니다.
 * - isEmail(value): 유효한 이메일 형식인지 체크합니다.
 * - isUrl(value): 유효한 URL 형식인지 체크합니다.
 * - isPhoneNumber(value): 한국 휴대전화 번호 형식인지 체크합니다.
 * - isAlphanumeric(value): 영문자와 숫자만 포함하는 문자열인지 체크합니다.
 * - isValidPassword(value): 비밀번호 규칙이 유효한지 체크합니다.
 * - isBlank(value): 문자열이 공백만으로 이루어졌는지 체크합니다.
 * - isDate(value): 날짜 형식(YYYY-MM-DD)이 유효한지 체크합니다.
 *
 */
class RegexUtil {
    constructor() {
    }

    /**
     * 기본 아이디 형식 검증 (영문자로 시작, 영문+숫자+언더스코어)
     * @param {string} value - 검사할 아이디
     * @param {Object} options - 검증 옵션
     * @param {number} options.minLength - 최소 길이 (기본: 4)
     * @param {number} options.maxLength - 최대 길이 (기본: 20)
     * @param {boolean} options.allowUnderscore - 언더스코어 허용 (기본: true)
     * @param {boolean} options.allowHyphen - 하이픈 허용 (기본: false)
     * @param {boolean} options.allowDot - 점 허용 (기본: false)
     * @returns {boolean} - 유효한 아이디이면 true
     */
    isValidUsername(value, options = {}) {
        const {
            minLength = 4,
            maxLength = 20,
            allowUnderscore = true,
            allowHyphen = false,
            allowDot = false
        } = options;

        // 길이 체크
        if (value.length < minLength || value.length > maxLength) {
            return false;
        }

        // 영문자로 시작해야 함
        if (!/^[a-zA-Z]/.test(value)) {
            return false;
        }

        // 허용된 문자만 포함
        let allowedChars = 'a-zA-Z0-9';
        if (allowUnderscore) allowedChars += '_';
        if (allowHyphen) allowedChars += '-';
        if (allowDot) allowedChars += '.';

        const regex = new RegExp(`^[${allowedChars}]+$`);
        if (!regex.test(value)) {
            return false;
        }

        // 특수문자로 끝나면 안됨
        if (/[._-]$/.test(value)) {
            return false;
        }

        // 연속된 특수문자 방지
        if (/[._-]{2,}/.test(value)) {
            return false;
        }

        return true;
    }

    /**
     * 금지된 아이디 체크
     * @param {string} value - 검사할 아이디
     * @param {Array} customForbiddenList - 사용자 정의 금지 단어 리스트
     * @returns {boolean} - 금지된 아이디가 아니면 true
     */
    isNotForbiddenUsername(value, customForbiddenList = []) {
        const defaultForbiddenList = [
            // 시스템 관련
            'admin', 'administrator', 'root', 'system', 'guest', 'user',
            'test', 'demo', 'sample', 'null', 'undefined', 'api',

            // 서비스 관련
            'support', 'help', 'service', 'contact', 'info', 'mail',
            'news', 'blog', 'www', 'ftp', 'mail', 'email',

            // 욕설/비속어 (기본적인 것만)
            'fuck', 'shit', 'damn', 'hell', 'ass', 'bitch',

            // 한글 금지 단어
            '관리자', '운영자', '테스트', '시스템', '고객센터'
        ];

        const allForbiddenList = [...defaultForbiddenList, ...customForbiddenList];
        const lowerValue = value.toLowerCase();

        return !allForbiddenList.some(forbidden =>
            lowerValue.includes(forbidden.toLowerCase())
        );
    }

    /**
     * 숫자만 포함하는 문자열인지 체크
     * @param {string} value - 검사할 문자열
     * @returns {boolean} - 숫자만 포함하면 true, 아니면 false
     */
    isNumeric(value) {
        const regex = /^\d+$/;
        return regex.test(value);
    }

    /**
     * 이메일 형식 체크
     * @param {string} value - 검사할 이메일 주소
     * @returns {boolean} - 유효한 이메일이면 true
     */
    isEmail(value) {
        const regex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        return regex.test(value);
    }

    /**
     * URL 형식 체크
     * @param {string} value - 검사할 URL
     * @returns {boolean} - 유효한 URL이면 true
     */
    isUrl(value) {
        const regex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;
        return regex.test(value);
    }

    /**
     * 한국 전화번호 형식 체크 (일반전화, 휴대전화 모두 지원)
     * @param {string} value - 검사할 전화번호
     * @param {string} type - 전화번호 타입 ('mobile', 'landline', 'all')
     * @returns {boolean} - 유효한 전화번호이면 true
     */
    isPhoneNumber(value, type = 'all') {
        const patterns = {
            mobile: /^01[016789]-\d{3,4}-\d{4}$/, // 휴대전화
            landline: /^(0(2|3[1-3]|4[1-4]|5[1-5]|6[1-4]))-\d{3,4}-\d{4}$/, // 일반전화
            all: /^(01[016789]|0(2|3[1-3]|4[1-4]|5[1-5]|6[1-4]))-\d{3,4}-\d{4}$/ // 전체
        };

        return patterns[type].test(value);
    }

    /**
     * 연락처 형식 체크 (국내·해외 번호 공용, 서버 검증과 동일 규칙)
     * - 숫자, +, -, (, ) 만 9~20자
     * @param {string} value - 검사할 연락처
     * @returns {boolean} - 유효하면 true
     */
    isContactNumber(value) {
        return /^[0-9+\-()]{9,20}$/.test(value ?? '');
    }

    /**
     * 영문자와 숫자만 포함하는 문자열인지 체크
     * @param {string} value - 검사할 문자열
     * @returns {boolean} - 영문자와 숫자만 포함하면 true, 아니면 false
     */
    isAlphanumeric(value) {
        const regex = /^[a-zA-Z0-9]+$/;
        return regex.test(value);
    }

    /**
     * 비밀번호 규칙 체크
     * @param {string} value - 검사할 비밀번호
     * @param {Object} options - 검증 옵션
     * @param {number} options.minLength - 최소 길이 (기본: 8)
     * @param {boolean} options.requireUppercase - 대문자 필수 여부 (기본: true)
     * @param {boolean} options.requireLowercase - 소문자 필수 여부 (기본: true)
     * @param {boolean} options.requireNumber - 숫자 필수 여부 (기본: true)
     * @param {boolean} options.requireSpecial - 특수문자 필수 여부 (기본: true)
     * @param {number} options.minSpecialCount - 최소 특수문자 개수 (기본: 1)
     * @returns {boolean|Object} - 유효하면 true, 아니면 오류 정보 객체
     */
    isValidPassword(value, options = {}) {
        const {
            minLength = 8,
            requireUppercase = true,
            requireLowercase = true,
            requireNumber = true,
            requireSpecial = true,
            minSpecialCount = 1
        } = options;

        const checks = {
            length: value.length >= minLength,
            hasUppercase: requireUppercase ? /[A-Z]/.test(value) : true,
            hasLowercase: requireLowercase ? /[a-z]/.test(value) : true,
            hasNumber: requireNumber ? /\d/.test(value) : true,
            hasSpecial: requireSpecial ? /[^A-Za-z0-9_ㄱ-ㅎㅏ-ㅣ가-힣]/.test(value) : true,
            specialCount: requireSpecial ? (value.match(/[^A-Za-z0-9_ㄱ-ㅎㅏ-ㅣ가-힣]/g) || []).length >= minSpecialCount : true,
            noSpace: !/\s/.test(value)
        };

        const isValid = Object.values(checks).every(check => check);

        // 간단한 true/false만 원하는 경우
        if (options.simple !== false) {
            return isValid;
        }

        // 상세한 검증 결과 반환
        return {
            isValid,
            checks,
            errors: Object.entries(checks)
                .filter(([key, valid]) => !valid)
                .map(([key, valid]) => key)
        };
    }

    /**
     * 문자열이 공백만으로 이루어졌는지 체크
     * @param {string} value - 검사할 문자열
     * @returns {boolean} - 공백 문자열이면 true, 아니면 false
     */
    isBlank(value) {
        const regex = /^\s*$/;
        return regex.test(value);
    }

    /**
     * 날짜 형식(YYYY-MM-DD)이 유효한지 체크
     * @param {string} value - 검사할 날짜 문자열
     * @returns {boolean} - 유효한 날짜 형식이면 true, 아니면 false
     */
    isDate(value) {
        const regex = /^\d{4}-\d{2}-\d{2}$/;
        return regex.test(value);
    }

    /**
     * 한국 주민등록번호 형식 체크
     * @param {string} value - 검사할 주민등록번호
     * @returns {boolean} - 유효한 형식이면 true
     */
    isResidentNumber(value) {
        const regex = /^\d{6}-[1-4]\d{6}$/;
        return regex.test(value);
    }

    /**
     * 한국 사업자등록번호 형식 체크
     * @param {string} value - 검사할 사업자등록번호
     * @returns {boolean} - 유효한 형식이면 true
     */
    isBusinessNumber(value) {
        const regex = /^\d{3}-\d{2}-\d{5}$/;
        if (!regex.test(value)) return false;

        // 체크섬 검증
        const numbers = value.replace(/-/g, '').split('').map(Number);
        const checkSum = [1, 3, 7, 1, 3, 7, 1, 3, 5];
        let sum = 0;

        for (let i = 0; i < 9; i++) {
            sum += numbers[i] * checkSum[i];
        }

        sum += parseInt((numbers[8] * 5) / 10);
        const remainder = sum % 10;
        const checkDigit = remainder === 0 ? 0 : 10 - remainder;

        return checkDigit === numbers[9];
    }

    /**
     * 한국 우편번호 형식 체크 (5자리)
     * @param {string} value - 검사할 우편번호
     * @returns {boolean} - 유효한 형식이면 true
     */
    isPostalCode(value) {
        const regex = /^\d{5}$/;
        return regex.test(value);
    }

    /**
     * 한글만 포함하는지 체크
     * @param {string} value - 검사할 문자열
     * @returns {boolean} - 한글만 포함하면 true
     */
    isKorean(value) {
        const regex = /^[가-힣]+$/;
        return regex.test(value);
    }

    /**
     * 영문만 포함하는지 체크
     * @param {string} value - 검사할 문자열
     * @returns {boolean} - 영문만 포함하면 true
     */
    isAlpha(value) {
        const regex = /^[a-zA-Z]+$/;
        return regex.test(value);
    }

    /**
     * IP 주소 형식 체크
     * @param {string} value - 검사할 IP 주소
     * @param {string} version - IP 버전 ('v4', 'v6', 'both')
     * @returns {boolean} - 유효한 IP 주소이면 true
     */
    isIpAddress(value, version = 'v4') {
        const ipv4Regex = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
        const ipv6Regex = /^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$/;

        switch (version) {
            case 'v4':
                return ipv4Regex.test(value);
            case 'v6':
                return ipv6Regex.test(value);
            case 'both':
                return ipv4Regex.test(value) || ipv6Regex.test(value);
            default:
                return ipv4Regex.test(value);
        }
    }

    /**
     * 신용카드 번호 형식 체크 (기본적인 형식만)
     * @param {string} value - 검사할 신용카드 번호
     * @returns {boolean} - 유효한 형식이면 true
     */
    isCreditCard(value) {
        // 공백과 하이픈 제거
        const cleaned = value.replace(/[\s-]/g, '');

        // 13~19자리 숫자
        const regex = /^\d{13,19}$/;
        if (!regex.test(cleaned)) return false;

        // Luhn 알고리즘 검증
        let sum = 0;
        let isEven = false;

        for (let i = cleaned.length - 1; i >= 0; i--) {
            let digit = parseInt(cleaned[i]);

            if (isEven) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            isEven = !isEven;
        }

        return sum % 10 === 0;
    }

    /**
     * 파일 확장자 체크
     * @param {string} filename - 파일명
     * @param {Array|string} allowedExtensions - 허용할 확장자 배열 또는 문자열
     * @returns {boolean} - 허용된 확장자이면 true
     */
    isValidFileExtension(filename, allowedExtensions) {
        if (typeof allowedExtensions === 'string') {
            allowedExtensions = [allowedExtensions];
        }

        const extension = filename.split('.').pop().toLowerCase();
        return allowedExtensions.map(ext => ext.toLowerCase().replace('.', '')).includes(extension);
    }

    /**
     * HTML 태그가 포함되어 있는지 체크
     * @param {string} value - 검사할 문자열
     * @returns {boolean} - HTML 태그가 포함되어 있으면 true
     */
    hasHtmlTags(value) {
        const regex = /<[^>]*>/;
        return regex.test(value);
    }

    /**
     * 특수문자만 포함하는지 체크
     * @param {string} value - 검사할 문자열
     * @returns {boolean} - 특수문자만 포함하면 true
     */
    isSpecialCharsOnly(value) {
        const regex = /^[^a-zA-Z0-9가-힣\s]+$/;
        return regex.test(value);
    }

    /**
     * 강력한 비밀번호 체크 (더 까다로운 규칙)
     * @param {string} value - 검사할 비밀번호
     * @returns {Object} - 검증 결과와 점수
     */
    isStrongPassword(value) {
        const checks = {
            length: value.length >= 12,
            hasUppercase: /[A-Z]/.test(value),
            hasLowercase: /[a-z]/.test(value),
            hasNumber: /\d/.test(value),
            hasSpecial: /[^A-Za-z0-9]/.test(value),
            noSequential: !/(?:abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz|123|234|345|456|567|678|789|890)/i.test(value),
            noRepeating: !/(.)\1{2,}/.test(value),
            noCommonWords: !/(?:password|admin|user|login|qwerty|123456|abcdef)/i.test(value)
        };

        const score = Object.values(checks).filter(Boolean).length;
        const maxScore = Object.keys(checks).length;

        return {
            isStrong: score >= 6,
            score: score,
            maxScore: maxScore,
            percentage: Math.round((score / maxScore) * 100),
            checks: checks,
            strength: score >= 7 ? 'Very Strong' :
                score >= 6 ? 'Strong' :
                    score >= 4 ? 'Medium' :
                        score >= 2 ? 'Weak' : 'Very Weak'
        };
    }

    /**
     * 날짜 형식 체크 (수정됨 - 다양한 형식 지원)
     * @param {string} value - 검사할 날짜 문자열
     * @param {string} format - 날짜 형식 ('YYYY-MM-DD', 'YYYY/MM/DD', 'DD-MM-YYYY', 'MM/DD/YYYY')
     * @returns {boolean} - 유효한 날짜 형식이면 true
     */
    isDate(value, format = 'YYYY-MM-DD') {
        const patterns = {
            'YYYY-MM-DD': /^\d{4}-\d{2}-\d{2}$/,
            'YYYY/MM/DD': /^\d{4}\/\d{2}\/\d{2}$/,
            'DD-MM-YYYY': /^\d{2}-\d{2}-\d{4}$/,
            'DD/MM/YYYY': /^\d{2}\/\d{2}\/\d{4}$/,
            'MM/DD/YYYY': /^\d{2}\/\d{2}\/\d{4}$/,
            'YYYY.MM.DD': /^\d{4}\.\d{2}\.\d{2}$/
        };

        const regex = patterns[format];
        if (!regex) return false;

        if (!regex.test(value)) return false;

        // 실제 날짜 유효성 검사
        const date = new Date(value.replace(/[.\-]/g, '/'));
        return date instanceof Date && !isNaN(date);
    }

    /**
     * 한국 법인등록번호 체크 (110111-0000001)
     * @param {string} value - 검사할 법인등록번호
     * @returns {boolean} - 유효한 형식이면 true
     */
    isCorporateNumber(value) {
        const regex = /^\d{6}-\d{7}$/;
        return regex.test(value);
    }

    /**
     * 계좌번호 형식 체크 (은행별)
     * @param {string} value - 검사할 계좌번호
     * @param {string} bankCode - 은행 코드 (선택적)
     * @returns {boolean} - 유효한 형식이면 true
     */
    isBankAccount(value, bankCode = null) {
        // 하이픈 제거
        const cleaned = value.replace(/-/g, '');

        // 기본적으로 6~20자리 숫자
        const basicRegex = /^\d{6,20}$/;
        if (!basicRegex.test(cleaned)) return false;

        // 은행별 상세 검증 (주요 은행만)
        const bankPatterns = {
            '004': /^\d{3}-\d{6}-\d{2,3}$/, // KB국민
            '011': /^\d{3}-\d{2}-\d{6}$/, // NH농협
            '020': /^\d{3}-\d{2}-\d{6}$/, // 우리
            '081': /^\d{7}-\d{2}-\d{6}$/, // KEB하나
            '088': /^\d{6}-\d{2}-\d{7}$/ // 신한
        };

        if (bankCode && bankPatterns[bankCode]) {
            return bankPatterns[bankCode].test(value);
        }

        return true;
    }

    /**
     * MAC 주소 형식 체크
     * @param {string} value - 검사할 MAC 주소
     * @returns {boolean} - 유효한 MAC 주소이면 true
     */
    isMacAddress(value) {
        const regex = /^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$/;
        return regex.test(value);
    }

    /**
     * 차량번호 형식 체크 (한국)
     * @param {string} value - 검사할 차량번호
     * @returns {boolean} - 유효한 차량번호이면 true
     */
    isVehicleNumber(value) {
        // 일반 번호판: 12가1234, 서울12가1234
        const generalRegex = /^[가-힣]{0,2}\d{2,3}[가-힣]{1}\d{4}$/;
        // 신형 번호판: 123가1234
        const newRegex = /^\d{3}[가-힣]{1}\d{4}$/;

        return generalRegex.test(value) || newRegex.test(value);
    }

    /**
     * 색상 코드 형식 체크
     * @param {string} value - 검사할 색상 코드
     * @param {string} format - 형식 ('hex', 'rgb', 'rgba', 'hsl', 'all')
     * @returns {boolean} - 유효한 색상 코드이면 true
     */
    isColorCode(value, format = 'hex') {
        const patterns = {
            hex: /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/,
            rgb: /^rgb\(\s*\d{1,3}\s*,\s*\d{1,3}\s*,\s*\d{1,3}\s*\)$/,
            rgba: /^rgba\(\s*\d{1,3}\s*,\s*\d{1,3}\s*,\s*\d{1,3}\s*,\s*(0|1|0?\.\d+)\s*\)$/,
            hsl: /^hsl\(\s*\d{1,3}\s*,\s*\d{1,3}%\s*,\s*\d{1,3}%\s*\)$/
        };

        if (format === 'all') {
            return Object.values(patterns).some(pattern => pattern.test(value));
        }

        return patterns[format] ? patterns[format].test(value) : false;
    }

    /**
     * 버전 번호 형식 체크 (Semantic Versioning)
     * @param {string} value - 검사할 버전 번호
     * @returns {boolean} - 유효한 버전 번호이면 true
     */
    isVersion(value) {
        const regex = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$/;
        return regex.test(value);
    }

    /**
     * 정수만 포함하는지 체크 (음수 포함)
     * @param {string} value - 검사할 문자열
     * @returns {boolean} - 정수만 포함하면 true
     */
    isInteger(value) {
        const regex = /^-?\d+$/;
        return regex.test(value);
    }

    /**
     * 소수 형식 체크
     * @param {string} value - 검사할 문자열
     * @param {number} decimalPlaces - 허용할 소수점 자릿수 (선택적)
     * @returns {boolean} - 유효한 소수이면 true
     */
    isDecimal(value, decimalPlaces = null) {
        let regex;

        if (decimalPlaces) {
            regex = new RegExp(`^-?\\d+\\.\\d{1,${decimalPlaces}}$`);
        } else {
            regex = /^-?\d+\.\d+$/;
        }

        return regex.test(value);
    }

    /**
     * JSON 형식 체크
     * @param {string} value - 검사할 JSON 문자열
     * @returns {boolean} - 유효한 JSON이면 true
     */
    isJson(value) {
        try {
            JSON.parse(value);
            return true;
        } catch (e) {
            return false;
        }
    }

    /**
     * Base64 형식 체크
     * @param {string} value - 검사할 문자열
     * @returns {boolean} - 유효한 Base64이면 true
     */
    isBase64(value) {
        const regex = /^[A-Za-z0-9+/]*={0,2}$/;
        return regex.test(value) && value.length % 4 === 0;
    }

    /**
     * UUID 형식 체크
     * @param {string} value - 검사할 UUID
     * @param {number} version - UUID 버전 (1,2,3,4,5 또는 null for any)
     * @returns {boolean} - 유효한 UUID이면 true
     */
    isUuid(value, version = null) {
        const generalRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

        if (!generalRegex.test(value)) return false;

        if (version) {
            const versionRegex = new RegExp(`^[0-9a-f]{8}-[0-9a-f]{4}-${version}[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`, 'i');
            return versionRegex.test(value);
        }

        return true;
    }

}

const regexUtil = new RegexUtil();
export default regexUtil;