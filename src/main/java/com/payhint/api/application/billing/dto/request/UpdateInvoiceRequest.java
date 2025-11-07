package com.payhint.api.application.billing.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateInvoiceRequest(@Size(max = 50) String invoiceReference, @Size(max = 3) String currency) {
}
