package com.payhint.api.application.crm.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Valid
public record UpdateCustomerRequest(@Size(max = 100) String companyName, @Size(max = 100) @Email String contactEmail) {

}
