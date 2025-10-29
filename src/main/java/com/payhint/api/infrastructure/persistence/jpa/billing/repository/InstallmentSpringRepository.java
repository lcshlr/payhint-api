package com.payhint.api.infrastructure.persistence.jpa.billing.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.payhint.api.domain.billing.model.PaymentStatus;
import com.payhint.api.infrastructure.persistence.jpa.billing.entity.InstallmentJpaEntity;

@Repository
public interface InstallmentSpringRepository extends JpaRepository<InstallmentJpaEntity, UUID> {

    List<InstallmentJpaEntity> findAllByInvoiceId(UUID invoiceId);

    List<InstallmentJpaEntity> findAllByStatus(PaymentStatus status);

    @Query("SELECT i FROM InstallmentJpaEntity i WHERE i.dueDate < :currentDate AND (i.status = 'PENDING' OR i.status = 'PARTIALLY_PAID')")
    List<InstallmentJpaEntity> findOverdueInstallments(@Param("currentDate") LocalDate currentDate);

    boolean existsByIdAndInvoiceId(UUID id, UUID invoiceId);
}