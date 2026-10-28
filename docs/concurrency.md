# 좌석·재고 동시성

## 방어 계층

| 단계 | 장치 | 막는 것 |
| --- | --- | --- |
| 좌석 선택 | Redis Lua 선점 (TTL 300초) | 화면에서 같은 좌석 동시 선택 |
| 결제 확정 | `seat_reservations UNIQUE(inventory_id, seat_number)` | 선점이 뚫려도 같은 좌석 이중 저장 |
| 재고 차감 | 원자적 UPDATE `WHERE available_seats >= n` | 음수 재고 · 초과 판매 |
| 실패 처리 | 비즈니스 실패 시 Toss 결제 자동 취소 | 돈만 나가고 예약이 없는 상태 |

## 재고 차감 방식 변경

**변경 전 — `@Version` 낙관적 락**
엔티티를 읽고 메모리에서 차감한 뒤 저장합니다. 같은 재고 row를 동시에 수정하면 하나만 성공합니다.
→ **서로 다른 좌석**을 고른 사용자끼리도 충돌해서, 좌석이 남아 있는데 결제가 실패했습니다.

**변경 후 — 원자적 UPDATE**
```java
@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query("update versioned FlightInventory i set i.availableSeats = i.availableSeats - :count, i.updatedAt = :now " +
       "where i.id = :id and i.availableSeats >= :count")
int decreaseSeats(Long id, int count, LocalDateTime now);
```
DB가 row 락을 잡고 순서대로 차감하므로 충돌 없이 모두 처리되고, 0건이면 좌석 부족으로 판단합니다.
`versioned`로 version도 증가시켜, 엔티티 저장을 쓰는 관리자 재고 조정과 겹치면 그쪽이 낙관적 락으로 실패합니다.

## 테스트

`SeatConcurrencyIntegrationTest` — Testcontainers MariaDB 11.4, 10개 스레드가 `CountDownLatch`로 동시에 `PaymentTxService.saveBooking` 호출

| 시나리오 | @Version | 원자적 UPDATE |
| --- | --- | --- |
| 같은 좌석 10명 | 성공 1 / 충돌 9 | 성공 1 / 충돌 9 (UNIQUE) |
| 다른 좌석 10명 | 성공 1 / 충돌 9 | **성공 10**, 재고 정확히 10 차감 |
| 남은 3석에 5명 (다른 좌석) | — | 성공 3 / 좌석 부족 2, 재고 0 |

```bash
./gradlew test --tests "*SeatConcurrencyIntegrationTest"   # Docker 필요
```
