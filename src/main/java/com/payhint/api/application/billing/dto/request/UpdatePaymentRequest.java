package com.payhint.api.application.billing.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Positive;

public record UpdatePaymentRequest(@Positive(message = "Payment amount must be positive") BigDecimal amount,
        String paymentDate) {
}
