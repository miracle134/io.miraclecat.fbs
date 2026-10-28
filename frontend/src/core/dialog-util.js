
/**
 * packageName    : static.js.core
 * fileName       : dialog-util
 * author         : MiracleCat
 * date           : 2025-07-03
 * description    : 다이얼로그 처리
 */

/**
 * DialogUtil 클래스
 * - 다이얼로그 창을 처리하는 유틸리티입니다.
 */

const OPEN = 'pm-is-open';
const ICONS = { info: 'i', success: '✓', warning: '!', error: '✕' };
const DEFAULT_FOOTER = '<button type="button" class="pm-btn pm-btn--secondary" data-pm-close>닫기</button>';

class DialogUtil {
    constructor() {
        this.root = null;
        this.pending = new Map();   // 열린 모달 → { resolve, dismissValue, lastFocus }
        this.legacyCount = 0;       // customOpen/customClose 전용
    }

    /* ---------- 내부: 공통 모달(alert/confirm/loading) DOM 확보 + 전역 리스너 1회 등록 ---------- */
    _build() {
        if (!this.root) {
            // modals.html(pm-root 프래그먼트)가 이미 페이지에 로딩되어 있어야 한다.
            this.root = document.querySelector('.pm-root');
            if (!this.root) {
                throw new Error('[modal.js] .pm-root를 찾을 수 없습니다. modals.html 공통 프래그먼트가 페이지에 로딩되어 있는지 확인하세요.');
            }
        }
        if (this.listenersReady) return;
        this.listenersReady = true;

        // 닫기: [data-pm-close] 클릭. document에 걸어서 pm-root 밖의 커스텀 다이얼로그도 함께 처리한다.
        // (배경 클릭으로는 닫히지 않음)
        document.addEventListener('click', (e) => {
            const modal = e.target.closest('.pm-modal');
            if (!modal) return;
            if (e.target.closest('[data-pm-close]')) this._close(modal);
        });

        // 확인 버튼 (공통 confirm 전용)
        this.root.querySelector('[data-pm="confirm-ok"]').addEventListener('click', () => {
            this._close(this._q('confirm'), true);
        });

        // ESC: 열려있는 모달 중 가장 위 티어(confirm/alert > 커스텀)를 닫는다.
        // 같은 티어가 여러 개 열려있으면 가장 나중에 연 것(Map 삽입 순서상 마지막)을 닫는다.
        document.addEventListener('keydown', (e) => {
            if (e.key !== 'Escape') return;
            const opened = Array.from(this.pending.keys()).filter((m) => m.classList.contains(OPEN));
            if (!opened.length) return;
            const topTier = opened.find((m) => m.dataset.pm === 'confirm' || m.dataset.pm === 'alert');
            this._close(topTier || opened[opened.length - 1]);
        });
    }

    _q(name) {
        this._build();
        return this.root.querySelector('[data-pm="' + name + '"]');
    }

    /* ---------- 내부: 열기 / 닫기 (공통 모달 + 커스텀 다이얼로그 공용) ---------- */
    _open(modal, dismissValue) {
        return new Promise((resolve) => {
            if (this.pending.has(modal)) this._close(modal);   // 이전 Promise가 pending으로 남지 않게 정리
            this.pending.set(modal, { resolve, dismissValue, lastFocus: document.activeElement });
            // 커스텀 다이얼로그 위에 커스텀 다이얼로그를 여는 경우 나중에 연 것이 위로 (confirm/alert 2050 미만)
            if (!modal.dataset.pm) {
                const stacked = Array.from(this.pending.keys()).filter((m) => !m.dataset.pm && m !== modal).length;
                modal.style.zIndex = String(2000 + Math.min(stacked, 40));
            }
            modal.classList.add(OPEN);
            modal.setAttribute('aria-hidden', 'false');
            lockScroll();
            const content = modal.querySelector('.pm-modal__content');
            if (content) content.focus();
        });
    }

    _close(modal, result) {
        const p = this.pending.get(modal);
        if (!p) return;
        this.pending.delete(modal);
        // 포커스를 먼저 빼낸 뒤 aria-hidden을 설정해야 한다.
        // 순서가 바뀌면 포커스가 모달 내부 버튼에 남아있는 상태로 aria-hidden=true가 걸려
        // "aria-hidden element has focus" 경고(접근성 위반)가 발생한다.
        if (p.lastFocus && p.lastFocus.focus) p.lastFocus.focus();
        // lastFocus가 포커스를 받을 수 없는 요소였다면 위 focus() 호출이 효과가 없어
        // 포커스가 여전히 모달 안에 남을 수 있다. 실제로 빠져나갔는지 다시 확인해서 강제로 blur한다.
        if (document.activeElement && modal.contains(document.activeElement)) {
            document.activeElement.blur();
        }
        modal.classList.remove(OPEN);
        modal.setAttribute('aria-hidden', 'true');
        if (!modal.dataset.pm) modal.style.zIndex = '';
        unlockScroll();
        p.resolve(result === undefined ? p.dismissValue : result);
    }

    /* ---------- Alert / Confirm (Promise) ---------- */
    // alert  : 닫히면 true
    // confirm: 확인 true / 취소·X·ESC false
    alert(msg, type = 'info') {
        const t = ICONS[type] ? type : 'info';
        const icon = this._q('alert-icon');
        icon.className = 'pm-modal__icon pm-modal__icon--' + t;
        icon.textContent = ICONS[t];
        this._q('alert-msg').textContent = msg;
        return this._open(this._q('alert'), true);
    }

    confirm(msg, type = 'warning') {
        const t = ICONS[type] ? type : 'warning';
        const icon = this._q('confirm-icon');
        icon.className = 'pm-modal__icon pm-modal__icon--' + t;
        icon.textContent = ICONS[t];
        this._q('confirm-msg').textContent = msg;
        return this._open(this._q('confirm'), false);
    }

    success(msg) { return this.alert(msg, 'success'); }
    error(msg)   { return this.alert(msg, 'error'); }
    warning(msg) { return this.alert(msg, 'warning'); }
    info(msg)    { return this.alert(msg, 'info'); }

    /* ---------- Loading ---------- */
    showLoading(msg = '처리 중입니다...') {
        this._q('loading-msg').textContent = msg;
        this._q('loading').classList.add(OPEN);
    }

    hideLoading() {
        if (this.root) this._q('loading').classList.remove(OPEN);
    }

    /* ---------- 페이지별 커스텀 다이얼로그 ----------
       - 대상 엘리먼트는 custom-md/lg/xl/fullscreen/form1/2/3.html 템플릿을 복사해
         각 페이지에 미리 import해둔 것이어야 한다 (pm-root 안에 있을 필요 없음).
       - 열기: 셀렉터(또는 id)로 찾아 _open과 동일한 방식으로 연다. 반환된 Promise는
         customClose(selector, result)를 호출하면 그 값으로 resolve된다.
       - ESC·data-pm-close·스크롤 잠금·포커스 복원은 공통 모달과 완전히 동일하게 동작한다. */
    customOpen(selector) {
        this._build();
        const modal = document.querySelector(selector);
        if (!modal) {
            console.error('[modal.js] customOpen: 요소를 찾을 수 없습니다 → ' + selector);
            return Promise.resolve(undefined);
        }
        return this._open(modal, undefined);
    }

    customClose(selector, result) {
        const modal = document.querySelector(selector);
        if (modal) this._close(modal, result);
    }

    // 지금 열려있는 모달을 전부 닫는다 (공통 + 커스텀 모두 대상)
    closeAll() {
        Array.from(this.pending.keys()).forEach((m) => this._close(m));
    }

}

let lockCount = 0;
function lockScroll() {
    if (lockCount === 0) {
        const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
        if (scrollbarWidth > 0) document.body.style.paddingRight = scrollbarWidth + 'px';
        document.body.classList.add('pm-modal-open');
    }
    lockCount++;
}
function unlockScroll() {
    lockCount = Math.max(0, lockCount - 1);
    if (lockCount === 0) {
        document.body.classList.remove('pm-modal-open');
        document.body.style.paddingRight = '';
    }
}

const dialogUtil = new DialogUtil();
export default dialogUtil;