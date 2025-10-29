package com.payhint.api.application.crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(@NotBlank @Size(max = 100) String companyName,
        @NotBlank @Size(max = 100) @Email String contactEmail) {
}
