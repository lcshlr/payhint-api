package com.payhint.api.infrastructure.billing.persistence.jpa.mapper;

import org.mapstruct.AfterMapping;
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
    InvoiceJpaEntity toEntity(Invoice invoice);

    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "payments", ignore = true)
    InstallmentJpaEntity toEntity(Installment installment);

    @Mapping(target = "installment", ignore = true)
    PaymentJpaEntity toEntity(Payment payment);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "isArchived", source = "archived")
    Invoice toDomain(InvoiceJpaEntity entity);

    Installment toDomain(InstallmentJpaEntity entity);

    Payment toDomain(PaymentJpaEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "installments", ignore = true)
    void updateEntity(Invoice domain, @MappingTarget InvoiceJpaEntity entity);

    @AfterMapping
    default void reconcileInstallments(Invoice domain, @MappingTarget InvoiceJpaEntity entity) {
        if (entity.getInstallments() != null) {
            entity.getInstallments().clear();
        }

        if (domain.getInstallments() != null) {
            domain.getInstallments().forEach(domainInstallment -> {
                InstallmentJpaEntity installmentEntity = toEntity(domainInstallment);
                installmentEntity.setInvoice(entity);
                reconcilePayments(domainInstallment, installmentEntity);
                entity.getInstallments().add(installmentEntity);
            });
        }
    }

    default void reconcilePayments(Installment domainInstallment, InstallmentJpaEntity installmentEntity) {
        if (installmentEntity.getPayments() != null) {
            installmentEntity.getPayments().clear();
        }

        if (domainInstallment.getPayments() != null) {
            domainInstallment.getPayments().forEach(domainPayment -> {
                PaymentJpaEntity paymentEntity = toEntity(domainPayment);
                paymentEntity.setInstallment(installmentEntity);
                installmentEntity.getPayments().add(paymentEntity);
            });
        }
    }
}