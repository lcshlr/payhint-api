package com.payhint.api.domain.billing.exception;

import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.shared.exception.DomainException;

public class PaymentDoesNotBelongToInstallmentException extends DomainException {
    public PaymentDoesNotBelongToInstallmentException(InstallmentId installmentId) {
        super(String.format("Payment does not belong to installment (Installment ID provided: %s)", installmentId));
    }
}
