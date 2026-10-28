/**
 * packageName    :
 * fileName       : dashboard
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 대시보드 (요약 카드 / 최근 예약 5건)
 */
import {dialog, dom, loadApis, loadUtils} from '@core/core.js';

const STATUS_VIEW = {
    confirmed: {label: '예약완료', badge: 'admin-badge-success'},
    cancelled: {label: '취소', badge: 'admin-badge-danger'}
};
const RECENT_SIZE = 5;

document.addEventListener("DOMContentLoaded", () => {
    initDashboard();
});

async function initDashboard() {
    const {stringUtil} = await loadUtils(['stringUtil']);
    const {adminApi, bookingApi} = await loadApis(['adminApi', 'bookingApi']);

    const num = (v) => stringUtil.addComma(v ?? 0) || "0";
    const won = (v) => `${num(v)}원`;

    const dashboard = {
        async init() {
            await Promise.all([this.loadStats(), this.loadRecentBookings()]);
        },

        async loadStats() {
            try {
                const res = await adminApi.getDashboardStats(false);
                const s = res?.result;
                if (!s) return;

                dom.setText("#statTodayBookings", `${num(s.todayBookings)}건`);
                this.setChange("#statTodayBookingsChange", s.todayBookings, s.yesterdayBookings, "어제 대비");

                dom.setText("#statMonthlySales", won(s.monthlySales));
                this.setChange("#statMonthlySalesChange", s.monthlySales, s.lastMonthSales, "지난달 같은 기간 대비");

                dom.setText("#statActiveFlights", `${num(s.activeFlights)}편`);
                this.setInfo("#statActiveFlightsChange", `오늘 운항 ${num(s.todayFlights)}편`);

                dom.setText("#statTotalMembers", `${num(s.totalMembers)}명`);
                this.setInfo("#statTotalMembersChange", `이번 달 신규 ${num(s.newMembersThisMonth)}명`);
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '대시보드 정보를 불러오지 못했습니다.');
            }
        },

        /**
         * 증감률 표시 (이전 값 0이면 증감률 대신 신규 표시)
         */
        setChange(selector, current, previous, label) {
            const el = dom.find(selector);
            if (!el) return;
            const cur = Number(current ?? 0);
            const prev = Number(previous ?? 0);

            el.classList.remove("positive", "negative");
            if (prev === 0) {
                el.textContent = cur > 0 ? `${label} 신규` : `${label} 변동 없음`;
                return;
            }
            const rate = Math.round(((cur - prev) / Math.abs(prev)) * 1000) / 10;
            const icon = rate > 0 ? "bi-arrow-up" : rate < 0 ? "bi-arrow-down" : "bi-dash";
            if (rate !== 0) el.classList.add(rate > 0 ? "positive" : "negative");
            el.innerHTML = `<i class="bi ${icon}"></i> `;
            el.append(`${Math.abs(rate)}% ${label}`);
        },

        setInfo(selector, text) {
            const el = dom.find(selector);
            if (!el) return;
            el.classList.remove("positive", "negative");
            el.textContent = text;
        },

        async loadRecentBookings() {
            const body = dom.find("#recentBookingBody");
            const template = dom.find("#recentBookingTemplate");
            try {
                const res = await bookingApi.getAdminBookings({page: 0, size: RECENT_SIZE}, false);
                const list = res?.result?.content ?? [];
                if (list.length === 0) return;

                body.innerHTML = "";
                list.forEach((b) => {
                    const clone = template.content.cloneNode(true);
                    const status = STATUS_VIEW[b.status] ?? {label: b.status ?? '-', badge: 'admin-badge-primary'};

                    dom.setNodeText(clone, ".booking-number", b.bookingNumber ?? "");
                    dom.setNodeText(clone, ".booker-name", b.memberName ?? "-");
                    dom.setNodeText(clone, ".booking-route",
                        `${b.departureAirportNameKo ?? b.departureAirport ?? ""} → ${b.arrivalAirportNameKo ?? b.arrivalAirport ?? ""}`);
                    dom.setNodeText(clone, ".flight-date", b.flightDate ?? "-");
                    dom.setNodeText(clone, ".total-amount", won(b.totalAmount));

                    const badge = clone.querySelector(".booking-status");
                    badge.textContent = status.label;
                    badge.classList.add(status.badge);

                    body.appendChild(clone);
                });
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                body.innerHTML = `<tr class="admin-table-empty"><td colspan="6">최근 예약을 불러오지 못했습니다.</td></tr>`;
            }
        },
    };

    await dashboard.init();
}
