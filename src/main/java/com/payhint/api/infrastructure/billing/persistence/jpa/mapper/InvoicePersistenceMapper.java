package com.payhint.api.infrastructure.billing.persistence.jpa.mapper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateInstallmentFields(Installment domain, @MappingTarget InstallmentJpaEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "installment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updatePaymentFields(Payment domain, @MappingTarget PaymentJpaEntity entity);

    @AfterMapping
    default void reconcileInstallments(Invoice domain, @MappingTarget InvoiceJpaEntity entity) {
        if (domain.getInstallments() == null) {
            entity.getInstallments().clear();
            return;
        }

        Map<UUID, InstallmentJpaEntity> existingMap = entity.getInstallments().stream()
                .collect(Collectors.toMap(InstallmentJpaEntity::getId, Function.identity()));

        Set<UUID> domainIds = new HashSet<>();

        for (Installment domainInstallment : domain.getInstallments()) {
            UUID id = domainInstallment.getId().value();
            domainIds.add(id);

            if (existingMap.containsKey(id)) {
                InstallmentJpaEntity existingEntity = existingMap.get(id);
                updateInstallmentFields(domainInstallment, existingEntity);
                reconcilePayments(domainInstallment, existingEntity);
            } else {
                InstallmentJpaEntity newEntity = toEntity(domainInstallment);
                reconcilePayments(domainInstallment, newEntity);
                entity.addInstallment(newEntity);
            }
        }

        List<InstallmentJpaEntity> toRemove = entity.getInstallments().stream()
                .filter(inst -> !domainIds.contains(inst.getId())).collect(Collectors.toList());

        toRemove.forEach(entity::removeInstallment);
    }

    default void reconcilePayments(Installment domainInstallment, InstallmentJpaEntity installmentEntity) {
        if (domainInstallment.getPayments() == null) {
            installmentEntity.getPayments().clear();
            return;
        }

        Map<UUID, PaymentJpaEntity> existingMap = installmentEntity.getPayments().stream()
                .collect(Collectors.toMap(PaymentJpaEntity::getId, Function.identity()));

        Set<UUID> domainIds = new HashSet<>();

        for (Payment domainPayment : domainInstallment.getPayments()) {
            UUID id = domainPayment.getId().value();
            domainIds.add(id);

            if (existingMap.containsKey(id)) {
                PaymentJpaEntity existingEntity = existingMap.get(id);
                updatePaymentFields(domainPayment, existingEntity);
            } else {
                PaymentJpaEntity newEntity = toEntity(domainPayment);
                installmentEntity.addPayment(newEntity);
            }
        }

        List<PaymentJpaEntity> toRemove = installmentEntity.getPayments().stream()
                .filter(p -> !domainIds.contains(p.getId())).collect(Collectors.toList());

        toRemove.forEach(installmentEntity::removePayment);
    }
}