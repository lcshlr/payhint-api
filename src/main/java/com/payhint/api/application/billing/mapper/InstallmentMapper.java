package com.payhint.api.application.billing.mapper;

import java.util.List;

import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import com.payhint.api.application.billing.dto.response.InstallmentResponse;
import com.payhint.api.domain.billing.model.Installment;

@Mapper(componentModel = "spring", uses = { BillingValueObjectMapper.class, PaymentMapper.class, DateMapper.class,
        PaymentStatusMapper.class })
public interface InstallmentMapper {

    @Named("toInstallmentResponse")
    InstallmentResponse toInstallmentResponse(Installment installment);

    @Named("toInstallmentResponseList")
    @IterableMapping(qualifiedByName = "toInstallmentResponse")
    List<InstallmentResponse> toInstallmentResponse(List<Installment> installments);
}
