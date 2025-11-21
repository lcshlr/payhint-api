package com.payhint.api.application.billing.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhint.api.application.billing.dto.request.CreatePaymentRequest;
import com.payhint.api.application.billing.dto.request.UpdatePaymentRequest;
import com.payhint.api.application.billing.dto.response.InvoiceResponse;
import com.payhint.api.application.billing.mapper.InvoiceMapper;
import com.payhint.api.application.billing.usecase.PaymentProcessingUseCase;
import com.payhint.api.application.shared.exception.NotFoundException;
import com.payhint.api.domain.billing.model.Installment;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.model.Payment;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.billing.valueobject.PaymentId;
import com.payhint.api.domain.crm.valueobject.UserId;

@Service
public class PaymentProcessingService implements PaymentProcessingUseCase {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessingService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;

    public PaymentProcessingService(InvoiceRepository invoiceRepository, InvoiceMapper invoiceMapper) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
    }

    @Transactional()
    @Override
    public InvoiceResponse recordPayment(UserId userId, InvoiceId invoiceId, InstallmentId installmentId,
            CreatePaymentRequest request) {
        Invoice invoice = invoiceRepository.findByIdAndOwner(invoiceId, userId).orElseThrow(
                () -> new NotFoundException("Invoice with ID " + invoiceId + " not found for user ID " + userId));
        Installment installment = invoice.findInstallmentById(installmentId);
        Money amount = new Money(request.amount());
        LocalDate paymentDate = LocalDate.parse(request.paymentDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        Payment payment = Payment.create(paymentId, installmentId, amount, paymentDate);
        invoice.addPayment(installment, payment);
        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Payment recorded in installment: " + installmentId.toString() + " for user ID " + userId);
        return invoiceMapper.toInvoiceResponse(savedInvoice);
    }

    @Transactional()
    @Override
    public InvoiceResponse updatePayment(UserId userId, InvoiceId invoiceId, InstallmentId installmentId,
            PaymentId paymentId, UpdatePaymentRequest request) {
        Invoice invoice = invoiceRepository.findByIdAndOwner(invoiceId, userId).orElseThrow(
                () -> new NotFoundException("Invoice with ID " + invoiceId + " not found for user ID " + userId));
        Installment installment = invoice.findInstallmentById(installmentId);
        Payment existingPayment = installment.findPaymentById(paymentId);
        Money newAmount = request.amount() != null ? new Money(request.amount()) : existingPayment.getAmount();
        LocalDate newPaymentDate = request.paymentDate() != null
                ? LocalDate.parse(request.paymentDate(), DateTimeFormatter.ISO_LOCAL_DATE)
                : existingPayment.getPaymentDate();
        Payment updatedPayment = new Payment(existingPayment.getId(), installmentId, newAmount, newPaymentDate,
                existingPayment.getCreatedAt(), existingPayment.getUpdatedAt());
        invoice.updatePayment(installment, updatedPayment);
        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Payment updated in installment: " + installmentId.toString() + " for user ID " + userId);
        return invoiceMapper.toInvoiceResponse(savedInvoice);
    }

    @Transactional()
    @Override
    public InvoiceResponse removePayment(UserId userId, InvoiceId invoiceId, InstallmentId installmentId,
            PaymentId paymentId) {
        Invoice invoice = invoiceRepository.findByIdAndOwner(invoiceId, userId).orElseThrow(
                () -> new NotFoundException("Invoice with ID " + invoiceId + " not found for user ID " + userId));
        Installment installment = invoice.findInstallmentById(installmentId);
        Payment existingPayment = installment.findPaymentById(paymentId);
        invoice.removePayment(installment, existingPayment);
        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Payment removed from installment: " + installmentId.toString() + " for user ID " + userId);
        return invoiceMapper.toInvoiceResponse(savedInvoice);
    }
}