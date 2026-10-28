/**
 * packageName    :
 * fileName       : mypage
 * author         : MiracleCat
 * date           : 2026-10-03
 * description    : 내 예약 (목록 / 좌석 변경 / 예약 취소)
 */
import {dialog, dom, loadApis, loadUtils} from '@core/core.js';
import seatSelect from '@feature/seat-select';
import seatSocket from '@feature/seat-socket';

const PAGE_SIZE = 5;

document.addEventListener("DOMContentLoaded", () => {
    initMypage();
});

async function initMypage() {
    // 추가 Utils & Module
    const {stringUtil, pagingUtil, validUtil, regexUtil} = await loadUtils(['stringUtil', 'pagingUtil', 'validUtil', 'regexUtil']);
    const {flightApi, bookingApi, memberApi} = await loadApis(['flightApi', 'bookingApi', 'memberApi']);

    const STATUS_VIEW = {
        upcoming: {label: '예정', badge: 'bg-success', border: 'border-primary', arrow: 'text-primary'},
        completed: {label: '완료', badge: 'bg-secondary', border: 'border-secondary', arrow: 'text-secondary'},
        cancelled: {label: '취소', badge: 'bg-danger', border: 'border-danger', arrow: 'text-danger'}
    };

    const mypage = {
        _filter: 'all',
        _page: 1,

        /**
         * 초기화
         */
        async init() {

            // 이벤트 핸들러
            this.bindEvents();

            // 내 정보 + 내 예약 조회
            await Promise.all([this.loadMe(), this.loadBookings()]);
        },

        _me: null,

        /**
         * 내 정보 조회
         */
        async loadMe() {
            try {
                const res = await memberApi.getMe();
                this._me = res?.result;
                this.renderMe();
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                console.error("loadMe error", error);
            }
        },

        renderMe() {
            const me = this._me;
            if (!me) return;
            dom.setText("#myName", me.memberName || "-");
            dom.setText("#myLoginId", me.loginId || "-");
            dom.setText("#myMobile", me.mobile || "미등록");
            dom.find("#myProviderBadge").classList.toggle("d-none", me.provider !== 'GOOGLE');
            dom.find("#btnChangePassword").classList.toggle("d-none", !me.hasPassword);
        },

        /**
         * 이름 / 전화번호 저장
         */
        async saveProfile() {
            const items = {
                memberName: dom.getValue("#editMemberName")?.trim(),
                mobile: dom.getValue("#editMobile")?.trim(),
            };
            if (!items.memberName) {
                await dialog.warning("이름을 입력해 주세요.");
                return;
            }
            if (items.mobile && !regexUtil.isContactNumber(items.mobile)) {
                await dialog.warning("전화번호 형식이 올바르지 않습니다.");
                return;
            }

            try {
                const res = await memberApi.updateMe(items, true);
                this._me = res?.result ?? this._me;
                this.renderMe();
                dialog.customClose("#popEditProfile");
                await dialog.success("내 정보가 수정되었습니다.");
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '내 정보를 수정하지 못했습니다.');
            }
        },

        /**
         * 비밀번호 변경
         */
        async savePassword() {
            const items = {
                currentPassword: dom.getValue("#currentPassword"),
                newPassword: dom.getValue("#myNewPassword"),
                newPasswordConfirm: dom.getValue("#myNewPasswordConfirm"),
            };

            const requiredErrors = validUtil.requiredError(items, {
                currentPassword: "현재 비밀번호를 입력해 주세요.",
                newPassword: "새 비밀번호를 입력해 주세요.",
                newPasswordConfirm: "새 비밀번호 확인을 입력해 주세요.",
            });
            if (requiredErrors) {
                await dialog.warning(requiredErrors);
                return;
            }

            const rulesError = validUtil.rulesError(items, {
                newPassword: [
                    v => regexUtil.isValidPassword(v, {requireUppercase: false}) || '비밀번호는 8자리 이상 영문, 숫자, 특수문자가 포함되어야 합니다.',
                    v => v === items.newPasswordConfirm || '확인 비밀번호가 일치하지 않습니다.',
                    v => v !== items.currentPassword || '현재 비밀번호와 다른 비밀번호를 입력해 주세요.'
                ]
            });
            if (rulesError) {
                await dialog.warning(rulesError);
                return;
            }

            try {
                await memberApi.changeMyPassword({
                    currentPassword: items.currentPassword,
                    newPassword: items.newPassword
                }, true);
                dialog.customClose("#popMyPassword");
                await dialog.success("비밀번호가 변경되었습니다.");
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '비밀번호를 변경하지 못했습니다.');
            }
        },

        /**
         * 이벤트 등록
         */
        bindEvents() {
            // 입력 제어
            stringUtil.setInputPhoneNumber("#editMobile");

            // 내 정보 수정 팝업
            dom.on("#btnEditProfile", "click", () => {
                const me = this._me ?? {};
                dom.setValue("#editLoginId", me.loginId ?? "");
                dom.setValue("#editMemberName", me.memberName ?? "");
                dom.setValue("#editMobile", me.mobile ?? "");
                dialog.customOpen("#popEditProfile");
            });
            dom.on("#btnSaveProfile", "click", async () => {
                await this.saveProfile();
            });

            // 비밀번호 변경 팝업
            dom.on("#btnChangePassword", "click", () => {
                ["#currentPassword", "#myNewPassword", "#myNewPasswordConfirm"].forEach((id) => dom.setValue(id, ""));
                dialog.customOpen("#popMyPassword");
            });
            dom.on("#btnSavePassword", "click", async () => {
                await this.savePassword();
            });

            // 상태 탭
            document.querySelectorAll("#bookingFilter [data-filter]").forEach((tab) => {
                tab.addEventListener("click", async (e) => {
                    e.preventDefault();
                    document.querySelectorAll("#bookingFilter .nav-link").forEach((el) => el.classList.remove("active"));
                    tab.classList.add("active");
                    this._filter = tab.dataset.filter;
                    this._page = 1;
                    await this.loadBookings();
                });
            });

            // 페이징
            pagingUtil.initPaginationListener(async (page) => {
                this._page = page;
                await this.loadBookings();
                window.scrollTo({top: 0, behavior: 'smooth'});
            }, "pagination");
        },

        /**
         * 내 예약 목록 조회
         */
        async loadBookings() {
            try {
                const res = await bookingApi.getMyBookings({
                    filter: this._filter,
                    page: this._page - 1,
                    size: PAGE_SIZE
                }, true);
                const page = res?.result;
                this.renderBookings(page?.content ?? []);
                this.renderPaging(page?.totalCount ?? 0);
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                console.error("loadBookings error", error);
                await dialog.warning(error?.message || '예약 내역을 불러오지 못했습니다.');
            }
        },

        /**
         * 페이징 렌더링
         */
        renderPaging(totalCount) {
            const container = dom.find("#pagination");
            container.innerHTML = totalCount > 0
                ? pagingUtil.setBootstrapPaging(this._page, totalCount, PAGE_SIZE)
                : "";
        },

        /**
         * 예약 카드 렌더링
         */
        renderBookings(bookings) {
            const body = dom.find("#bookingList");
            const template = dom.find("#bookingCardTemplate");
            body.innerHTML = "";
            dom.find("#emptyState").classList.toggle("d-none", bookings.length > 0);

            bookings.forEach((b) => {
                const clone = template.content.cloneNode(true);
                const kind = this.statusKind(b);
                const view = STATUS_VIEW[kind];
                const passengers = b.passengers ?? [];

                const status = clone.querySelector(".booking-status");
                status.textContent = view.label;
                status.classList.add(view.badge);

                clone.querySelector(".booking-flight").classList.add(view.border);
                clone.querySelector(".booking-arrow").classList.add(view.arrow);
                if (kind === 'cancelled') clone.querySelector(".card-body").classList.add("opacity-75");

                dom.setNodeText(clone, ".booking-route", `${b.departureAirportNameKo ?? b.departureAirport} → ${b.arrivalAirportNameKo ?? b.arrivalAirport}`);
                dom.setNodeText(clone, ".booking-number", b.bookingNumber ?? "");
                dom.setNodeText(clone, ".departure-time", b.departureTime?.substring(0, 5) ?? "-");
                dom.setNodeText(clone, ".arrival-time", b.arrivalTime?.substring(0, 5) ?? "-");
                clone.querySelectorAll(".flight-date").forEach((el) => el.textContent = b.flightDate ?? "");
                dom.setNodeText(clone, ".departure-airport", b.departureAirportNameKo ?? b.departureAirport ?? "");
                dom.setNodeText(clone, ".arrival-airport", b.arrivalAirportNameKo ?? b.arrivalAirport ?? "");
                dom.setNodeText(clone, ".duration", stringUtil.formatMinutes(b.durationMinutes) ?? "");
                dom.setNodeText(clone, ".airline", `${b.airlineNameKo ?? ""} ${b.flightNumber ?? ""}`.trim());
                dom.setNodeText(clone, ".booking-created", `예약일: ${b.createdAt?.substring(0, 10) ?? "-"}`);

                // 금액
                const amount = clone.querySelector(".booking-amount");
                const amountText = `${stringUtil.addComma(b.totalAmount ?? 0)}원`;
                if (kind === 'cancelled') {
                    amount.classList.add("text-muted");
                    amount.innerHTML = `<del></del>`;
                    amount.querySelector("del").textContent = amountText;
                    dom.setNodeText(clone, ".booking-amount-sub", "환불 처리");
                    clone.querySelector(".booking-amount-sub").classList.replace("text-muted", "text-success");
                } else {
                    amount.classList.add(kind === 'upcoming' ? "text-primary" : "text-muted");
                    amount.textContent = amountText;
                    dom.setNodeText(clone, ".booking-amount-sub", `성인 ${b.passengerCount ?? passengers.length}명`);
                }

                // 탑승객 / 취소일
                const info = clone.querySelector(".booking-info");
                if (kind === 'cancelled') {
                    info.classList.add("text-danger");
                    info.innerHTML = `<i class="bi bi-info-circle me-1"></i><span></span>`;
                    info.querySelector("span").textContent = `취소일: ${b.cancelledAt?.substring(0, 10) ?? "-"}`;
                } else {
                    info.classList.add("text-muted");
                    info.innerHTML = `<i class="bi bi-person me-1"></i><span></span>`;
                    info.querySelector("span").textContent = `승객: ${passengers.map((p) => p.name).join(", ")}`;
                    dom.setNodeText(clone, ".booking-seats", `좌석: ${passengers.map((p) => p.seatNumber ?? "-").join(", ")}`);
                }

                // 버튼
                if (kind === 'upcoming' && b.changeable) {
                    const btnSeat = clone.querySelector(".btn-seat-change");
                    const btnCancel = clone.querySelector(".btn-cancel");
                    btnSeat.classList.remove("d-none");
                    btnCancel.classList.remove("d-none");
                    btnSeat.addEventListener("click", () => this.openSeatChange(b));
                    btnCancel.addEventListener("click", () => this.cancelBooking(b));
                } else if (kind === 'upcoming') {
                    clone.querySelector(".booking-deadline").classList.remove("d-none");
                } else if (kind === 'completed') {
                    clone.querySelector(".btn-completed").classList.remove("d-none");
                }

                body.appendChild(clone);
            });
        },

        /**
         * 예정 / 완료 / 취소 구분
         */
        statusKind(b) {
            if (b.status === 'cancelled') return 'cancelled';
            const today = new Date();
            const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
            return b.flightDate < todayStr ? 'completed' : 'upcoming';
        },

        /**
         * 예약 취소
         */
        async cancelBooking(b) {
            const message = `예약을 취소하시겠습니까?\n`
                + `환불 금액: ${stringUtil.addComma(b.fareAmount ?? 0)}원\n`
                + `(예약 수수료 ${stringUtil.addComma(b.bookingFeeAmount ?? 0)}원은 환불되지 않습니다.)`;
            if (!(await dialog.confirm(message))) return;

            try {
                await bookingApi.cancelBooking(b.id, {}, true);
                await dialog.success("예약이 취소되었습니다.");
                await this.loadBookings();
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '예약을 취소하지 못했습니다.');
                await this.loadBookings();
            }
        },

        /**
         * 좌석 변경: 현재 좌석을 선택된 상태로 띄우고, 새 좌석만 선점 → 확인 시 변경 요청
         */
        async openSeatChange(b) {
            const passengers = b.passengers ?? [];
            const ownSeats = passengers.map((p) => p.seatNumber);
            const ownSet = new Set(ownSeats);

            let status;
            try {
                const res = await flightApi.getSeatStatus(b.inventoryId, true);
                status = res?.result;
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await dialog.warning(error?.message || '좌석 정보를 불러오지 못했습니다.');
                return;
            }
            if (!status) return;

            // 이전에 고르다 만 선점은 해제
            await Promise.allSettled((status.myHeld ?? [])
                .filter((code) => !ownSet.has(code))
                .map((code) => this.releaseSeat(b.inventoryId, code)));

            const occupied = [...(status.booked ?? []), ...(status.held ?? [])].filter((code) => !ownSet.has(code));

            seatSelect.prepare({
                total: status.totalSeats,
                occupied,
                passengerCount: passengers.length,
                mySeats: ownSeats,
                onHold: (code) => ownSet.has(code) ? true : this.holdSeat(b.inventoryId, code),
                onRelease: (code) => ownSet.has(code) ? undefined : this.releaseSeat(b.inventoryId, code)
            });

            seatSocket.connect(b.inventoryId, (event) => {
                const seatStatus = event.type === 'RELEASED' ? 'available' : 'occupied';
                (event.seats ?? []).filter((code) => !ownSet.has(code))
                    .forEach((code) => seatSelect.applyRemoteSeatUpdate(code, seatStatus));
            });

            await seatSelect.show();
            seatSocket.disconnect();

            const seatMap = seatSelect.getSeatMap();
            const newHolds = Object.values(seatMap).filter((code) => code && !ownSet.has(code));

            if (!seatSelect.wasConfirmed()) {
                await Promise.allSettled(newHolds.map((code) => this.releaseSeat(b.inventoryId, code)));
                return;
            }

            const changes = passengers
                .map((p, i) => ({passengerId: p.id, seatNumber: seatMap[i + 1], before: p.seatNumber}))
                .filter((c) => c.seatNumber && c.seatNumber !== c.before);
            if (changes.length === 0) return;

            const summary = changes.map((c) => `${c.before} → ${c.seatNumber}`).join(", ");
            if (!(await dialog.confirm(`좌석을 변경하시겠습니까?\n${summary}`))) {
                await Promise.allSettled(newHolds.map((code) => this.releaseSeat(b.inventoryId, code)));
                return;
            }

            try {
                await bookingApi.changeSeats(b.id, {
                    clientId: seatSocket.getClientId(),
                    changes: changes.map(({passengerId, seatNumber}) => ({passengerId, seatNumber}))
                }, true);
                await dialog.success("좌석이 변경되었습니다.");
            } catch (error) {
                if (error?.name === 'HandledForbiddenError') return;
                await Promise.allSettled(newHolds.map((code) => this.releaseSeat(b.inventoryId, code)));
                await dialog.warning(error?.message || '좌석을 변경하지 못했습니다.');
            }
            await this.loadBookings();
        },

        /**
         * 좌석 선점 → 성공 시 true
         */
        async holdSeat(inventoryId, code) {
            try {
                await flightApi.holdSeat(inventoryId, code, {clientId: seatSocket.getClientId()});
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
         * 좌석 선점 해제
         */
        async releaseSeat(inventoryId, code) {
            await flightApi.releaseSeat(inventoryId, code, {clientId: seatSocket.getClientId()});
        },

    };

    // init start
    await mypage.init();

}
