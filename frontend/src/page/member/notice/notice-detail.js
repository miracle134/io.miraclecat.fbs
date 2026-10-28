/**
 * packageName    :
 * fileName       : notice-detail
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 공지사항 상세 (사용자)
 */
import {dialog, dom, loadApis} from '@core/core.js';

document.addEventListener("DOMContentLoaded", () => {
    initNoticeDetail();
});

async function initNoticeDetail() {
    const {noticeApi} = await loadApis(['noticeApi']);

    try {
        const res = await noticeApi.getNoticeDetail(window.noticeId, true);
        const n = res?.result;
        if (!n) {
            await dialog.warning("존재하지 않거나 비공개된 공지사항입니다.");
            location.href = "/notice";
            return;
        }

        dom.setText("#noticeTitle", n.title ?? "");
        dom.setText("#noticeDate", n.createdAt?.substring(0, 10) ?? "");
        dom.setText("#noticeViews", n.views ?? 0);
        dom.setText("#noticeContent", n.content ?? "");
        dom.find("#noticeImportant").classList.toggle("d-none", !n.isImportant);
        document.title = `${n.title} - 공지사항`;
    } catch (error) {
        await dialog.warning(error?.message || "공지사항을 불러오지 못했습니다.");
        location.href = "/notice";
    }
}
