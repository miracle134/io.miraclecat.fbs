/**
 * packageName    :
 * fileName       : flight-list
 * author         : MiracleCat
 * date           : 2026-10-03
 * description    : 항공권 검색 조회 페이지
 */
import {ajax, dialog, dom, global, loadApis, loadUtils} from '@core/core.js';
const searchValue = window.searchValue;

document.addEventListener("DOMContentLoaded", () => {
    initFlightList();
});

async function initFlightList() {
    // 추가 Utils & Module
    const {stringUtil, dateUtil, pagingUtil, validUtil} = await loadUtils(
        ['stringUtil', 'dateUtil', 'pagingUtil', 'validUtil']
    );
    const {flightApi, bookingApi} = await loadApis(['flightApi', 'bookingApi']);
    const flightList = {
        /**
         * 초기화
         */
        async init() {

            // 초기 값 설정
            this.initValues();

            // 이벤트 핸들러
            this.bindEvents();

            // 항공편 검색
            await this.flightSearch()
        },

        /**
         * 초기값 설정
         */
        initValues() {
            // 헤더 값 설정
            searchValue.date = dateUtil.formatDay(searchValue.date, 'YYYY-MM-DD', 'YYYYMMDD')
            dom.setText("#departureAirport", searchValue.src);
            dom.setText("#arrivalAirport", searchValue.dst);
            dom.setText("#departureDate", searchValue.date);
            dom.setText("#passengerCount", searchValue.counts);

            this.initEditForm();
        },

        initEditForm() {
            const d = new Date();
            d.setDate(d.getDate() + 1);
            const tomorrow = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
            dom.find("#editDepartureDate").setAttribute('min', tomorrow);

            dom.setValue("#editDepartureAirport", searchValue.src);
            dom.setValue("#editArrivalAirport", searchValue.dst);
            dom.setValue("#editDepartureDate", searchValue.date);
            dom.setValue("#editPassengerCount", String(searchValue.counts));
        },

        async moveSearch() {
            const items = {
                departureAirport: dom.getValue("#editDepartureAirport"),
                arrivalAirport: dom.getValue("#editArrivalAirport"),
                departureDate: dom.getValue("#editDepartureDate"),
                passengerCount: dom.getValue("#editPassengerCount")
            };

            const requiredErrors = validUtil.requiredError(items, {
                departureAirport: "출발지를 선택해 주세요.",
                arrivalAirport: "도착지를 선택해 주세요.",
                departureDate: "출발일을 선택해 주세요.",
                passengerCount: "승객 수를 선택해 주세요.",
            });
            if (requiredErrors) {
                await dialog.warning(requiredErrors);
                return;
            }

            const min = dom.find("#editDepartureDate").getAttribute('min');
            const rulesError = validUtil.rulesError(items, {
                departureAirport: [
                    v => items.departureAirport !== items.arrivalAirport || '출발지와 도착지가 동일합니다.',
                ],
                departureDate: [
                    v => items.departureDate >= min || '출발일은 내일 이후로 선택해 주세요.',
                ],
                passengerCount: [
                    v => items.passengerCount > 0 && items.passengerCount <= 9 || '승객 수는 1명 이상 9명 이하여야 합니다.',
                ]
            });
            if (rulesError) {
                await dialog.warning(rulesError);
                return;
            }

            const date = stringUtil.replaceHyphen(items.departureDate);
            location.href = `/flight/flight-list/${items.departureAirport}/${items.arrivalAirport}/${date}/${items.passengerCount}`;
        },

        /**
         * 이벤트 등록
         */
        bindEvents() {

            /**
             * 버튼 이벤트
             */
            // 가격 범위 text 수정
            dom.on("#priceRange", "input", (e) => {
                dom.setText("#priceRangeTxt", `${stringUtil.addComma(e.target.min)}원 ~ ${stringUtil.addComma(e.target.value)}원`);
            });

            // 가격 범위 & 정렬 & 출발 시간대 & 항공사 변경 감지 및 조회
            dom.onMultiple("#priceRange, #sortSelect, .dst-time-slot, .airline-code", "change", async (e) => {
                await this.flightSearch();
            })

            // 필터 초기화
            dom.on("#resetFilters", "click", () => {
                const priceRange = dom.find("#priceRange");
                priceRange.value = 200000;
                priceRange.dispatchEvent(new Event('input', { bubbles: true }));

                dom.setCheckedAll("#filterBar input[type=checkbox]", false);

                this.flightSearch();
            });

            // 항공편 검색
            dom.on("#btnSearch", "click", async () => {
                await this.flightSearch()
            });

            // 검색 수정 폼 토글
            dom.on("#btnToggleSearch", "click", () => {
                const form = dom.find("#searchEditForm");
                const opening = form.classList.contains('d-none');
                if (opening) this.initEditForm();
                form.classList.toggle('d-none');
            });

            // 출발지/도착지 바꾸기
            dom.on("#btnSwapAirport", "click", () => {
                const src = dom.getValue("#editDepartureAirport");
                dom.setValue("#editDepartureAirport", dom.getValue("#editArrivalAirport"));
                dom.setValue("#editArrivalAirport", src);
            });

            // 수정 검색 → 페이지 이동
            dom.on("#btnEditSearch", "click", async () => {
                await this.moveSearch();
            });

            // 인기 노선 선택
            dom.onMultiple(".route-card", "click", (e) => {
                dom.setValue("#departureAirport", dom.getDataset(e.currentTarget, "departure"));
                dom.setValue("#arrivalAirport", dom.getDataset(e.currentTarget, "arrival"));

                // 스크롤 애니메이션
                document.querySelector('.search-card').scrollIntoView({
                    behavior: 'smooth',
                    block: 'center'
                });
            });

        },

        /**
         * 항공편 검색
         */
        async flightSearch() {
            try {
                // 데이터
                const searchItems = {
                    departureAirport: searchValue.src,
                    arrivalAirport: searchValue.dst,
                    flightDate: searchValue.date,
                    passengers: searchValue.counts,
                    minPrice: dom.find("#priceRange").min,
                    maxPrice: dom.getValue("#priceRange"),
                    sortType: dom.getValue("#sortSelect")
                };

                const departureTimeSlots = Array.from(
                    document.querySelectorAll('.dst-time-slot:checked')
                ).map(el => el.value);

                const airlineCodes = Array.from(
                    document.querySelectorAll('.airline-code:checked')
                ).map(el => el.value);

                searchItems.departureTimeSlots = departureTimeSlots;
                searchItems.airlineCodes = airlineCodes;

                // 항공편 검색
                const data = await flightApi.getFlightsSearch(searchItems);

                // 총 카운트
                const totalCount = data?.result?.totalCount || 0;
                dom.setText("#totalCount", totalCount);

                // 리스트 렌더링
                this.renderFlightList(data?.result?.content || []);

            } catch (error) {
                console.error("flightSearch error", error);
            }
        },

        /**
         * 항공편 검색 렌더링
         */
        renderFlightList(content) {
            const body = dom.find("#flightList");
            if (!body) return;

            body.innerHTML = "";
            const template = dom.find("#flightListTemplate");
            if (!template) return;
            if (content.length > 0) {
                content.forEach((result, i) => {
                    const clone = template.content.cloneNode(true);

                    let id = result?.id ?? "";
                    let flightInventoryId = result?.flightInventoryId ?? "";
                    let logoUrl = result?.logoUrl ?? "";
                    let airlineNameKo = result?.airlineNameKo ?? "";
                    let flightNumber = result?.flightNumber ?? "";
                    let departureTime = result?.departureTime?.substring(0, 5) ?? "-";
                    let departureAirport = result?.departureAirport ?? "";
                    let durationMinutes = stringUtil.formatMinutes(result?.durationMinutes) ?? "";
                    let arrivalTime = result?.arrivalTime?.substring(0, 5) ?? "-";
                    let arrivalAirport = result?.arrivalAirport ?? "";
                    let basePrice = stringUtil.addComma(result?.basePrice ?? "0");
                    let availableSeats = result?.availableSeats ?? "";

                    dom.setNodeDataset(clone, ".flight-id", "flightId", id);
                    dom.setNodeDataset(clone, ".flight-id", "flightInventoryId", flightInventoryId);
                    dom.setNodeText(clone, ".airlineNameKo", airlineNameKo);
                    dom.setNodeText(clone, ".flightNumber", flightNumber);
                    dom.setNodeText(clone, ".departureAirport", departureAirport);
                    dom.setNodeText(clone, ".arrivalAirport", arrivalAirport);
                    dom.setNodeText(clone, ".departureTime", departureTime);
                    dom.setNodeText(clone, ".arrivalTime", arrivalTime);
                    dom.setNodeText(clone, ".durationMinutes", durationMinutes);
                    dom.setNodeText(clone, ".basePrice", basePrice);
                    dom.setNodeText(clone, ".availableSeats", availableSeats);
                    clone.querySelector('.logoUrl').src = logoUrl;

                    dom.nodeOn(clone, ".btnBooking", "click", async (e) => {
                        const el = e.target.closest(".flight-id");
                        try {
                            const res = await bookingApi.createSession({
                                inventoryId: Number(dom.getDataset(el, "flightInventoryId")),
                                passengerCount: Number(searchValue.counts)
                            }, true);
                            const sessionId = res?.result?.sessionId;
                            if (!sessionId) return;
                            location.href = `/flight/flight-booking/${sessionId}`;
                        } catch (error) {
                            if (error?.name === 'HandledForbiddenError') return;
                            await dialog.warning(error?.message || '예약을 시작할 수 없습니다.');
                        }
                    });

                    body.appendChild(clone);
                });
            } else {
                body.innerHTML = pagingUtil.getDivRow();
            }
        },

    };

    // init start
    await flightList.init();

}