/**
 * packageName    :
 * fileName       : password-reset
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 비밀번호 재설정 (메일 링크로 진입)
 */
import {ajax, dialog, dom, loadUtils} from '@core/core.js';

document.addEventListener("DOMContentLoaded", () => {
    initPasswordReset();
});

async function initPasswordReset() {
    const {validUtil, regexUtil} = await loadUtils(['validUtil', 'regexUtil']);

    const passwordReset = {
        init() {
            if (!dom.find("#resetForm")) return; // 만료된 링크
            this.bindEvents();
        },

        bindEvents() {
            dom.on("#btnResetPassword", "click",
                ajax.preventDisable(async () => {
                    await this.resetPassword();
                })
            );
        },

        /**
         * 새 비밀번호 저장
         */
        async resetPassword() {
            const items = {
                newPassword: dom.getValue("#newPassword"),
                newPasswordConfirm: dom.getValue("#newPasswordConfirm"),
            };

            const requiredErrors = validUtil.requiredError(items, {
                newPassword: "새 비밀번호를 입력해 주세요.",
                newPasswordConfirm: "새 비밀번호 확인을 입력해 주세요.",
            });
            if (requiredErrors) {
                await dialog.warning(requiredErrors);
                return;
            }

            const rulesError = validUtil.rulesError(items, {
                newPassword: [
                    v => regexUtil.isValidPassword(v, {requireUppercase: false}) || '비밀번호는 8자리 이상 영문, 숫자, 특수문자가 포함되어야 합니다.',
                    v => v === items.newPasswordConfirm || '확인 비밀번호가 일치하지 않습니다.'
                ]
            });
            if (rulesError) {
                await dialog.warning(rulesError);
                return;
            }

            try {
                const res = await ajax.postAjax({
                    token: window.resetToken,
                    newPassword: items.newPassword
                }, "/auth/password/reset", true);
                await dialog.success(res?.message || "비밀번호가 변경되었습니다.");
                location.href = "/auth/login";
            } catch (error) {
                await dialog.warning(error?.message || "비밀번호를 변경하지 못했습니다.");
            }
        },
    };

    passwordReset.init();
}
