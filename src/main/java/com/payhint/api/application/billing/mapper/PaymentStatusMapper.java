package com.payhint.api.application.billing.mapper;

import org.mapstruct.Mapper;

import com.payhint.api.domain.billing.model.PaymentStatus;

@Mapper(componentModel = "spring")
public interface PaymentStatusMapper {

    default String mapPaymentStatusToString(PaymentStatus status) {
        return status == null ? null : status.name();
    }

    default PaymentStatus mapStringToPaymentStatus(String status) {
        return status == null ? null : PaymentStatus.valueOf(status);
    }
}
