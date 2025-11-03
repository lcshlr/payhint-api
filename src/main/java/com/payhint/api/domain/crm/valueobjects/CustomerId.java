package com.payhint.api.domain.crm.valueobjects;

import java.util.UUID;

import com.payhint.api.domain.shared.exception.InvalidPropertyException;

public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new InvalidPropertyException("CustomerId cannot be null");
        }
    }

    public static CustomerId fromString(String id) {
        if (id == null) {
            throw new InvalidPropertyException("Invalid CustomerId format: null");
        }
        try {
            return new CustomerId(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            throw new InvalidPropertyException("Invalid CustomerId format: " + id);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
