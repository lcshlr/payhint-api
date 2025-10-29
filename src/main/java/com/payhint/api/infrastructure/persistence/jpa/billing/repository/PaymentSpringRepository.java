package com.payhint.api.infrastructure.persistence.jpa.billing.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.payhint.api.infrastructure.persistence.jpa.billing.entity.PaymentJpaEntity;

@Repository
public interface PaymentSpringRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    List<PaymentJpaEntity> findAllByInstallmentId(UUID installmentId);
}
