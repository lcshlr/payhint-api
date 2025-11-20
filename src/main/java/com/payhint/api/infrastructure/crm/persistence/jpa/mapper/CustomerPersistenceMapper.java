package com.payhint.api.infrastructure.crm.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.payhint.api.application.shared.ValueObjectMapper;
import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.infrastructure.crm.persistence.jpa.entity.CustomerJpaEntity;

@Mapper(componentModel = "spring", uses = { ValueObjectMapper.class, UserReferencePersistenceMapper.class })
public interface CustomerPersistenceMapper {

    @Mapping(target = "user", source = "userId")
    @Mapping(target = "invoices", ignore = true)
    @Mapping(target = "new", ignore = true)
    CustomerJpaEntity toEntity(Customer customer);

    @Mapping(target = "userId", source = "user.id")
    Customer toDomain(CustomerJpaEntity entity);

    @Mapping(target = "user", source = "userId")
    @Mapping(target = "invoices", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "new", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDomain(Customer customer, @MappingTarget CustomerJpaEntity entity);
}