package com.payhint.api.application.crm.service;

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
import com.payhint.api.application.crm.port.in.AuthenticationUseCase;
import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.infrastructure.security.JwtTokenProvider;

@Service
public class AuthenticationService implements AuthenticationUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

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
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists.");
        });

        User user = userMapper.toDomain(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.register(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public LoginResponse login(LoginUserRequest request) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        UserDetails authUser = (UserDetails) authentication.getPrincipal();
        return new LoginResponse(jwtTokenProvider.generateToken(authUser));
    }
}
