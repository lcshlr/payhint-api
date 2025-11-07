package com.payhint.api.application.billing.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Positive;

public record UpdateInstallmentRequest(@Positive(message = "Amount due must be positive") BigDecimal amountDue,
        String dueDate) {
}
