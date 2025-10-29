package com.payhint.api.infrastructure.persistence.jpa.billing.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.payhint.api.domain.billing.model.Installment;
import com.payhint.api.infrastructure.persistence.jpa.billing.entity.InstallmentJpaEntity;

@Mapper(componentModel = "spring", uses = { PaymentPersistenceMapper.class })
public interface InstallmentPersistenceMapper {

    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "payments", ignore = true)
    InstallmentJpaEntity toEntity(Installment installment);

    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "payments", ignore = true)
    Installment toDomain(InstallmentJpaEntity entity);
}
