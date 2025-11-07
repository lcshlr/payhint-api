package com.payhint.api.application.billing.mapper;

import java.util.List;

import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.payhint.api.application.billing.dto.response.InvoiceResponse;
import com.payhint.api.application.billing.dto.response.InvoiceWithInstallmentsAndPaymentsResponse;
import com.payhint.api.application.billing.dto.response.InvoiceWithInstallmentsResponse;
import com.payhint.api.application.shared.ValueObjectMapper;
import com.payhint.api.domain.billing.model.Invoice;

@Mapper(componentModel = "spring", uses = { BillingValueObjectMapper.class, ValueObjectMapper.class,
                InstallmentMapper.class, DateMapper.class, PaymentStatusMapper.class })
public interface InvoiceMapper {

        @Named("toInvoiceResponse")
        @Mapping(source = "archived", target = "isArchived")
        InvoiceResponse toInvoiceResponse(Invoice invoice);

        @Named("toInvoiceWithInstallmentsResponse")
        @Mapping(source = "archived", target = "isArchived")
        @Mapping(source = "installments", target = "installments", qualifiedByName = "toInstallmentResponse")
        InvoiceWithInstallmentsResponse toInvoiceWithInstallmentsResponse(Invoice invoice);

        @Named("toInvoiceWithInstallmentsAndPaymentsResponse")
        @Mapping(source = "archived", target = "isArchived")
        @Mapping(source = "overdue", target = "isOverdue")
        @Mapping(source = "installments", target = "installments", qualifiedByName = "toInstallmentWithPaymentsResponse")
        InvoiceWithInstallmentsAndPaymentsResponse toInvoiceWithInstallmentsAndPaymentsResponse(Invoice invoice);

        @IterableMapping(qualifiedByName = { "toInvoiceWithInstallmentsAndPaymentsResponse" })
        List<InvoiceWithInstallmentsAndPaymentsResponse> toInvoiceWithInstallmentsAndPaymentsResponse(
                        List<Invoice> invoices);

        @IterableMapping(qualifiedByName = "toInvoiceResponse")
        List<InvoiceResponse> toInvoiceResponse(List<Invoice> invoices);
}
