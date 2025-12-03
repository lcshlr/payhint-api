package com.payhint.api.infrastructure.billing.persistence.jpa.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import com.payhint.api.infrastructure.crm.persistence.jpa.entity.CustomerJpaEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
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
public class InvoiceJpaEntity implements Persistable<UUID> {

    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    private UUID id;

    @Version
    private Long version;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private CustomerJpaEntity customer;

    @Column(name = "invoice_reference", nullable = false)
    private String invoiceReference;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPaid;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_status_change_at", nullable = false)
    private LocalDateTime lastStatusChangeAt;

    @Column(name = "is_archived", nullable = false)
    private boolean isArchived;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<InstallmentJpaEntity> installments = new LinkedHashSet<>();

    @PostLoad
    protected void onPostLoad() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    public void addInstallment(InstallmentJpaEntity installment) {
        installments.add(installment);
        installment.setInvoice(this);
    }

    public void removeInstallment(InstallmentJpaEntity installment) {
        installments.remove(installment);
        installment.setInvoice(null);
    }
}
