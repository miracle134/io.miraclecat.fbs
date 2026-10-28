# ERD

엔티티 기준 (2026-10-28).

```mermaid
erDiagram
    airlines ||--o{ flights : "운항"
    airports ||--o{ flights : "출발/도착"
    flights ||--o{ flight_inventories : "날짜별 재고"
    flights ||--o{ bookings : "예약"
    flights ||--o{ inventory_logs : "재고 변동"
    flight_inventories ||--o{ seat_reservations : "확정 좌석"
    member ||--o{ bookings : "예약자"
    bookings ||--|{ booking_passengers : "탑승객"
    booking_passengers ||--|| seat_reservations : "좌석"
    bookings ||--o{ booking_histories : "상태 이력"
    bookings ||--|| payments : "결제"
    bookings ||--o{ inventory_logs : "관련 예약"
    member ||--o{ authentication_log : "로그인 이력"

    airlines { varchar code PK "IATA 2자리" varchar name_ko varchar name_en varchar logo_url }
    airports { varchar code PK "IATA 3자리" varchar name_ko varchar name_en varchar city }
    flights { bigint id PK varchar flight_number varchar airline_code FK varchar departure_airport FK varchar arrival_airport FK time departure_time time arrival_time int duration_minutes int total_seats int base_price varchar status "active/inactive" }
    flight_inventories { bigint id PK bigint flight_id FK date flight_date int available_seats int version "UK(flight_id, flight_date)" }
    seat_reservations { bigint id PK bigint inventory_id FK varchar seat_number bigint booking_id FK bigint booking_passenger_id FK "UK(inventory_id, seat_number)" }
    member { bigint member_id PK varchar login_id varchar login_password "소셜은 null" varchar provider varchar provider_id "UK(provider, provider_id)" varchar role varchar member_name varchar mobile varchar status "active/suspended/withdrawn" datetime withdrawn_at }
    bookings { bigint id PK varchar booking_number "BK yyyyMMdd + 6자리" bigint member_id FK bigint flight_id FK date flight_date int passenger_count int fare_amount int booking_fee_amount int total_amount varchar status "confirmed/cancelled" varchar payment_method varchar cancelled_by varchar cancel_reason datetime cancelled_at }
    booking_passengers { bigint id PK bigint booking_id FK varchar name varchar name_en varchar email varchar mobile date birth_date varchar gender varchar seat_number }
    booking_histories { bigint id PK bigint booking_id FK varchar previous_status varchar current_status varchar changed_by varchar reason }
    payments { bigint id PK bigint booking_id FK varchar payment_key int amount int refund_amount varchar method varchar status "APPROVED/CANCELLED" datetime approved_at datetime cancelled_at }
    inventory_logs { bigint id PK bigint flight_id FK date flight_date varchar change_type "RESERVE/RELEASE/ADJUST" int quantity int before_seats int after_seats bigint booking_id FK }
    notices { bigint id PK varchar title text content boolean is_important int views varchar status "published/hidden" }
    authentication_log { bigint authentication_log_id PK bigint member_id varchar login_type varchar ip_address boolean is_success varchar failure_reason }
    settings { bigint id PK varchar setting_key varchar setting_value }
    scheduler_logs { bigint id PK varchar job_name varchar status int processed_count bigint execution_time_ms }
```

좌석 **선점**(결제 전)은 DB가 아니라 Redis에 있고, `seat_reservations`에는 결제가 끝난 **확정 좌석**만 저장됩니다.

## 핵심 제약

| 제약 | 이유 |
| --- | --- |
| `seat_reservations UK(inventory_id, seat_number)` | 같은 편·날짜·좌석의 중복 예약을 DB가 최종 차단 |
| `flight_inventories UK(flight_id, flight_date)` | 같은 날짜 재고 중복 생성 방지 (일괄 생성 시 있는 날짜는 건너뜀) |
| 재고 차감 `WHERE available_seats >= n` + `chk_available_seats >= 0` | 원자적 UPDATE로 초과 판매 방지, CHECK 제약이 음수 최종 차단 |
| `bookings UNIQUE(booking_number)` | Redis INCR 채번의 최종 안전망 |
| `member UK(provider, provider_id)` | 소셜 계정 중복 연결 방지 |
| `airlines`/`airports` 문자열 PK | IATA 코드가 자연키 |

## 상태값

| 테이블 | 상태 |
| --- | --- |
| flights | `active` / `inactive` (삭제 대신 운항중지 → 기존 예약 유지, 사용자 검색에서 제외) |
| bookings | `confirmed` / `cancelled` (결제 후 저장이라 pending 없음) |
| payments | `APPROVED` / `CANCELLED` (부분·전액 모두 CANCELLED, 금액은 `refund_amount`) |
| member | `active` / `suspended` / `withdrawn` |
| notices | `published` / `hidden` |
