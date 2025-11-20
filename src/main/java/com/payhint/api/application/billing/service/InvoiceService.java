package com.payhint.api.application.billing.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhint.api.application.billing.dto.request.CreateInstallmentRequest;
import com.payhint.api.application.billing.dto.request.CreateInvoiceRequest;
import com.payhint.api.application.billing.dto.request.CreatePaymentRequest;
import com.payhint.api.application.billing.dto.request.UpdateInstallmentRequest;
import com.payhint.api.application.billing.dto.request.UpdateInvoiceRequest;
import com.payhint.api.application.billing.dto.request.UpdatePaymentRequest;
import com.payhint.api.application.billing.dto.response.InvoiceResponse;
import com.payhint.api.application.billing.dto.response.InvoiceWithInstallmentsAndPaymentsResponse;
import com.payhint.api.application.billing.dto.response.InvoiceWithInstallmentsResponse;
import com.payhint.api.application.billing.mapper.InstallmentMapper;
import com.payhint.api.application.billing.mapper.InvoiceMapper;
import com.payhint.api.application.billing.usecase.InstallmentManagementUseCase;
import com.payhint.api.application.billing.usecase.InvoiceManagementUseCase;
import com.payhint.api.application.billing.usecase.PaymentManagementUseCase;
import com.payhint.api.application.shared.exception.AlreadyExistsException;
import com.payhint.api.application.shared.exception.NotFoundException;
import com.payhint.api.application.shared.exception.PermissionDeniedException;
import com.payhint.api.domain.billing.model.Installment;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.model.Payment;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.billing.valueobject.PaymentId;
import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.domain.crm.repository.CustomerRepository;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.UserId;

@Service
public class InvoiceService
        implements InvoiceManagementUseCase, InstallmentManagementUseCase, PaymentManagementUseCase {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceMapper invoiceMapper;

    public InvoiceService(InvoiceRepository invoiceRepository, CustomerRepository customerRepository,
            InvoiceMapper invoiceMapper, InstallmentMapper installmentMapper) {
        this.invoiceRepository = invoiceRepository;
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

    private Invoice findInvoiceForUser(UserId userId, InvoiceId invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> {
            var errorMessage = "Invoice with ID " + invoiceId + " not found";
            logger.warn(errorMessage);
            return new NotFoundException(errorMessage);
        });
        validateCustomerBelongsToUser(userId, invoice.getCustomerId());
        return invoice;
    }

    private Invoice findInvoiceForUserWithInstallments(UserId userId, InvoiceId invoiceId) {
        Invoice invoice = invoiceRepository.findByIdWithInstallments(invoiceId).orElseThrow(() -> {
            var errorMessage = "Invoice with ID " + invoiceId + " not found";
            logger.warn(errorMessage);
            return new NotFoundException(errorMessage);
        });
        validateCustomerBelongsToUser(userId, invoice.getCustomerId());
        return invoice;
    }

    private Invoice findInvoiceForUserWithInstallmentsAndPayments(UserId userId, InvoiceId invoiceId) {
        Invoice invoice = invoiceRepository.findByIdWithInstallmentsAndPayments(invoiceId).orElseThrow(() -> {
            var errorMessage = "Invoice with ID " + invoiceId + " not found";
            logger.warn(errorMessage);
            return new NotFoundException(errorMessage);
        });
        validateCustomerBelongsToUser(userId, invoice.getCustomerId());
        return invoice;
    }

    @Transactional(readOnly = true)
    @Override
    public InvoiceResponse viewInvoiceSummary(UserId userId, InvoiceId invoiceId) {
        Invoice invoice = findInvoiceForUser(userId, invoiceId);
        return invoiceMapper.toInvoiceResponse(invoice);
    }

    @Transactional(readOnly = true)
    @Override
    public List<InvoiceResponse> listInvoicesByCustomer(UserId userId, CustomerId customerId) {
        validateCustomerBelongsToUser(userId, customerId);
        List<Invoice> invoices = invoiceRepository.findAllByCustomerId(customerId);
        return invoiceMapper.toInvoiceResponse(invoices);
    }

    @Transactional(readOnly = true)
    @Override
    public List<InvoiceWithInstallmentsAndPaymentsResponse> listAllInvoicesWithDetailsByCustomer(UserId userId,
            CustomerId customerId) {
        validateCustomerBelongsToUser(userId, customerId);
        List<Invoice> invoices = invoiceRepository.findAllWithInstallmentsAndPaymentsByCustomerId(customerId);
        return invoiceMapper.toInvoiceWithInstallmentsAndPaymentsResponse(invoices);
    }

    // InvoiceManagementUseCase implementation
    @Transactional()
    @Override
    public InvoiceResponse createInvoice(UserId userId, CreateInvoiceRequest request) {
        CustomerId customerId = new CustomerId(request.customerId());
        validateCustomerBelongsToUser(userId, customerId);

        InvoiceReference invoiceReference = new InvoiceReference(request.invoiceReference());

        ensureInvoiceReferenceUniqueForCustomer(customerId, invoiceReference);

        InvoiceId invoiceId = new InvoiceId(UUID.randomUUID());
        Invoice invoice = Invoice.create(invoiceId, customerId, invoiceReference, request.currency());

        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Invoice created successfully: " + invoiceReference.toString() + " for user ID " + userId);
        return invoiceMapper.toInvoiceResponse(savedInvoice);
    }

    @Transactional()
    @Override
    public InvoiceResponse updateInvoice(UserId userId, InvoiceId invoiceId, UpdateInvoiceRequest request) {
        Invoice invoice = findInvoiceForUserWithInstallments(userId, invoiceId);
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
        Invoice invoice = findInvoiceForUser(userId, invoiceId);
        invoiceRepository.deleteById(invoice.getId());
        logger.info("Invoice deleted successfully: " + invoiceId.toString() + " for user ID " + userId);
    }

    @Transactional()
    @Override
    public InvoiceResponse archiveInvoice(UserId userId, InvoiceId invoiceId) {
        Invoice invoice = findInvoiceForUser(userId, invoiceId);
        invoice.archive();
        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Invoice archived successfully: " + invoiceId.toString() + " for user ID " + userId);
        return invoiceMapper.toInvoiceResponse(savedInvoice);
    }

    @Transactional()
    @Override
    public InvoiceResponse unarchiveInvoice(UserId userId, InvoiceId invoiceId) {
        Invoice invoice = findInvoiceForUser(userId, invoiceId);
        invoice.unArchive();
        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Invoice unarchived successfully: " + invoiceId.toString() + " for user ID " + userId);
        return invoiceMapper.toInvoiceResponse(savedInvoice);
    }

    // InstallmentManagementUseCase implementation
    @Transactional()
    @Override
    public InvoiceWithInstallmentsResponse addInstallment(UserId userId, InvoiceId invoiceId,
            CreateInstallmentRequest request) {
        Invoice invoice = findInvoiceForUserWithInstallments(userId, invoiceId);

        Money amountDue = new Money(request.amountDue());
        LocalDate dueDate = LocalDate.parse(request.dueDate(), DateTimeFormatter.ISO_LOCAL_DATE);

        InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
        Installment installment = Installment.create(installmentId, invoice.getId(), amountDue, dueDate);
        invoice.addInstallment(installment);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Installment added to invoice: " + invoiceId.toString() + " for user ID " + userId);
        return invoiceMapper.toInvoiceWithInstallmentsResponse(savedInvoice);
    }

    @Transactional()
    @Override
    public InvoiceWithInstallmentsResponse updateInstallment(UserId userId, InvoiceId invoiceId,
            InstallmentId installmentId, UpdateInstallmentRequest request) {

        Invoice invoice = findInvoiceForUserWithInstallments(userId, invoiceId);

        Installment existingInstallment = invoice.findInstallmentById(installmentId);

        LocalDate newDueDate = request.dueDate() != null
                ? LocalDate.parse(request.dueDate(), DateTimeFormatter.ISO_LOCAL_DATE)
                : existingInstallment.getDueDate();
        Money newAmountDue = request.amountDue() != null ? new Money(request.amountDue())
                : existingInstallment.getAmountDue();

        Installment updatedInstallment = new Installment(existingInstallment.getId(), invoice.getId(), newAmountDue,
                newDueDate, existingInstallment.getCreatedAt(), existingInstallment.getUpdatedAt());
        invoice.updateInstallment(updatedInstallment);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Installment updated in invoice: " + existingInstallment.getInvoiceId().toString() + " for user ID "
                + userId);
        return invoiceMapper.toInvoiceWithInstallmentsResponse(savedInvoice);
    }

    @Transactional()
    @Override
    public InvoiceWithInstallmentsResponse removeInstallment(UserId userId, InvoiceId invoiceId,
            InstallmentId installmentId) {
        Invoice invoice = findInvoiceForUserWithInstallments(userId, invoiceId);

        Installment existingInstallment = invoice.findInstallmentById(installmentId);

        invoice.removeInstallment(existingInstallment);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Installment removed from invoice: " + existingInstallment.getInvoiceId().toString()
                + " for user ID " + userId);
        return invoiceMapper.toInvoiceWithInstallmentsResponse(savedInvoice);
    }

    @Transactional()
    @Override
    public InvoiceWithInstallmentsAndPaymentsResponse recordPayment(UserId userId, InvoiceId invoiceId,
            InstallmentId installmentId, CreatePaymentRequest request) {
        Invoice invoice = findInvoiceForUserWithInstallmentsAndPayments(userId, invoiceId);
        Installment installment = invoice.findInstallmentById(installmentId);
        Money amount = new Money(request.amount());
        LocalDate paymentDate = LocalDate.parse(request.paymentDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        Payment payment = Payment.create(paymentId, installmentId, amount, paymentDate);
        invoice.addPayment(installment, payment);
        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Payment recorded in installment: " + installmentId.toString() + " for user ID " + userId);
        return invoiceMapper.toInvoiceWithInstallmentsAndPaymentsResponse(savedInvoice);
    }

    @Transactional()
    @Override
    public InvoiceWithInstallmentsAndPaymentsResponse updatePayment(UserId userId, InvoiceId invoiceId,
            InstallmentId installmentId, PaymentId paymentId, UpdatePaymentRequest request) {
        Invoice invoice = findInvoiceForUserWithInstallmentsAndPayments(userId, invoiceId);
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
        return invoiceMapper.toInvoiceWithInstallmentsAndPaymentsResponse(savedInvoice);
    }

    @Transactional()
    @Override
    public InvoiceWithInstallmentsAndPaymentsResponse removePayment(UserId userId, InvoiceId invoiceId,
            InstallmentId installmentId, PaymentId paymentId) {
        Invoice invoice = findInvoiceForUserWithInstallmentsAndPayments(userId, invoiceId);
        Installment installment = invoice.findInstallmentById(installmentId);
        Payment existingPayment = installment.findPaymentById(paymentId);
        invoice.removePayment(installment, existingPayment);
        Invoice savedInvoice = invoiceRepository.save(invoice);
        logger.info("Payment removed from installment: " + installmentId.toString() + " for user ID " + userId);
        return invoiceMapper.toInvoiceWithInstallmentsAndPaymentsResponse(savedInvoice);
    }
}
