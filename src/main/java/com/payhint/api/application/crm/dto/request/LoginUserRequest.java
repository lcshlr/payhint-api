package com.payhint.api.application.crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginUserRequest {
    @NotBlank
    @Email
    private String email;
    @NotBlank
    @Size(min = 8, max = 30)
    private String password;
}
