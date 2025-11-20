package com.payhint.api.application.crm.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.payhint.api.application.crm.dto.request.LoginUserRequest;
import com.payhint.api.application.crm.dto.request.RegisterUserRequest;
import com.payhint.api.application.crm.dto.response.LoginResponse;
import com.payhint.api.application.crm.dto.response.UserResponse;
import com.payhint.api.application.crm.mapper.UserMapper;
import com.payhint.api.application.crm.usecase.AuthenticationUseCase;
import com.payhint.api.application.shared.exception.AlreadyExistsException;
import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.domain.crm.valueobject.Email;
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.infrastructure.shared.security.JwtTokenProvider;

@Service
public class AuthenticationService implements AuthenticationUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    public AuthenticationService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public UserResponse register(RegisterUserRequest request) {
        Email email = request.email() == null ? null : new Email(request.email());
        userRepository.findByEmail(email).ifPresent(user -> {
            var errorMessage = "User with email " + email + " already exists";
            logger.warn(errorMessage);
            throw new AlreadyExistsException(errorMessage);
        });

        UserId userId = new UserId(UUID.randomUUID());
        User user = User.create(userId, email, passwordEncoder.encode(request.password()), request.firstName(),
                request.lastName());
        User savedUser = userRepository.register(user);
        logger.info("User registered successfully: " + savedUser.getEmail());
        return userMapper.toResponse(savedUser);
    }

    @Override
    public LoginResponse login(LoginUserRequest request) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserDetails authUser = (UserDetails) authentication.getPrincipal();
        logger.info("User logged in successfully: " + authUser.getUsername());
        return new LoginResponse(jwtTokenProvider.generateToken(authUser));
    }
}
