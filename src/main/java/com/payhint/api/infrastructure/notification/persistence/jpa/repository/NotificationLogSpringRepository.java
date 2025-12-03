package com.payhint.api.infrastructure.notification.persistence.jpa.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.payhint.api.infrastructure.notification.persistence.jpa.entity.NotificationLogJpaEntity;

@Repository
public interface NotificationLogSpringRepository extends JpaRepository<NotificationLogJpaEntity, UUID> {
    boolean existsByInstallmentId(UUID installmentId);
}