package com.payhint.api.infrastructure.billing.persistence.jpa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
public class InstallmentJpaEntity implements Persistable<UUID> {

    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

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
    @BatchSize(size = 25)
    @Builder.Default
    private Set<PaymentJpaEntity> payments = new LinkedHashSet<>();

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void onPostLoad() {
        this.isNew = false;
    }

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
}