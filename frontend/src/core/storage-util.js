
/**
 * packageName    : static.js.core
 * fileName       : storage-util
 * author         : MiracleCat 
 * date           : 2025-07-03
 * description    : 세션, 로컬 스토리지 및 쿠키 관리
 */

/**
 * StorageUtil 클래스
 * - 세션 스토리지, 로컬 스토리지 및 쿠키를 관리하는 유틸리티입니다.
 *
 * 주요 메서드:
 * - setSession(key, value): 세션 스토리지에 값을 저장합니다.
 * - getSession(key): 세션 스토리지에서 값을 조회합니다.
 * - deleteSession(key): 세션 스토리지에서 값을 삭제합니다.
 * - deleteAllSession(): 세션 스토리지의 모든 값을 삭제합니다.
 * - setLocal(key, value): 로컬 스토리지에 값을 저장합니다.
 * - getLocal(key): 로컬 스토리지에서 값을 조회합니다.
 * - deleteLocal(key): 로컬 스토리지에서 값을 삭제합니다.
 * - deleteAllLocal(): 로컬 스토리지의 모든 값을 삭제합니다.
 * - setCookie(key, value, days, unit): 쿠키를 저장합니다.
 * - getCookie(key): 쿠키를 조회합니다.
 * - deleteCookie(key): 쿠키를 삭제합니다.
 *
 */
class StorageUtil {
	constructor() {
	}
	
	/**
	 * 세션 스토리지 등록
     * @param {string} key
     * @param {*} value
	 */
	setSession(key, value) {
		sessionStorage.setItem(key, JSON.stringify(value));
	}
	
	/**
	 * 세션 스토리지 조회
     * @param {string} key
     * @return {*|null} 값이 없거나 파싱 실패 시 null
	 */
	getSession(key) {
		const item = sessionStorage.getItem(key);
        if (item === null) {
            return null;
        }

        try {
            return JSON.parse(item);
        } catch {
            return null;
        }
	}

    /**
     * 세션 스토리지 등록 with ExpireDate
     * @param key
     * @param value
     * @param time 초 단위
     */
    setSessionWithExpire(key, value, time) {
        const now = Date.now();
        const item = {
            value: value,
            expire: now + time * 1000, // time
        };
        sessionStorage.setItem(key, JSON.stringify(item));
    }

    /**
     * 로컬 스토리지 조회 with ExpireDate
     * @param key
     * @return {string|null}
     */
    getSessionWithExpire(key) {
        const itemStr = sessionStorage.getItem(key);
        if (!itemStr) return null;

        const item = JSON.parse(itemStr);
        const now = Date.now();

        if (now > item.expire) {
            sessionStorage.removeItem(key);
            return null;
        }
        return item.value;
    }

	/**
	 * 세션 스토리지 삭제
	 * @param key
	 */
	deleteSession(key) {
		sessionStorage.removeItem(key);
	}
	
	/**
	 * 세션 스토리지 전체 삭제
	 */
	deleteAllSession() {
		sessionStorage.clear();
	}
	
	
	/**
	 * 로컬 스토리지 등록
     * @param {string} key
     * @param {*} value
	 */
	setLocal(key, value) {
		localStorage.setItem(key, JSON.stringify(value));
	}
	
	/**
	 * 로컬 스토리지 조회
     * @param {string} key
     * @return {*|null} 값이 없거나 파싱 실패 시 null
	 */
	getLocal(key) {
		const item = localStorage.getItem(key);
        if (item === null) {
            return null;
        }

        try {
            return JSON.parse(item);
        } catch {
            return null;
        }
	}

	/**
	 * 로컬 스토리지 등록 with ExpireDate
	 * @param key
	 * @param value
	 * @param time 초 단위
	 */
	setLocalWithExpire(key, value, time) {
		const now = Date.now();
		const item = {
			value: value,
			expire: now + time * 1000, // time
		};
		localStorage.setItem(key, JSON.stringify(item));
	}
	
	/**
	 * 로컬 스토리지 조회 with ExpireDate
	 * @param key
	 * @return {string|null}
	 */
	getLocalWithExpire(key) {
		const itemStr = localStorage.getItem(key);
		if (!itemStr) return null;
		
		const item = JSON.parse(itemStr);
		const now = Date.now();
		
		if (now > item.expire) {
			localStorage.removeItem(key);
			return null;
		}
		return item.value;
	}
	
	/**
	 * 로컬 스토리지 삭제
	 * @param key
	 */
	deleteLocal(key) {
		localStorage.removeItem(key);
	}
	
	/**
	 * 로컬 스토리지 전체 삭제
	 */
	deleteAllLocal() {
		localStorage.clear();
	}
	
	/**
	 * 쿠키 등록
	 * @param key
	 * @param value
	 * @param duration 시간/일 단위
	 * @param unit 빈값 : 일 단위, hours : 시간 단위
	 */
	setCookie(key, value, duration, unit = 'days') {
		const expires = new Date();
		if (unit === 'hours') {
			expires.setTime(expires.getTime() + duration * 60 * 60 * 1000); // 시간 단위
		} else {
			expires.setTime(expires.getTime() + duration * 24 * 60 * 60 * 1000); // 기본: 일 단위
		}
		document.cookie = `${key}=${encodeURIComponent(value)};expires=${expires.toUTCString()};path=/`;
	}
	
	/**
	 * 쿠키 조회
	 * @param key
	 * @return {null|string}
	 */
	getCookie(key) {
		const cookieArr = document.cookie.split(';');
		for (let cookie of cookieArr) {
			const [cookieKey, cookieValue] = cookie.split('=');
			if (key === cookieKey.trim()) {
				return decodeURIComponent(cookieValue);
			}
		}
		return null;
	}
	
	/**
	 * 쿠키 삭제
	 * @param key
	 */
	deleteCookie(key) {
		const expires = new Date();
		expires.setTime(expires.getTime() - 1); // 과거 시간으로 설정하여 쿠키 만료
		document.cookie = `${key}=;expires=${expires.toUTCString()};path=/`;
	}
}

const storageUtil = new StorageUtil();
export default storageUtil;