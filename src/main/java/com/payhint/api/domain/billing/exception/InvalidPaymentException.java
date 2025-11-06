package com.payhint.api.domain.billing.exception;

import com.payhint.api.domain.shared.exception.DomainException;

public class InvalidPaymentException extends DomainException {
    public InvalidPaymentException(String message) {
        super(message);
    }

}
