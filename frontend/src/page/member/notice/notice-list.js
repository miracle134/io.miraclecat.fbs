/**
 * packageName    :
 * fileName       : notice-list
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 공지사항 목록 (사용자, 게시 공지만 — 서버에서 고정)
 */
import {dialog, dom, loadApis, loadUtils} from '@core/core.js';

const PAGE_SIZE = 10;

document.addEventListener("DOMContentLoaded", () => {
    initNoticeList();
});

async function initNoticeList() {
    const {pagingUtil} = await loadUtils(['pagingUtil']);
    const {noticeApi} = await loadApis(['noticeApi']);

    const noticeList = {
        _page: 1,

        async init() {
            this.bindEvents();
            await this.loadNotices();
        },

        bindEvents() {
            dom.on("#btnSearch", "click", async () => {
                this._page = 1;
                await this.loadNotices();
            });
            dom.on("#searchTitle", "keydown", async (e) => {
                if (e.key !== 'Enter') return;
                this._page = 1;
                await this.loadNotices();
            });

            pagingUtil.initPaginationListener(async (page) => {
                this._page = page;
                await this.loadNotices();
                window.scrollTo({top: 0, behavior: 'smooth'});
            }, "pagination");
        },

        async loadNotices() {
            try {
                const title = dom.getValue("#searchTitle");
                const res = await noticeApi.getNotices({
                    page: this._page - 1,
                    size: PAGE_SIZE,
                    ...(title ? {title} : {})
                }, true);
                const page = res?.result;
                this.render(page?.content ?? [], page?.totalCount ?? 0);
            } catch (error) {
                console.error("loadNotices error", error);
                await dialog.warning(error?.message || '공지사항을 불러오지 못했습니다.');
            }
        },

        render(notices, totalCount) {
            const body = dom.find("#noticeList");
            const template = dom.find("#noticeItemTemplate");
            body.innerHTML = "";
            dom.setText("#totalCount", totalCount);
            dom.find("#emptyState").classList.toggle("d-none", notices.length > 0);

            notices.forEach((n) => {
                const clone = template.content.cloneNode(true);
                clone.querySelector(".notice-item").href = `/notice/${n.id}`;
                dom.setNodeText(clone, ".notice-title", n.title ?? "");
                dom.setNodeText(clone, ".notice-date", n.createdAt?.substring(0, 10) ?? "");
                dom.setNodeText(clone, ".notice-views", n.views ?? 0);
                if (n.isImportant) {
                    clone.querySelector(".notice-important").classList.remove("d-none");
                    clone.querySelector(".notice-item").classList.add("bg-light");
                }
                body.appendChild(clone);
            });

            dom.find("#pagination").innerHTML = totalCount > 0
                ? pagingUtil.setBootstrapPaging(this._page, totalCount, PAGE_SIZE)
                : "";
        },
    };

    await noticeList.init();
}
