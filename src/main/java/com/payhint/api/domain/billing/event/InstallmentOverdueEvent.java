package com.payhint.api.domain.billing.event;

import java.time.LocalDate;

import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.crm.valueobject.UserId;

public record InstallmentOverdueEvent(InstallmentId installmentId, InvoiceId invoiceId, UserId userId,
        LocalDate dueDate) {
}