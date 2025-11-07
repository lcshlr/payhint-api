package com.payhint.api.application.billing.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentRequest(
        @NotNull(message = "Payment amount is required") @Positive(message = "Payment amount must be positive") BigDecimal amount,
        @NotBlank(message = "Payment date is required") String paymentDate) {
}
