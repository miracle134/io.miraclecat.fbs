/**
 * packageName    : static.js.feature
 * fileName       : seat-socket
 * author         : MiracleCat
 * date           : 2026-10-24
 * description    : 좌석 실시간 반영용 STOMP 웹소켓 클라이언트
 *                  - /topic/seats/{inventoryId} 구독
 *                  - 재연결 시 자동 재구독, onConnected는 최초 연결·재연결마다 호출
 *                    (끊긴 동안 놓친 이벤트가 있으므로 onConnected에서 좌석 상태를 다시 조회하는 용도 — 4단계)
 *                  - clientId: 브라우저(페이지)별 식별값. 선점 요청 시 함께 보내고, 본인 이벤트는 무시한다
 *
 * 사용법:
 *   import seatSocket from '@feature/seat-socket';
 *
 *   seatSocket.connect(inventoryId, (event) => {
 *       // event: { type: 'HELD'|'RELEASED'|'BOOKED', inventoryId, seats: ['12A'], clientId }
 *   }, {
 *       onConnected: () => { // 좌석 상태 재조회 }
 *   });
 *
 *   seatSocket.getClientId();   // 선점 API 요청 시 함께 전송
 *   seatSocket.disconnect();    // 페이지 이탈 시
 */
import {Client} from '@stomp/stompjs';

const RECONNECT_DELAY_MS = 5000;
const HEARTBEAT_MS = 10000;

const clientId = createClientId();
let client = null;

function createClientId() {
    // crypto.randomUUID는 https 또는 localhost에서만 사용 가능 → 그 외 환경 대비 fallback
    if (window.crypto?.randomUUID) return window.crypto.randomUUID();
    return Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10);
}

function socketUrl() {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    return protocol + '//' + location.host + '/ws';
}

/**
 * 연결 + 구독
 * @param {number} inventoryId 구독할 재고(항공편+날짜) ID
 * @param {(event:object) => void} onEvent 좌석 이벤트 수신 콜백 (본인 이벤트는 걸러서 전달)
 * @param {{onConnected?: Function}} options
 */
function connect(inventoryId, onEvent, {onConnected} = {}) {
    disconnect();

    client = new Client({
        brokerURL: socketUrl(),
        reconnectDelay: RECONNECT_DELAY_MS,
        heartbeatIncoming: HEARTBEAT_MS,
        heartbeatOutgoing: HEARTBEAT_MS,

        // 재연결 때도 다시 호출되므로 여기서 구독해야 재구독이 보장됨
        onConnect: () => {
            client.subscribe('/topic/seats/' + inventoryId, (message) => {
                let event;
                try {
                    event = JSON.parse(message.body);
                } catch (e) {
                    console.error('seat event parse error', e, message.body);
                    return;
                }
                if (event.clientId === clientId) return; // 본인이 발생시킨 이벤트는 무시
                onEvent(event);
            });

            if (onConnected) onConnected();
        },

        onStompError: (frame) => {
            console.error('seat socket STOMP error', frame.headers['message'], frame.body);
        },

        onWebSocketClose: () => {
            console.warn('seat socket closed — 재연결 시도');
        },
    });

    client.activate();
}

/**
 * 연결 해제 (구독도 함께 정리됨)
 */
function disconnect() {
    if (!client) return;
    client.deactivate();
    client = null;
}

function getClientId() {
    return clientId;
}

function isConnected() {
    return Boolean(client?.connected);
}

export default {connect, disconnect, getClientId, isConnected};
