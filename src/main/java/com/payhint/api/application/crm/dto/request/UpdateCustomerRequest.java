package com.payhint.api.application.crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(@Size(min = 1, max = 100) String companyName,
        @Size(max = 100) @Email String contactEmail) {

}
