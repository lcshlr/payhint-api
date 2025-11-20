package com.payhint.api.infrastructure.crm.persistence.jpa.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import com.payhint.api.infrastructure.shared.utils.Normalize;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserJpaEntity implements Persistable<UUID> {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CustomerJpaEntity> customers = new ArrayList<>();

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    @PrePersist
    protected void onCreate() {
        email = Normalize.email(email);
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
        email = Normalize.email(email);
        updatedAt = LocalDateTime.now();
    }
}
