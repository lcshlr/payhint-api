package com.payhint.api.application.billing.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateInstallmentRequest(
        @NotNull(message = "Amount due is required") @Positive(message = "Amount due must be positive") BigDecimal amountDue,
        @NotBlank(message = "Due date is required") @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Due date must be in the format YYYY-MM-DD") String dueDate) {
}