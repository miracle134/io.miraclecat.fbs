# 핵심 흐름

## 1. 좌석 선택 (Redis 선점 + WebSocket)

```mermaid
sequenceDiagram
    actor A as 사용자 A
    actor B as 사용자 B
    participant S as 서버
    participant R as Redis
    participant DB as MariaDB

    A->>S: GET /api/flight/inventory/{id}/seats
    S->>DB: 확정 좌석 (seat_reservations)
    S->>R: 선점 좌석 (seat:holds:{id})
    S-->>A: 좌석 배치
    A->>S: STOMP 구독 /topic/seats/{id}
    B->>S: STOMP 구독 /topic/seats/{id}
    A->>S: POST .../seats/12A/hold
    S->>R: Lua: 비어있거나 본인 소유면 SET PX 300s
    alt 다른 사용자가 선점 중
        S-->>A: 이미 선택된 좌석
    else 성공
        S-->>A: 선점 성공
        S-->>B: HELD 12A
    end
    Note over S,R: 결제 없이 TTL이 지나면<br/>SeatHoldExpireJob(5초, ShedLock)이 정리 후 RELEASED 전파
```

- 선점 해제: `DELETE .../seats/{seat}/hold` → 본인 소유일 때만 삭제 → `RELEASED`
- 결제 준비 시 본인 선점을 예약 세션 만료 시각까지 연장

## 2. 결제 → 예약 확정

```mermaid
sequenceDiagram
    actor U as 사용자
    participant F as 프론트
    participant S as 서버
    participant R as Redis
    participant T as Toss
    participant DB as MariaDB

    U->>F: 탑승객 입력 · 좌석 선택
    F->>S: POST /api/booking/session
    S->>R: 예약 세션 저장 (금액은 서버 계산)
    F->>S: POST /api/payment/prepare
    S->>R: 본인 선점 TTL을 세션 만료까지 연장
    F->>T: 결제창
    T-->>F: successUrl (paymentKey, orderId, amount)
    F->>S: POST /api/payment/confirm
    S->>R: 세션 조회 · 금액 대조 · 선점 유효 확인
    S->>T: 결제 승인
    T-->>S: DONE
    S->>DB: 한 트랜잭션 (PaymentTxService)<br/>bookings → booking_passengers → seat_reservations(UNIQUE)<br/>→ 재고 원자적 UPDATE + inventory_logs → payments → booking_histories
    alt 비즈니스 실패 (좌석 중복 · 매진)
        S->>DB: 롤백
        S->>T: 결제 즉시 취소 (보상)
        S-->>F: 실패 안내
    else 기술적 실패
        S-->>F: PAYMENT_BOOKING_FAILED (로그 · 수동 처리)
    else 성공
        S-->>F: 완료
        S-)S: BOOKED 전파 · 예약 확정 메일(@Async)
    end
```

- 같은 예약번호로 재호출(새로고침)하면 토스 재승인 없이 성공 응답

## 3. 예약 취소

```mermaid
flowchart TD
    A[취소 요청] --> B{누가?}
    B -->|사용자| C{출발 전날까지?}
    C -->|아니오| X[취소 불가]
    C -->|예| D[환불 = 항공권 금액<br/>예약 수수료 제외]
    B -->|관리자| E{환불 방식}
    E -->|FULL| F[환불 = 결제 전액]
    E -->|FARE| D
    D --> G[Toss 취소 API<br/>Idempotency-Key]
    F --> G
    G -->|실패| Y[DB 변경 없음]
    G -->|성공 또는 이미 취소됨| H[bookings cancelled<br/>payments CANCELLED + refund_amount<br/>seat_reservations 삭제 · 재고 복구<br/>booking_histories · inventory_logs<br/>RELEASED 전파]
```

## 4. 로그인 / Google 로그인

```mermaid
sequenceDiagram
    actor U as 사용자
    participant S as 서버
    participant R as Redis
    participant G as Google
    participant DB as MariaDB

    rect rgb(240,248,255)
    Note over U,DB: 일반 로그인
    U->>S: 로그인 요청
    S->>DB: 회원 조회 · 비밀번호 검증 · 정지 회원 차단
    S-->>U: JWE 쿠키 (access · refresh) + 내부 경로로만 redirect
    end

    rect rgb(245,245,245)
    Note over U,DB: Google (Authorization Code)
    U->>S: /oauth2/authorization/google
    S->>R: 인가 요청(state) 저장
    S-->>U: Google 로그인으로 이동
    U->>G: 로그인 · 동의
    G-->>S: /login/oauth2/code/google?code&state
    S->>R: state 검증 후 삭제
    S->>G: code → ID Token
    S->>S: email_verified 확인
    S->>DB: provider_id 조회 → 없으면 같은 이메일 계정 연결 → 없으면 신규 가입
    S-->>U: JWE 쿠키
    end
```
