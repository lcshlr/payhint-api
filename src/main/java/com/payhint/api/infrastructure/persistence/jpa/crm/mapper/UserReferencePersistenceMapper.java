package com.payhint.api.infrastructure.persistence.jpa.crm.mapper;

import org.springframework.stereotype.Component;

import com.payhint.api.domain.crm.valueobjects.UserId;
import com.payhint.api.infrastructure.persistence.jpa.crm.entity.UserJpaEntity;

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