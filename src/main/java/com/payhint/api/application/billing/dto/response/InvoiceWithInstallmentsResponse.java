package com.payhint.api.application.billing.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceWithInstallmentsResponse(String id, String customerId, String invoiceReference,
        BigDecimal totalAmount, String currency, boolean isArchived, String status, String createdAt, String updatedAt,
        List<InstallmentResponse> installments) {

}
