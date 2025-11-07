package com.payhint.api.infrastructure.billing.persistence.jpa.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InvoiceJpaEntity;

@Repository
public interface InvoiceSpringRepository extends JpaRepository<InvoiceJpaEntity, UUID> {
    @Query("select i from InvoiceJpaEntity i left join fetch i.installments inst left join fetch inst.payments where i.id = :id")
    Optional<InvoiceJpaEntity> findByIdWithInstallmentsAndPayments(UUID id);

    @Query("select i from InvoiceJpaEntity i left join fetch i.installments where i.id = :id")
    Optional<InvoiceJpaEntity> findByIdWithInstallments(UUID id);

    @Query("select i from InvoiceJpaEntity i left join fetch i.installments inst left join fetch inst.payments where i.customer.id = :customerId")
    List<InvoiceJpaEntity> findAllWithInstallmentsAndPaymentsByCustomerId(UUID customerId);

    List<InvoiceJpaEntity> findAllByCustomerId(UUID customerId);

    Optional<InvoiceJpaEntity> findByCustomerIdAndInvoiceReference(UUID customerId, String invoiceReference);
}
