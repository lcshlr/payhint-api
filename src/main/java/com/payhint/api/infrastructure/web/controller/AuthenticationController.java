package com.payhint.api.infrastructure.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.payhint.api.application.crm.dto.request.LoginUserRequest;
import com.payhint.api.application.crm.dto.request.RegisterUserRequest;
import com.payhint.api.application.crm.dto.response.LoginResponse;
import com.payhint.api.application.crm.dto.response.UserResponse;
import com.payhint.api.application.crm.port.in.AuthenticationUseCase;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationUseCase authenticationUseCase;

    public AuthenticationController(AuthenticationUseCase authenticationUseCase) {
        this.authenticationUseCase = authenticationUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        UserResponse response = authenticationUseCase.register(request);
        return response;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginUserRequest request) {
        LoginResponse response = authenticationUseCase.login(request);
        return response;
    }

}