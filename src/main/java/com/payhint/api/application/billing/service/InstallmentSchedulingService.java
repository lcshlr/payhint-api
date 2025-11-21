package com.payhint.api.application.billing.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhint.api.application.billing.dto.request.CreateInstallmentRequest;
import com.payhint.api.application.billing.dto.request.UpdateInstallmentRequest;
import com.payhint.api.application.billing.dto.response.InvoiceResponse;
import com.payhint.api.application.billing.mapper.InstallmentMapper;
import com.payhint.api.application.billing.mapper.InvoiceMapper;
import com.payhint.api.application.billing.usecase.InstallmentSchedulingUseCase;
import com.payhint.api.application.shared.exception.NotFoundException;
import com.payhint.api.domain.billing.model.Installment;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.crm.valueobject.UserId;

@Service
public class InstallmentSchedulingService implements InstallmentSchedulingUseCase {

        private static final Logger logger = LoggerFactory.getLogger(InstallmentSchedulingService.class);

        private final InvoiceRepository invoiceRepository;
        private final InvoiceMapper invoiceMapper;

        public InstallmentSchedulingService(InvoiceRepository invoiceRepository, InvoiceMapper invoiceMapper,
                        InstallmentMapper installmentMapper) {
                this.invoiceRepository = invoiceRepository;
                this.invoiceMapper = invoiceMapper;
        }

        @Transactional()
        @Override
        public InvoiceResponse addInstallment(UserId userId, InvoiceId invoiceId, CreateInstallmentRequest request) {
                Invoice invoice = invoiceRepository.findByIdAndOwner(invoiceId, userId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Invoice with ID " + invoiceId + " not found for user ID " + userId));

                Money amountDue = new Money(request.amountDue());
                LocalDate dueDate = LocalDate.parse(request.dueDate(), DateTimeFormatter.ISO_LOCAL_DATE);

                InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
                Installment installment = Installment.create(installmentId, invoice.getId(), amountDue, dueDate);
                invoice.addInstallment(installment);

                Invoice savedInvoice = invoiceRepository.save(invoice);
                logger.info("Installment added to invoice: " + invoiceId.toString() + " for user ID " + userId);
                return invoiceMapper.toInvoiceResponse(savedInvoice);
        }

        @Transactional()
        @Override
        public InvoiceResponse updateInstallment(UserId userId, InvoiceId invoiceId, InstallmentId installmentId,
                        UpdateInstallmentRequest request) {

                Invoice invoice = invoiceRepository.findByIdAndOwner(invoiceId, userId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Invoice with ID " + invoiceId + " not found for user ID " + userId));

                Installment existingInstallment = invoice.findInstallmentById(installmentId);

                LocalDate newDueDate = request.dueDate() != null
                                ? LocalDate.parse(request.dueDate(), DateTimeFormatter.ISO_LOCAL_DATE)
                                : existingInstallment.getDueDate();
                Money newAmountDue = request.amountDue() != null ? new Money(request.amountDue())
                                : existingInstallment.getAmountDue();

                Installment updatedInstallment = new Installment(existingInstallment.getId(), invoice.getId(),
                                newAmountDue, newDueDate, existingInstallment.getCreatedAt(),
                                existingInstallment.getUpdatedAt(), existingInstallment.getPayments());
                invoice.updateInstallment(updatedInstallment);

                Invoice savedInvoice = invoiceRepository.save(invoice);
                logger.info("Installment updated in invoice: " + existingInstallment.getInvoiceId().toString()
                                + " for user ID " + userId);
                return invoiceMapper.toInvoiceResponse(savedInvoice);
        }

        @Transactional()
        @Override
        public InvoiceResponse removeInstallment(UserId userId, InvoiceId invoiceId, InstallmentId installmentId) {
                Invoice invoice = invoiceRepository.findByIdAndOwner(invoiceId, userId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Invoice with ID " + invoiceId + " not found for user ID " + userId));

                Installment existingInstallment = invoice.findInstallmentById(installmentId);

                invoice.removeInstallment(existingInstallment);

                Invoice savedInvoice = invoiceRepository.save(invoice);
                logger.info("Installment removed from invoice: " + existingInstallment.getInvoiceId().toString()
                                + " for user ID " + userId);
                return invoiceMapper.toInvoiceResponse(savedInvoice);
        }

}