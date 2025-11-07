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

import com.payhint.api.application.billing.dto.request.CreatePaymentRequest;
import com.payhint.api.application.billing.dto.request.UpdatePaymentRequest;
import com.payhint.api.application.billing.dto.response.InvoiceWithInstallmentsAndPaymentsResponse;
import com.payhint.api.application.billing.usecase.PaymentManagementUseCase;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.PaymentId;
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.infrastructure.shared.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/invoices/{invoiceId}/installments/{installmentId}/payments")
public class PaymentController {

    private final PaymentManagementUseCase paymentManagementUseCase;

    public PaymentController(PaymentManagementUseCase paymentManagementUseCase) {
        this.paymentManagementUseCase = paymentManagementUseCase;
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceWithInstallmentsAndPaymentsResponse addPayment(@AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreatePaymentRequest request, @PathVariable String invoiceId,
            @PathVariable String installmentId) {
        InvoiceId invoiceUUID = new InvoiceId(UUID.fromString(invoiceId));
        InstallmentId installmentUUID = new InstallmentId(UUID.fromString(installmentId));
        UserId userId = new UserId(userPrincipal.getId());
        return paymentManagementUseCase.recordPayment(userId, invoiceUUID, installmentUUID, request);
    }

    @PutMapping("{paymentId}")
    public InvoiceWithInstallmentsAndPaymentsResponse updatePayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable String invoiceId,
            @PathVariable String installmentId, @PathVariable String paymentId,
            @Valid @RequestBody UpdatePaymentRequest request) {
        UserId userId = new UserId(userPrincipal.getId());
        InvoiceId invoiceUUID = new InvoiceId(UUID.fromString(invoiceId));
        InstallmentId installmentUUID = new InstallmentId(UUID.fromString(installmentId));
        PaymentId paymentUUID = new PaymentId(UUID.fromString(paymentId));
        return paymentManagementUseCase.updatePayment(userId, invoiceUUID, installmentUUID, paymentUUID, request);
    }

    @DeleteMapping("{paymentId}")
    public InvoiceWithInstallmentsAndPaymentsResponse deletePayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable String invoiceId,
            @PathVariable String installmentId, @PathVariable String paymentId) {
        UserId userId = new UserId(userPrincipal.getId());
        InvoiceId invoiceUUID = new InvoiceId(UUID.fromString(invoiceId));
        InstallmentId installmentUUID = new InstallmentId(UUID.fromString(installmentId));
        PaymentId paymentUUID = new PaymentId(UUID.fromString(paymentId));
        return paymentManagementUseCase.removePayment(userId, invoiceUUID, installmentUUID, paymentUUID);
    }
}