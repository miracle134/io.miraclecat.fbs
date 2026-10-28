/**
 * packageName    : io.miraclecat.fbs.api.payment.repository
 * fileName       : PaymentRepository
 * author         : MiracleCat
 * date           : 2026-05-19
 * description    : 결제 Repository
 */
package io.miraclecat.fbs.api.payment.domain.repository;

import io.miraclecat.fbs.api.payment.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

}
