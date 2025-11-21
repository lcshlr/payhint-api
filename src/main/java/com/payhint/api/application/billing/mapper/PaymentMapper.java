package com.payhint.api.application.billing.mapper;

import java.util.List;

import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.payhint.api.application.billing.dto.request.UpdatePaymentRequest;
import com.payhint.api.application.billing.dto.response.PaymentResponse;
import com.payhint.api.domain.billing.model.Payment;

@Mapper(componentModel = "spring", uses = { BillingValueObjectMapper.class, DateMapper.class })
public interface PaymentMapper {

    @Named("toPaymentResponse")
    @Mapping(source = "id", target = "id")
    @Mapping(source = "amount", target = "amount")
    @Mapping(source = "paymentDate", target = "paymentDate")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    PaymentResponse toResponse(Payment payment);

    @Named("toPaymentResponseList")
    @IterableMapping(qualifiedByName = "toPaymentResponse")
    List<PaymentResponse> toResponseList(List<Payment> payments);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "amount", target = "amount")
    @Mapping(source = "paymentDate", target = "paymentDate")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Payment toDomain(UpdatePaymentRequest updatePaymentRequest);
}
