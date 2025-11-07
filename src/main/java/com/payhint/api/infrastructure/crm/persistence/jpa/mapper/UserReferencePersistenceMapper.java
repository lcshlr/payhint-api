package com.payhint.api.infrastructure.crm.persistence.jpa.mapper;

import org.springframework.stereotype.Component;

import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.infrastructure.crm.persistence.jpa.entity.UserJpaEntity;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserReferencePersistenceMapper {

    private final EntityManager entityManager;

    public UserJpaEntity toEntity(UserId userId) {
        if (userId == null)
            return null;
        return entityManager.getReference(UserJpaEntity.class, userId.value());
    }

    public UserId toId(UserJpaEntity user) {
        return user != null ? new UserId(user.getId()) : null;
    }
}