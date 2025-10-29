package com.payhint.api.domain.crm.valueobjects;

import java.util.UUID;

public record UserId(UUID value) {

    @Override
    public String toString() {
        return value.toString();
    }
}
