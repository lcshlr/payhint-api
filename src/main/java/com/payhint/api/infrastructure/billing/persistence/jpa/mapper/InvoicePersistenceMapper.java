package com.payhint.api.infrastructure.billing.persistence.jpa.mapper;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @Mapping(target = "new", ignore = true)
    InvoiceJpaEntity toEntity(Invoice invoice);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "isArchived", source = "archived")
    @Mapping(target = "installments", ignore = true)
    Invoice toDomain(InvoiceJpaEntity entity);

    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "new", ignore = true)
    InstallmentJpaEntity toEntity(Installment installment);

    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "payments", ignore = true)
    Installment toDomain(InstallmentJpaEntity entity);

    @Mapping(target = "installment", ignore = true)
    @Mapping(target = "new", ignore = true)
    PaymentJpaEntity toEntity(Payment payment);

    @Mapping(target = "installmentId", source = "installment.id")
    Payment toDomain(PaymentJpaEntity entity);

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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "new", ignore = true)
    @Mapping(target = "installments", ignore = true)
    void updateEntityFromDomain(Invoice domain, @MappingTarget InvoiceJpaEntity entity);

    @AfterMapping
    default void reconcileInstallments(Invoice domain, @MappingTarget InvoiceJpaEntity entity) {
        Map<UUID, Installment> domainInstallmentsMap = domain.getInstallments().stream()
                .collect(Collectors.toMap(i -> i.getId().value(), i -> i));

        entity.getInstallments().removeIf(entityInstallment -> {
            Installment domainInstallment = domainInstallmentsMap.get(entityInstallment.getId());
            if (domainInstallment == null) {
                return true;
            } else {
                updateEntity(domainInstallment, entityInstallment);
                return false;
            }
        });

        domain.getInstallments().forEach(domainInstallment -> {
            boolean exists = entity.getInstallments().stream()
                    .anyMatch(i -> i.getId().equals(domainInstallment.getId().value()));

            if (!exists) {
                InstallmentJpaEntity newEntity = toEntity(domainInstallment);
                entity.addInstallment(newEntity);
            }
        });
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "new", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "payments", ignore = true)
    void updateEntity(Installment domain, @MappingTarget InstallmentJpaEntity entity);

    @AfterMapping
    default void reconcilePayments(Installment domain, @MappingTarget InstallmentJpaEntity entity) {
        Map<UUID, Payment> domainPaymentsMap = domain.getPayments().stream()
                .collect(Collectors.toMap(p -> p.getId().value(), p -> p));

        entity.getPayments().removeIf(entityPayment -> {
            Payment domainPayment = domainPaymentsMap.get(entityPayment.getId());
            if (domainPayment == null) {
                return true;
            } else {
                updateEntity(domainPayment, entityPayment);
                return false;
            }
        });

        domain.getPayments().forEach(domainPayment -> {
            boolean exists = entity.getPayments().stream()
                    .anyMatch(p -> p.getId().equals(domainPayment.getId().value()));
            if (!exists) {
                PaymentJpaEntity newPayment = toEntity(domainPayment);
                entity.addPayment(newPayment);
            }
        });
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "installment", ignore = true)
    @Mapping(target = "new", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(Payment domain, @MappingTarget PaymentJpaEntity entity);
}
