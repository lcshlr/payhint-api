package com.payhint.api.infrastructure.billing.persistence.jpa.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.payhint.api.application.billing.mapper.BillingValueObjectMapper;
import com.payhint.api.application.shared.ValueObjectMapper;
import com.payhint.api.domain.billing.model.Installment;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.model.Payment;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InstallmentJpaEntity;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InvoiceJpaEntity;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.PaymentJpaEntity;

@Mapper(componentModel = "spring", uses = { BillingValueObjectMapper.class, ValueObjectMapper.class })
public interface InvoicePersistenceMapper {

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "installments", ignore = true)
    @Mapping(target = "isArchived", source = "archived")
    InvoiceJpaEntity toEntity(Invoice invoice);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "isArchived", source = "archived")
    @Mapping(target = "installments", ignore = true)
    Invoice toDomain(InvoiceJpaEntity entity);

    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "payments", ignore = true)
    InstallmentJpaEntity toEntity(Installment installment);

    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "payments", ignore = true)
    Installment toDomain(InstallmentJpaEntity entity);

    @Mapping(target = "installment", ignore = true)
    PaymentJpaEntity toEntity(Payment payment);

    @Mapping(target = "installmentId", source = "installment.id")
    Payment toDomain(PaymentJpaEntity entity);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "installments", ignore = true)
    void mapInvoiceFields(Invoice source, @MappingTarget InvoiceJpaEntity target);

    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "payments", ignore = true)
    void mapInstallmentFields(Installment source, @MappingTarget InstallmentJpaEntity target);

    @Mapping(target = "installment", ignore = true)
    void mapPaymentFields(Payment source, @MappingTarget PaymentJpaEntity target);

    default Invoice toDomainWithDetails(InvoiceJpaEntity entity) {
        Invoice invoice = toDomain(entity);
        for (InstallmentJpaEntity instEntity : entity.getInstallments()) {
            invoice.addInstallment(toDomainWithPayments(instEntity));
        }
        return invoice;
    }

    default Installment toDomainWithPayments(InstallmentJpaEntity entity) {
        Installment installment = toDomain(entity);
        for (PaymentJpaEntity paymentEntity : entity.getPayments()) {
            installment.addPayment(toDomain(paymentEntity));
        }
        return installment;
    }
}
