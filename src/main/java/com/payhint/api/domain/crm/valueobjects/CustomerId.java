package com.payhint.api.domain.crm.valueobjects;

import java.util.UUID;

public record CustomerId(UUID value) {
    @Override
    public String toString() {
        return value.toString();
    }
}
