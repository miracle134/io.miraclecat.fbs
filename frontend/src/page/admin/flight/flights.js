/**
 * packageName    :
 * fileName       : flights
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 관리자 항공편 관리 (목록 / 상세·재고 생성 / 등록 / 수정 / 운항 중지)
 */
import {ajax, dialog, dom, loadApis, loadUtils} from '@core/core.js';

const STATUS_VIEW = {
    active: {label: '운항', badge: 'admin-badge-success'},
    inactive: {label: '운항 중지', badge: 'admin-badge-danger'}
};
const COLSPAN = 10;

document.addEventListener("DOMContentLoaded", () => {
    initFlights();
});

async function initFlights() {
    const {pagingUtil, stringUtil, validUtil} = await loadUtils(['pagingUtil', 'stringUtil', 'validUtil']);
    const {flightApi, codeApi} = await loadApis(['flightApi', 'codeApi']);

    const won = (v) => `${stringUtil.addComma(v ?? 0) || 0}원`;
    const hhmm = (v) => v?.substring(0, 5) ?? '-';
    const dateTime = (v) => v ? v.replace('T', ' ').substring(0, 16) : '-';
    const ymd = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

    const flights = {
        _page: 1,
        _editId: null,
        _detail: null,
        _airlineNames: {},

        async init() {
            await this.loadCodes();
            this.bindEvents();
            await this.loadFlights();
        },

        /**
         * 항공사·공항 select 채우기
         */
        async loadCodes() {
            try {
                const [airlines, airports] = await Promise.all([codeApi.getAirlines(), codeApi.getAirports()]);
                const fill = (selector, list, label) => document.querySelectorAll(selector).forEach((select) => {
                    list.forEach((item) => {
                        const option = document.createElement("option");
                        option.value = item.code;
                        option.textContent = label(item);
                        select.appendChild(option);
                    });
                });
                const airlineList = airlines?.result ?? [];
                airlineList.forEach((a) => this._airlineNames[a.code] = a.nameKo);
                fill(".airline-select", airlineList, (a) => `${a.nameKo} (${a.code})`);
                fill(".airport-select", airports?.result ?? [], (a) => `${a.nameKo} (${a.code})`);
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '항공사·공항 목록을 불러오지 못했습니다.');
            }
        },

        bindEvents() {
            // 검색 / 초기화
            dom.on("#btnSearch", "click", () => this.search());
            dom.on("#filterFlightNumber", "keydown", (e) => { if (e.key === 'Enter') this.search(); });
            dom.on("#btnReset", "click", () => {
                ["#filterFlightNumber", "#filterDeparture", "#filterArrival", "#filterAirline", "#filterStatus"]
                    .forEach((id) => dom.setValue(id, ""));
                this.search();
            });

            // 페이징 / 페이지당 건수
            pagingUtil.initPaginationListener(async (page) => {
                this._page = page;
                await this.loadFlights();
            }, "pagination");
            dom.on("#pageSize", "change", () => this.search());

            // 등록
            dom.on("#btnAddFlight", "click", () => this.openForm(null));
            dom.on("#btnSaveFlight", "click", ajax.preventDisable(async () => {
                await this.saveFlight();
            }));

            // 입력 제어
            stringUtil.setInputPrice("#formPrice", 10000000);
            dom.on("#formTotalSeats", "input", (e) => {
                e.target.value = e.target.value.replace(/[^0-9]/g, '').slice(0, 3);
            });
            dom.on("#formFlightNumber", "input", (e) => {
                e.target.value = e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6);
            });
            dom.on("#formAirline", "change", (e) => {
                const number = dom.find("#formFlightNumber");
                if (!number.disabled && !number.value) number.value = e.target.value;
            });
            ["#formDepartureTime", "#formArrivalTime"].forEach((id) =>
                dom.on(id, "change", () => this.updateDuration()));

            // 상세 → 수정
            dom.on("#btnEditFromDetail", "click", async () => {
                const id = this._detail?.id;
                if (!id) return;
                dialog.customClose("#popFlightDetail");
                await this.openForm(id);
            });

            // 재고 생성
            dom.on("#btnGenerateInventory", "click", ajax.preventDisable(async () => {
                await this.generateInventory();
            }));

            // 재고 조정
            dom.delegate("#detailInventoryBody", "click", "button.btn-adjust", (e) => {
                this.openAdjust(Number(e.target.closest("button.btn-adjust").dataset.id));
            });
            dom.on("#adjustAvailableSeats", "input", (e) => {
                e.target.value = e.target.value.replace(/[^0-9]/g, '').slice(0, 3);
            });
            dom.on("#btnSaveAdjust", "click", ajax.preventDisable(async () => {
                await this.saveAdjust();
            }));

            // 행 버튼 (이벤트 위임)
            dom.delegate("#flightTableBody", "click", "button[data-id]", async (e) => {
                const btn = e.target.closest("button[data-id]");
                const id = Number(btn.dataset.id);
                if (btn.classList.contains("btn-detail")) await this.openDetail(id);
                else if (btn.classList.contains("btn-edit")) await this.openForm(id);
                else if (btn.classList.contains("btn-delete")) await this.deactivate(id);
            });
        },

        async search() {
            this._page = 1;
            await this.loadFlights();
        },

        pageSize() {
            return Number(dom.getValue("#pageSize")) || 10;
        },

        /**
         * 목록 조회
         */
        async loadFlights() {
            const params = {
                page: this._page - 1,
                size: this.pageSize(),
                flightNumber: dom.getValue("#filterFlightNumber")?.toUpperCase(),
                departureAirport: dom.getValue("#filterDeparture"),
                arrivalAirport: dom.getValue("#filterArrival"),
                airlineCode: dom.getValue("#filterAirline"),
                status: dom.getValue("#filterStatus")
            };
            Object.keys(params).forEach((k) => { if (params[k] === "" || params[k] == null) delete params[k]; });

            try {
                const res = await flightApi.getAdminFlights(params, true);
                const page = res?.result;
                this.render(page?.content ?? [], page?.totalCount ?? 0);
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '항공편 목록을 불러오지 못했습니다.');
            }
        },

        render(list, totalCount) {
            const body = dom.find("#flightTableBody");
            const template = dom.find("#flightRowTemplate");
            body.innerHTML = "";
            dom.setText("#totalCount", stringUtil.addComma(totalCount) || "0");

            if (list.length === 0) {
                body.innerHTML = `<tr class="admin-table-empty"><td colspan="${COLSPAN}">조회된 항공편이 없습니다.</td></tr>`;
            }

            list.forEach((f, i) => {
                const clone = template.content.cloneNode(true);
                const status = STATUS_VIEW[f.status] ?? {label: f.status ?? '-', badge: 'admin-badge-primary'};

                dom.setNodeText(clone, ".row-no", pagingUtil.pageIndex(i, this._page, this.pageSize()));
                dom.setNodeText(clone, ".row-number", f.flightNumber ?? "");
                const logo = clone.querySelector(".row-logo");
                if (f.logoUrl) { logo.src = f.logoUrl; logo.alt = f.airlineNameKo ?? ""; } else logo.remove();
                dom.setNodeText(clone, ".row-airline", f.airlineNameKo ?? f.airlineCode ?? "");
                dom.setNodeText(clone, ".row-route",
                    `${f.departureAirportNameKo ?? ""} (${f.departureAirport ?? ""}) → ${f.arrivalAirportNameKo ?? ""} (${f.arrivalAirport ?? ""})`);
                dom.setNodeText(clone, ".row-departure", hhmm(f.departureTime));
                dom.setNodeText(clone, ".row-arrival", hhmm(f.arrivalTime));
                dom.setNodeText(clone, ".row-price", won(f.basePrice));
                dom.setNodeText(clone, ".row-seats", `${f.totalSeats ?? 0}석`);

                const badge = clone.querySelector(".row-status");
                badge.textContent = status.label;
                badge.classList.add(status.badge);

                clone.querySelectorAll("button").forEach((b) => b.dataset.id = f.id);
                if (f.status !== 'active') {
                    clone.querySelector(".btn-delete").disabled = true;
                    clone.querySelector("tr").style.opacity = '0.6';
                }

                body.appendChild(clone);
            });

            dom.find("#pagination").innerHTML = totalCount > 0
                ? pagingUtil.setBootstrapPaging(this._page, totalCount, this.pageSize())
                : "";
        },

        async fetchDetail(id) {
            try {
                const res = await flightApi.getAdminFlightDetail(id, true);
                return res?.result ?? null;
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return null;
                await dialog.warning(error?.message || '항공편 정보를 불러오지 못했습니다.');
                return null;
            }
        },

        /**
         * 상세 팝업 (재고 현황 + 재고 생성)
         */
        async openDetail(id) {
            const d = await this.fetchDetail(id);
            if (!d) return;
            this.renderDetail(d);
            dialog.customOpen("#popFlightDetail");
        },

        renderDetail(d) {
            this._detail = d;
            const status = STATUS_VIEW[d.status] ?? {label: d.status ?? '-', badge: 'admin-badge-primary'};

            dom.setText("#detailFlightNumber", d.flightNumber ?? "");
            const logo = dom.find("#detailAirlineLogo");
            logo.src = d.logoUrl ?? "";
            logo.alt = d.airlineNameKo ?? "";
            logo.style.visibility = d.logoUrl ? "visible" : "hidden";
            dom.setText("#detailAirlineName", d.airlineNameKo ?? "-");
            dom.setText("#detailAirlineCode", d.airlineCode ?? "");
            dom.setText("#detailDepartureTime", hhmm(d.departureTime));
            dom.setText("#detailDepartureCity", d.departureAirportNameKo ?? "");
            dom.setText("#detailDepartureCode", d.departureAirport ?? "");
            dom.setText("#detailArrivalTime", hhmm(d.arrivalTime));
            dom.setText("#detailArrivalCity", d.arrivalAirportNameKo ?? "");
            dom.setText("#detailArrivalCode", d.arrivalAirport ?? "");
            dom.setText("#detailDuration", stringUtil.formatMinutes(d.durationMinutes) || "-");
            dom.setText("#detailPrice", won(d.basePrice));
            dom.setText("#detailTotalSeats", d.totalSeats ?? 0);

            const badge = dom.find("#detailStatusBadge");
            badge.className = `admin-badge ${status.badge}`;
            badge.textContent = status.label;

            dom.setText("#detailUpcomingCount", d.upcomingInventoryCount ?? 0);
            dom.setText("#detailLastInventory", d.lastInventoryDate ?? "-");
            dom.setText("#detailCreatedAt", dateTime(d.createdAt));
            dom.setText("#detailUpdatedAt", dateTime(d.updatedAt));

            // 재고 생성 기본값: 마지막 재고 다음날(없으면 내일)부터 30일
            const active = d.status === 'active';
            dom.find("#generateBox").classList.toggle("d-none", !active);
            if (active) {
                const tomorrow = new Date();
                tomorrow.setDate(tomorrow.getDate() + 1);
                let start = tomorrow;
                if (d.lastInventoryDate) {
                    const next = new Date(`${d.lastInventoryDate}T00:00:00`);
                    next.setDate(next.getDate() + 1);
                    if (next > start) start = next;
                }
                const end = new Date(start);
                end.setDate(end.getDate() + 29);
                const today = ymd(new Date());
                dom.find("#generateStart").setAttribute("min", today);
                dom.find("#generateEnd").setAttribute("min", today);
                dom.setValue("#generateStart", ymd(start));
                dom.setValue("#generateEnd", ymd(end));
            }

            this.renderInventories(d.upcomingInventories ?? []);
        },

        renderInventories(list) {
            const body = dom.find("#detailInventoryBody");
            body.innerHTML = "";
            this._inventories = {};
            if (list.length === 0) {
                body.innerHTML = `<tr class="admin-table-empty"><td colspan="4">오늘 이후 생성된 재고가 없습니다.</td></tr>`;
                return;
            }
            list.forEach((inv) => {
                this._inventories[inv.id] = inv;
                const tr = document.createElement("tr");
                [inv.flightDate, `${inv.availableSeats ?? 0}석`, `${inv.bookedSeats ?? 0}석`].forEach((value, idx) => {
                    const td = document.createElement("td");
                    td.textContent = value;
                    if (idx > 0) td.className = "text-center";
                    tr.appendChild(td);
                });

                const action = document.createElement("td");
                action.className = "admin-table-actions";
                const btn = document.createElement("button");
                btn.type = "button";
                btn.className = "admin-btn admin-btn-outline admin-btn-sm btn-adjust";
                btn.dataset.id = inv.id;
                btn.title = "재고 조정";
                btn.innerHTML = `<i class="bi bi-sliders"></i>`;
                action.appendChild(btn);
                tr.appendChild(action);

                body.appendChild(tr);
            });
        },

        /**
         * 재고 조정 팝업 (판매 가능 좌석: 0 ~ 총 좌석 - 확정 예약)
         */
        openAdjust(inventoryId) {
            const inv = this._inventories?.[inventoryId];
            const d = this._detail;
            if (!inv || !d) return;

            const total = d.totalSeats ?? 0;
            const booked = inv.bookedSeats ?? 0;
            this._adjustTarget = {id: inv.id, max: Math.max(0, total - booked)};

            dom.setText("#adjustTitle", `${d.flightNumber} · ${inv.flightDate}`);
            dom.setText("#adjustTotalSeats", total);
            dom.setText("#adjustBookedSeats", booked);
            dom.setText("#adjustCurrentSeats", inv.availableSeats ?? 0);
            dom.setText("#adjustRangeHint", `0 ~ ${this._adjustTarget.max}석 사이로 입력하세요.`);
            dom.setValue("#adjustAvailableSeats", inv.availableSeats ?? 0);
            dialog.customOpen("#popInventoryAdjust");
        },

        async saveAdjust() {
            const target = this._adjustTarget;
            if (!target) return;

            const raw = dom.getValue("#adjustAvailableSeats");
            const seats = Number(raw);
            if (raw === "" || !Number.isInteger(seats) || seats < 0 || seats > target.max) {
                await dialog.warning(`판매 가능 좌석은 0 ~ ${target.max}석 사이로 입력해주세요.`);
                return;
            }
            if (!(await dialog.confirm(`판매 가능 좌석을 ${seats}석으로 변경하시겠습니까?`))) return;

            try {
                await flightApi.adjustInventory(target.id, {availableSeats: seats}, true);
                dialog.customClose("#popInventoryAdjust");
                this._adjustTarget = null;
                await dialog.success("재고가 조정되었습니다.");
                const refreshed = await this.fetchDetail(this._detail.id);
                if (refreshed) this.renderDetail(refreshed);
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '재고를 조정하지 못했습니다.');
            }
        },

        /**
         * 재고 일괄 생성
         */
        async generateInventory() {
            const d = this._detail;
            if (!d) return;
            const startDate = dom.getValue("#generateStart");
            const endDate = dom.getValue("#generateEnd");
            if (!startDate || !endDate) {
                await dialog.warning("생성 기간을 입력해 주세요.");
                return;
            }
            if (startDate > endDate) {
                await dialog.warning("종료일이 시작일보다 빠릅니다.");
                return;
            }
            if (!(await dialog.confirm(`${d.flightNumber} 재고를 생성하시겠습니까?\n${startDate} ~ ${endDate}`))) return;

            try {
                const res = await flightApi.generateInventory(d.id, {startDate, endDate}, true);
                const created = res?.result?.createdCount ?? 0;
                const skipped = res?.result?.skippedCount ?? 0;
                await dialog.success(`재고가 생성되었습니다.\n생성 ${created}일 / 이미 있어 건너뜀 ${skipped}일`);
                const refreshed = await this.fetchDetail(d.id);
                if (refreshed) this.renderDetail(refreshed);
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '재고를 생성하지 못했습니다.');
            }
        },

        /**
         * 등록 / 수정 팝업 (id 없으면 등록)
         */
        async openForm(id) {
            this._editId = id;
            const number = dom.find("#formFlightNumber");
            const seats = dom.find("#formTotalSeats");

            if (id) {
                const d = await this.fetchDetail(id);
                if (!d) return;
                dom.setText("#flightFormTitle", "항공편 수정");
                dom.setValue("#formAirline", d.airlineCode ?? "");
                number.value = d.flightNumber ?? "";
                number.disabled = true;
                dom.setValue("#formDeparture", d.departureAirport ?? "");
                dom.setValue("#formArrival", d.arrivalAirport ?? "");
                dom.setValue("#formDepartureTime", hhmm(d.departureTime));
                dom.setValue("#formArrivalTime", hhmm(d.arrivalTime));
                dom.setValue("#formPrice", stringUtil.addComma(d.basePrice ?? ""));
                seats.value = d.totalSeats ?? "";
                const locked = !!d.lastInventoryDate;
                seats.disabled = locked;
                dom.find("#seatLockHint").classList.toggle("d-none", !locked);
                dom.setValue("#formStatus", d.status ?? "active");
            } else {
                dom.setText("#flightFormTitle", "항공편 등록");
                ["#formAirline", "#formDeparture", "#formArrival", "#formDepartureTime", "#formArrivalTime", "#formPrice"]
                    .forEach((sel) => dom.setValue(sel, ""));
                number.value = "";
                number.disabled = false;
                seats.value = "";
                seats.disabled = false;
                dom.find("#seatLockHint").classList.add("d-none");
                dom.setValue("#formStatus", "active");
            }
            this.updateDuration();
            dialog.customOpen("#popFlightForm");
        },

        /**
         * 소요 시간 표시 (도착이 더 이르면 다음날 도착)
         */
        updateDuration() {
            const dep = dom.getValue("#formDepartureTime");
            const arr = dom.getValue("#formArrivalTime");
            if (!dep || !arr) {
                dom.setText("#formDuration", "-");
                return;
            }
            const toMin = (t) => Number(t.substring(0, 2)) * 60 + Number(t.substring(3, 5));
            let minutes = toMin(arr) - toMin(dep);
            if (minutes <= 0) minutes += 24 * 60;
            dom.setText("#formDuration", `${stringUtil.formatMinutes(minutes)}${toMin(arr) <= toMin(dep) ? ' (+1일)' : ''}`);
        },

        /**
         * 저장 (등록 / 수정)
         */
        async saveFlight() {
            const items = {
                airlineCode: dom.getValue("#formAirline"),
                flightNumber: dom.getValue("#formFlightNumber")?.toUpperCase(),
                departureAirport: dom.getValue("#formDeparture"),
                arrivalAirport: dom.getValue("#formArrival"),
                departureTime: dom.getValue("#formDepartureTime"),
                arrivalTime: dom.getValue("#formArrivalTime"),
                basePrice: stringUtil.removeComma(dom.getValue("#formPrice")),
                totalSeats: dom.getValue("#formTotalSeats"),
                status: dom.getValue("#formStatus")
            };

            const requiredErrors = validUtil.requiredError(items, {
                airlineCode: "항공사를 선택해 주세요.",
                flightNumber: "편명을 입력해 주세요.",
                departureAirport: "출발지를 선택해 주세요.",
                arrivalAirport: "도착지를 선택해 주세요.",
                departureTime: "출발 시간을 입력해 주세요.",
                arrivalTime: "도착 시간을 입력해 주세요.",
                basePrice: "기본 운임을 입력해 주세요.",
                totalSeats: "총 좌석 수를 입력해 주세요.",
            });
            if (requiredErrors) {
                await dialog.warning(requiredErrors);
                return;
            }

            const rulesError = validUtil.rulesError(items, {
                flightNumber: [
                    v => /^[A-Z0-9]{2}\d{1,4}$/.test(v) || '편명 형식이 올바르지 않습니다. (예: KE1234)',
                    v => v.startsWith(items.airlineCode) || `편명은 항공사 코드(${items.airlineCode})로 시작해야 합니다.`
                ],
                arrivalAirport: [
                    v => v !== items.departureAirport || '출발지와 도착지가 같습니다.'
                ],
                arrivalTime: [
                    v => v !== items.departureTime || '출발 시간과 도착 시간이 같습니다.'
                ],
                basePrice: [
                    v => Number(v) > 0 || '기본 운임을 확인해 주세요.'
                ],
                totalSeats: [
                    v => (Number(v) >= 6 && Number(v) <= 300) || '총 좌석 수는 6~300석이어야 합니다.'
                ]
            });
            if (rulesError) {
                await dialog.warning(rulesError);
                return;
            }

            const isEdit = !!this._editId;
            if (!(await dialog.confirm(`항공편을 ${isEdit ? '수정' : '등록'}하시겠습니까?`))) return;

            const data = {
                airlineCode: items.airlineCode,
                departureAirport: items.departureAirport,
                arrivalAirport: items.arrivalAirport,
                departureTime: items.departureTime,
                arrivalTime: items.arrivalTime,
                basePrice: Number(items.basePrice),
                totalSeats: Number(items.totalSeats),
                status: items.status
            };

            try {
                if (isEdit) {
                    await flightApi.updateFlight(this._editId, data, true);
                } else {
                    await flightApi.createFlight({...data, flightNumber: items.flightNumber}, true);
                }
                dialog.customClose("#popFlightForm");
                await dialog.success(`항공편이 ${isEdit ? '수정' : '등록'}되었습니다.`);
                if (!isEdit) this._page = 1;
                await this.loadFlights();
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '저장하지 못했습니다.');
            }
        },

        /**
         * 운항 중지
         */
        async deactivate(id) {
            const ok = await dialog.confirm("항공편 운항을 중지하시겠습니까?\n사용자 검색에서 제외되며, 기존 예약은 유지됩니다.\n수정에서 '운항'으로 바꾸면 다시 판매됩니다.");
            if (!ok) return;

            try {
                await flightApi.deleteFlight(id, true);
                await dialog.success("운항이 중지되었습니다.");
                await this.loadFlights();
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '처리하지 못했습니다.');
            }
        },
    };

    await flights.init();
}
