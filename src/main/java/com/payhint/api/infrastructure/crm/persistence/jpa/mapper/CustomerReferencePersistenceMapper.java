package com.payhint.api.infrastructure.crm.persistence.jpa.mapper;

import org.springframework.stereotype.Component;

import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.infrastructure.crm.persistence.jpa.entity.CustomerJpaEntity;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerReferencePersistenceMapper {

    private final EntityManager entityManager;

    public CustomerJpaEntity toEntity(CustomerId customerId) {
        if (customerId == null)
            return null;
        return entityManager.getReference(CustomerJpaEntity.class, customerId.value());
    }

    public CustomerId toId(CustomerJpaEntity customer) {
        return customer != null ? new CustomerId(customer.getId()) : null;
    }
}
