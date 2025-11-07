package com.payhint.api.application.billing.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceWithInstallmentsAndPaymentsResponse(String id, String customerId, String invoiceReference,
        BigDecimal totalAmount, String currency, String status, BigDecimal totalPaid, BigDecimal remainingAmount,
        boolean isOverdue, List<InstallmentWithPaymentsResponse> installments, boolean isArchived, String createdAt,
        String updatedAt) {
}
