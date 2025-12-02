package com.payhint.api.infrastructure.billing.web.controller;

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

import com.payhint.api.application.billing.dto.request.CreateInvoiceRequest;
import com.payhint.api.application.billing.dto.request.UpdateInvoiceRequest;
import com.payhint.api.application.billing.dto.response.InvoiceResponse;
import com.payhint.api.application.billing.dto.response.InvoiceSummaryResponse;
import com.payhint.api.application.billing.usecase.InvoiceLifecycleUseCase;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.infrastructure.shared.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceLifecycleUseCase invoiceManagementUseCase;

    public InvoiceController(InvoiceLifecycleUseCase invoiceManagementUseCase) {
        this.invoiceManagementUseCase = invoiceManagementUseCase;
    }

    @GetMapping()
    public List<InvoiceSummaryResponse> getAll(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserId userId = new UserId(userPrincipal.getId());
        return invoiceManagementUseCase.listInvoicesByUser(userId);
    }

    @GetMapping("/{id}")
    public InvoiceResponse getById(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable String id) {
        UserId userId = new UserId(userPrincipal.getId());
        InvoiceId invoiceId = new InvoiceId(UUID.fromString(id));
        return invoiceManagementUseCase.viewInvoice(userId, invoiceId);
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateInvoiceRequest request) {
        UserId userId = new UserId(userPrincipal.getId());
        return invoiceManagementUseCase.createInvoice(userId, request);
    }

    @PutMapping("/{id}")
    public InvoiceResponse update(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable String id,
            @Valid @RequestBody UpdateInvoiceRequest request) {
        UserId userId = new UserId(userPrincipal.getId());
        InvoiceId invoiceId = new InvoiceId(UUID.fromString(id));
        return invoiceManagementUseCase.updateInvoice(userId, invoiceId, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable String id) {
        UserId userId = new UserId(userPrincipal.getId());
        InvoiceId invoiceId = new InvoiceId(UUID.fromString(id));
        invoiceManagementUseCase.deleteInvoice(userId, invoiceId);
        return ResponseEntity.noContent().build();
    }
}