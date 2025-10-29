package com.payhint.api.infrastructure.persistence.jpa.billing.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.infrastructure.persistence.jpa.billing.entity.InvoiceJpaEntity;

@Mapper(componentModel = "spring", uses = { InstallmentPersistenceMapper.class })
public interface InvoicePersistenceMapper {

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "installments", ignore = true)
    InvoiceJpaEntity toEntity(Invoice invoice);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "installments", ignore = true)
    Invoice toDomain(InvoiceJpaEntity entity);
}
