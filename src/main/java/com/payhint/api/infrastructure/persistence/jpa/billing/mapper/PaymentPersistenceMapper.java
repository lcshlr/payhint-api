package com.payhint.api.infrastructure.persistence.jpa.billing.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.payhint.api.domain.billing.model.Payment;
import com.payhint.api.infrastructure.persistence.jpa.billing.entity.PaymentJpaEntity;

@Mapper(componentModel = "spring")
public interface PaymentPersistenceMapper {

    @Mapping(target = "installment", ignore = true)
    PaymentJpaEntity toEntity(Payment payment);

    @Mapping(target = "installmentId", source = "installment.id")
    Payment toDomain(PaymentJpaEntity entity);
}
