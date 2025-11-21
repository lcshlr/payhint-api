package com.payhint.api.application.billing.usecase;

import com.payhint.api.application.billing.dto.request.CreateInstallmentRequest;
import com.payhint.api.application.billing.dto.request.UpdateInstallmentRequest;
import com.payhint.api.application.billing.dto.response.InvoiceResponse;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.crm.valueobject.UserId;

public interface InstallmentSchedulingUseCase {
        InvoiceResponse addInstallment(UserId userId, InvoiceId invoiceId, CreateInstallmentRequest request);

        InvoiceResponse updateInstallment(UserId userId, InvoiceId invoiceId, InstallmentId installmentId,
                        UpdateInstallmentRequest request);

        InvoiceResponse removeInstallment(UserId userId, InvoiceId invoiceId, InstallmentId installmentId);
}
