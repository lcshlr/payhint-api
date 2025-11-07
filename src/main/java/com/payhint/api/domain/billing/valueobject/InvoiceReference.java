package com.payhint.api.domain.billing.valueobject;

public record InvoiceReference(String value) {
    public InvoiceReference {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("InvoiceReference value cannot be null or blank.");
        }
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        InvoiceReference that = (InvoiceReference) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
