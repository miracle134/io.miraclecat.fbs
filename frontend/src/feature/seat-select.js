/**
 * packageName    : static.js.feature
 * fileName       : seat-select
 * author         : MiracleCat
 * date           : 2026-10-24
 * description    : 좌석 선택 다이얼로그(#popSeatSelect) 로직
 *                  - total/occupied로 좌석맵을 자동 생성
 *                  - 좌측 승객 패널에서 승객을 고르고, 우측 좌석을 클릭해 배정
 */

import dialogUtil from '@core/dialog-util.js';


const LETTERS = ['A', 'B', 'C', 'D', 'E', 'F'];
const AISLE_AFTER = 3; // C 다음에 통로

let passengerCount = 0;
let assignments = {};   // { [passengerNo(1-base)]: seatCode | null }
let active = 1;
let hooks = { onHold: null, onRelease: null };
let busy = false;
let confirmed = false;

const seatContainer = () => document.getElementById('seatContainer');
const passengerList = () => document.getElementById('seatPassengerList');

/* ---------- 좌석맵 생성 ---------- */
function renderSeats(total, occupied) {
    const occupiedSet = new Set(occupied);
    const rows = Math.ceil(total / LETTERS.length);
    let html = '';

    for (let row = 1; row <= rows; row++) {
        const seatsInRow = Math.min(LETTERS.length, total - (row - 1) * LETTERS.length);
        html += '<div class="seat-row d-flex justify-content-center align-items-center mb-2">'
            + '<span class="seat-row-number">' + row + '</span>';

        for (let i = 0; i < seatsInRow; i++) {
            if (i === AISLE_AFTER) html += '<div class="seat-aisle"></div>';
            const code = row + LETTERS[i];
            const isOccupied = occupiedSet.has(code);
            html += '<button type="button" class="seat ' + (isOccupied ? 'seat-occupied' : 'seat-available') + '" '
                + 'data-seat="' + code + '"' + (isOccupied ? ' disabled' : '') + '>' + LETTERS[i] + '</button>';
        }
        html += '</div>';
    }
    seatContainer().innerHTML = html;
}

function seatEl(code) {
    return seatContainer().querySelector('[data-seat="' + code + '"]');
}

// passengerNo: 이 좌석을 선택한 승객 번호(선택 해제 시에는 생략).
// 좌석에 승객 번호 배지를 붙이고 title(hover 시 "승객N")을 달아서, 여러 승객이 있을 때
// "이 좌석은 누가 선택한 좌석인지"가 보이게 한다.
function setSeatSelected(code, selected, passengerNo) {
    const el = seatEl(code);
    if (!el) return;
    el.classList.toggle('seat-selected', selected);
    el.classList.toggle('seat-available', !selected);

    let badge = el.querySelector('.seat-owner-badge');
    if (selected && passengerNo) {
        el.title = '승객' + passengerNo;
        if (!badge) {
            badge = document.createElement('span');
            badge.className = 'seat-owner-badge';
            el.appendChild(badge);
        }
        badge.textContent = passengerNo;
    } else {
        el.removeAttribute('title');
        if (badge) badge.remove();
    }
}

/* ---------- 승객 패널 (숫자 기반, 1~9) ---------- */
function seatLabel(code) {
    const m = /^(\d+)([A-Z])$/.exec(code);
    return m ? m[1] + '열 ' + m[2] : code;
}

function renderPassengers(count) {
    let html = '';
    for (let p = 1; p <= count; p++) {
        html += '<button type="button" class="seat-passenger" data-passenger="' + p + '">'
            + '<span class="seat-passenger__name">승객' + p + '</span>'
            + '<span class="seat-passenger__seat" data-passenger-seat="' + p + '">좌석을 선택해주세요</span>'
            + '</button>';
    }
    passengerList().innerHTML = html;

    passengerList().querySelectorAll('.seat-passenger').forEach((btn) => {
        btn.addEventListener('click', () => setActive(Number(btn.dataset.passenger)));
    });
}

function refreshPassengerRow(p) {
    const btn = passengerList().querySelector('[data-passenger="' + p + '"]');
    if (!btn) return;
    const seatText = btn.querySelector('[data-passenger-seat]');
    const code = assignments[p];
    seatText.textContent = code ? seatLabel(code) : '좌석을 선택해주세요';
    btn.classList.toggle('seat-passenger--active', p === active);
    btn.classList.toggle('seat-passenger--done', Boolean(code));
}

function refreshAllPassengerRows() {
    for (let p = 1; p <= passengerCount; p++) refreshPassengerRow(p);
}

function setActive(p) {
    active = p;
    refreshAllPassengerRows();
}

function firstUnassignedFrom(start) {
    for (let i = 0; i < passengerCount; i++) {
        const p = ((start - 1 + i) % passengerCount) + 1;
        if (!assignments[p]) return p;
    }
    return null;
}

/* ---------- 서버 선점 훅 ---------- */
async function requestHold(code) {
    if (!hooks.onHold) return true;
    return Boolean(await hooks.onHold(code));
}

async function requestRelease(code) {
    if (!hooks.onRelease) return;
    try {
        await hooks.onRelease(code);
    } catch (e) {
        console.error('seat release error', e);
    }
}

/* ---------- 좌석 배정 ---------- */
async function assignSeatToActive(code) {
    // 다른 승객이 이미 선택한 좌석이면 빼앗지 못하게 막는다. 본인 좌석을 다시 누르면 해제.
    const prevOwner = Object.keys(assignments).find((p) => assignments[p] === code);
    if (prevOwner && Number(prevOwner) !== active) {
        dialogUtil.warning('승객' + prevOwner + '님이 이미 선택한 좌석입니다.');
        return;
    }

    const target = active;
    const prevSeatOfActive = assignments[target];

    if (prevSeatOfActive === code) {
        // 활성 승객이 이미 가진 좌석을 다시 클릭 → 배정 해제
        await requestRelease(code);
        assignments[target] = null;
        setSeatSelected(code, false);
    } else {
        // 새 좌석을 먼저 선점하고, 성공했을 때만 기존 좌석을 놓는다
        if (!(await requestHold(code))) return;
        if (prevSeatOfActive) {
            await requestRelease(prevSeatOfActive);
            setSeatSelected(prevSeatOfActive, false);
        }
        assignments[target] = code;
        setSeatSelected(code, true, target);
    }
    refreshPassengerRow(target);

    // 방금 "배정"한 경우에만 다음 미배정 승객으로 넘어간다.
    // 방금 "해제"한 경우(assignments[active]가 비었음)는 같은 승객이 다시 고르도록 그대로 둔다.
    if (assignments[target]) {
        const next = firstUnassignedFrom(target + 1 > passengerCount ? 1 : target + 1);
        if (next !== null) setActive(next);
    }
}

async function resetAll() {
    const seats = Object.values(assignments).filter(Boolean);
    await Promise.all(seats.map((code) => requestRelease(code)));
    seats.forEach((code) => setSeatSelected(code, false));
    assignments = {};
    active = 1;
    refreshAllPassengerRows();
}

// 이전 화면에서 이미 선점해 둔 내 좌석을 승객 1번부터 순서대로 복원, 승객 수를 넘는 좌석은 해제
function restoreMySeats(mySeats) {
    mySeats.forEach((code, i) => {
        const p = i + 1;
        if (p > passengerCount) {
            requestRelease(code);
            return;
        }
        assignments[p] = code;
        setSeatSelected(code, true, p);
    });
    const next = firstUnassignedFrom(1);
    active = next !== null ? next : 1;
}

/* ---------- 외부(웹소켓 등)에서 좌석 상태 변경 반영 ----------
   내가 선택해둔 좌석이 다른 사람에게 선점되면 배정을 해제하고 안내한다. */
function applyRemoteSeatUpdate(code, status) {
    const el = seatEl(code);
    if (!el) return;

    const ownerIdx = Object.keys(assignments).find((p) => assignments[p] === code);

    if (status === 'occupied') {
        if (ownerIdx) {
            assignments[ownerIdx] = null;
            refreshPassengerRow(Number(ownerIdx));
            dialogUtil.warning('승객' + ownerIdx + '님이 선택한 ' + seatLabel(code) + ' 좌석이 다른 사용자에게 선점되었습니다.');
        }
        el.classList.remove('seat-available', 'seat-selected');
        el.classList.add('seat-occupied');
        el.disabled = true;
        el.removeAttribute('title');
        const badge = el.querySelector('.seat-owner-badge');
        if (badge) badge.remove();
    } else if (status === 'available') {
        if (!ownerIdx) {   // 내가 선택 중인 좌석이면 그대로 유지, 아니면 다시 선택 가능하게
            el.classList.remove('seat-occupied');
            el.classList.add('seat-available');
            el.disabled = false;
        }
    }
}

// 서버에서 다시 조회한 점유 좌석 목록으로 전체 상태를 맞춘다 (웹소켓 재연결 시 놓친 이벤트 보정)
function syncOccupied(occupied = []) {
    const occupiedSet = new Set(occupied);
    seatContainer().querySelectorAll('.seat').forEach((el) => {
        const code = el.dataset.seat;
        const isOccupied = occupiedSet.has(code);
        if (isOccupied && !el.classList.contains('seat-occupied')) {
            applyRemoteSeatUpdate(code, 'occupied');
        } else if (!isOccupied && el.classList.contains('seat-occupied')) {
            applyRemoteSeatUpdate(code, 'available');
        }
    });
}

/* ---------- 이벤트 바인딩 (한 번만) ---------- */
let bound = false;
function bindOnce() {
    if (bound) return;
    bound = true;

    seatContainer().addEventListener('click', async (e) => {
        const btn = e.target.closest('.seat');
        if (!btn || btn.disabled || busy) return;
        busy = true;
        try {
            await assignSeatToActive(btn.dataset.seat);
        } finally {
            busy = false;
        }
    });

    document.getElementById('clearSeatsBtn').addEventListener('click', async () => {
        if (busy) return;
        busy = true;
        try {
            await resetAll();
        } finally {
            busy = false;
        }
    });

    document.getElementById('confirmSeatsBtn').addEventListener('click', () => {
        if (!validateSeats()) return;
        confirmed = true;
        dialogUtil.customClose('#popSeatSelect');   // 결과는 customClose가 아니라 getSeatMap()으로 가져온다
    });
}

/* ---------- Public API ---------- */

// 좌석맵·승객1~N 패널을 그린다. 팝업은 열지 않는다 — 열려면 show()를 따로 호출.
// mySeats: 이미 내가 선점 중인 좌석 / onHold(code): 서버 선점, 성공 시 true / onRelease(code): 서버 해제
function prepare({ total, occupied = [], passengerCount: count = 1, mySeats = [], onHold = null, onRelease = null }) {
    passengerCount = count;
    assignments = {};
    active = 1;
    hooks = { onHold, onRelease };

    renderSeats(total, occupied);
    renderPassengers(passengerCount);
    restoreMySeats(mySeats);
    refreshAllPassengerRows();
    bindOnce();
}

// 팝업을 띄운다. 반환하는 Promise는 "언제 닫혔는지" 용도일 뿐, 결과값은 의미 없음
// (취소/X/ESC로 닫혀도, 확인으로 닫혀도 그냥 resolve됨) — 데이터는 getSeatMap()으로 따로 가져온다.
function show() {
    confirmed = false;
    return dialogUtil.customOpen('#popSeatSelect');
}

// 마지막으로 열린 팝업이 확인 버튼으로 닫혔는지 (X·ESC·배경 클릭이면 false)
function wasConfirmed() {
    return confirmed;
}

// 현재 배정 상태를 그대로 가져온다 (완료 여부 상관없이, 언제든 호출 가능).
// { 1:'2A', 2:'5F', 3:null, ... } — 아직 안 고른 승객은 null.
function getSeatMap() {
    return { ...assignments };
}

// 전원 배정됐는지만 검사. 비어있으면 alert 띄우고 false, 다 찼으면 true.
function validateSeats() {
    const unassigned = Object.keys(assignments).length < passengerCount
        || Object.values(assignments).some((v) => !v);
    if (unassigned) {
        dialogUtil.warning('모든 승객의 좌석을 선택해주세요.');
        return false;
    }
    return true;
}

// 좌석 배정 결과(seatMap)와 승객 정보 배열을 합쳐서 돌려주는 순수 함수.
// seatMap: getSeatMap()이 반환한 { 1:'2A', 2:'5F', ... } / passengerInfoList: [{...승객1정보}, {...승객2정보}, ...]
// 두 길이가 다르면 짧은 쪽 기준으로 합친다.
function mapSeatsToPassengers(seatMap, passengerInfoList) {
    return passengerInfoList.map((info, i) => ({ ...info, seat: seatMap[i + 1] }));
}

export default { prepare, show, wasConfirmed, getSeatMap, validateSeats, mapSeatsToPassengers, applyRemoteSeatUpdate, syncOccupied };