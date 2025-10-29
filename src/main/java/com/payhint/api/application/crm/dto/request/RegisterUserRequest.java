package com.payhint.api.application.crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(@Email @NotBlank @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 30) String password, @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName) {
}
