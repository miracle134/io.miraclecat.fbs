package io.miraclecat.fbs.api.payment.service;

import io.miraclecat.fbs.api.booking.domain.repository.BookingRepository;
import io.miraclecat.fbs.api.booking.dto.BookingSessionDto;
import io.miraclecat.fbs.api.flight.domain.entity.Flight;
import io.miraclecat.fbs.api.flight.domain.entity.FlightInventory;
import io.miraclecat.fbs.api.flight.domain.repository.FlightInventoryRepository;
import io.miraclecat.fbs.api.flight.domain.repository.FlightRepository;
import io.miraclecat.fbs.api.flight.domain.repository.SeatReservationRepository;
import io.miraclecat.fbs.api.flight.dto.FlightInventoryDto;
import io.miraclecat.fbs.api.flight.service.FlightInventoryService;
import io.miraclecat.fbs.core.exception.BusinessException;
import io.miraclecat.fbs.pg.toss.TossPaymentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 좌석 동시 예약 통합 테스트 (실제 MariaDB)
 * - 결제 승인 후 저장(PaymentTxService.saveBooking)을 여러 스레드가 동시에 실행
 * - 검증 대상: seat_reservations UNIQUE(inventory_id, seat_number) + flight_inventories 원자적 UPDATE
 *
 * Docker가 없으면 실패하지 않고 건너뜀
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({PaymentTxService.class, FlightInventoryService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)   // 스레드별 트랜잭션이 각자 커밋되도록
class SeatConcurrencyIntegrationTest {

    private static final int TOTAL_SEATS = 30;

    @Container
    @ServiceConnection
    static MariaDBContainer mariadb = new MariaDBContainer("mariadb:11.4")
            .withDatabaseName("flight-booking");

    @Autowired PaymentTxService paymentTxService;
    @Autowired FlightRepository flightRepository;
    @Autowired FlightInventoryRepository flightInventoryRepository;
    @Autowired SeatReservationRepository seatReservationRepository;
    @Autowired BookingRepository bookingRepository;

    private Flight flight;
    private FlightInventory inventory;

    @BeforeEach
    void setUp() {
        seatReservationRepository.deleteAll();
        bookingRepository.deleteAll();
        flightInventoryRepository.deleteAll();
        flightRepository.deleteAll();

        flight = flightRepository.save(Flight.builder()
                .flightNumber("KE" + ThreadLocalRandom.current().nextInt(1000, 9999))
                .airlineCode("KE")
                .departureAirport("GMP")
                .arrivalAirport("CJU")
                .departureTime(LocalTime.of(9, 0))
                .arrivalTime(LocalTime.of(10, 10))
                .durationMinutes(70)
                .totalSeats(TOTAL_SEATS)
                .basePrice(69_000)
                .build());
        inventory = flightInventoryRepository.save(FlightInventory.builder()
                .flightId(flight.getId())
                .flightDate(LocalDate.now().plusDays(7))
                .availableSeats(TOTAL_SEATS)
                .build());
    }

    @Test
    @DisplayName("같은 좌석에 10명이 동시에 결제 저장 → 1건만 성공, 좌석·재고 중복 없음")
    void sameSeat_onlyOneSucceeds() throws Exception {
        Result result = runConcurrently(10, i -> "12A");

        assertThat(result.success()).isEqualTo(1);
        assertThat(result.conflict()).isEqualTo(9);
        assertThat(result.unexpected()).isZero();

        assertThat(seatReservationRepository.countByInventoryId(inventory.getId())).isEqualTo(1);
        assertThat(bookingRepository.count()).isEqualTo(1);
        assertThat(availableSeats()).isEqualTo(TOTAL_SEATS - 1);
    }

    @Test
    @DisplayName("서로 다른 좌석 10건 동시 저장 → 원자적 UPDATE라 10건 모두 성공, 재고 정확히 10 차감")
    void differentSeats_allSucceed() throws Exception {
        Result result = runConcurrently(10, i -> (i + 1) + "A");

        // 개선 전(@Version 낙관적 락): success=1, conflict=9 → 개선 후 기대: success=10
        System.out.printf("[different seats] success=%d, conflict=%d, soldOut=%d%n",
                result.success(), result.conflict(), result.soldOut());

        assertThat(result.unexpected()).isZero();
        assertThat(result.success()).isEqualTo(10);
        assertThat(seatReservationRepository.countByInventoryId(inventory.getId())).isEqualTo(10);
        assertThat(bookingRepository.count()).isEqualTo(10);
        assertThat(availableSeats()).isEqualTo(TOTAL_SEATS - 10);
    }

    @Test
    @DisplayName("남은 좌석 3석에 서로 다른 좌석 5건 동시 저장 → 3건 성공, 2건 좌석 부족, 재고 0 (음수·초과 판매 없음)")
    void soldOut_neverOversells() throws Exception {
        setAvailableSeats(3);

        Result result = runConcurrently(5, i -> (i + 1) + "B");

        assertThat(result.unexpected()).isZero();
        assertThat(result.success()).isEqualTo(3);
        assertThat(result.soldOut()).isEqualTo(2);
        assertThat(seatReservationRepository.countByInventoryId(inventory.getId())).isEqualTo(3);
        assertThat(availableSeats()).isZero();
    }

    /**
     * threads 개 스레드가 동시에 saveBooking 실행
     */
    private Result runConcurrently(int threads, java.util.function.IntFunction<String> seatOf) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int n = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    paymentTxService.saveBooking(session(n, seatOf.apply(n)), toss(n), 1000L + n);
                    success.incrementAndGet();
                } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
                    conflict.incrementAndGet();
                } catch (BusinessException e) {
                    soldOut.incrementAndGet();
                } catch (Throwable e) {
                    unexpected.add(e);
                }
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        unexpected.forEach(Throwable::printStackTrace);
        return new Result(success.get(), conflict.get(), soldOut.get(), unexpected.size());
    }

    private int availableSeats() {
        return flightInventoryRepository.findById(inventory.getId()).orElseThrow().getAvailableSeats();
    }

    private void setAvailableSeats(int seats) {
        FlightInventory current = flightInventoryRepository.findById(inventory.getId()).orElseThrow();
        FlightInventoryDto.AdjustReq req = new FlightInventoryDto.AdjustReq();
        req.setAvailableSeats(seats);
        current.adjustSeats(req);
        flightInventoryRepository.save(current);
    }

    private BookingSessionDto.Session session(int n, String seat) {
        return BookingSessionDto.Session.builder()
                .sessionId("session-" + n)
                .memberId(1000L + n)
                .flightId(flight.getId())
                .inventoryId(inventory.getId())
                .flightDate(inventory.getFlightDate().toString())
                .passengerCount(1)
                .fareAmount(69_000)
                .bookingFeeAmount(5_000)
                .totalAmount(74_000)
                .orderId(String.format("BK%s%06d", LocalDate.now().toString().replace("-", ""), n + 1))
                .passengers(List.of(BookingSessionDto.Passenger.builder()
                        .name("탑승객" + n).nameEn("PASSENGER " + n).email("p" + n + "@test.com")
                        .mobile("010-0000-000" + (n % 10)).birthDate("1990-01-01").gender("male")
                        .seatNumber(seat).build()))
                .build();
    }

    private TossPaymentDto.ConfirmRes toss(int n) {
        return TossPaymentDto.ConfirmRes.builder()
                .paymentKey("pay-key-" + n)
                .status("DONE")
                .method("카드")
                .totalAmount(74_000)
                .approvedAt("2026-10-27T10:00:00+09:00")
                .build();
    }

    private record Result(int success, int conflict, int soldOut, int unexpected) {
    }
}
