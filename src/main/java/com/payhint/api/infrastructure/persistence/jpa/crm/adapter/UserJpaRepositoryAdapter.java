package com.payhint.api.infrastructure.persistence.jpa.crm.adapter;

import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.infrastructure.persistence.jpa.crm.mapper.UserPersistenceMapper;
import com.payhint.api.infrastructure.persistence.jpa.crm.repository.UserSpringRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserJpaRepositoryAdapter implements UserRepository, UserDetailsService {

    private final UserSpringRepository springDataUserRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public User register(User user) {
        var entity = mapper.toEntity(user);
        var savedEntity = springDataUserRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataUserRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email.toLowerCase()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email.toLowerCase());
    }

    @Override
    public void deleteById(UUID id) {
        springDataUserRepository.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return springDataUserRepository.findByEmail(email.toLowerCase())
                .map(user -> org.springframework.security.core.userdetails.User.builder().username(user.getEmail())
                        .password(user.getPassword()).roles("USER").build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
