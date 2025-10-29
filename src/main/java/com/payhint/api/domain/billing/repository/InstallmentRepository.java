package com.payhint.api.domain.billing.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.payhint.api.domain.billing.model.Installment;
import com.payhint.api.domain.billing.model.PaymentStatus;

public interface InstallmentRepository {

    Installment save(Installment installment);

    Optional<Installment> findById(UUID id);

    List<Installment> findAllByInvoiceId(UUID invoiceId);

    List<Installment> findAllByStatus(PaymentStatus status);

    List<Installment> findOverdueInstallments(LocalDate currentDate);

    void deleteById(UUID id);

    boolean existsByIdAndInvoiceId(UUID id, UUID invoiceId);
}
