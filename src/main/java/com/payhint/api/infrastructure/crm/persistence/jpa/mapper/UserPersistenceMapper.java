package com.payhint.api.infrastructure.crm.persistence.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.payhint.api.application.shared.ValueObjectMapper;
import com.payhint.api.domain.crm.model.User;
import com.payhint.api.infrastructure.crm.persistence.jpa.entity.UserJpaEntity;

@Mapper(componentModel = "spring", uses = { ValueObjectMapper.class })
public interface UserPersistenceMapper {

    @Mapping(target = "email", source = "email")
    @Mapping(target = "customers", ignore = true)
    UserJpaEntity toEntity(User user);

    @Mapping(target = "email", source = "email")
    User toDomain(UserJpaEntity entity);
}