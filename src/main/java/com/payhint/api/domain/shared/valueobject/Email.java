package com.payhint.api.domain.shared.valueobject;

import java.util.regex.Pattern;

import com.payhint.api.domain.shared.exception.InvalidPropertyException;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("^[A-Za-z0-9+_-]+(\\.[A-Za-z0-9+_-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new InvalidPropertyException("Email cannot be null or empty");
        }
        String normalized = value.toLowerCase().trim();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidPropertyException("Invalid email format: " + value);
        }
        value = normalized;
    }

    @Override
    public String toString() {
        return value;
    }
}