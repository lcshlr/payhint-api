package com.payhint.api.domain.crm.valueobjects;

import java.util.UUID;

import com.payhint.api.domain.shared.exception.InvalidPropertyException;

public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new InvalidPropertyException("UserId cannot be null");
        }
    }

    public static UserId fromString(String id) {
        if (id == null) {
            throw new InvalidPropertyException("Invalid UserId format: null");
        }
        try {
            return new UserId(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            throw new InvalidPropertyException("Invalid UserId format: " + id);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
