package com.payhint.api.domain.crm.valueobjects;

public class Email {
    private String value;

    public Email(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        this.value = value.toLowerCase();
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value.toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Email email = (Email) o;
        return value != null ? value.equals(email.value) : email.value == null;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}