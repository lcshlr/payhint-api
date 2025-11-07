package com.payhint.api.application.billing.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record InstallmentWithPaymentsResponse(String id, BigDecimal amountDue, BigDecimal amountPaid, String dueDate,
                String status, List<PaymentResponse> payments, String createdAt, String updatedAt) {
}
