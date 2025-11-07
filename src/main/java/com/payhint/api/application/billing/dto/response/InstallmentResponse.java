package com.payhint.api.application.billing.dto.response;

import java.math.BigDecimal;

public record InstallmentResponse(String id, BigDecimal amountDue, String dueDate, String status, String createdAt,
                String updatedAt) {
}
