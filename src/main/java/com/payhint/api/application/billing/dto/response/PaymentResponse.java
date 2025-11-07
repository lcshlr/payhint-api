package com.payhint.api.application.billing.dto.response;

import java.math.BigDecimal;

public record PaymentResponse(String id, String installmentId, BigDecimal amount, String paymentDate, String createdAt,
        String updatedAt) {
}
