/**
 * packageName    : io.miraclecat.fbs.api.flight.dto
 * fileName       : SeatDto
 * author         : MiracleCat
 * date           : 2026-10-24
 * description    : 좌석 DTO (웹소켓 이벤트 / 추후 선점·조회 요청·응답 추가)
 */
package io.miraclecat.fbs.api.flight.dto;

import lombok.*;

import java.util.List;

public class SeatDto {

    /**
     * 좌석 이벤트 유형
     * HELD     : 임시 선점됨
     * RELEASED : 선점 해제 / 만료 / 예약 취소로 다시 선택 가능
     * BOOKED   : 결제 완료로 예약 확정
     */
    public enum EventType {
        HELD, RELEASED, BOOKED
    }

    /**
     * 좌석 이벤트 (/topic/seats/{inventoryId} 로 전파)
     * clientId : 이벤트를 발생시킨 브라우저 식별값 — 클라이언트가 본인 이벤트를 무시하는 용도
     *            (회원 ID를 다른 사용자에게 노출하지 않기 위해 별도 값 사용)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Event {
        private EventType type;
        private Long inventoryId;
        private List<String> seats;
        private String clientId;
    }

    /**
     * 좌석 상태 조회 응답
     * booked : 예약 확정 좌석 (seat_reservations)
     * held   : 다른 사용자가 임시 선점 중인 좌석
     * myHeld : 내가 임시 선점 중인 좌석 (새로고침 후 복원용)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusRes {
        private Long inventoryId;
        private Integer totalSeats;
        private Integer availableSeats;
        private List<String> booked;
        private List<String> held;
        private List<String> myHeld;
    }

    /**
     * 좌석 선점/해제 요청
     * clientId : seat-socket.js의 브라우저 식별값 (본인 이벤트 무시용)
     */
    @Getter
    @Setter
    public static class HoldReq {
        private String clientId;
    }

    /**
     * 좌석 선점 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HoldRes {
        private String seatNumber;
        private Long expiresAt;
    }

    /**
     * 내 선점 일괄 연장 응답
     * seats : 연장된 좌석 (요청 시점에 이미 만료된 좌석은 포함되지 않음)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtendRes {
        private List<String> seats;
        private Long expiresAt;
    }

}
