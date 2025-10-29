package com.payhint.api.application.crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginUserRequest(@NotBlank @Size(max = 100) @Email String email,
        @NotBlank @Size(min = 8, max = 30) String password) {
}
