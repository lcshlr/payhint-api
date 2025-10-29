package com.payhint.api.infrastructure.persistence.jpa.crm.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.payhint.api.infrastructure.persistence.jpa.crm.entity.CustomerJpaEntity;

@Repository
public interface CustomerSpringRepository extends JpaRepository<CustomerJpaEntity, UUID> {

    List<CustomerJpaEntity> findAllByUserId(UUID userId);

    boolean existsByUserIdAndCompanyName(UUID userId, String companyName);
}
