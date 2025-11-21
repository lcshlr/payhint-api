package com.payhint.api.infrastructure.billing.persistence.jpa.mapper;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
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
    InvoiceJpaEntity toEntity(Invoice invoice);

    @Mapping(target = "invoice", ignore = true)
    InstallmentJpaEntity toEntity(Installment installment);

    @Mapping(target = "installment", ignore = true)
    PaymentJpaEntity toEntity(Payment payment);

    @AfterMapping
    default void linkParentsOnCreate(@MappingTarget InvoiceJpaEntity target) {
        if (target.getInstallments() != null) {
            target.getInstallments().forEach(inst -> {
                inst.setInvoice(target);
                if (inst.getPayments() != null) {
                    inst.getPayments().forEach(pay -> pay.setInstallment(inst));
                }
            });
        }
    }

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "isArchived", source = "archived")
    Invoice toDomain(InvoiceJpaEntity entity);

    @Mapping(target = "invoiceId", source = "invoice.id")
    Installment toDomain(InstallmentJpaEntity entity);

    @Mapping(target = "installmentId", source = "installment.id")
    Payment toDomain(PaymentJpaEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(Invoice domain, @MappingTarget InvoiceJpaEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "payments", ignore = true)
    void updateEntity(Installment domain, @MappingTarget InstallmentJpaEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "installment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(Payment domain, @MappingTarget PaymentJpaEntity entity);

    @AfterMapping
    default void reconcileInstallments(Invoice domain, @MappingTarget InvoiceJpaEntity entity) {
        if (domain.getInstallments() == null) {
            if (entity.getInstallments() != null) {
                entity.getInstallments().clear();
            }
            return;
        }

        Map<UUID, Installment> domainMap = domain.getInstallments().stream().filter(i -> i.getId() != null)
                .collect(Collectors.toMap(i -> i.getId().value(), Function.identity()));

        entity.getInstallments().removeIf(existing -> {
            Installment domainItem = domainMap.get(existing.getId());
            if (domainItem != null) {
                updateEntity(domainItem, existing);
                reconcilePayments(domainItem, existing);
                return false;
            }
            return true;
        });

        for (Installment domainItem : domain.getInstallments()) {
            if (domainItem.getId() == null || entity.getInstallments().stream()
                    .noneMatch(e -> Objects.equals(e.getId(), domainItem.getId().value()))) {
                InstallmentJpaEntity newEntity = toEntity(domainItem);
                newEntity.setInvoice(entity);
                entity.getInstallments().add(newEntity);
            }
        }
    }

    default void reconcilePayments(Installment domain, InstallmentJpaEntity entity) {
        if (domain.getPayments() == null) {
            if (entity.getPayments() != null) {
                entity.getPayments().clear();
            }
            return;
        }

        Map<UUID, Payment> domainMap = domain.getPayments().stream().filter(p -> p.getId() != null)
                .collect(Collectors.toMap(p -> p.getId().value(), Function.identity()));

        entity.getPayments().removeIf(existing -> {
            Payment domainItem = domainMap.get(existing.getId());
            if (domainItem != null) {
                updateEntity(domainItem, existing);
                return false;
            }
            return true;
        });

        for (Payment domainItem : domain.getPayments()) {
            if (domainItem.getId() == null || entity.getPayments().stream()
                    .noneMatch(e -> Objects.equals(e.getId(), domainItem.getId().value()))) {
                PaymentJpaEntity newEntity = toEntity(domainItem);
                newEntity.setInstallment(entity);
                entity.getPayments().add(newEntity);
            }
        }
    }
}