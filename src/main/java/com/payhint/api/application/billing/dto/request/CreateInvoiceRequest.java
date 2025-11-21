package com.payhint.api.application.billing.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInvoiceRequest(@NotNull(message = "Customer ID is required") UUID customerId,
        @NotBlank(message = "Invoice reference is required") String invoiceReference,
        @NotBlank(message = "Currency is required") String currency,
        @Valid List<CreateInstallmentRequest> installments) {
}