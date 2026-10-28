/**
 * packageName    :
 * fileName       : notices
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 공지사항 관리 (목록 / 상세 / 등록 / 수정 / 숨김)
 */
import {ajax, dialog, dom, loadApis, loadUtils} from '@core/core.js';

const STATUS_VIEW = {
    published: {label: '게시중', badge: 'admin-badge-success'},
    hidden: {label: '숨김', badge: 'admin-badge-danger'}
};
const COLSPAN = 8;

document.addEventListener("DOMContentLoaded", () => {
    initNotices();
});

async function initNotices() {
    const {pagingUtil, stringUtil, validUtil} = await loadUtils(['pagingUtil', 'stringUtil', 'validUtil']);
    const {noticeApi} = await loadApis(['noticeApi']);

    const notices = {
        _page: 1,
        _editId: null,     // null: 등록, 값: 수정
        _detailId: null,

        async init() {
            this.bindEvents();
            await this.loadNotices();
        },

        bindEvents() {
            // 검색 / 초기화
            dom.on("#btnSearch", "click", () => this.search());
            ["#filterTitle", "#filterAuthor"].forEach((id) =>
                dom.on(id, "keydown", (e) => { if (e.key === 'Enter') this.search(); }));
            dom.on("#btnReset", "click", () => {
                ["#filterTitle", "#filterAuthor", "#filterStartDate", "#filterEndDate", "#filterStatus"]
                    .forEach((id) => dom.setValue(id, ""));
                this.search();
            });

            // 페이징 / 페이지당 건수
            pagingUtil.initPaginationListener(async (page) => {
                this._page = page;
                await this.loadNotices();
            }, "pagination");
            dom.on("#pageSize", "change", () => this.search());

            // 등록
            dom.on("#btnAddNotice", "click", () => this.openForm(null));
            dom.on("#btnSaveNotice", "click", ajax.preventDisable(async () => {
                await this.saveNotice();
            }));

            // 상세 → 수정
            dom.on("#btnEditFromDetail", "click", async () => {
                const id = this._detailId;
                dialog.customClose("#popNoticeDetail");
                await this.openForm(id);
            });

            // 행 버튼 (이벤트 위임)
            dom.delegate("#noticeTableBody", "click", "button[data-id]", async (e) => {
                const btn = e.target.closest("button[data-id]");
                const id = Number(btn.dataset.id);
                if (btn.classList.contains("btn-detail")) await this.openDetail(id);
                else if (btn.classList.contains("btn-edit")) await this.openForm(id);
                else if (btn.classList.contains("btn-delete")) await this.deleteNotice(id);
            });
        },

        async search() {
            this._page = 1;
            await this.loadNotices();
        },

        pageSize() {
            return Number(dom.getValue("#pageSize")) || 10;
        },

        /**
         * 목록 조회
         */
        async loadNotices() {
            const startDate = dom.getValue("#filterStartDate");
            const endDate = dom.getValue("#filterEndDate");
            if (startDate && endDate && startDate > endDate) {
                await dialog.warning("시작일이 종료일보다 늦습니다.");
                return;
            }

            const params = {
                page: this._page - 1,
                size: this.pageSize(),
                title: dom.getValue("#filterTitle"),
                authorName: dom.getValue("#filterAuthor"),
                status: dom.getValue("#filterStatus"),
                startDate,
                endDate
            };
            Object.keys(params).forEach((k) => { if (params[k] === "" || params[k] == null) delete params[k]; });

            try {
                const res = await noticeApi.getAdminNotices(params, true);
                const page = res?.result;
                this.render(page?.content ?? [], page?.totalCount ?? 0);
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '공지사항을 불러오지 못했습니다.');
            }
        },

        render(list, totalCount) {
            const body = dom.find("#noticeTableBody");
            const template = dom.find("#noticeRowTemplate");
            body.innerHTML = "";
            dom.setText("#totalCount", stringUtil.addComma(totalCount) || "0");

            if (list.length === 0) {
                body.innerHTML = `<tr class="admin-table-empty"><td colspan="${COLSPAN}">조회된 공지사항이 없습니다.</td></tr>`;
            }

            list.forEach((n, i) => {
                const clone = template.content.cloneNode(true);
                const status = STATUS_VIEW[n.status] ?? {label: n.status ?? '-', badge: 'admin-badge-primary'};

                dom.setNodeText(clone, ".row-no", pagingUtil.pageIndex(i, this._page, this.pageSize()));
                clone.querySelector(".row-important").innerHTML = n.isImportant
                    ? '<i class="bi bi-pin-angle-fill" style="color: #e74c3c; font-size: 1.125rem;"></i>'
                    : '<span style="color: #dcdde1;">-</span>';

                const title = document.createElement(n.isImportant ? "strong" : "span");
                title.textContent = n.title ?? "";
                if (n.status === 'hidden') title.style.color = '#95a5a6';
                clone.querySelector(".row-title").appendChild(title);

                dom.setNodeText(clone, ".row-author", n.authorName ?? "-");
                dom.setNodeText(clone, ".row-date", n.createdAt?.substring(0, 10) ?? "");
                dom.setNodeText(clone, ".row-views", stringUtil.addComma(n.views ?? 0) || "0");

                const badge = clone.querySelector(".row-status");
                badge.textContent = status.label;
                badge.classList.add(status.badge);

                clone.querySelectorAll("button").forEach((b) => b.dataset.id = n.id);
                if (n.status === 'hidden') clone.querySelector(".btn-delete").disabled = true;

                body.appendChild(clone);
            });

            dom.find("#pagination").innerHTML = totalCount > 0
                ? pagingUtil.setBootstrapPaging(this._page, totalCount, this.pageSize())
                : "";
        },

        /**
         * 상세 팝업
         */
        async openDetail(id) {
            const n = await this.fetchDetail(id);
            if (!n) return;

            this._detailId = id;
            dom.setText("#detailTitle", n.title ?? "");
            dom.setText("#detailAuthor", n.authorName ?? "-");
            dom.setText("#detailDate", n.createdAt?.substring(0, 10) ?? "");
            dom.setText("#detailViews", stringUtil.addComma(n.views ?? 0) || "0");
            dom.setText("#detailStatus", STATUS_VIEW[n.status]?.label ?? n.status ?? "");
            dom.setText("#detailContent", n.content ?? "");
            dom.find("#detailImportant").classList.toggle("d-none", !n.isImportant);
            dialog.customOpen("#popNoticeDetail");
        },

        /**
         * 등록 / 수정 팝업 (id 없으면 등록)
         */
        async openForm(id) {
            this._editId = id;
            if (id) {
                const n = await this.fetchDetail(id);
                if (!n) return;
                dom.setText("#noticeFormTitle", "공지사항 수정");
                dom.setValue("#formTitle", n.title ?? "");
                dom.setValue("#formContent", n.content ?? "");
                dom.setValue("#formStatus", n.status ?? "published");
                dom.setChecked("#formImportant", !!n.isImportant);
            } else {
                dom.setText("#noticeFormTitle", "공지사항 등록");
                dom.setValue("#formTitle", "");
                dom.setValue("#formContent", "");
                dom.setValue("#formStatus", "published");
                dom.setChecked("#formImportant", false);
            }
            dialog.customOpen("#popNoticeForm");
        },

        async fetchDetail(id) {
            try {
                const res = await noticeApi.getAdminNoticeDetail(id, true);
                return res?.result ?? null;
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return null;
                await dialog.warning(error?.message || '공지사항을 불러오지 못했습니다.');
                return null;
            }
        },

        /**
         * 저장 (등록 / 수정)
         */
        async saveNotice() {
            const items = {
                title: dom.getValue("#formTitle"),
                content: dom.getValue("#formContent", false),
                status: dom.getValue("#formStatus"),
                isImportant: dom.getChecked("#formImportant")
            };

            const requiredErrors = validUtil.requiredError(items, {
                title: "제목을 입력해 주세요.",
                content: "내용을 입력해 주세요.",
            });
            if (requiredErrors) {
                await dialog.warning(requiredErrors);
                return;
            }
            if (!items.content.trim()) {
                await dialog.warning("내용을 입력해 주세요.");
                return;
            }

            const isEdit = !!this._editId;
            if (!(await dialog.confirm(`공지사항을 ${isEdit ? '수정' : '등록'}하시겠습니까?`))) return;

            try {
                if (isEdit) await noticeApi.updateNotice(this._editId, items, true);
                else await noticeApi.createNotice(items, true);

                dialog.customClose("#popNoticeForm");
                await dialog.success(`공지사항이 ${isEdit ? '수정' : '등록'}되었습니다.`);
                if (!isEdit) this._page = 1;
                await this.loadNotices();
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '저장하지 못했습니다.');
            }
        },

        /**
         * 삭제 (숨김 처리)
         */
        async deleteNotice(id) {
            const ok = await dialog.confirm("공지사항을 삭제하시겠습니까?\n사용자 화면에서 숨겨지며, 수정에서 '게시중'으로 바꾸면 다시 노출됩니다.");
            if (!ok) return;

            try {
                await noticeApi.deleteNotice(id, true);
                await dialog.success("공지사항이 삭제(숨김)되었습니다.");
                await this.loadNotices();
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '삭제하지 못했습니다.');
            }
        },
    };

    await notices.init();
}
