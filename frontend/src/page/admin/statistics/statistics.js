/**
 * packageName    :
 * fileName       : statistics
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 통계 (기간 선택 / 요약 카드 / Chart.js 차트 / 노선별 상세)
 */
import {dialog, dom, loadApis, loadUtils} from '@core/core.js';
import Chart from 'chart.js/auto';

const COLORS = ['#3498db', '#27ae60', '#f39c12', '#e74c3c', '#9b59b6', '#1abc9c', '#95a5a6'];
const RANK_COLORS = ['#f39c12', '#95a5a6', '#cd7f32'];
const TOP_CHART_ROUTES = 5;

document.addEventListener("DOMContentLoaded", () => {
    initStatistics();
});

async function initStatistics() {
    const {stringUtil} = await loadUtils(['stringUtil']);
    const {adminApi} = await loadApis(['adminApi']);

    Chart.defaults.font.family = "'Noto Sans KR', sans-serif";
    Chart.defaults.color = '#7f8c8d';

    const num = (v) => stringUtil.addComma(v ?? 0) || "0";
    const won = (v) => `${num(v)}원`;
    const ymd = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    const routeName = (r) => `${r.departureAirportNameKo ?? r.departureAirport} → ${r.arrivalAirportNameKo ?? r.arrivalAirport}`;

    const statistics = {
        _charts: {},

        async init() {
            const today = new Date();
            dom.setValue("#endDate", ymd(today));
            dom.setValue("#startDate", ymd(new Date(today.getFullYear(), today.getMonth(), today.getDate() - 29)));
            this.bindEvents();
            await this.load();
        },

        bindEvents() {
            dom.on("#periodType", "change", async () => {
                const custom = dom.getValue("#periodType") === 'custom';
                dom.find("#startDateGroup").classList.toggle("d-none", !custom);
                dom.find("#endDateGroup").classList.toggle("d-none", !custom);
                if (!custom) await this.load();
            });
            dom.on("#btnApply", "click", () => this.load());
        },

        period() {
            const type = dom.getValue("#periodType");
            if (type === 'custom') {
                return {startDate: dom.getValue("#startDate"), endDate: dom.getValue("#endDate")};
            }
            const today = new Date();
            const start = new Date(today.getFullYear(), today.getMonth(), today.getDate() - (Number(type) - 1));
            return {startDate: ymd(start), endDate: ymd(today)};
        },

        async load() {
            const {startDate, endDate} = this.period();
            if (!startDate || !endDate) {
                await dialog.warning("조회 기간을 선택해주세요.");
                return;
            }
            if (startDate > endDate) {
                await dialog.warning("시작일이 종료일보다 늦습니다.");
                return;
            }

            try {
                const res = await adminApi.getPeriodStats({startDate, endDate}, true);
                const s = res?.result;
                if (!s) return;
                dom.setText("#periodLabel", `${s.startDate} ~ ${s.endDate} (이전 같은 기간 대비)`);
                this.renderCards(s);
                this.renderTrend(s.dailyBookings ?? []);
                this.renderAirlines(s.airlineShares ?? []);
                this.renderRoutesChart(s.topRoutes ?? []);
                this.renderRevenue(s.monthlySales ?? []);
                this.renderRouteTable(s.topRoutes ?? [], s.airlineShares ?? []);
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '통계를 불러오지 못했습니다.');
            }
        },

        renderCards(s) {
            dom.setText("#statTotalBookings", `${num(s.totalBookings)}건`);
            this.setChange("#statTotalBookingsChange", s.totalBookings, s.prevTotalBookings);

            dom.setText("#statSales", won(s.sales));
            this.setChange("#statSalesChange", s.sales, s.prevSales);

            dom.setText("#statNewMembers", `${num(s.newMembers)}명`);
            this.setChange("#statNewMembersChange", s.newMembers, s.prevNewMembers);

            dom.setText("#statCancelRate", `${s.cancelRate ?? 0}%`);
            this.setPointChange("#statCancelRateChange", s.cancelRate, s.prevCancelRate);
        },

        /**
         * 증감률 (%)
         */
        setChange(selector, current, previous) {
            const cur = Number(current ?? 0);
            const prev = Number(previous ?? 0);
            if (prev === 0) {
                this.setChangeText(selector, 0, cur > 0 ? "이전 기간 없음" : "변동 없음");
                return;
            }
            const rate = Math.round(((cur - prev) / Math.abs(prev)) * 1000) / 10;
            this.setChangeText(selector, rate, `${Math.abs(rate)}%`);
        },

        /**
         * 취소율은 %p 차이, 낮아지면 긍정
         */
        setPointChange(selector, current, previous) {
            const diff = Math.round((Number(current ?? 0) - Number(previous ?? 0)) * 10) / 10;
            this.setChangeText(selector, diff, `${Math.abs(diff)}%p`, true);
        },

        setChangeText(selector, delta, text, lowerIsBetter = false) {
            const el = dom.find(selector);
            if (!el) return;
            el.classList.remove("positive", "negative");
            const icon = delta > 0 ? "bi-arrow-up" : delta < 0 ? "bi-arrow-down" : "bi-dash";
            if (delta !== 0) {
                const good = lowerIsBetter ? delta < 0 : delta > 0;
                el.classList.add(good ? "positive" : "negative");
            }
            el.innerHTML = `<i class="bi ${icon}"></i> `;
            el.append(text);
        },

        /**
         * 차트 생성/교체
         */
        draw(key, canvasId, config) {
            this._charts[key]?.destroy();
            this._charts[key] = new Chart(dom.find(canvasId), config);
        },

        baseOptions(yCallback) {
            return {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {legend: {display: false}},
                scales: {
                    y: {beginAtZero: true, ticks: {precision: 0, callback: yCallback}}
                }
            };
        },

        renderTrend(daily) {
            this.draw("trend", "#bookingTrendChart", {
                type: 'line',
                data: {
                    labels: daily.map((d) => d.date.substring(5).replace('-', '/')),
                    datasets: [{
                        label: '예약 건수',
                        data: daily.map((d) => d.count),
                        borderColor: '#3498db',
                        backgroundColor: 'rgba(52, 152, 219, 0.1)',
                        tension: 0.3,
                        fill: true,
                        pointRadius: daily.length > 45 ? 0 : 3,
                        pointHoverRadius: 5
                    }]
                },
                options: this.baseOptions((v) => `${v}건`)
            });
        },

        renderAirlines(shares) {
            const empty = shares.length === 0;
            this.draw("airline", "#airlineChart", {
                type: 'doughnut',
                data: {
                    labels: empty ? ['데이터 없음'] : shares.map((a) => a.name),
                    datasets: [{
                        data: empty ? [1] : shares.map((a) => a.count),
                        backgroundColor: empty ? ['#ecf0f1'] : shares.map((_, i) => COLORS[i % COLORS.length]),
                        borderWidth: 0
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {position: 'bottom', labels: {padding: 15, font: {size: 11}}},
                        tooltip: {enabled: !empty, callbacks: {label: (ctx) => ` ${ctx.label}: ${num(ctx.raw)}건`}}
                    }
                }
            });
        },

        renderRoutesChart(routes) {
            const top = routes.slice(0, TOP_CHART_ROUTES);
            this.draw("routes", "#routesChart", {
                type: 'bar',
                data: {
                    labels: top.map(routeName),
                    datasets: [{
                        label: '예약 건수',
                        data: top.map((r) => r.count),
                        backgroundColor: '#3498db',
                        borderRadius: 4
                    }]
                },
                options: this.baseOptions((v) => `${v}건`)
            });
        },

        renderRevenue(months) {
            this.draw("revenue", "#revenueChart", {
                type: 'bar',
                data: {
                    labels: months.map((m) => `${Number(m.month.substring(5))}월`),
                    datasets: [{
                        label: '순매출',
                        data: months.map((m) => m.amount),
                        backgroundColor: '#27ae60',
                        borderRadius: 4
                    }]
                },
                options: {
                    ...this.baseOptions((v) => v >= 10000 ? `${num(Math.round(v / 10000))}만` : num(v)),
                    plugins: {
                        legend: {display: false},
                        tooltip: {callbacks: {label: (ctx) => ` ${won(ctx.raw)}`}}
                    }
                }
            });
        },

        /**
         * 노선 테이블 (점유율 = 노선 확정 예약 / 기간 전체 확정 예약)
         */
        renderRouteTable(routes, airlineShares) {
            const body = dom.find("#routeTableBody");
            const template = dom.find("#routeRowTemplate");
            body.innerHTML = "";

            if (routes.length === 0) {
                body.innerHTML = `<tr class="admin-table-empty"><td colspan="6">조회된 데이터가 없습니다.</td></tr>`;
                return;
            }

            const totalConfirmed = airlineShares.reduce((sum, a) => sum + (a.count ?? 0), 0);
            routes.forEach((r, i) => {
                const clone = template.content.cloneNode(true);
                const share = totalConfirmed > 0 ? Math.round(r.count * 1000 / totalConfirmed) / 10 : 0;

                const rank = clone.querySelector(".row-rank");
                rank.textContent = i + 1;
                rank.style.color = RANK_COLORS[i] ?? '#7f8c8d';

                const route = clone.querySelector(".row-route");
                if (i < RANK_COLORS.length) {
                    const strong = document.createElement("strong");
                    strong.textContent = routeName(r);
                    route.appendChild(strong);
                } else {
                    route.textContent = routeName(r);
                }

                dom.setNodeText(clone, ".row-count", num(r.count));
                dom.setNodeText(clone, ".row-revenue", won(r.revenue));
                dom.setNodeText(clone, ".row-avg", won(r.count > 0 ? Math.round(r.revenue / r.count) : 0));
                clone.querySelector(".row-share-bar").style.width = `${share}%`;
                dom.setNodeText(clone, ".row-share", `${share}%`);

                body.appendChild(clone);
            });
        },
    };

    await statistics.init();
}
