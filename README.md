# ✈️ Flight Booking System

국내선 항공권 검색 → 좌석 선택 → 결제 → 취소까지 다루는 예약 서비스입니다. (1인 풀스택)
**좌석 동시성**, **결제 후 정합성**, **무상태 인증**을 중심으로 설계했습니다.

- 도메인: https://flight-booking.site
- 문서: [아키텍처](docs/architecture.md) · [ERD](docs/erd.md) · [핵심 흐름](docs/flows.md) · [동시성 테스트](docs/concurrency.md) · [Postman](docs/postman/)

## 주요 기능

### 사용자
- **항공편 검색**: 출발·도착·날짜·인원, 항공사·가격·시간대 필터, 정렬 (운항 중인 항공편만 노출)
- **좌석 선택**: Redis 좌석 선점(TTL 5분) + WebSocket(STOMP)으로 다른 사용자에게 실시간 공유
- **결제**: Toss Payments 승인·취소·부분 환불, 결제 승인 후 예약 일괄 저장
- **마이페이지**: 예약 내역, 좌석 변경, 예약 취소(출발 하루 전까지), 내 정보·비밀번호 변경
- **회원**: 회원가입, 로그인, Google 로그인(같은 이메일 계정 연결), 비밀번호 재설정 메일
- **알림**: 예약 확정 메일 (`@Async`)
- 공지사항

### 관리자 (`/admin`)
- 대시보드 / 통계: 예약 추이, 항공사 비율, 인기 노선, 월별 순매출 (Chart.js)
- 항공편: 등록·수정·운항중지, 날짜별 재고 일괄 생성, 재고 수동 조정
- 예약: 검색·상세·이력, 관리자 취소 (전액 / 항공권 금액만 환불)
- 회원 정지·해제, 공지사항, 설정, 스케줄러 로그
- 권한: `ROLE_ADMIN`, `ROLE_SUPER_ADMIN`

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| Backend | Java, Spring Boot 4, Spring Security, Spring Data JPA, QueryDSL (OpenFeign) |
| DB | MariaDB |
| Redis | 좌석 선점, 예약 세션, 예약번호 채번, OAuth2 인가 요청, 비밀번호 재설정 토큰 |
| 실시간 | WebSocket + STOMP (`/ws`, `/topic/seats/{inventoryId}`) |
| 스케줄러 | Spring Scheduler + ShedLock |
| 결제 / 인증 | Toss Payments API / JWE 쿠키, OAuth2 Client (Google OIDC) |
| Frontend | Thymeleaf Layout + Vanilla JS (ES Module, webpack), Chart.js |
| Test | JUnit 5, Mockito, Testcontainers (MariaDB) |

## 핵심 설계

| 주제 | 방식 |
| --- | --- |
| 좌석 선점 | Redis `SET` + Lua 스크립트로 "확인 후 저장"을 원자적으로 처리, TTL 300초. 만료 좌석은 5초 주기 ShedLock 잡이 정리 후 `RELEASED` 전파 |
| 좌석 중복 최종 방어 | `seat_reservations UNIQUE(inventory_id, seat_number)` — 결제 시점에 DB가 최종 보장 |
| 재고 차감 | `UPDATE ... SET available_seats = available_seats - n WHERE id = ? AND available_seats >= n` 원자적 UPDATE (0건이면 매진) |
| 결제 후 일괄 저장 | 결제 전에는 예약을 DB에 남기지 않음(Redis 예약 세션). 승인 후 한 트랜잭션으로 예약·탑승객·좌석·재고·결제·이력 저장 |
| 보상 처리 | 좌석 중복·매진 같은 비즈니스 실패 → 즉시 Toss 결제 취소. 기술적 실패 → 로그(`PAYMENT_BOOKING_FAILED`) 후 수동 복구 |
| 금액 검증 | 결제 금액은 서버가 계산해 세션에 저장, Toss 승인 전 요청 금액과 대조 |
| 요금·환불 | 항공권(기본운임 × 인원) + 1인당 예약 수수료(설정값). 사용자 취소는 항공권 금액만, 관리자는 전액/항공권만 선택 |
| 인증 | STATELESS, JWT를 JWE로 암호화해 HttpOnly 쿠키 저장 (access 15분 / refresh 360분), 갱신 시 정지·탈퇴 회원 차단 |
| CSRF | 쿠키 기반 CSRF 토큰 (`csrf.spa()`), axios가 `X-XSRF-TOKEN` 자동 전송 |
| 조회 분리 | CUD·단건은 JPA Repository, 목록·검색·통계는 QueryDSL DAO |
| 예약번호 | `BK{yyyyMMdd}{6자리}`, Redis `INCR`로 다중 서버에서도 중복 없이 채번 |

## 동시성 테스트 결과

Testcontainers(MariaDB 11.4)에서 10개 스레드가 동시에 결제 저장을 시도한 결과입니다. → [상세](docs/concurrency.md)

| 시나리오 | @Version 낙관적 락 | 원자적 UPDATE (현재) |
| --- | --- | --- |
| 같은 좌석 10명 | 성공 1 / 충돌 9 | 성공 1 / 충돌 9 |
| 다른 좌석 10명 | 성공 1 / 충돌 9 ❌ | **성공 10** ✅ |
| 남은 3석에 5명 | — | 성공 3 / 매진 2, 남은 좌석 0 |

## 패키지 구조

```
io.miraclecat.fbs
 ├─ api                  도메인별: controller / service / dao / dto / domain(entity, repository)
 │   ├─ admin            설정, 스케줄러 로그, 대시보드·통계
 │   ├─ booking          예약, 예약 세션(Redis), 취소, 좌석 변경, 이력
 │   ├─ code             항공사, 공항
 │   ├─ flight           항공편, 재고, 좌석 선점(Redis), 좌석 이벤트(WebSocket)
 │   ├─ member           회원가입, 내 정보, 회원 관리
 │   ├─ notice           공지사항
 │   └─ payment          결제 준비·승인, 예약 일괄 저장
 ├─ core                 auth, config, exception, response, mail, utils, audit(로그인 이력)
 ├─ pg/toss              Toss API 클라이언트
 ├─ scheduler            ShedLock 설정, 좌석 선점 만료 잡
 └─ web                  화면 ViewController
frontend/src             페이지별 JS 엔트리 (webpack)
```

## 실행 방법

### 1. 준비
- JDK, Node 22.17.1, MariaDB, Redis
- 환경변수

| 변수 | 설명 |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MariaDB |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE` | Redis |
| `SECRET_JWT_KEY` | JWE 암호화 키 |
| `TOSS_CLIENT_KEY`, `TOSS_SECRET_KEY` | Toss Payments |
| `GOOGLE_CLIENT_KEY`, `GOOGLE_SECRET_KEY` | Google OAuth2 |
| `GOOGLE_SMTP_MAIL`, `GOOGLE_SMTP_KEY`, `GOOGLE_SMTP_PORT` | 메일 발송 (SMTP) |
| `APP_BASE_URL` | 메일 링크·WebSocket 허용 Origin (기본: local `http://localhost:8080`) |
| `LOGGING_PATH`, `SERVER_PORT` | 선택 |

### 2. 빌드·실행
```bash
# 프론트
cd frontend && npm install && npm run build && cd ..

# 서버 (local 프로필)
./gradlew bootRun --args='--spring.profiles.active=local'

# 테스트 (Docker 필요 — 없으면 통합 테스트는 건너뜀)
./gradlew test
```

## 테스트

| 테스트 | 내용 |
| --- | --- |
| `PaymentCheckoutServiceTest` | 금액 대조, 세션 검증, 비즈니스 실패 시 결제 자동 취소 |
| `BookingCancelServiceTest` | 사용자/관리자 취소, 취소 기한, 환불 금액 |
| `BookingTest` | 예약 엔티티 상태 전이 |
| `AdminStatsServiceTest` | 대시보드 증감 계산 |
| `SeatConcurrencyIntegrationTest` | 실제 MariaDB에서 좌석·재고 동시성 |

## 알려진 한계와 개선 방향

| 한계 | 개선 방향 |
| --- | --- |
| WebSocket SimpleBroker(메모리) — 서버 여러 대면 이벤트가 공유되지 않음 | Redis Pub/Sub 또는 외부 브로커 |
| 결제 후 기술적 DB 실패는 수동 복구 | Outbox / 재시도 큐 |
| 정지 회원의 access 토큰은 만료(15분) 전까지 유효 | Redis 블랙리스트 |
| 비밀번호 재설정 토큰을 DB 저장 전에 소비 | 저장 성공 후 삭제 |
| 재고는 관리자가 일괄 생성 | 매일 N일치를 채우는 스케줄러 |
