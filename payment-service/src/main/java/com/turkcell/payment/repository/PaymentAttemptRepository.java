package com.turkcell.payment.repository;

import com.turkcell.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    List<PaymentAttempt> findAllByPaymentIdOrderByAttemptNoAsc(UUID paymentId);

    int countByPaymentId(UUID paymentId);
}
