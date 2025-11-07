package com.payhint.api.infrastructure.crm.persistence.jpa.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.payhint.api.infrastructure.crm.persistence.jpa.entity.UserJpaEntity;

@Repository
public interface UserSpringRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsById(@NonNull UUID id);
}
