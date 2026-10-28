/**

 * packageName    :
 * fileName       : signUp
 * author         : MiracleCat
 * date           : 2026-09-22
 * description    : 회원가입
 */
// Utils
import {ajax, dialog, dom, global, loadApis, loadUtils} from '@core/core.js';
import regexUtil from "@core/regex-util";
import {getChecked} from "@core/dom-util";

document.addEventListener("DOMContentLoaded", () => {
    initSignUp();
});

/**
 * 로그인 후 이동할 내부 경로 (외부 URL, /auth 경로는 차단)
 */
function getSafeRedirect() {
    const r = new URLSearchParams(location.search).get('redirect');
    if (!r || !r.startsWith('/') || r.startsWith('//') || r.startsWith('/\\') || r.startsWith('/auth')) return null;
    return r;
}

function loginUrl() {
    const redirect = getSafeRedirect();
    return redirect ? `/auth/login?redirect=${encodeURIComponent(redirect)}` : "/auth/login";
}

async function initSignUp() {
    // 추가 Utils & Module
    const {validUtil, regexUtil, stringUtil} = await loadUtils(
        ['validUtil', 'regexUtil', 'stringUtil']
    );
    const {memberApi} = await loadApis(
        ['memberApi']
    );
    const signUp = {
        /**
         * 초기화
         */
        async init() {

            // 초기 값 설정
            this.initValues();

            // 이벤트 핸들러
            this.bindEvents();
        },

        /**
         * 초기값 설정
         */
        initValues() {
            // 로그인 링크에 redirect 유지
            dom.find("#loginLink").href = loginUrl();
        },

        /**
         * 이벤트 등록
         */
        bindEvents() {

            /**
             * 입력 제어
             */
            stringUtil.setInputEmail("#loginId", 64);
            stringUtil.setInputPhoneNumber("#mobile");

            /**
             * 버튼 이벤트
             */
            // 회원 가입
            dom.on("#btnRegister", "click", async () => {
                await this.handleCreateMember();
            });

            // 구글로 가입/로그인
            dom.on("#btnGoogleLogin", "click", () => {
                const redirect = getSafeRedirect();
                location.href = redirect
                    ? `/oauth2/authorization/google?redirect=${encodeURIComponent(redirect)}`
                    : '/oauth2/authorization/google';
            });


        },

        /**
         * 회원 생성
         */
        async handleCreateMember() {
            try {
                // 데이터
                const newMember = {
                    loginId: dom.getValue("#loginId"),
                    loginPassword: dom.getValue("#loginPassword"),
                    passwordConfirm: dom.getValue("#passwordConfirm"),
                    memberName: dom.getValue("#memberName"),
                    mobile: dom.getValue("#mobile"),
                    agreeTerms: dom.getChecked("#agreeTerms")
                };

                // 1차 검증
                const required = {
                    loginId: "아이디를 입력해 주세요.",
                    loginPassword: "비밀번호를 입력해 해주세요.",
                    passwordConfirm: "비밀번호 확인을 입력해 해주세요.",
                    memberName: "이름을 입력해 주세요.",
                }
                const requiredErrors = validUtil.requiredError(newMember, required);
                if (requiredErrors) {
                    await dialog.warning(requiredErrors);
                    return false;
                }

                // 2차 검증
                let rules = {
                    loginId: [
                        v => regexUtil.isEmail(v) || '아이디를 확인해주세요.',
                    ],
                    loginPassword: [
                        v => regexUtil.isValidPassword(v, {requireUppercase: false}) || '비밀번호는 8자리 이상 영문 대소문자, 숫자, 특수문자가 포함되어야 합니다.',
                        v => v === newMember.passwordConfirm || '확인 비밀번호가 일치하지 않습니다.'
                    ],
                    mobile: [
                        v => !v || regexUtil.isContactNumber(v) || '전화번호 형식이 올바르지 않습니다.'
                    ],
                    agreeTerms: [
                        v => v === true || '약관에 동의 하셔야 가입이 가능합니다.'
                    ]
                };
                const rulesError = validUtil.rulesError(newMember, rules);
                if (rulesError) {
                    await dialog.warning(rulesError);
                    return false;
                }

                // 회원 생성
                await memberApi.createMember(newMember);

                // 성공 알림
                if(await dialog.info(`회원이 등록 되었습니다. 로그인을 해주세요`)){
                    location.href = loginUrl();
                };

            } catch (error) {
                if (global.isHandledError(error)) return;

                console.error("handleCreateMember error", error);
                if (error.status === 409) {
                    await dialog.warning("중복된 아이디입니다.");
                } else {
                    await dialog.error("회원 등록 처리 중 오류가 발생했습니다.");
                }
            }
        },

    };

    // init start
    await signUp.init();

}