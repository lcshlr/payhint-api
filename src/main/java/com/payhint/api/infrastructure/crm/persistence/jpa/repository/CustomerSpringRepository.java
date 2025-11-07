package com.payhint.api.infrastructure.crm.persistence.jpa.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.payhint.api.infrastructure.crm.persistence.jpa.entity.CustomerJpaEntity;

@Repository
public interface CustomerSpringRepository extends JpaRepository<CustomerJpaEntity, UUID> {

    List<CustomerJpaEntity> findAllByUserId(UUID userId);

    boolean existsByUserIdAndCompanyName(UUID userId, String companyName);
}
