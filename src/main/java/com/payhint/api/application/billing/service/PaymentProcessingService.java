package com.payhint.api.application.billing.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
import com.payhint.api.domain.billing.model.Invoice;
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
                                () -> new NotFoundException("Invoice with ID not found for user ID " + userId));
                Money amount = new Money(request.amount());
                LocalDate paymentDate = LocalDate.parse(request.paymentDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                invoice.addPayment(installmentId, paymentDate, amount);
                Invoice savedInvoice = invoiceRepository.save(invoice);
                logger.info("Payment recorded in installment: " + installmentId.toString() + " for user ID " + userId);
                return invoiceMapper.toInvoiceResponse(savedInvoice);
        }

        @Transactional()
        @Override
        public InvoiceResponse updatePayment(UserId userId, InvoiceId invoiceId, InstallmentId installmentId,
                        PaymentId paymentId, UpdatePaymentRequest request) {
                Invoice invoice = invoiceRepository.findByIdAndOwner(invoiceId, userId).orElseThrow(
                                () -> new NotFoundException("Invoice with ID not found for user ID " + userId));
                Money newAmount = request.amount() != null ? new Money(request.amount()) : null;
                LocalDate newPaymentDate = request.paymentDate() != null
                                ? LocalDate.parse(request.paymentDate(), DateTimeFormatter.ISO_LOCAL_DATE)
                                : null;
                invoice.updatePayment(installmentId, paymentId, newPaymentDate, newAmount);
                Invoice savedInvoice = invoiceRepository.save(invoice);
                logger.info("Payment updated in installment: " + installmentId.toString() + " for user ID " + userId);
                return invoiceMapper.toInvoiceResponse(savedInvoice);
        }

        @Transactional()
        @Override
        public InvoiceResponse removePayment(UserId userId, InvoiceId invoiceId, InstallmentId installmentId,
                        PaymentId paymentId) {
                Invoice invoice = invoiceRepository.findByIdAndOwner(invoiceId, userId)
                                .orElseThrow(() -> new NotFoundException("Invoice not found for user ID " + userId));
                invoice.removePayment(installmentId, paymentId);
                Invoice savedInvoice = invoiceRepository.save(invoice);
                logger.info("Payment removed from installment: " + installmentId.toString() + " for user ID " + userId);
                return invoiceMapper.toInvoiceResponse(savedInvoice);
        }
}