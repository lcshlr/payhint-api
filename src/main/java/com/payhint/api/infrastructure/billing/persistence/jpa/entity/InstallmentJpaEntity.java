package com.payhint.api.infrastructure.billing.persistence.jpa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.payhint.api.domain.billing.model.Installment;
import com.payhint.api.domain.billing.model.Payment;
import com.payhint.api.infrastructure.billing.persistence.jpa.mapper.InvoicePersistenceMapper;

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
@Table(name = "installments", uniqueConstraints = @UniqueConstraint(columnNames = { "invoice_id", "due_date" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InstallmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private InvoiceJpaEntity invoice;

    @Column(name = "amount_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "installment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PaymentJpaEntity> payments = new LinkedHashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addPayment(PaymentJpaEntity payment) {
        payments.add(payment);
        payment.setInstallment(this);
    }

    public void removePayment(PaymentJpaEntity payment) {
        payments.remove(payment);
        payment.setInstallment(null);
    }

    public void updateFromDomain(Installment domain, InvoicePersistenceMapper mapper) {
        mapper.mapInstallmentFields(domain, this);

        Map<UUID, PaymentJpaEntity> existing = payments.stream()
                .collect(Collectors.toMap(PaymentJpaEntity::getId, p -> p));

        List<PaymentJpaEntity> toRemove = payments.stream()
                .filter(p -> domain.getPayments().stream().noneMatch(d -> d.getId().value().equals(p.getId())))
                .collect(Collectors.toList());

        toRemove.forEach(this::removePayment);

        for (Payment d : domain.getPayments()) {
            if (d.getId() == null) {
                PaymentJpaEntity newPay = mapper.toEntity(d);
                addPayment(newPay);
            } else {
                PaymentJpaEntity existingPay = existing.get(d.getId().value());
                if (existingPay != null) {
                    existingPay.updateFromDomain(d, mapper);
                } else {
                    PaymentJpaEntity newPay = mapper.toEntity(d);
                    addPayment(newPay);
                    newPay.updateFromDomain(d, mapper);
                }
            }
        }
    }
}