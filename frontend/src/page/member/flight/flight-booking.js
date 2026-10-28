/**
 * packageName    :
 * fileName       : flight-booking
 * author         : MiracleCat
 * date           : 2026-10-03
 * description    : 항공권 예약 페이지
 */
import {ajax, dialog, dom, global, loadApis, loadUtils} from '@core/core.js';
import seatSelect from '@feature/seat-select';
import seatSocket from '@feature/seat-socket';
const bookingNo = window.bookingNo;

document.addEventListener("DOMContentLoaded", () => {
    initFlightBooking();
});

async function initFlightBooking() {
    // 추가 Utils & Module
    const {stringUtil, regexUtil} = await loadUtils(['stringUtil', 'regexUtil']);
    const {flightApi, bookingApi, paymentApi} = await loadApis(['flightApi', 'bookingApi', 'paymentApi']);
    const flightBooking = {
        _bookingInfo: null,
        _seatPrepared: false,
        _clockOffset: 0,
        _timerId: null,
        _timerPaused: false,
        _warnedLowTime: false,
        _expiring: false,
        _holdTimerId: null,
        _paying: false,

        /**
         * 초기화
         */
        async init() {

            // 토스 결제창 복귀 처리 (성공: 승인 후 종료 / 실패·취소: 안내 후 예약 화면 계속)
            const params = new URLSearchParams(location.search);
            if (params.get('paymentKey')) {
                await this.handlePaymentSuccess(params);
                return;
            }
            if (params.get('code')) {
                history.replaceState(null, '', location.pathname);
                await dialog.warning(params.get('message') || '결제가 취소되었습니다.');
            }

            // 예약 세션 조회
            this._bookingInfo = await this.loadSession();

            // 검증
            if(this._bookingInfo === null){
                if(await dialog.warning("만료된 예약입니다. 다시 예약해주세요.")){
                    location.href = "/";
                }

                return false;
            }

            // 남은 시간 표시
            this.startTimer();

            // 초기 값 설정
            this.initValues();

            // 이벤트 핸들러
            this.bindEvents();

            // 항공편 상세 조회
            await this.flightDetail()

            // 좌석 실시간 반영 (구독 먼저 연결 후 좌석 상태 조회)
            await this.initSeats();
        },

        /**
         * 예약 세션 조회 (서버 세션 기준, 만료·타인 세션이면 null)
         */
        async loadSession() {
            try {
                const res = await bookingApi.getSession(bookingNo);
                const session = res?.result;
                if (!session) return null;

                return {
                    session,
                    searchValue: {
                        src: session.departureAirport,
                        dst: session.arrivalAirport,
                        date: session.flightDate,
                        counts: session.passengerCount
                    },
                    flightId: session.flightId,
                    flightInventoryId: session.inventoryId
                };
            } catch (error) {
                console.error("loadSession error", error);
                return null;
            }
        },

        /**
         * 남은 시간 카운트다운 시작 (서버 시각 기준으로 보정)
         */
        startTimer() {
            const session = this._bookingInfo.session;
            this._clockOffset = (session.serverTime ?? Date.now()) - Date.now();

            if (this._timerId) clearInterval(this._timerId);
            this.tickTimer();
            this._timerId = setInterval(() => this.tickTimer(), 1000);
        },

        /**
         * 서버 기준 남은 시간(ms)
         */
        remainMs() {
            return this._bookingInfo.session.expireAt - (Date.now() + this._clockOffset);
        },

        /**
         * 1초마다 남은 시간 갱신, 3분 남으면 1회 안내, 0이면 만료 처리
         */
        tickTimer() {
            if (this._timerPaused || this._expiring) return;

            const remain = this.remainMs();
            if (remain <= 0) {
                this.expireBooking();
                return;
            }

            const totalSec = Math.ceil(remain / 1000);
            const mm = String(Math.floor(totalSec / 60)).padStart(2, '0');
            const ss = String(totalSec % 60).padStart(2, '0');
            dom.setText("#bookingTimerText", `${mm}:${ss}`);

            const badge = dom.find("#bookingTimer");
            const danger = remain <= 60 * 1000;
            badge.classList.toggle("bg-light", !danger);
            badge.classList.toggle("text-primary", !danger);
            badge.classList.toggle("bg-danger", danger);
            badge.classList.toggle("text-white", danger);

            if (!this._warnedLowTime && remain <= 3 * 60 * 1000) {
                this._warnedLowTime = true;
                dialog.warning("예약 가능 시간이 3분 남았습니다. 시간이 지나면 처음부터 다시 진행해야 합니다.");
            }
        },

        /**
         * 예약 가능 시간 만료 → 선점 해제, 세션 삭제, 같은 조건의 목록으로 이동
         */
        async expireBooking() {
            if (this._expiring) return;
            this._expiring = true;
            if (this._timerId) clearInterval(this._timerId);
            this.stopHoldRefresh();
            dom.setText("#bookingTimerText", "00:00");

            seatSocket.disconnect();
            const mySeats = Object.values(seatSelect.getSeatMap()).filter(Boolean);
            await Promise.allSettled([
                ...mySeats.map((code) => this.releaseSeat(code)),
                bookingApi.deleteSession(bookingNo)
            ]);

            await dialog.warning("예약 가능 시간이 지나 좌석 및 운임 정보가 변경되었을 수 있습니다. 최신 정보로 다시 조회합니다.");
            location.href = this._bookingInfo.session.listUrl || "/";
        },

        /**
         * 초기값 설정
         */
        initValues() {
            // 승객 설정
            const body = dom.find("#passengerForms");
            const template = dom.find("#passengerFormTemplate");
            body.innerHTML = "";
            for (let i = 1; i <= this._bookingInfo.searchValue.counts; i++) {
                const clone = template.content.cloneNode(true);

                dom.setNodeText(clone, ".passenger-index", i);

                body.appendChild(clone);
            }
        },

        /**
         * 이벤트 등록
         */
        bindEvents() {

            /**
             * 입력 제어 (승객 폼은 동적 생성 → 이벤트 위임)
             */
            dom.delegate("#passengerForms", "input", ".passenger-tel", (e) => {
                e.target.value = stringUtil.formatPhoneNumber(e.target.value);
            });
            dom.delegate("#passengerForms", "input", ".passenger-email", (e) => {
                e.target.value = stringUtil.filterEmail(e.target.value, 64);
            });

            /**
             * 버튼 이벤트
             */
            // 좌석선택
            dom.on("#btnSeatSelect", "click", async () => {
                await seatSelect.show();
            });

            // 결제하기
            dom.on("#btnPayment", "click", async () => {
                await this.paymentProcess();
            });

            // 페이지 이탈 시 웹소켓 종료 (선점은 TTL로 자동 만료)
            window.addEventListener("pagehide", () => {
                this.stopHoldRefresh();
                seatSocket.disconnect();
            });

        },

        /**
         * 항공편 상세 조회
         */
        async flightDetail() {
            try {
                // 데이터

                // 항공편 상세 조회
                const [detail, inven] = await Promise.all([
                    flightApi.getFlightDetail(this._bookingInfo.flightId),
                    flightApi.getInventoryDetail(this._bookingInfo.flightInventoryId)
                ]);

                // 렌더링
                this.renderFlightDetail(detail?.result || {}, inven?.result || {});

            } catch (error) {
                console.error("flightSearch error", error);
            }
        },

        /**
         * 항공편 상세 조회 렌더링
         */
        renderFlightDetail(result, inven) {
            let logoUrl = result?.logoUrl ?? "";
            let airlineNameKo = result?.airlineNameKo ?? "";
            let flightNumber = result?.flightNumber ?? "";
            let departureTime = result?.departureTime?.substring(0, 5) ?? "-";
            let departureAirport = result?.departureAirport ?? "";
            let durationMinutes = stringUtil.formatMinutes(result?.durationMinutes) ?? "";
            let arrivalTime = result?.arrivalTime?.substring(0, 5) ?? "-";
            let arrivalAirport = result?.arrivalAirport ?? "";
            const session = this._bookingInfo.session;
            let basePrice = session.basePrice;
            let perBasePrice = session.fareAmount;
            let perFeePrice = session.bookingFeeAmount;
            let totalPrice = session.totalAmount;

            dom.find('#logoUrl').src = logoUrl;
            dom.setText( "#airlineNameKo", airlineNameKo);
            dom.setText( "#flightNumber", flightNumber);
            dom.setText("#departureTime", departureTime);
            dom.setText("#departureAirport", departureAirport);
            dom.setText("#departureDate", this._bookingInfo.searchValue.date);
            dom.setText("#durationMinutes", durationMinutes);
            dom.setText("#arrivalTime", arrivalTime);
            dom.setText("#arrivalAirport", arrivalAirport);
            dom.setText("#arrivalDate", this._bookingInfo.searchValue.date);
            dom.findAll(".passengerCountText").forEach(e => e.textContent = this._bookingInfo.searchValue.counts);
            dom.setText("#basePrice", `${stringUtil.addComma(basePrice ?? "0")}원`);
            dom.setText("#perBasePric", `${stringUtil.addComma(perBasePrice ?? "0")}원`);
            dom.setText("#feePrice", `${stringUtil.addComma(perFeePrice ?? "0")}원`);
            dom.setText("#totalPrice", `${stringUtil.addComma(totalPrice ?? "0")}원`);
            //         dom.setNodeText(".id", id);
            //         dom.setNodeText(clone, ".basePrice", basePrice);
            //         dom.setNodeText(clone, ".availableSeats", availableSeats);
            //
            //         dom.nodeOn(clone, ".btnBooking", "click", async () => {
            //             dialog.alert('예약하기')
            //         });
        },

        /**
         * 좌석 웹소켓 연결 + 최초 좌석 상태 조회
         * onConnected: 최초 연결·재연결마다 좌석 상태 재조회 (끊긴 동안 놓친 이벤트 보정)
         */
        async initSeats() {
            const inventoryId = this._bookingInfo.flightInventoryId;

            seatSocket.connect(inventoryId, (event) => this.onSeatEvent(event), {
                onConnected: () => this.loadSeatStatus()
            });

            await this.loadSeatStatus();
        },

        /**
         * 좌석 상태 조회 → 최초 1회 좌석맵 생성, 이후에는 점유 상태만 동기화
         */
        async loadSeatStatus() {
            try {
                const res = await flightApi.getSeatStatus(this._bookingInfo.flightInventoryId);
                const status = res?.result;
                if (!status) return;

                const occupied = [...(status.booked ?? []), ...(status.held ?? [])];

                if (!this._seatPrepared) {
                    this._seatPrepared = true;
                    seatSelect.prepare({
                        total: status.totalSeats,
                        occupied,
                        passengerCount: this._bookingInfo.searchValue.counts,
                        mySeats: status.myHeld ?? [],
                        onHold: (code) => this.holdSeat(code),
                        onRelease: (code) => this.releaseSeat(code)
                    });
                    this.startHoldRefresh();
                } else {
                    seatSelect.syncOccupied(occupied);
                }
            } catch (error) {
                console.error("loadSeatStatus error", error);
            }
        },

        /**
         * 웹소켓 좌석 이벤트 반영 (본인 이벤트는 seatSocket에서 걸러짐)
         */
        onSeatEvent(event) {
            const status = event.type === 'RELEASED' ? 'available' : 'occupied';
            (event.seats ?? []).forEach((code) => seatSelect.applyRemoteSeatUpdate(code, status));
        },

        /**
         * 좌석 선점 요청 → 성공 시 true
         */
        async holdSeat(code) {
            try {
                await flightApi.holdSeat(this._bookingInfo.flightInventoryId, code, {clientId: seatSocket.getClientId()});
                return true;
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return false;
                if (error?.status === 409) {
                    seatSelect.applyRemoteSeatUpdate(code, 'occupied');
                }
                await dialog.warning(error?.message || '좌석을 선택할 수 없습니다.');
                return false;
            }
        },

        /**
         * 예약 화면이 열려 있는 동안 2분마다 내 선점 연장 (선점 TTL 5분)
         */
        startHoldRefresh() {
            if (this._holdTimerId) clearInterval(this._holdTimerId);
            this._holdTimerId = setInterval(() => this.refreshHolds(), 2 * 60 * 1000);
        },

        stopHoldRefresh() {
            if (this._holdTimerId) clearInterval(this._holdTimerId);
            this._holdTimerId = null;
        },

        /**
         * 내 선점 일괄 연장 → 이미 풀린 좌석은 재선점, 다른 사람이 가져갔으면 배정 해제
         * @return {Promise<boolean>} 내 좌석이 모두 유지되면 true
         */
        async refreshHolds() {
            if (this._expiring) return false;
            const mySeats = Object.values(seatSelect.getSeatMap()).filter(Boolean);
            if (mySeats.length === 0) return true;

            const inventoryId = this._bookingInfo.flightInventoryId;
            let extended;
            try {
                const res = await flightApi.extendSeatHolds(inventoryId);
                extended = new Set(res?.result?.seats ?? []);
            } catch (error) {
                console.error("refreshHolds error", error);
                return false;
            }

            let allKept = true;
            for (const code of mySeats.filter((c) => !extended.has(c))) {
                try {
                    await flightApi.holdSeat(inventoryId, code, {clientId: seatSocket.getClientId()});
                } catch (error) {
                    if (error?.name === 'HandledForbiddenError') return false;
                    allKept = false;
                    seatSelect.applyRemoteSeatUpdate(code, 'occupied');
                }
            }
            return allKept;
        },

        /**
         * 좌석 선점 해제 요청
         */
        async releaseSeat(code) {
            await flightApi.releaseSeat(this._bookingInfo.flightInventoryId, code, {clientId: seatSocket.getClientId()});
        },

        /**
         * 승객정보 get data
         */
        collectPassengers(containerSelector = '#passengerForms') {
            const FIELDS = {
                name: '.passenger-name',
                nameEn: '.passenger-name-en',
                birth: '.passenger-birth',
                gender: '.passenger-gender',
                tel: '.passenger-tel',
                email: '.passenger-email'
            };

            const container = document.querySelector(containerSelector);
            if (!container) return [];

            return Array.from(container.querySelectorAll('.passenger-form')).map((form, i) => {
                const value = (selector) => {
                    const el = form.querySelector(selector);
                    return el ? el.value.trim() : '';
                };
                const indexEl = form.querySelector('.passenger-index');

                return {
                    index: indexEl ? Number(indexEl.textContent) : i + 1,
                    name: value(FIELDS.name),
                    nameEn: value(FIELDS.nameEn),
                    birth: value(FIELDS.birth),
                    gender: value(FIELDS.gender),
                    tel: value(FIELDS.tel),
                    email: value(FIELDS.email)
                };
            });
        },

        /**
         * 승객정보 data validation
         */
        validatePassengers(passengers) {
            const FIELD_LABELS = {
                name: '이름(한글)',
                nameEn: '이름(영문)',
                birth: '생년월일',
                gender: '성별',
                tel: '전화번호',
                email: '이메일'
            };

            const today = new Date();
            const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

            for (const p of passengers) {
                for (const key of Object.keys(FIELD_LABELS)) {
                    if (!p[key]) {
                        dialog.warning('승객' + p.index + '의 ' + FIELD_LABELS[key] + '을 입력해주세요.');
                        return false;
                    }
                }
                // 형식 검사 (서버 검증과 동일 규칙)
                const formatError =
                    !regexUtil.isEmail(p.email) ? '이메일 형식이 올바르지 않습니다.'
                    : !regexUtil.isContactNumber(p.tel) ? '전화번호 형식이 올바르지 않습니다.'
                    : p.birth > todayStr ? '생년월일이 올바르지 않습니다.'
                    : null;
                if (formatError) {
                    dialog.warning('승객' + p.index + '의 ' + formatError);
                    return false;
                }
            }
            return true;
        },

        /**
         * 결제
         */
        async paymentProcess() {
            const items = {};

            // 좌석 선택 검사
            if(!seatSelect.validateSeats()){
                return;
            }

            // 승객 정보 검사
            const passengers  = this.collectPassengers();
            if(this.validatePassengers(passengers)){
                items.passengerInfos = await seatSelect.mapSeatsToPassengers(seatSelect.getSeatMap(), passengers);
            }else{
                return;
            }

            if(!dom.getChecked("#agreeTerms")){
                await dialog.alert("운임 규정 및 취소 환불 규정에 동의해주세요.");
                return;
            }

            if(!dom.getChecked("#agreePrivacy")){
                await dialog.alert("개인정보 제공에 동의해주세요.");
                return;
            }

            if (this._paying) return;
            this._paying = true;
            try {
                await this.startPayment(items.passengerInfos);
            } finally {
                this._paying = false;
            }
        },

        /**
         * 결제 시작: 선점 확인 → 결제 준비(서버 검증·세션 연장·주문번호) → 토스 결제창
         */
        async startPayment(passengerInfos) {
            if (this.remainMs() <= 0) {
                await this.expireBooking();
                return;
            }

            if (!(await this.refreshHolds())) {
                await dialog.warning("선택한 좌석 중 다른 사용자에게 선점된 좌석이 있습니다. 좌석을 다시 선택해주세요.");
                return;
            }

            const passengers = passengerInfos.map((p) => ({
                name: p.name,
                nameEn: p.nameEn,
                email: p.email,
                mobile: p.tel,
                birthDate: p.birth,
                gender: p.gender,
                seatNumber: p.seat
            }));

            let prepared;
            try {
                const res = await paymentApi.preparePayment({sessionId: bookingNo, passengers}, true);
                prepared = res?.result;
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                if (error?.status === 408) {
                    await this.expireBooking();
                    return;
                }
                if (error?.status === 409) {
                    await this.loadSeatStatus();
                }
                await dialog.warning(error?.message || '결제를 시작할 수 없습니다.');
                return;
            }
            if (!prepared) return;

            // 결제 중에는 카운트다운·선점 연장 정지 (서버가 세션·선점을 결제 제한시간까지 연장함)
            this._bookingInfo.session.expireAt = prepared.expireAt;
            this._clockOffset = prepared.serverTime - Date.now();
            this._timerPaused = true;
            this.stopHoldRefresh();

            try {
                await this.requestTossPayment(prepared);
            } catch (error) {
                // 리다이렉트 없이 결제창이 닫히거나 오류가 난 경우 → 예약 화면 계속
                this._timerPaused = false;
                this.startHoldRefresh();
                this.tickTimer();
                if (error?.code !== 'USER_CANCEL') {
                    await dialog.warning(error?.message || '결제를 진행하지 못했습니다.');
                }
            }
        },

        /**
         * 토스 결제창 호출 (API 개별 연동 - 결제창 v2)
         * 성공·실패 모두 현재 예약 화면으로 복귀하고, 쿼리 파라미터로 구분
         */
        async requestTossPayment(prepared) {
            if (!window.TossPayments || !window.tossClientKey) {
                throw new Error('결제 모듈을 불러오지 못했습니다.');
            }
            const tossPayments = window.TossPayments(window.tossClientKey);
            const payment = tossPayments.payment({customerKey: window.TossPayments.ANONYMOUS});
            const returnUrl = `${location.origin}${location.pathname}`;

            await payment.requestPayment({
                method: 'CARD',
                amount: {currency: 'KRW', value: prepared.amount},
                orderId: prepared.orderId,
                orderName: prepared.orderName,
                successUrl: returnUrl,
                failUrl: returnUrl,
                customerEmail: prepared.customerEmail,
                customerName: prepared.customerName
            });
        },

        /**
         * 결제 성공 복귀 → 서버 승인
         */
        async handlePaymentSuccess(params) {
            history.replaceState(null, '', location.pathname);
            try {
                const res = await paymentApi.confirmPayment({
                    sessionId: bookingNo,
                    paymentKey: params.get('paymentKey'),
                    orderId: params.get('orderId'),
                    amount: Number(params.get('amount')),
                    clientId: seatSocket.getClientId()
                }, true);
                const bookingNumber = res?.result?.bookingNumber ?? '';
                await dialog.alert(`예약이 완료되었습니다.\n예약번호: ${bookingNumber}`);
                location.href = '/mypage';
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '결제 승인에 실패했습니다.');
                location.reload();
            }
        },

    };

    // init start
    await flightBooking.init();

}