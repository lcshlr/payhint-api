package com.payhint.api.infrastructure.billing.persistence.jpa.entity;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.payhint.api.domain.billing.model.Installment;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.infrastructure.billing.persistence.jpa.mapper.InvoicePersistenceMapper;
import com.payhint.api.infrastructure.crm.persistence.jpa.entity.CustomerJpaEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "invoices", uniqueConstraints = @UniqueConstraint(columnNames = { "customer_id", "invoice_reference" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InvoiceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private CustomerJpaEntity customer;

    @Column(name = "invoice_reference", nullable = false)
    private String invoiceReference;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_archived", nullable = false)
    private boolean isArchived;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<InstallmentJpaEntity> installments = new LinkedHashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        invoiceReference = invoiceReference.trim().toUpperCase();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addInstallment(InstallmentJpaEntity installment) {
        installments.add(installment);
        installment.setInvoice(this);
    }

    public void removeInstallment(InstallmentJpaEntity installment) {
        installments.remove(installment);
        installment.setInvoice(null);
    }

    public void updateFromDomain(Invoice domain, InvoicePersistenceMapper mapper, CustomerJpaEntity customerEntity) {
        mapper.mapInvoiceFields(domain, this);

        this.customer = customerEntity;

        Map<UUID, InstallmentJpaEntity> existing = installments.stream()
                .collect(Collectors.toMap(InstallmentJpaEntity::getId, i -> i));

        List<InstallmentJpaEntity> toRemove = installments.stream()
                .filter(i -> domain.getInstallments().stream().noneMatch(d -> d.getId().value().equals(i.getId())))
                .collect(Collectors.toList());

        toRemove.forEach(this::removeInstallment);

        for (Installment d : domain.getInstallments()) {
            if (d.getId() == null) {
                InstallmentJpaEntity newEntity = mapper.toEntity(d);
                addInstallment(newEntity);
                newEntity.updateFromDomain(d, mapper);
            } else {
                InstallmentJpaEntity existingEntity = existing.get(d.getId().value());
                if (existingEntity != null) {
                    existingEntity.updateFromDomain(d, mapper);
                } else {
                    InstallmentJpaEntity newEntity = mapper.toEntity(d);
                    addInstallment(newEntity);
                    newEntity.updateFromDomain(d, mapper);
                }
            }
        }
    }

}
