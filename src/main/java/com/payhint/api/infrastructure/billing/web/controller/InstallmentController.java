package com.payhint.api.infrastructure.billing.web.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.payhint.api.application.billing.dto.request.CreateInstallmentRequest;
import com.payhint.api.application.billing.dto.request.UpdateInstallmentRequest;
import com.payhint.api.application.billing.dto.response.InvoiceWithInstallmentsResponse;
import com.payhint.api.application.billing.usecase.InstallmentManagementUseCase;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.infrastructure.shared.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/invoices/{invoiceId}/installments")
public class InstallmentController {

    private final InstallmentManagementUseCase installmentManagementUseCase;

    public InstallmentController(InstallmentManagementUseCase installmentManagementUseCase) {
        this.installmentManagementUseCase = installmentManagementUseCase;
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceWithInstallmentsResponse addInstallment(@AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateInstallmentRequest request, @PathVariable String invoiceId) {
        InvoiceId invoiceUUID = new InvoiceId(UUID.fromString(invoiceId));
        UserId userId = new UserId(userPrincipal.getId());
        return installmentManagementUseCase.addInstallment(userId, invoiceUUID, request);
    }

    @PutMapping("{installmentId}")
    public InvoiceWithInstallmentsResponse updateInstallment(@AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String invoiceId, @PathVariable String installmentId,
            @Valid @RequestBody UpdateInstallmentRequest request) {
        UserId userId = new UserId(userPrincipal.getId());
        InvoiceId invoiceUUID = new InvoiceId(UUID.fromString(invoiceId));
        InstallmentId installmentUUID = new InstallmentId(UUID.fromString(installmentId));
        return installmentManagementUseCase.updateInstallment(userId, invoiceUUID, installmentUUID, request);
    }

    @DeleteMapping("{installmentId}")
    public InvoiceWithInstallmentsResponse deleteInstallment(@AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String invoiceId, @PathVariable String installmentId) {
        UserId userId = new UserId(userPrincipal.getId());
        InvoiceId invoiceUUID = new InvoiceId(UUID.fromString(invoiceId));
        InstallmentId installmentUUID = new InstallmentId(UUID.fromString(installmentId));
        return installmentManagementUseCase.removeInstallment(userId, invoiceUUID, installmentUUID);
    }
}