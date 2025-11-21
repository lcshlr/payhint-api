package com.payhint.api.application.billing.usecase;

import com.payhint.api.application.billing.dto.request.CreatePaymentRequest;
import com.payhint.api.application.billing.dto.request.UpdatePaymentRequest;
import com.payhint.api.application.billing.dto.response.InvoiceResponse;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.PaymentId;
import com.payhint.api.domain.crm.valueobject.UserId;

public interface PaymentProcessingUseCase {
        InvoiceResponse recordPayment(UserId userId, InvoiceId invoiceId, InstallmentId installmentId,
                        CreatePaymentRequest request);

        InvoiceResponse updatePayment(UserId userId, InvoiceId invoiceId, InstallmentId installmentId,
                        PaymentId paymentId, UpdatePaymentRequest request);

        InvoiceResponse removePayment(UserId userId, InvoiceId invoiceId, InstallmentId installmentId,
                        PaymentId paymentId);
}
