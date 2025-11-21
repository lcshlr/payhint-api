package com.payhint.api.application.billing.dto.response;

import java.math.BigDecimal;

public record PaymentResponse(String id, BigDecimal amount, String paymentDate, String createdAt, String updatedAt) {
}
