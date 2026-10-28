/**
 * packageName    :
 * fileName       : main
 * author         : MiracleCat
 * date           : 2026-09-29
 * description    : 랜딩 페이지
 */
// Utils
import {ajax, dialog, dom, global, loadApis, loadUtils} from '@core/core.js';

document.addEventListener("DOMContentLoaded", () => {
    initMain();
});

async function initMain() {
    // 추가 Utils & Module
    const {stringUtil, dateUtil, validUtil} = await loadUtils(
        ['stringUtil', 'dateUtil', 'validUtil']
    );
    const main = {
        /**
         * 초기화
         */
        async init() {

            // 초기 값 설정
            this.initValues();

            // 이벤트 핸들러
            this.bindEvents();
        },

        /**
         * 초기값 설정
         */
        initValues() {
            // 오늘 날짜 최소값 설정
            const d = new Date();
            d.setDate(d.getDate() + 1);
            const tomorrow = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
            dom.find("#departureDate").setAttribute('min', tomorrow);
            dom.setValue("#departureDate", tomorrow);
        },

        /**
         * 이벤트 등록
         */
        bindEvents() {

            /**
             * 버튼 이벤트
             */
            // 항공편 검색
            dom.on("#btnSearch", "click", () => {
                this.flightSearch()
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
            const items = {
                departureAirport: dom.getValue("#departureAirport"),
                arrivalAirport: dom.getValue("#arrivalAirport"),
                departureDate: dom.getValue("#departureDate"),
                passengerCount: dom.getValue("#passengerCount")
            };

            // 1차 검증
            const required = {
                departureAirport: "출발지를 선택해 주세요.",
                arrivalAirport: "도착지를 선택해 주세요.",
                departureDate: "출발일을 선택해 주세요.",
                passengerCount: "승객 수를 선택해 주세요.",
            }
            const requiredErrors = validUtil.requiredError(items, required);
            if (requiredErrors) {
                await dialog.warning(requiredErrors);
                return false;
            }

            // 2차 검증
            let rules = {
                departureAirport: [
                    v => items.departureAirport !== items.arrivalAirport || '출발지와 도착지가 동일합니다.',
                ],
                passengerCount: [
                    v => items.passengerCount > 0 && items.passengerCount <= 9 || '승객 수는 1명 이상 9명 이하여야 합니다.',
                ]
            };
            const rulesError = validUtil.rulesError(items, rules);
            if (rulesError) {
                await dialog.warning(rulesError);
                return false;
            }

            items.departureDate = stringUtil.replaceHyphen(items.departureDate);

            location.href = `/flight/flight-list/${items.departureAirport}/${items.arrivalAirport}/${items.departureDate}/${items.passengerCount}`;
        },

    };

    // init start
    await main.init();

}