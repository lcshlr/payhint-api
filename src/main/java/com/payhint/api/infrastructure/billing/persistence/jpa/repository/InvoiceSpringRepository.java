package com.payhint.api.infrastructure.billing.persistence.jpa.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InvoiceJpaEntity;

@Repository
public interface InvoiceSpringRepository extends JpaRepository<InvoiceJpaEntity, UUID> {
    @Query("SELECT DISTINCT i FROM InvoiceJpaEntity i LEFT JOIN FETCH i.installments WHERE i.id = :id")
    @NonNull
    Optional<InvoiceJpaEntity> findById(@NonNull UUID id);

    @Query("SELECT DISTINCT i FROM InvoiceJpaEntity i LEFT JOIN FETCH i.installments WHERE i.customer.id = :customerId")
    List<InvoiceJpaEntity> findAllByCustomerId(@NonNull UUID customerId);

    Optional<InvoiceJpaEntity> findByCustomerIdAndInvoiceReference(@NonNull UUID customerId,
            @NonNull String invoiceReference);

    @Query("SELECT DISTINCT i FROM InvoiceJpaEntity i LEFT JOIN FETCH i.installments WHERE i.id = :invoiceId AND i.customer.user.id = :userId")
    Optional<InvoiceJpaEntity> findByIdAndOwner(@NonNull UUID invoiceId, @NonNull UUID userId);
}
