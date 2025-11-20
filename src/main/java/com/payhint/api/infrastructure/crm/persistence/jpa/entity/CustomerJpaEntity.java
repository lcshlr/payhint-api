package com.payhint.api.infrastructure.crm.persistence.jpa.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InvoiceJpaEntity;
import com.payhint.api.infrastructure.shared.utils.Normalize;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customers", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "company_name" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserJpaEntity user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceJpaEntity> invoices = new ArrayList<>();

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    @PrePersist
    protected void onCreate() {
        contactEmail = Normalize.email(contactEmail);
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        this.isNew = false;
    }

    @PostLoad
    protected void onPostLoad() {
        this.isNew = false;
    }

    @PreUpdate
    protected void onUpdate() {
        contactEmail = Normalize.email(contactEmail);
        updatedAt = LocalDateTime.now();
    }
}
