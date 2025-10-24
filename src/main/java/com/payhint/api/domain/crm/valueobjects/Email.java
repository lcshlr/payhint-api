package com.payhint.api.domain.crm.valueobjects;

import jakarta.validation.constraints.NotBlank;

public record Email(
        @NotBlank(message = "Email cannot be empty") @jakarta.validation.constraints.Email(message = "Invalid email format") String value) {
    @Override
    public String toString() {
        return value.toLowerCase();
    }
}