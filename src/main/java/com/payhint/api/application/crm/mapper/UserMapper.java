package com.payhint.api.application.crm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.payhint.api.application.crm.dto.request.RegisterUserRequest;
import com.payhint.api.application.crm.dto.response.UserResponse;
import com.payhint.api.application.shared.ValueObjectMapper;
import com.payhint.api.domain.crm.model.User;

@Mapper(componentModel = "spring", uses = { ValueObjectMapper.class })
public interface UserMapper {

    @Mapping(source = "email", target = "email")
    UserResponse toResponse(User user);

    @Mapping(source = "email", target = "email")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toDomain(RegisterUserRequest request);
}
