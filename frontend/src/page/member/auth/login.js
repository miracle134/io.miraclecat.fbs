/**

 * packageName    : statijs.page.auth
 * fileName       : login
 * author         : MiracleCat
 * date           : 2025-07-04
 * description    :
 */
// Utils
import {ajax, dialog, dom, global, loadApis, loadUtils} from '@core/core.js';

document.addEventListener("DOMContentLoaded", () => {
    initLogin();
});

/**
 * 로그인 후 이동할 내부 경로 (외부 URL, /auth 경로는 차단)
 */
function getSafeRedirect() {
    const r = new URLSearchParams(location.search).get('redirect');
    if (!r || !r.startsWith('/') || r.startsWith('//') || r.startsWith('/\\') || r.startsWith('/auth')) return null;
    return r;
}

function googleLoginUrl() {
    const redirect = getSafeRedirect();
    return redirect
        ? `/oauth2/authorization/google?redirect=${encodeURIComponent(redirect)}`
        : '/oauth2/authorization/google';
}

const OAUTH_ERROR_MESSAGES = {
    oauth_failed: '구글 로그인에 실패했습니다. 다시 시도해주세요.',
    withdrawn: '탈퇴한 계정입니다.',
    suspended: '이용이 정지된 계정입니다. 고객센터에 문의해주세요.',
    email_unverified: '이메일 인증이 완료되지 않은 구글 계정입니다.',
    already_linked: '이미 다른 소셜 계정이 연결된 회원입니다.',
    no_email: '구글 계정의 이메일 정보를 가져올 수 없습니다.',
};

async function initLogin() {
	
	// 추가 Utils & Module
	const {validUtil, storageUtil, stringUtil, regexUtil} = await loadUtils(['validUtil', 'storageUtil', 'stringUtil', 'regexUtil']);
	const {memberApi} = await loadApis(['memberApi']);
	const login = {
		/**
		 * 로그인 init
		 */
		async init() {
			// 초기 값 설정
			await this.initValues();
			
			// 이벤트 핸들러
			this.bindEvents();
		},
		
		/**
		 * 초기값 설정
		 */
		async initValues() {
			let id = storageUtil.getLocalWithExpire("idSave");
			if(!stringUtil.isBlank(id)){
				dom.setValue("#loginId", id);
				dom.setChecked("#idSave", true);
			}

			// 회원가입 링크에 redirect 유지
			const redirect = getSafeRedirect();
			if (redirect) {
				dom.find("#signupLink").href = `/member/signup?redirect=${encodeURIComponent(redirect)}`;
			}

			// 소셜 로그인 실패 안내
			const error = new URLSearchParams(location.search).get('error');
			if (error) {
				dialog.warning(OAUTH_ERROR_MESSAGES[error] || OAUTH_ERROR_MESSAGES.oauth_failed);
			}
		},
		
		/**
		 * 이벤트 등록
		 */
		bindEvents() {
			/**
             * 엔터 이벤트
             */
			global.enterInstance("#loginPassword", () => {
				this.loginEvent();
			},false, true);
			
			/**
             * 버튼 이벤트
             */
			dom.on("#loginButton", "click",
                ajax.preventDisable(async () => {
                    this.loginEvent();
                })
			);

			// 구글 로그인
			dom.on("#btnGoogleLogin", "click", () => {
				location.href = googleLoginUrl();
			});

			// 입력 제어
			stringUtil.setInputEmail("#forgotEmail", 64);

			// 비밀번호 찾기 팝업
			dom.on("#linkForgotPassword", "click", (e) => {
				e.preventDefault();
				dom.setValue("#forgotEmail", dom.getValue("#loginId"));
				dialog.customOpen("#popForgotPassword");
			});

			// 재설정 메일 발송
			dom.on("#btnSendResetMail", "click",
				ajax.preventDisable(async () => {
					await this.sendResetMail();
				})
			);
		},

		/**
		 * 비밀번호 재설정 메일 요청
		 */
		async sendResetMail() {
			const email = dom.getValue("#forgotEmail")?.trim();
			if (!email) {
				await dialog.warning("이메일을 입력해 주세요.");
				return;
			}
			if (!regexUtil.isEmail(email)) {
				await dialog.warning("이메일 형식이 올바르지 않습니다.");
				return;
			}

			try {
				const res = await ajax.postAjax({email}, "/auth/password/forgot", true);
				dialog.customClose("#popForgotPassword");
				await dialog.success(res?.message || "비밀번호 재설정 메일을 발송했습니다.");
			} catch (error) {
				await dialog.warning(error?.message || "메일을 발송하지 못했습니다.");
			}
		},
		
		/**
		 * 로그인 Event
		 * @return {boolean}
		 */
		async loginEvent() {
			try {
				// 데이터
				const loginMember = {
					loginId: dom.getValue("#loginId"),
					loginPassword: dom.getValue("#loginPassword")
				};
				
				// 1차 검증
				const required = {
				    loginId: "아이디를 입력해 주세요.",
					loginPassword: "비밀번호를 입력해 주세요."
				}
				const requiredErrors = validUtil.requiredError(loginMember, required);
				if (requiredErrors) {
					await dialog.warning(requiredErrors);
					return false;
				}
				
				await ajax.postAjax(loginMember, "/auth/loginProcess");

                // 아이디 저장
                if(dom.getChecked("#idSave")) storageUtil.setLocalWithExpire("idSave", loginMember.loginId, 2629743);

                location.href = getSafeRedirect() || "/";

			} catch (error) {
                dialog.warning(error?.message || "아이디와 비밀번호를 확인해주세요.");
			}
		},
        
	};

	// init start
	login.init();
	
}