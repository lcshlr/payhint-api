package com.payhint.api.domain.billing.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.payhint.api.domain.billing.model.Payment;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    List<Payment> findAllByInstallmentId(UUID installmentId);

    void deleteById(UUID id);
}
