/**

 * packageName    : static.js.core
 * fileName       : ajax-util
 * author         : MiracleCat 
 * date           : 2025-07-03
 * description    : Ajax 요청을 처리
 */
import axios from "axios";
import dialogUtil from '@core/dialog-util.js';

/**
 * AjaxUtil 클래스
 * - Ajax 요청을 처리하는 유틸리티 클래스입니다.
 *
 * 주요 메서드:
 * - `getAjax(data, url, isLoading)`: GET 요청을 수행합니다.
 * - `postAjax(data, url, isLoading)`: POST 요청을 수행합니다.
 * - `putAjax(data, url, isLoading)`: PUT 요청을 수행합니다.
 * - `patchAjax(data, url, isLoading)`: PATCH 요청을 수행합니다.
 * - `deleteAjax(data, url, isLoading)`: DELETE 요청을 수행합니다.
 * - `multiPartAjax(data, url, isLoading)`: MULTI_PART 요청을 수행합니다.
 *
 */
const popLoadingId = "popLoading";

class AjaxUtil {

    // GET 요청
    async getAjax(data, url, isLoading = false) {
        return await makeAjaxRequest("GET", data, url, isLoading);
    }

    // POST 요청
    async postAjax(data, url, isLoading = false) {
        return await makeAjaxRequest("POST", data, url, isLoading);
    }

    // PUT 요청
    async putAjax(data, url, isLoading = false) {
        return await makeAjaxRequest("PUT", data, url, isLoading);
    }

    // PATCH 요청
    async patchAjax(data, url, isLoading = false) {
        return await makeAjaxRequest("PATCH", data, url, isLoading);
    }

    // DELETE 요청
    async deleteAjax(data, url, isLoading = false) {
        return await makeAjaxRequest("DELETE", data, url, isLoading);
    }

    // MULTIPART 요청 (Upload)
    async multiPartAjax(data, url, isLoading = true) {
        return await multiPartAjaxRequest(data, url, isLoading);
    }

    // FILE DOWN 요청
    async downloadAjax(method, data, url, isLoading = true) {
        return await downloadAjax(method, data, url, isLoading);
    }

    // FILE DOWN 요청 (대용량 stream)
    async downloadStreamAjax(method, data, url, isLoading = true) {
        return await downloadStreamAjax(method, data, url, isLoading);
    }

    // Html Template 요청
    async loadPartialAjax(url, isLoading = true) {
        return await loadPartialRequest(url, isLoading);
    }

    // ajax-util.js에 추가
    preventClick(asyncFunction) {
        let isExecuting = false;

        return async function(...args) {
            if (isExecuting) return;

            isExecuting = true;
            try {
                return await asyncFunction.apply(this, args);
            } finally {
                isExecuting = false;
            }
        };
    }

    // 버튼 자동 disable
    preventDisable(asyncFunction) {
        let lastClick = 0;
        let isExecuting = false;

        return async function(event) {
            const now = Date.now();

            // 300ms 이내 클릭 무시
            if (now - lastClick < 300) return;
            if (isExecuting) return;

            lastClick = now;
            isExecuting = true;

            const btn = event?.currentTarget || event?.target;
            if (btn) btn.disabled = true;

            try {
                return await asyncFunction.call(this, event);
            } finally {
                isExecuting = false;
                if (btn) btn.disabled = false;
            }
        };
    }

}

/**
 * Ajax 요청을 만드는 메서드
 * @param {'GET'|'POST'|'PUT'|'PATCH'|'DELETE'} method - HTTP Method
 * @param {Object} data - 요청 데이터
 * @param {string} url - Controller 경로
 * @param {boolean} isLoading - 로딩 다이얼로그 표시 여부
 */
const makeAjaxRequest = async (method, data, url, isLoading) => {
    const m = (method || 'GET').toUpperCase();
    const isGet = (m === 'GET');

    try {
        if (isLoading) dialogUtil.showLoading("처리중");

        const response = await axios({
            url,
            method,
            data: isGet ? null : (data ?? null),
            params: isGet ? (data ?? null) : null,
            headers: {
                "Content-Type": "application/json",
                "Ajax-Request": "Ajax"
            },
            responseType: "json"
        });

        return response.data;
    } catch (error) {
        // 통합 오류 처리
        await handleAxiosError(error);
        return false;
    } finally {
        if (isLoading) dialogUtil.hideLoading();
    }
}

/**
 * 멀티파트 Ajax 요청 메서드
 * @param {Object} data - 요청 데이터
 * @param {string} url - Controller 경로
 * @param {boolean} isLoading - 로딩 다이얼로그 표시 여부
 */
const multiPartAjaxRequest = async (data, url, isLoading) => {

    try {
        if (isLoading) dialogUtil.showLoading("처리중");

        const response = await axios.post(url, data, {
            headers: {"Ajax-Request": "Ajax"},
            responseType: "json"
        });

        return response.data;
    } catch (error) {
        await handleAxiosError(error);
        return false;
    } finally {
        if (isLoading) dialogUtil.hideLoading();
    }
};

/**
 * Html Template 요청 메서드
 * @param {string} url - Controller 경로
 * @param {boolean} isLoading - 로딩 다이얼로그 표시 여부
 */
const loadPartialRequest = async (url, isLoading) => {

    try {
        if (isLoading) dialogUtil.showLoading("처리중");
        const res = await fetch(url, {
            method: "GET",
            headers: {
                "X-Requested-With": "XMLHttpRequest",
                "Ajax-Request": "Ajax",
                "Accept": "text/html"
            }
        });

        if (!res.ok) throw await toAxiosLikeError(res, url, 'GET');

        return await res.text();
    } catch (error) {
        await handleAxiosError(error);
        return false;
    } finally {
        if (isLoading) dialogUtil.hideLoading();
    }
};

/**
 * 파일 다운로드 Ajax 요청 메서드
 * @param {'GET'|'POST'} method - HTTP Method
 * @param {Object} data - 요청 데이터
 * @param {string} url - Controller 경로
 * @param {boolean} isLoading - 로딩 다이얼로그 표시 여부
 */
const downloadAjax = async (method, data, url, isLoading) => {
    const m = (method || 'GET').toUpperCase();
    const isGet = (m === 'GET');

    try {
        if (isLoading) dialogUtil.showLoading("처리중");

        const response = await axios({
            url,
            method,
            data: isGet ? null : (data ?? null),
            params: isGet ? (data ?? null) : null,
            headers: {
                "Ajax-Request": "Ajax"
            },
            responseType: 'blob'
        });

        const blob = new Blob([response.data], {type: response.headers['content-type'] || 'application/octet-stream'});
        let fileName = getFileNameFromHeader(response.headers['content-disposition']);
        if (!fileName) {
            try {
                const u = new URL(url, location.origin);
                const last = u.pathname.split('/').filter(Boolean).pop();
                fileName = last || 'download.bin';
            } catch {
                fileName = 'download.bin';
            }
        }

        saveBlobToFile(blob, fileName);
        return fileName;
    } catch (error) {
        await handleAxiosError(error, {context: 'file'});
        return false;
    } finally {
        if (isLoading) dialogUtil.hideLoading();
    }
}

/**
 * 파일 다운로드 Ajax (진행률/취소 지원)
 * - 진행률 요소: #progress, #pct, #bytes, #total (있으면 갱신)
 * - 취소: 반환된 controller.abort() 호출
 * @param {'GET'|'POST'} method
 * @param {Object|null} data
 * @param {string} url
 * @param {boolean} [isLoading]
 * @returns {Promise<{fileName: string, controller: AbortController}>}
 */
const downloadStreamAjax = async (method, data, url, isLoading) => {
    const m = (method || 'GET').toUpperCase();
    const isGet = (m === 'GET');
    const controller = new AbortController();

    const human = (n) => (typeof n === 'number' ? n.toLocaleString() : n);

    try {
        if (isLoading) dialogUtil.showLoading("처리중");

        // 진행률 UI 초기화(선택)
        const $progress = document.getElementById('progress');
        const $pct = document.getElementById('pct');
        const $bytes = document.getElementById('bytes');
        const $total = document.getElementById('total');
        if ($progress) {
            $progress.style.display = '';
            if ($pct) $pct.textContent = '0%';
            if ($bytes) $bytes.textContent = '0';
            if ($total) $total.textContent = '?';
        }

        const headers = {
            'Accept': 'application/octet-stream,application/pdf,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,*/*',
            'Ajax-Request': 'Ajax'
        };
        if (!isGet) headers['Content-Type'] = 'application/json';

        const res = await axios({
            url,
            method: m,
            data: isGet ? null : (data ?? null),
            params: isGet ? (data ?? null) : null,
            headers,
            responseType: 'blob',
            signal: controller.signal,
            onDownloadProgress: function (e) {
                if (!$progress) return;
                if (e.lengthComputable) {
                    const pct = Math.round((e.loaded / e.total) * 100);
                    if ($pct) $pct.textContent = pct + '%';
                    if ($bytes) $bytes.textContent = human(e.loaded);
                    if ($total) $total.textContent = human(e.total);
                } else {
                    if ($pct) $pct.textContent = '...';
                    if ($bytes) $bytes.textContent = human(e.loaded);
                }
            }
        });

        const blob = new Blob([res.data], {type: res.headers['content-type'] || 'application/octet-stream'});
        let fileName = getFileNameFromHeader(res.headers['content-disposition']);
        if (!fileName) {
            try {
                const u = new URL(url, location.origin);
                const last = u.pathname.split('/').filter(Boolean).pop();
                fileName = last || 'download.bin';
            } catch {
                fileName = 'download.bin';
            }
        }

        saveBlobToFile(blob, fileName);
        return {fileName, controller};

    } catch (error) {
        // 사용자 취소는 오류 안내 없이 종료
        const canceled = error?.name === 'CanceledError' || error?.message === 'canceled';
        if (!canceled) await handleAxiosError(error, {context: 'file'});
        throw error;
    } finally {
        const $progress = document.getElementById('progress');
        if ($progress) $progress.style.display = 'none';

        if (isLoading) dialogUtil.hideLoading();
    }

};

// Content-Disposition에서 filename 추출
const getFileNameFromHeader = (cd) => {
    if (!cd) return null;
    // filename*=UTF-8''encodedName
    const m1 = cd.match(/filename\*\s*=\s*UTF-8''([^;]+)/i);
    if (m1 && m1[1]) return decodeURIComponent(m1[1].trim());
    // filename="..."
    const m2 = cd.match(/filename\s*=\s*"([^"]+)"/i);
    if (m2 && m2[1]) return m2[1].trim();
    // filename=bare
    const m3 = cd.match(/filename\s*=\s*([^;]+)/i);
    if (m3 && m3[1]) return m3[1].trim();
    return null;
}

const saveBlobToFile = (blob, fileName) => {
    if (window.navigator && window.navigator.msSaveOrOpenBlob) {
        window.navigator.msSaveOrOpenBlob(blob, fileName);
        return;
    }
    const objectUrl = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = objectUrl;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(objectUrl);
}

/**
 * fetch Response -> Axios와 비슷한 에러 객체로 변환
 *  - handleAxiosError에서 status/headers/data 접근 가능하도록 맞춤
 */
const toAxiosLikeError = async (res, url, method) => {
    const contentType = res.headers.get('content-type') || '';
    const isJson = contentType.includes('application/json');
    const data = isJson
        ? await res.json().catch(() => null)
        : await res.text().catch(() => null);

    return {
        name: 'AxiosLikeError',
        message: `Request failed with status code ${res.status}`,
        code: String(res.status), // Axios 관례상 문자열/숫자 혼용
        isAxiosError: true,                    // 식별 플래그
        config: {url, method},
        request: {url, method},
        response: {
            status: res.status,
            statusText: res.statusText,
            headers: Object.fromEntries(res.headers.entries()),
            data, // JSON or 텍스트
        },
    };
}

/**
 * 통합 오류 처리
 * @param {any} error
 * @param {{context?: 'file'|'default', silent?: boolean}} [opt]
 * @throws normalized Error
 */
const handleAxiosError = async (error, opt = {}) => {
    const {context = 'default', silent = false} = opt;
    const resp = error?.response;
    let status = resp?.status;
    let message;
    let code;

    // 1) 에러 바디 추출/표준화 (Blob/Text/JSON) — 서버 메시지가 없으면 상태 코드별 기본 문구
    if (resp?.data instanceof Blob) {
        try {
            const text = await resp.data.text();
            try {
                const json = JSON.parse(text);
                code = json.code;
                message = json.message;
            } catch {
                message = null;
            }
        } catch {
            message = null;
        }
    } else {
        code = resp?.data?.code;
        message = resp?.data?.message;
    }
    if (!message) message = defaultErrorMessage(status);

    // 2) 공통 로깅
    console.error('AJAX Error:', {status, context, error});
    console.error(`Message: ${message}`);

    // 3) 공통 가드
    if (status === 403) {
        let redirectUrl = '/';
        if (code === 'ACCESS_DENIED') {
            if (!silent) dialogUtil.alert('권한이 없습니다. 관리자에게 문의해주세요.');
        } else if (code === 'LOGIN_REQUIRED') {
            if (!silent) dialogUtil.alert('로그인이 필요합니다. 로그인 페이지로 이동합니다.');
            redirectUrl = `/auth/login?redirect=${encodeURIComponent(location.pathname + location.search)}`;
        } else {
            if (!silent) dialogUtil.alert('장시간 미사용으로 로그아웃 되었습니다.');
        }
        setTimeout(() => (location.href = redirectUrl), 1500);
        throw new HandledForbiddenError();
    }

    // 4) 컨텍스트 예: 파일 전용 후처리 필요 시 분기
    if (context === 'file') {
        // 예: 파일 컨텍스트에서만 다른 토스트 문구
        if (!silent) dialogUtil.alert(message);
    }

    // 5) 최종 throw (메시지 덧붙여 전달)
    const enriched = new Error(message);
    enriched.original = error;
    enriched.status = status;
    enriched.code = code;
    throw enriched;
};

/**
 * 서버 메시지가 없을 때 사용할 상태 코드별 기본 문구
 */
const defaultErrorMessage = (status) => {
    if (!status) return '서버와 연결할 수 없습니다. 네트워크 상태를 확인해주세요.';
    if (status === 400) return '요청 값이 올바르지 않습니다.';
    if (status === 404) return '요청한 정보를 찾을 수 없습니다.';
    if (status === 408) return '요청 시간이 초과되었습니다.';
    if (status === 409) return '이미 처리된 요청입니다.';
    if (status >= 500) return '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
    return '요청을 처리할 수 없습니다.';
};

class HandledForbiddenError extends Error {
    constructor() {
        super("__FORBIDDEN_HANDLED__");
        this.name = "HandledForbiddenError";
    }
}

const ajaxUtil = new AjaxUtil();
export default ajaxUtil;