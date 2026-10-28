/**
 * packageName    : io.miraclecat.fbs.api.flight.websocket
 * fileName       : SeatEventPublisher
 * author         : MiracleCat
 * date           : 2026-10-24
 * description    : 좌석 이벤트 웹소켓 전파
 *                  - Redis 선점/해제(HELD, RELEASED)는 DB 트랜잭션이 없으므로 처리 직후 바로 호출
 *                  - DB 커밋이 걸린 이벤트(BOOKED, 취소 RELEASED)는 커밋 이후에만 호출해야 함
 *                    → @TransactionalEventListener(phase = AFTER_COMMIT) 에서 호출 (3단계에서 추가)
 */
package io.miraclecat.fbs.api.flight.websocket;

import io.miraclecat.fbs.api.flight.dto.SeatDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatEventPublisher {

    private static final String SEAT_TOPIC = "/topic/seats/";

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 좌석 이벤트 전파
     */
    public void publish(SeatDto.Event event) {
        if (event.getSeats() == null || event.getSeats().isEmpty()) return;

        messagingTemplate.convertAndSend(SEAT_TOPIC + event.getInventoryId(), event);
        log.debug("seat event published: type={}, inventoryId={}, seats={}",
                event.getType(), event.getInventoryId(), event.getSeats());
    }

    public void publish(SeatDto.EventType type, Long inventoryId, List<String> seats, String clientId) {
        publish(SeatDto.Event.builder()
                .type(type)
                .inventoryId(inventoryId)
                .seats(seats)
                .clientId(clientId)
                .build());
    }

}
