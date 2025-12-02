package com.payhint.api.application.billing.dto.response;

import java.math.BigDecimal;

public record InvoiceSummaryResponse(String id, String customerId, String invoiceReference, BigDecimal totalAmount,
                String currency, String status, boolean isOverdue, boolean isArchived, String createdAt,
                String updatedAt, String lastStatusChangeAt) {
}
