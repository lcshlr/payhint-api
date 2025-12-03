package com.payhint.api.infrastructure.billing.persistence.jpa.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.payhint.api.domain.billing.model.PaymentStatus;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InvoiceJpaEntity;

@Repository
public interface InvoiceSpringRepository extends JpaRepository<InvoiceJpaEntity, UUID> {
    interface InvoiceSummaryProjection {
        UUID getId();

        UUID getCustomerId();

        String getInvoiceReference();

        BigDecimal getTotalAmount();

        BigDecimal getTotalPaid();

        String getCurrency();

        PaymentStatus getStatus();

        LocalDateTime getCreatedAt();

        LocalDateTime getUpdatedAt();

        LocalDateTime getLastStatusChangeAt();

        boolean getIsArchived();

        boolean getIsOverdue();
    }

    @Query("SELECT DISTINCT i FROM InvoiceJpaEntity i LEFT JOIN FETCH i.installments inst LEFT JOIN FETCH inst.payments WHERE i.id = :id")
    @NonNull
    Optional<InvoiceJpaEntity> findById(@NonNull UUID id);

    @Query("SELECT DISTINCT i FROM InvoiceJpaEntity i LEFT JOIN FETCH i.installments inst LEFT JOIN FETCH inst.payments WHERE i.customer.id = :customerId")
    List<InvoiceJpaEntity> findAllByCustomerId(@NonNull UUID customerId);

    @Query("SELECT i.id AS id, i.customer.id AS customerId, i.invoiceReference AS invoiceReference, i.totalAmount AS totalAmount, i.totalPaid AS totalPaid, i.currency AS currency, i.status AS status, i.createdAt AS createdAt, i.updatedAt AS updatedAt, i.lastStatusChangeAt AS lastStatusChangeAt, i.isArchived AS isArchived, CASE WHEN EXISTS (SELECT 1 FROM InstallmentJpaEntity inst WHERE inst.invoice.id = i.id AND inst.dueDate < CURRENT_DATE AND inst.status <> 'PAID') THEN true ELSE false END AS isOverdue FROM InvoiceJpaEntity i WHERE i.customer.user.id = :userId")
    List<InvoiceSummaryProjection> findSummariesByUserId(@NonNull UUID userId);

    @Query("SELECT i.id AS id, i.customer.id AS customerId, i.invoiceReference AS invoiceReference, i.totalAmount AS totalAmount, i.totalPaid AS totalPaid, i.currency AS currency, i.status AS status, i.createdAt AS createdAt, i.updatedAt AS updatedAt, i.lastStatusChangeAt AS lastStatusChangeAt, i.isArchived AS isArchived, CASE WHEN EXISTS (SELECT 1 FROM InstallmentJpaEntity inst WHERE inst.invoice.id = i.id AND inst.dueDate < CURRENT_DATE AND inst.status <> 'PAID') THEN true ELSE false END AS isOverdue FROM InvoiceJpaEntity i WHERE i.customer.id = :customerId")
    List<InvoiceSummaryProjection> findSummariesByCustomerId(@NonNull UUID customerId);

    Optional<InvoiceJpaEntity> findByCustomerIdAndInvoiceReference(@NonNull UUID customerId,
            @NonNull String invoiceReference);

    @Query("SELECT DISTINCT i FROM InvoiceJpaEntity i LEFT JOIN FETCH i.installments inst LEFT JOIN FETCH inst.payments WHERE i.id = :invoiceId AND i.customer.user.id = :userId")
    Optional<InvoiceJpaEntity> findByIdAndOwner(@NonNull UUID invoiceId, @NonNull UUID userId);
}
