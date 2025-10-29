package com.payhint.api.infrastructure.persistence.jpa.crm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.payhint.api.application.shared.ValueObjectMapper;
import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.infrastructure.persistence.jpa.crm.entity.CustomerJpaEntity;

@Mapper(componentModel = "spring", uses = { ValueObjectMapper.class, UserReferencePersistenceMapper.class })
public interface CustomerPersistenceMapper {

    @Mapping(target = "user", source = "userId")
    @Mapping(target = "invoices", ignore = true)
    CustomerJpaEntity toEntity(Customer customer);

    @Mapping(target = "userId", source = "user.id")
    Customer toDomain(CustomerJpaEntity entity);
}