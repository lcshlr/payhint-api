package com.payhint.api.application.billing.usecase;

import com.payhint.api.application.billing.dto.request.CreateInstallmentRequest;
import com.payhint.api.application.billing.dto.request.UpdateInstallmentRequest;
import com.payhint.api.application.billing.dto.response.InvoiceWithInstallmentsResponse;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.crm.valueobject.UserId;

public interface InstallmentManagementUseCase {
        InvoiceWithInstallmentsResponse addInstallment(UserId userId, InvoiceId invoiceId,
                        CreateInstallmentRequest request);

        InvoiceWithInstallmentsResponse updateInstallment(UserId userId, InvoiceId invoiceId,
                        InstallmentId installmentId, UpdateInstallmentRequest request);

        InvoiceWithInstallmentsResponse removeInstallment(UserId userId, InvoiceId invoiceId,
                        InstallmentId installmentId);
}
