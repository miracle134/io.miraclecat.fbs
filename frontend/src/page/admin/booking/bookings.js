/**
 * packageName    :
 * fileName       : bookings
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 예약 관리 (목록 / 상세 / 관리자 취소 — 전액·항공권 금액 환불 선택)
 */
import {ajax, dialog, dom, loadApis, loadUtils} from '@core/core.js';

const STATUS_VIEW = {
    confirmed: {label: '예약완료', badge: 'admin-badge-success'},
    cancelled: {label: '취소', badge: 'admin-badge-danger'}
};
const ACTOR_LABEL = {USER: '고객', ADMIN: '관리자', SYSTEM: '시스템'};
const PAYMENT_METHOD_LABEL = {
    CARD: '신용카드', BANK_TRANSFER: '계좌이체', VIRTUAL_ACCOUNT: '가상계좌',
    EASY_PAY: '간편결제', MOBILE_PHONE: '휴대폰'
};
const PAYMENT_STATUS_LABEL = {APPROVED: '승인', CANCELLED: '취소'};
const GENDER_LABEL = {male: '남', female: '여'};
const COLSPAN = 10;

document.addEventListener("DOMContentLoaded", () => {
    initBookings();
});

async function initBookings() {
    const {pagingUtil, stringUtil} = await loadUtils(['pagingUtil', 'stringUtil']);
    const {bookingApi} = await loadApis(['bookingApi']);

    const won = (v) => `${stringUtil.addComma(v ?? 0) || 0}원`;
    const dateTime = (v) => v ? v.replace('T', ' ').substring(0, 16) : '-';
    const airport = (code, name) => name ? `${name}(${code})` : (code ?? '-');

    const bookings = {
        _page: 1,
        _detail: null,    // 상세 팝업에 띄운 예약
        _cancelTarget: null,

        async init() {
            this.bindEvents();
            await this.loadBookings();
        },

        bindEvents() {
            // 검색 / 초기화
            dom.on("#btnSearch", "click", () => this.search());
            ["#filterBookingNo", "#filterBooker"].forEach((id) =>
                dom.on(id, "keydown", (e) => { if (e.key === 'Enter') this.search(); }));
            dom.on("#btnReset", "click", () => {
                ["#filterBookingNo", "#filterBooker", "#filterStartDate", "#filterEndDate", "#filterStatus"]
                    .forEach((id) => dom.setValue(id, ""));
                dom.setValue("#filterDateType", "created");
                this.search();
            });

            // 페이징 / 페이지당 건수
            pagingUtil.initPaginationListener(async (page) => {
                this._page = page;
                await this.loadBookings();
            }, "pagination");
            dom.on("#pageSize", "change", () => this.search());

            // 행 버튼 (이벤트 위임)
            dom.delegate("#bookingTableBody", "click", "button[data-id]", async (e) => {
                const btn = e.target.closest("button[data-id]");
                const id = Number(btn.dataset.id);
                if (btn.classList.contains("btn-detail")) await this.openDetail(id);
                else if (btn.classList.contains("btn-cancel")) await this.openCancel(id);
            });

            // 상세 → 취소
            dom.on("#btnCancelFromDetail", "click", async () => {
                const id = this._detail?.id;
                if (!id) return;
                dialog.customClose("#popBookingDetail");
                await this.openCancel(id);
            });

            // 취소 처리
            dom.on("#btnConfirmCancel", "click", ajax.preventDisable(async () => {
                await this.cancelBooking();
            }));
        },

        async search() {
            this._page = 1;
            await this.loadBookings();
        },

        pageSize() {
            return Number(dom.getValue("#pageSize")) || 10;
        },

        /**
         * 목록 조회
         */
        async loadBookings() {
            const startDate = dom.getValue("#filterStartDate");
            const endDate = dom.getValue("#filterEndDate");
            if (startDate && endDate && startDate > endDate) {
                await dialog.warning("시작일이 종료일보다 늦습니다.");
                return;
            }

            const params = {
                page: this._page - 1,
                size: this.pageSize(),
                bookingNumber: dom.getValue("#filterBookingNo"),
                bookerName: dom.getValue("#filterBooker"),
                dateType: dom.getValue("#filterDateType"),
                startDate,
                endDate,
                status: dom.getValue("#filterStatus")
            };
            Object.keys(params).forEach((k) => { if (params[k] === "" || params[k] == null) delete params[k]; });

            try {
                const res = await bookingApi.getAdminBookings(params, true);
                const page = res?.result;
                this.render(page?.content ?? [], page?.totalCount ?? 0);
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '예약 목록을 불러오지 못했습니다.');
            }
        },

        render(list, totalCount) {
            const body = dom.find("#bookingTableBody");
            const template = dom.find("#bookingRowTemplate");
            body.innerHTML = "";
            dom.setText("#totalCount", stringUtil.addComma(totalCount) || "0");

            if (list.length === 0) {
                body.innerHTML = `<tr class="admin-table-empty"><td colspan="${COLSPAN}">조회된 예약이 없습니다.</td></tr>`;
            }

            list.forEach((b, i) => {
                const clone = template.content.cloneNode(true);
                const status = STATUS_VIEW[b.status] ?? {label: b.status ?? '-', badge: 'admin-badge-primary'};

                dom.setNodeText(clone, ".row-no", pagingUtil.pageIndex(i, this._page, this.pageSize()));
                dom.setNodeText(clone, ".row-number", b.bookingNumber ?? "");
                dom.setNodeText(clone, ".row-member", b.memberName ?? "-");
                dom.setNodeText(clone, ".row-login-id", b.loginId ?? "");
                dom.setNodeText(clone, ".row-flight",
                    `${b.flightNumber ?? ""} (${b.departureAirportNameKo ?? b.departureAirport ?? ""}→${b.arrivalAirportNameKo ?? b.arrivalAirport ?? ""})`);
                dom.setNodeText(clone, ".row-flight-date", b.flightDate ?? "");
                dom.setNodeText(clone, ".row-passengers", `${b.passengerCount ?? 0}명`);
                dom.setNodeText(clone, ".row-amount", won(b.totalAmount));
                dom.setNodeText(clone, ".row-created", dateTime(b.createdAt));

                const badge = clone.querySelector(".row-status");
                badge.textContent = status.label;
                badge.classList.add(status.badge);

                clone.querySelectorAll("button").forEach((btn) => btn.dataset.id = b.id);
                if (b.status !== 'confirmed') clone.querySelector(".btn-cancel").disabled = true;
                if (b.status === 'cancelled') clone.querySelector("tr").style.opacity = '0.6';

                body.appendChild(clone);
            });

            dom.find("#pagination").innerHTML = totalCount > 0
                ? pagingUtil.setBootstrapPaging(this._page, totalCount, this.pageSize())
                : "";
        },

        async fetchDetail(id) {
            try {
                const res = await bookingApi.getAdminBookingDetail(id, true);
                return res?.result ?? null;
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return null;
                await dialog.warning(error?.message || '예약 정보를 불러오지 못했습니다.');
                return null;
            }
        },

        /**
         * 상세 팝업
         */
        async openDetail(id) {
            const d = await this.fetchDetail(id);
            if (!d) return;
            this._detail = d;

            const status = STATUS_VIEW[d.status] ?? {label: d.status ?? '-', badge: 'admin-badge-primary'};

            dom.setText("#detailBookingNumber", d.bookingNumber ?? "");
            dom.setText("#detailCreatedAt", dateTime(d.createdAt));
            dom.setText("#detailMemberName", d.memberName ?? "-");
            dom.setText("#detailLoginId", d.loginId ?? "-");
            dom.setText("#detailMemberMobile", d.memberMobile || "-");

            dom.setText("#detailDepartureCity", airport(d.departureAirport, d.departureAirportNameKo));
            dom.setText("#detailArrivalCity", airport(d.arrivalAirport, d.arrivalAirportNameKo));
            dom.setText("#detailDepartureTime", d.departureTime?.substring(0, 5) ?? "-");
            dom.setText("#detailArrivalTime", d.arrivalTime?.substring(0, 5) ?? "-");
            dom.setText("#detailFlightNo", d.flightNumber ?? "");
            dom.setText("#detailFlightDate", d.flightDate ?? "-");
            dom.setText("#detailAirline", d.airlineNameKo ?? "-");
            dom.setText("#detailPassengerCount", d.passengerCount ?? 0);

            this.renderPassengers(d.passengers ?? []);

            dom.setText("#detailFareAmount", won(d.fareAmount));
            dom.setText("#detailFeeAmount", won(d.bookingFeeAmount));
            dom.setText("#detailTotalAmount", won(d.totalAmount));
            const refunded = d.refundAmount != null;
            dom.find("#detailRefundRow").classList.toggle("d-none", !refunded);
            if (refunded) dom.setText("#detailRefundAmount", won(d.refundAmount));

            const badge = dom.find("#detailStatusBadge");
            badge.className = `admin-badge ${status.badge}`;
            badge.textContent = status.label;
            dom.setText("#detailPaymentMethod", PAYMENT_METHOD_LABEL[d.paymentMethod] ?? d.paymentMethod ?? "-");
            dom.setText("#detailPaymentStatus", PAYMENT_STATUS_LABEL[d.paymentStatus] ?? d.paymentStatus ?? "-");
            dom.setText("#detailApprovedAt", dateTime(d.approvedAt));

            const cancelled = d.status === 'cancelled';
            dom.find("#detailCancelBox").classList.toggle("d-none", !cancelled);
            if (cancelled) {
                dom.setText("#detailCancelledBy", ACTOR_LABEL[d.cancelledBy] ?? d.cancelledBy ?? "-");
                dom.setText("#detailCancelledAt", dateTime(d.cancelledAt));
                dom.setText("#detailCancelReason", d.cancelReason ?? "-");
            }

            this.renderHistories(d.histories ?? []);
            dom.find("#btnCancelFromDetail").disabled = d.status !== 'confirmed';
            dialog.customOpen("#popBookingDetail");
        },

        renderPassengers(passengers) {
            const body = dom.find("#detailPassengerBody");
            body.innerHTML = "";
            if (passengers.length === 0) {
                body.innerHTML = `<tr class="admin-table-empty"><td colspan="7">탑승객 정보가 없습니다.</td></tr>`;
                return;
            }
            passengers.forEach((p) => {
                const tr = document.createElement("tr");
                [p.name, p.nameEn, GENDER_LABEL[p.gender] ?? p.gender, p.birthDate, p.mobile, p.email, p.seatNumber]
                    .forEach((value, idx) => {
                        const td = document.createElement("td");
                        td.textContent = value ?? "-";
                        if (idx === 6) td.className = "text-center fw-semibold";
                        tr.appendChild(td);
                    });
                body.appendChild(tr);
            });
        },

        renderHistories(histories) {
            const box = dom.find("#detailHistories");
            box.innerHTML = "";
            if (histories.length === 0) {
                box.textContent = "이력이 없습니다.";
                return;
            }
            histories.forEach((h, i) => {
                const row = document.createElement("div");
                row.style.padding = "0.5rem 0";
                if (i < histories.length - 1) row.style.borderBottom = "1px solid #e0e0e0";

                // 상태가 바뀐 경우만 "예약완료 → 취소", 그대로면 사유만 표시
                const statusLabel = (s) => STATUS_VIEW[s]?.label ?? s ?? "-";
                const changed = h.previousStatus && h.currentStatus && h.previousStatus !== h.currentStatus;
                const reason = h.reason ?? "";
                const title = document.createElement("div");
                title.style.fontWeight = "600";
                title.textContent = changed
                    ? `${statusLabel(h.previousStatus)} → ${statusLabel(h.currentStatus)}${reason ? ` · ${reason}` : ""}`
                    : (reason || statusLabel(h.currentStatus));

                const meta = document.createElement("div");
                meta.style.cssText = "font-size: 0.8rem; color: #7f8c8d;";
                meta.textContent = `${dateTime(h.createdAt)} · ${ACTOR_LABEL[h.changedBy] ?? h.changedBy ?? "-"}`;

                row.append(title, meta);
                box.appendChild(row);
            });
        },

        /**
         * 관리자 취소 팝업 (환불 방식 선택)
         */
        async openCancel(id) {
            const d = this._detail?.id === id ? this._detail : await this.fetchDetail(id);
            if (!d) return;
            if (d.status !== 'confirmed') {
                await dialog.warning("이미 취소된 예약입니다.");
                return;
            }

            this._cancelTarget = d;
            dom.setText("#cancelBookingNumber", d.bookingNumber ?? "");
            dom.setText("#refundFullAmount", won(d.totalAmount));
            dom.setText("#refundFareAmount", won(d.fareAmount));
            dom.setRadioValue("refundType", "FULL");
            dom.setValue("#cancelReason", "");
            dialog.customOpen("#popAdminCancel");
        },

        async cancelBooking() {
            const d = this._cancelTarget;
            if (!d) return;

            const refundType = dom.getRadioValue("refundType");
            const refundAmount = refundType === 'FULL' ? d.totalAmount : d.fareAmount;
            const ok = await dialog.confirm(
                `예약 ${d.bookingNumber}을(를) 취소하시겠습니까?\n환불 금액: ${won(refundAmount)}\n취소 후에는 되돌릴 수 없습니다.`);
            if (!ok) return;

            try {
                await bookingApi.cancelByAdmin(d.id, {
                    refundType,
                    cancelReason: dom.getValue("#cancelReason")
                }, true);
                dialog.customClose("#popAdminCancel");
                this._cancelTarget = null;
                this._detail = null;
                await dialog.success("예약이 취소되었습니다.");
                await this.loadBookings();
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '예약을 취소하지 못했습니다.');
                await this.loadBookings();
            }
        },
    };

    await bookings.init();
}
