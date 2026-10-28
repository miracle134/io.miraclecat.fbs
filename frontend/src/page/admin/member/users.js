/**
 * packageName    :
 * fileName       : users
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 회원 관리 (일반 회원 목록 / 상세 / 정지·해제)
 */
import {ajax, dialog, dom, loadApis, loadUtils} from '@core/core.js';

const STATUS_VIEW = {
    active: {label: '활성', badge: 'admin-badge-success'},
    suspended: {label: '정지', badge: 'admin-badge-warning'},
    withdrawn: {label: '탈퇴', badge: 'admin-badge-danger'}
};
const BOOKING_STATUS_LABEL = {confirmed: '예약완료', cancelled: '취소'};
const COLSPAN = 8;

document.addEventListener("DOMContentLoaded", () => {
    initUsers();
});

async function initUsers() {
    const {pagingUtil, stringUtil} = await loadUtils(['pagingUtil', 'stringUtil']);
    const {memberApi} = await loadApis(['memberApi']);

    const won = (v) => `${stringUtil.addComma(v ?? 0) || 0}원`;

    const users = {
        _page: 1,
        _detail: null,

        async init() {
            stringUtil.setInputPhoneNumber("#filterPhone");
            this.bindEvents();
            await this.loadUsers();
        },

        bindEvents() {
            // 검색 / 초기화
            dom.on("#btnSearch", "click", () => this.search());
            ["#filterEmail", "#filterName", "#filterPhone"].forEach((id) =>
                dom.on(id, "keydown", (e) => { if (e.key === 'Enter') this.search(); }));
            dom.on("#btnReset", "click", () => {
                ["#filterEmail", "#filterName", "#filterPhone", "#filterJoinDate", "#filterStatus"]
                    .forEach((id) => dom.setValue(id, ""));
                this.search();
            });

            // 페이징 / 페이지당 건수
            pagingUtil.initPaginationListener(async (page) => {
                this._page = page;
                await this.loadUsers();
            }, "pagination");
            dom.on("#pageSize", "change", () => this.search());

            // 행 버튼 (이벤트 위임)
            dom.delegate("#userTableBody", "click", "button[data-id]", async (e) => {
                const btn = e.target.closest("button[data-id]");
                const id = Number(btn.dataset.id);
                if (btn.classList.contains("btn-detail")) await this.openDetail(id);
                else if (btn.classList.contains("btn-status")) await this.toggleStatus(id, btn.dataset.status);
            });

            // 상세 → 정지/해제
            dom.on("#btnStatusFromDetail", "click", ajax.preventDisable(async () => {
                const d = this._detail;
                if (!d) return;
                const changed = await this.toggleStatus(d.memberId, d.status);
                if (changed) dialog.customClose("#popUserDetail");
            }));
        },

        async search() {
            this._page = 1;
            await this.loadUsers();
        },

        pageSize() {
            return Number(dom.getValue("#pageSize")) || 10;
        },

        /**
         * 목록 조회
         */
        async loadUsers() {
            const params = {
                page: this._page - 1,
                size: this.pageSize(),
                loginId: dom.getValue("#filterEmail"),
                memberName: dom.getValue("#filterName"),
                mobile: dom.getValue("#filterPhone"),
                joinDate: dom.getValue("#filterJoinDate"),
                status: dom.getValue("#filterStatus")
            };
            Object.keys(params).forEach((k) => { if (params[k] === "" || params[k] == null) delete params[k]; });

            try {
                const res = await memberApi.getAdminMembers(params, true);
                const page = res?.result;
                this.render(page?.content ?? [], page?.totalCount ?? 0);
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '회원 목록을 불러오지 못했습니다.');
            }
        },

        render(list, totalCount) {
            const body = dom.find("#userTableBody");
            const template = dom.find("#userRowTemplate");
            body.innerHTML = "";
            dom.setText("#totalCount", stringUtil.addComma(totalCount) || "0");

            if (list.length === 0) {
                body.innerHTML = `<tr class="admin-table-empty"><td colspan="${COLSPAN}">조회된 회원이 없습니다.</td></tr>`;
            }

            list.forEach((m, i) => {
                const clone = template.content.cloneNode(true);
                const status = STATUS_VIEW[m.status] ?? STATUS_VIEW.active;

                dom.setNodeText(clone, ".row-no", pagingUtil.pageIndex(i, this._page, this.pageSize()));
                dom.setNodeText(clone, ".row-email", m.loginId ?? "");
                clone.querySelector(".row-google").classList.toggle("d-none", m.provider !== 'GOOGLE');
                dom.setNodeText(clone, ".row-name", m.memberName ?? "-");
                dom.setNodeText(clone, ".row-phone", m.mobile || "-");
                dom.setNodeText(clone, ".row-join", m.createdAt?.substring(0, 10) ?? "");
                dom.setNodeText(clone, ".row-bookings", m.bookingCount ?? 0);

                const badge = clone.querySelector(".row-status");
                badge.textContent = status.label;
                badge.classList.add(status.badge);

                clone.querySelector(".btn-detail").dataset.id = m.memberId;
                this.decorateStatusButton(clone.querySelector(".btn-status"), m);

                const tr = clone.querySelector("tr");
                if (m.status === 'suspended') tr.style.opacity = '0.7';
                if (m.status === 'withdrawn') {
                    tr.style.opacity = '0.45';
                    clone.querySelector(".row-email").style.textDecoration = 'line-through';
                }

                body.appendChild(clone);
            });

            dom.find("#pagination").innerHTML = totalCount > 0
                ? pagingUtil.setBootstrapPaging(this._page, totalCount, this.pageSize())
                : "";
        },

        /**
         * 정지/해제 버튼 (탈퇴 회원은 비활성)
         */
        decorateStatusButton(btn, m) {
            btn.dataset.id = m.memberId;
            btn.dataset.status = m.status;
            btn.classList.remove("admin-btn-danger", "admin-btn-success", "admin-btn-secondary");

            if (m.status === 'withdrawn') {
                btn.classList.add("admin-btn-secondary");
                btn.innerHTML = '<i class="bi bi-x-circle"></i>';
                btn.title = "탈퇴 회원";
                btn.disabled = true;
            } else if (m.status === 'suspended') {
                btn.classList.add("admin-btn-success");
                btn.innerHTML = '<i class="bi bi-play-circle"></i>';
                btn.title = "정지 해제";
            } else {
                btn.classList.add("admin-btn-danger");
                btn.innerHTML = '<i class="bi bi-pause-circle"></i>';
                btn.title = "정지";
            }
        },

        /**
         * 상세 팝업
         */
        async openDetail(id) {
            let d;
            try {
                const res = await memberApi.getAdminMemberDetail(id, true);
                d = res?.result;
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '회원 정보를 불러오지 못했습니다.');
                return;
            }
            if (!d) return;
            this._detail = d;

            const status = STATUS_VIEW[d.status] ?? STATUS_VIEW.active;
            dom.setText("#detailName", d.memberName ?? "-");
            dom.setText("#detailEmail", d.loginId ?? "");
            dom.find("#detailGoogle").classList.toggle("d-none", d.provider !== 'GOOGLE');
            dom.setText("#detailPhone", d.mobile || "-");
            dom.setText("#detailJoinDate", d.createdAt?.substring(0, 10) ?? "-");
            dom.setText("#detailWithdrawnAt", d.withdrawnAt?.substring(0, 10) ?? "-");
            dom.setText("#detailBookingCount", d.bookingCount ?? 0);
            dom.setText("#detailCancelledCount", d.cancelledCount ?? 0);

            const badge = dom.find("#detailStatusBadge");
            badge.className = `admin-badge ${status.badge}`;
            badge.textContent = status.label;

            this.renderRecentBookings(d.recentBookings ?? []);

            const btn = dom.find("#btnStatusFromDetail");
            btn.disabled = d.status === 'withdrawn';
            btn.className = `admin-btn ${d.status === 'suspended' ? 'admin-btn-success' : 'admin-btn-danger'}`;
            btn.innerHTML = d.status === 'suspended'
                ? '<i class="bi bi-play-circle"></i>정지 해제'
                : '<i class="bi bi-pause-circle"></i>정지';

            dialog.customOpen("#popUserDetail");
        },

        renderRecentBookings(list) {
            const box = dom.find("#detailRecentBookings");
            box.innerHTML = "";
            if (list.length === 0) {
                box.textContent = "예약 내역이 없습니다.";
                return;
            }
            list.forEach((b, i) => {
                const row = document.createElement("div");
                row.style.padding = "0.5rem 0";
                if (i < list.length - 1) row.style.borderBottom = "1px solid #e0e0e0";

                const title = document.createElement("div");
                title.style.fontWeight = "600";
                title.style.marginBottom = "0.25rem";
                title.textContent = `${b.flightNumber ?? ""} - ${b.departureAirportNameKo ?? ""} → ${b.arrivalAirportNameKo ?? ""}`;

                const meta = document.createElement("div");
                meta.style.cssText = "font-size: 0.8rem; color: #7f8c8d;";
                meta.textContent = `${b.bookingNumber ?? ""} · ${b.flightDate ?? ""} · ${b.passengerCount ?? 0}명 · `
                    + `${won(b.totalAmount)} · ${BOOKING_STATUS_LABEL[b.status] ?? b.status ?? ""}`;

                row.append(title, meta);
                box.appendChild(row);
            });
        },

        /**
         * 정지 ↔ 해제
         * @return {Promise<boolean>} 변경 여부
         */
        async toggleStatus(id, current) {
            if (current === 'withdrawn') return false;
            const next = current === 'suspended' ? 'active' : 'suspended';
            const message = next === 'suspended'
                ? "회원을 정지하시겠습니까?\n정지된 회원은 다음 로그인부터 이용할 수 없습니다."
                : "회원 정지를 해제하시겠습니까?";
            if (!(await dialog.confirm(message))) return false;

            try {
                await memberApi.changeMemberStatus(id, {status: next}, true);
                await dialog.success(next === 'suspended' ? "회원이 정지되었습니다." : "정지가 해제되었습니다.");
                this._detail = null;
                await this.loadUsers();
                return true;
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return false;
                await dialog.warning(error?.message || '상태를 변경하지 못했습니다.');
                return false;
            }
        },
    };

    await users.init();
}
