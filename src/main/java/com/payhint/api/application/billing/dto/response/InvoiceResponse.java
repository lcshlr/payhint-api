package com.payhint.api.application.billing.dto.response;

public record InvoiceResponse(String id, String customerId, String invoiceReference, String currency,
                boolean isArchived, String createdAt, String updatedAt) {
}
