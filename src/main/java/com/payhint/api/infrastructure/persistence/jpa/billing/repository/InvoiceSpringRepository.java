package com.payhint.api.infrastructure.persistence.jpa.billing.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.payhint.api.infrastructure.persistence.jpa.billing.entity.InvoiceJpaEntity;

@Repository
public interface InvoiceSpringRepository extends JpaRepository<InvoiceJpaEntity, UUID> {
    List<InvoiceJpaEntity> findAllByCustomerId(UUID customerId);

    Optional<InvoiceJpaEntity> findByCustomerIdAndInvoiceReference(UUID customerId, String invoiceReference);
}
