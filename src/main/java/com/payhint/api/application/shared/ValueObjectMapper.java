package com.payhint.api.application.shared;

import org.mapstruct.Mapper;

import com.payhint.api.domain.crm.valueobjects.Email;

@Mapper(componentModel = "spring")
public interface ValueObjectMapper {

    default String map(Email email) {
        return email == null ? null : email.value();
    }

    default Email map(String email) {
        return email == null ? null : new Email(email);
    }
}
