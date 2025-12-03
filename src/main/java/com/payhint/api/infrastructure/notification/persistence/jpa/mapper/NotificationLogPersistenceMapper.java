package com.payhint.api.infrastructure.notification.persistence.jpa.mapper;

import org.mapstruct.Mapper;

import com.payhint.api.application.billing.mapper.BillingValueObjectMapper;
import com.payhint.api.application.shared.ValueObjectMapper;
import com.payhint.api.domain.notification.model.NotificationLog;
import com.payhint.api.infrastructure.notification.persistence.jpa.entity.NotificationLogJpaEntity;

@Mapper(componentModel = "spring", uses = { ValueObjectMapper.class, BillingValueObjectMapper.class })
public interface NotificationLogPersistenceMapper {

    NotificationLogJpaEntity toEntity(NotificationLog log);

    NotificationLog toDomain(NotificationLogJpaEntity entity);
}