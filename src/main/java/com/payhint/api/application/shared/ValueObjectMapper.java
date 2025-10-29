package com.payhint.api.application.shared;

import java.util.UUID;

import org.mapstruct.Mapper;

import com.payhint.api.domain.crm.valueobjects.CustomerId;
import com.payhint.api.domain.crm.valueobjects.Email;
import com.payhint.api.domain.crm.valueobjects.UserId;

@Mapper(componentModel = "spring")
public interface ValueObjectMapper {

    default String map(Email email) {
        return email == null ? null : email.value();
    }

    default Email map(String email) {
        return email == null ? null : new Email(email);
    }

    default String map(CustomerId customerId) {
        return customerId == null ? null : customerId.toString();
    }

    default CustomerId mapToCustomerId(String id) {
        return id == null ? null : new CustomerId(UUID.fromString(id));
    }

    default String map(UserId userId) {
        return userId == null ? null : userId.toString();
    }

    default UserId mapToUserId(String id) {
        return id == null ? null : new UserId(UUID.fromString(id));
    }
}
