package com.payhint.api.application.crm.port.in;

import com.payhint.api.application.crm.dto.request.LoginUserRequest;
import com.payhint.api.application.crm.dto.request.RegisterUserRequest;
import com.payhint.api.application.crm.dto.response.LoginResponse;
import com.payhint.api.application.crm.dto.response.UserResponse;

public interface AuthenticationUseCase {
    UserResponse register(RegisterUserRequest request);

    LoginResponse login(LoginUserRequest request);
}
