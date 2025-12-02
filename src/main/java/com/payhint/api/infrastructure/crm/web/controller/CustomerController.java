package com.payhint.api.infrastructure.crm.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.payhint.api.application.billing.dto.response.InvoiceSummaryResponse;
import com.payhint.api.application.billing.usecase.InvoiceLifecycleUseCase;
import com.payhint.api.application.crm.dto.request.CreateCustomerRequest;
import com.payhint.api.application.crm.dto.request.UpdateCustomerRequest;
import com.payhint.api.application.crm.dto.response.CustomerResponse;
import com.payhint.api.application.crm.usecase.CustomerManagementUseCase;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.infrastructure.shared.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerManagementUseCase customerManagementUseCase;
    private final InvoiceLifecycleUseCase invoiceLifecycleUseCase;

    public CustomerController(CustomerManagementUseCase customerManagementUseCase,
            InvoiceLifecycleUseCase invoiceLifecycleUseCase) {
        this.customerManagementUseCase = customerManagementUseCase;
        this.invoiceLifecycleUseCase = invoiceLifecycleUseCase;
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable String id) {
        UserId userId = new UserId(userPrincipal.getId());
        CustomerId customerId = new CustomerId(UUID.fromString(id));
        return customerManagementUseCase.viewCustomerProfile(userId, customerId);
    }

    @GetMapping()
    public List<CustomerResponse> getAll(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserId userId = new UserId(userPrincipal.getId());
        return customerManagementUseCase.listAllCustomers(userId);
    }

    @GetMapping("/{id}/invoices")
    public List<InvoiceSummaryResponse> getInvoicesByCustomerId(@AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String id) {
        UserId userId = new UserId(userPrincipal.getId());
        CustomerId customerId = new CustomerId(UUID.fromString(id));
        return invoiceLifecycleUseCase.listInvoicesByCustomer(userId, customerId);
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateCustomerRequest request) {
        UserId userId = new UserId(userPrincipal.getId());
        return customerManagementUseCase.createCustomer(userId, request);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable String id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        UserId userId = new UserId(userPrincipal.getId());
        CustomerId customerId = new CustomerId(UUID.fromString(id));
        return customerManagementUseCase.updateCustomerDetails(userId, customerId, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable String id) {
        UserId userId = new UserId(userPrincipal.getId());
        CustomerId customerId = new CustomerId(UUID.fromString(id));
        customerManagementUseCase.deleteCustomer(userId, customerId);
        return ResponseEntity.noContent().build();
    }
}