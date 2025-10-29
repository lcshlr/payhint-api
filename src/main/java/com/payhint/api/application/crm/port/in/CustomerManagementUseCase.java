package com.payhint.api.application.crm.port.in;

import java.util.List;

import com.payhint.api.application.crm.dto.request.CreateCustomerRequest;
import com.payhint.api.application.crm.dto.request.UpdateCustomerRequest;
import com.payhint.api.application.crm.dto.response.CustomerResponse;
import com.payhint.api.domain.crm.valueobjects.CustomerId;
import com.payhint.api.domain.crm.valueobjects.UserId;

import jakarta.validation.Valid;

public interface CustomerManagementUseCase {
    CustomerResponse viewCustomerProfile(UserId userId, CustomerId customerId);

    List<CustomerResponse> listAllCustomers(UserId userId);

    CustomerResponse createCustomer(UserId userId, @Valid CreateCustomerRequest request);

    CustomerResponse updateCustomerDetails(UserId userId, CustomerId customerId, @Valid UpdateCustomerRequest request);

    void deleteCustomer(UserId userId, CustomerId customerId);
}
