package com.payhint.api.infrastructure.crm.persistence.jpa.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.domain.crm.valueobject.Email;
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.infrastructure.crm.persistence.jpa.entity.UserJpaEntity;
import com.payhint.api.infrastructure.crm.persistence.jpa.mapper.UserPersistenceMapper;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.UserSpringRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserJpaRepositoryAdapter implements UserRepository {

    private final UserSpringRepository springDataUserRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public User register(User user) {
        UserJpaEntity entity = mapper.toEntity(user);
        UserJpaEntity savedEntity = springDataUserRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return springDataUserRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return springDataUserRepository.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return springDataUserRepository.existsByEmail(email.value());
    }

    @Override
    public void delete(User user) {
        springDataUserRepository.deleteById(user.getId().value());
    }

    @Override
    public boolean existsById(UserId id) {
        return springDataUserRepository.existsById(id.value());
    }
}
