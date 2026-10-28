/**
 * packageName    :
 * fileName       : login
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 로그인 (일반 로그인 처리 재사용 + 관리자 권한 확인)
 */
import {ajax, dialog, dom, global, loadUtils} from '@core/core.js';

const ADMIN_ROLES = ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN'];

document.addEventListener("DOMContentLoaded", () => {
    initAdminLogin();
});

async function initAdminLogin() {
    const {validUtil} = await loadUtils(['validUtil']);

    const adminLogin = {
        init() {
            dom.find("#adminId")?.focus();
            this.bindEvents();
        },

        bindEvents() {
            dom.on("#btnAdminLogin", "click",
                ajax.preventDisable(async () => {
                    await this.login();
                })
            );

            global.enterInstance("#adminPassword", () => {
                dom.find("#btnAdminLogin").click();
            }, false, true);
        },

        async login() {
            const items = {
                loginId: dom.getValue("#adminId"),
                loginPassword: dom.getValue("#adminPassword", false)
            };

            const requiredErrors = validUtil.requiredError(items, {
                loginId: "아이디를 입력해 주세요.",
                loginPassword: "비밀번호를 입력해 주세요."
            });
            if (requiredErrors) {
                await dialog.warning(requiredErrors);
                return;
            }

            let res;
            try {
                res = await ajax.postAjax(items, "/auth/loginProcess", true);
            } catch (error) {
                console.error("admin login error", error);
                await dialog.warning(error?.message || "아이디와 비밀번호를 확인해주세요.");
                return;
            }

            // 관리자 권한이 아니면 발급된 토큰을 지우고 차단
            if (!ADMIN_ROLES.includes(res?.result?.role)) {
                await fetch("/auth/logout", {credentials: "same-origin"}).catch(() => null);
                await dialog.warning("관리자 계정이 아닙니다.");
                dom.setValue("#adminPassword", "");
                return;
            }

            location.href = "/admin/dashboard";
        },
    };

    adminLogin.init();
}
