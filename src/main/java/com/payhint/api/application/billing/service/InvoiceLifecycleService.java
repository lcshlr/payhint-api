package com.payhint.api.application.billing.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhint.api.application.billing.dto.request.CreateInvoiceRequest;
import com.payhint.api.application.billing.dto.request.UpdateInvoiceRequest;
import com.payhint.api.application.billing.dto.response.InvoiceResponse;
import com.payhint.api.application.billing.dto.response.InvoiceSummaryResponse;
import com.payhint.api.application.billing.mapper.InstallmentMapper;
import com.payhint.api.application.billing.mapper.InvoiceMapper;
import com.payhint.api.application.billing.repository.InvoiceQueryRepository;
import com.payhint.api.application.billing.usecase.InvoiceLifecycleUseCase;
import com.payhint.api.application.shared.exception.AlreadyExistsException;
import com.payhint.api.application.shared.exception.NotFoundException;
import com.payhint.api.application.shared.exception.PermissionDeniedException;
import com.payhint.api.domain.billing.model.Installment;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.domain.crm.repository.CustomerRepository;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.UserId;

@Service
public class InvoiceLifecycleService implements InvoiceLifecycleUseCase {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceLifecycleService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceQueryRepository invoiceQueryRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceMapper invoiceMapper;

    public InvoiceLifecycleService(InvoiceRepository invoiceRepository, InvoiceQueryRepository invoiceQueryRepository,
            CustomerRepository customerRepository, InvoiceMapper invoiceMapper, InstallmentMapper installmentMapper) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceQueryRepository = invoiceQueryRepository;
        this.customerRepository = customerRepository;
        this.invoiceMapper = invoiceMapper;
    }

    private Customer validateCustomerBelongsToUser(UserId userId, CustomerId customerId) {
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> {
            var errorMessage = "Customer with ID " + customerId + " not found";
            logger.warn(errorMessage);
            return new NotFoundException(errorMessage);
        });

        if (!customer.belongsToUser(userId)) {
            var errorMessage = "User with ID " + userId + " does not have permission to access customer with ID "
                    + customerId;
            logger.warn(errorMessage);
            throw new PermissionDeniedException(errorMessage);
        }

        return customer;
    }

    private void ensureInvoiceReferenceUniqueForCustomer(CustomerId customerId, InvoiceReference invoiceReference) {
        if (invoiceRepository.findByCustomerIdAndInvoiceReference(customerId, invoiceReference).isPresent()) {
            var errorMessage = "Invoice with reference " + invoiceReference.toString()
                    + " already exists for this customer";
            logger.warn(errorMessage);
            throw new AlreadyExistsException(errorMessage);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public InvoiceResponse viewInvoice(UserId userId, InvoiceId invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndOwner(invoiceId, userId).orElseThrow(
                () -> new NotFoundException("Invoice with ID " + invoiceId + " not found for user ID " + userId));
        return invoiceMapper.toInvoiceResponse(invoice);
    }

    @Transactional(readOnly = true)
    @Override
    public List<InvoiceSummaryResponse> listInvoicesByUser(UserId userId) {
        return invoiceQueryRepository.findSummariesByUserId(userId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<InvoiceSummaryResponse> listInvoicesByCustomer(UserId userId, CustomerId customerId) {
        validateCustomerBelongsToUser(userId, customerId);
        return invoiceQueryRepository.findSummariesByCustomerId(customerId);
    }

    @Transactional()
    @Override
    public InvoiceResponse createInvoice(UserId userId, CreateInvoiceRequest request) {
        CustomerId customerId = new CustomerId(request.customerId());
        validateCustomerBelongsToUser(userId, customerId);

        InvoiceReference invoiceReference = new InvoiceReference(request.invoiceReference());

        ensureInvoiceReferenceUniqueForCustomer(customerId, invoiceReference);

        InvoiceId invoiceId = new InvoiceId(UUID.randomUUID());
        Invoice invoice = Invoice.create(invoiceId, customerId, invoiceReference, request.currency());

        if (request.installments() != null && !request.installments().isEmpty()) {

            List<Installment> installments = request.installments().stream().map(instReq -> {
                Money amount = new Money(instReq.amountDue());
                LocalDate dueDate = LocalDate.parse(instReq.dueDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                return Installment.create(new InstallmentId(UUID.randomUUID()), amount, dueDate);
            }).toList();

            invoice.addInstallments(installments);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Invoice created successfully: " + invoiceReference.toString() + " for user ID " + userId);
        return invoiceMapper.toInvoiceResponse(savedInvoice);
    }

    @Transactional()
    @Override
    public InvoiceResponse updateInvoice(UserId userId, InvoiceId invoiceId, UpdateInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findByIdAndOwner(invoiceId, userId).orElseThrow(
                () -> new NotFoundException("Invoice with ID " + invoiceId + " not found for user ID " + userId));
        InvoiceReference newReference = request.invoiceReference() != null
                ? new InvoiceReference(request.invoiceReference())
                : null;
        if (newReference != null) {
            ensureInvoiceReferenceUniqueForCustomer(invoice.getCustomerId(), newReference);
        }

        invoice.updateDetails(newReference, request.currency());

        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Invoice updated successfully: " + invoiceId.toString() + " for user ID " + userId);
        return invoiceMapper.toInvoiceResponse(savedInvoice);
    }

    @Transactional()
    @Override
    public void deleteInvoice(UserId userId, InvoiceId invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndOwner(invoiceId, userId).orElseThrow(
                () -> new NotFoundException("Invoice with ID " + invoiceId + " not found for user ID " + userId));
        invoiceRepository.deleteById(invoice.getId());
        logger.info("Invoice deleted successfully: " + invoiceId.toString() + " for user ID " + userId);
    }

    @Transactional()
    @Override
    public InvoiceResponse archiveInvoice(UserId userId, InvoiceId invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndOwner(invoiceId, userId).orElseThrow(
                () -> new NotFoundException("Invoice with ID " + invoiceId + " not found for user ID " + userId));
        invoice.archive();
        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Invoice archived successfully: " + invoiceId.toString() + " for user ID " + userId);
        return invoiceMapper.toInvoiceResponse(savedInvoice);
    }

    @Transactional()
    @Override
    public InvoiceResponse unarchiveInvoice(UserId userId, InvoiceId invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndOwner(invoiceId, userId).orElseThrow(
                () -> new NotFoundException("Invoice with ID " + invoiceId + " not found for user ID " + userId));
        invoice.unArchive();
        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Invoice unarchived successfully: " + invoiceId.toString() + " for user ID " + userId);
        return invoiceMapper.toInvoiceResponse(savedInvoice);
    }
}