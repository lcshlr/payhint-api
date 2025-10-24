package com.payhint.api.domain.crm.repository;

import java.util.Optional;
import java.util.UUID;

import com.payhint.api.domain.crm.model.User;

public interface UserRepository {

    User register(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteById(UUID id);
}
