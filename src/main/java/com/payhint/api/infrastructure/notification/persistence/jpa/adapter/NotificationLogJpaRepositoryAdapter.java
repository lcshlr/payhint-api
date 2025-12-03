package com.payhint.api.infrastructure.notification.persistence.jpa.adapter;

import org.springframework.stereotype.Repository;

import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.notification.model.NotificationLog;
import com.payhint.api.domain.notification.repository.NotificationLogRepository;
import com.payhint.api.infrastructure.notification.persistence.jpa.entity.NotificationLogJpaEntity;
import com.payhint.api.infrastructure.notification.persistence.jpa.mapper.NotificationLogPersistenceMapper;
import com.payhint.api.infrastructure.notification.persistence.jpa.repository.NotificationLogSpringRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class NotificationLogJpaRepositoryAdapter implements NotificationLogRepository {

    private final NotificationLogSpringRepository springRepository;
    private final NotificationLogPersistenceMapper mapper;

    @Override
    public NotificationLog save(NotificationLog log) {
        NotificationLogJpaEntity entity = mapper.toEntity(log);
        entity.setNew(true);
        return mapper.toDomain(springRepository.save(entity));
    }

    @Override
    public boolean existsByInstallmentId(InstallmentId installmentId) {
        return springRepository.existsByInstallmentId(installmentId.value());
    }
}