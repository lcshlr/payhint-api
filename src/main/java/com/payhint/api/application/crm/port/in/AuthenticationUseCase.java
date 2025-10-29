package com.payhint.api.application.crm.port.in;

import com.payhint.api.application.crm.dto.request.LoginUserRequest;
import com.payhint.api.application.crm.dto.request.RegisterUserRequest;
import com.payhint.api.application.crm.dto.response.LoginResponse;
import com.payhint.api.application.crm.dto.response.UserResponse;

import jakarta.validation.Valid;

public interface AuthenticationUseCase {
    UserResponse register(@Valid RegisterUserRequest request);

    LoginResponse login(@Valid LoginUserRequest request);
}
