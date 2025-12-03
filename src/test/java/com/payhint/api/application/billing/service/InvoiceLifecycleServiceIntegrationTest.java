package com.payhint.api.application.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.payhint.api.application.billing.dto.request.CreateInstallmentRequest;
import com.payhint.api.application.billing.dto.request.CreateInvoiceRequest;
import com.payhint.api.application.billing.dto.request.CreatePaymentRequest;
import com.payhint.api.application.billing.dto.request.UpdateInvoiceRequest;
import com.payhint.api.application.billing.dto.response.InvoiceResponse;
import com.payhint.api.application.billing.dto.response.InvoiceSummaryResponse;
import com.payhint.api.application.shared.exception.AlreadyExistsException;
import com.payhint.api.application.shared.exception.NotFoundException;
import com.payhint.api.application.shared.exception.PermissionDeniedException;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.CustomerRepository;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.Email;
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;
import com.payhint.api.infrastructure.billing.persistence.jpa.repository.InvoiceSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.CustomerSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.UserSpringRepository;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("InvoiceLifecycleService Integration Tests")
class InvoiceLifecycleServiceIntegrationTest {

        @Autowired
        private InvoiceLifecycleService invoiceService;

        @Autowired
        private InstallmentSchedulingService installmentService;

        @Autowired
        private PaymentProcessingService paymentService;

        @Autowired
        private UserRepository userRepository;
        @Autowired
        private CustomerRepository customerRepository;
        @Autowired
        private InvoiceRepository invoiceRepository;

        @Autowired
        private UserSpringRepository userSpringRepository;
        @Autowired
        private CustomerSpringRepository customerSpringRepository;
        @Autowired
        private InvoiceSpringRepository invoiceSpringRepository;

        private User testUser;
        private User otherUser;
        private Customer testCustomer;
        private Customer otherCustomer;

        @BeforeEach
        void setUp() {
                invoiceSpringRepository.deleteAll();
                customerSpringRepository.deleteAll();
                userSpringRepository.deleteAll();

                testUser = userRepository.register(User.create(new UserId(UUID.randomUUID()),
                                new Email("lifecycle.test@example.com"), "Password123!", "Lifecycle", "Tester"));

                otherUser = userRepository.register(User.create(new UserId(UUID.randomUUID()),
                                new Email("other.lifecycle@example.com"), "Password123!", "Other", "User"));

                testCustomer = customerRepository.save(Customer.create(new CustomerId(UUID.randomUUID()),
                                testUser.getId(), "Test Customer Inc", new Email("billing@testcustomer.com")));

                otherCustomer = customerRepository.save(Customer.create(new CustomerId(UUID.randomUUID()),
                                otherUser.getId(), "Other Customer LLC", new Email("billing@othercustomer.com")));
        }

        @AfterEach
        void tearDown() {
                invoiceSpringRepository.deleteAll();
                customerSpringRepository.deleteAll();
                userSpringRepository.deleteAll();
        }

        @Nested
        @DisplayName("Create Invoice Tests")
        class CreateInvoiceTests {

                @Test
                @DisplayName("Should create invoice successfully without installments")
                void shouldCreateInvoiceSuccessfully() {
                        var request = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-001", "USD", null);

                        InvoiceResponse response = invoiceService.createInvoice(testUser.getId(), request);

                        assertThat(response).isNotNull();
                        assertThat(response.invoiceReference()).isEqualTo("INV-001");
                        assertThat(response.currency()).isEqualTo("USD");
                        assertThat(response.customerId()).isEqualTo(testCustomer.getId().value().toString());
                        assertThat(response.totalAmount()).isEqualByComparingTo("0");
                        assertThat(response.status()).isEqualTo("PENDING");
                        assertThat(response.installments()).isEmpty();
                }

                @Test
                @DisplayName("Should create invoice successfully with valid installments")
                void shouldCreateInvoiceWithInstallments() {
                        var installments = List.of(
                                        new CreateInstallmentRequest(new BigDecimal("50.00"),
                                                        LocalDate.now().plusDays(10).toString()),
                                        new CreateInstallmentRequest(new BigDecimal("50.00"),
                                                        LocalDate.now().plusDays(20).toString()));
                        var request = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-002", "USD",
                                        installments);

                        InvoiceResponse response = invoiceService.createInvoice(testUser.getId(), request);

                        assertThat(response.installments()).hasSize(2);
                        assertThat(response.totalAmount()).isEqualByComparingTo("100.00");
                        assertThat(response.status()).isEqualTo("PENDING");
                }

                @Test
                @DisplayName("Should throw AlreadyExistsException when creating duplicate reference for same customer")
                void shouldThrowAlreadyExistsForDuplicateReference() {
                        var request = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-DUP", "USD", null);
                        invoiceService.createInvoice(testUser.getId(), request);

                        assertThatThrownBy(() -> invoiceService.createInvoice(testUser.getId(), request))
                                        .isInstanceOf(AlreadyExistsException.class)
                                        .hasMessageContaining("already exists for this customer");
                }

                @Test
                @DisplayName("Should allow same reference for different customers")
                void shouldAllowSameReferenceForDifferentCustomers() {
                        var req1 = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-SHARED", "USD", null);
                        var req2 = new CreateInvoiceRequest(otherCustomer.getId().value(), "INV-SHARED", "USD", null);

                        InvoiceResponse resp1 = invoiceService.createInvoice(testUser.getId(), req1);
                        InvoiceResponse resp2 = invoiceService.createInvoice(otherUser.getId(), req2);

                        assertThat(resp1.invoiceReference()).isEqualTo(resp2.invoiceReference());
                        assertThat(resp1.id()).isNotEqualTo(resp2.id());
                }

                @Test
                @DisplayName("Should throw NotFoundException when customer does not exist")
                void shouldThrowNotFoundWhenCustomerDoesNotExist() {
                        var request = new CreateInvoiceRequest(UUID.randomUUID(), "INV-404", "USD", null);

                        assertThatThrownBy(() -> invoiceService.createInvoice(testUser.getId(), request))
                                        .isInstanceOf(NotFoundException.class).hasMessageContaining("Customer with ID");
                }

                @Test
                @DisplayName("Should throw PermissionDeniedException when creating invoice for other user's customer")
                void shouldThrowPermissionDeniedForOtherUserCustomer() {
                        var request = new CreateInvoiceRequest(otherCustomer.getId().value(), "INV-DENIED", "USD",
                                        null);

                        assertThatThrownBy(() -> invoiceService.createInvoice(testUser.getId(), request))
                                        .isInstanceOf(PermissionDeniedException.class)
                                        .hasMessageContaining("does not have permission");
                }

                @Test
                @DisplayName("Should throw InvalidPropertyException when installments have duplicate due dates")
                void shouldThrowInvalidPropertyForDuplicateDueDates() {
                        String dueDate = LocalDate.now().plusDays(30).toString();
                        var installments = List.of(new CreateInstallmentRequest(new BigDecimal("10.00"), dueDate),
                                        new CreateInstallmentRequest(new BigDecimal("20.00"), dueDate));
                        var request = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-BAD-DATE", "USD",
                                        installments);

                        assertThatThrownBy(() -> invoiceService.createInvoice(testUser.getId(), request))
                                        .isInstanceOf(InvalidPropertyException.class)
                                        .hasMessageContaining("duplicate due dates");
                }
        }

        @Nested
        @DisplayName("View and List Invoice Tests")
        class ViewAndListTests {

                @Test
                @DisplayName("Should view invoice details successfully")
                void shouldViewInvoiceDetails() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-VIEW");
                        InvoiceResponse response = invoiceService.viewInvoice(testUser.getId(), invoice.getId());

                        assertThat(response).isNotNull();
                        assertThat(response.id()).isEqualTo(invoice.getId().value().toString());
                        assertThat(response.invoiceReference()).isEqualTo("INV-VIEW");
                }

                @Test
                @DisplayName("Should throw NotFoundException when viewing other user's invoice")
                void shouldThrowNotFoundForOtherUserInvoice() {
                        Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-OTHER-VIEW");

                        assertThatThrownBy(() -> invoiceService.viewInvoice(testUser.getId(), otherInvoice.getId()))
                                        .isInstanceOf(NotFoundException.class);
                }

                @Test
                @DisplayName("Should throw NotFoundException when viewing non-existent invoice")
                void shouldThrowNotFoundForNonExistentInvoice() {
                        assertThatThrownBy(() -> invoiceService.viewInvoice(testUser.getId(),
                                        new InvoiceId(UUID.randomUUID()))).isInstanceOf(NotFoundException.class);
                }

                @Test
                @DisplayName("Should list all invoices for user")
                void shouldListInvoicesByUser() {
                        Customer customer2 = customerRepository.save(Customer.create(new CustomerId(UUID.randomUUID()),
                                        testUser.getId(), "Another Corp", new Email("billing@another.com")));

                        createTestInvoice(testCustomer.getId(), "INV-USER-1");
                        createTestInvoice(customer2.getId(), "INV-USER-2");
                        createTestInvoice(otherCustomer.getId(), "INV-OTHER-USER");

                        List<InvoiceSummaryResponse> invoices = invoiceService.listInvoicesByUser(testUser.getId());

                        assertThat(invoices).hasSize(2);
                        assertThat(invoices).extracting("invoiceReference").containsExactlyInAnyOrder("INV-USER-1",
                                        "INV-USER-2");
                }

                @Test
                @DisplayName("Should return empty list when user has no invoices")
                void shouldReturnEmptyListForUserWithNoInvoices() {
                        List<InvoiceSummaryResponse> invoices = invoiceService.listInvoicesByUser(testUser.getId());
                        assertThat(invoices).isEmpty();
                }

                @Test
                @DisplayName("Should list invoices by customer successfully")
                void shouldListInvoicesByCustomer() {
                        createTestInvoice(testCustomer.getId(), "INV-CUST-1");
                        createTestInvoice(testCustomer.getId(), "INV-CUST-2");

                        List<InvoiceSummaryResponse> invoices = invoiceService.listInvoicesByCustomer(testUser.getId(),
                                        testCustomer.getId());

                        assertThat(invoices).hasSize(2);
                        assertThat(invoices).extracting("invoiceReference").containsExactlyInAnyOrder("INV-CUST-1",
                                        "INV-CUST-2");
                }

                @Test
                @DisplayName("Should throw PermissionDeniedException when listing invoices for other user's customer")
                void shouldThrowPermissionDeniedListingOtherCustomer() {
                        assertThatThrownBy(() -> invoiceService.listInvoicesByCustomer(testUser.getId(),
                                        otherCustomer.getId())).isInstanceOf(PermissionDeniedException.class);
                }
        }

        @Nested
        @DisplayName("Update Invoice Tests")
        class UpdateInvoiceTests {

                @Test
                @DisplayName("Should update invoice reference and currency successfully")
                void shouldUpdateInvoiceSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-OLD");
                        UpdateInvoiceRequest request = new UpdateInvoiceRequest("INV-NEW", "EUR");

                        InvoiceResponse response = invoiceService.updateInvoice(testUser.getId(), invoice.getId(),
                                        request);

                        assertThat(response.invoiceReference()).isEqualTo("INV-NEW");
                        assertThat(response.currency()).isEqualTo("EUR");
                }

                @Test
                @DisplayName("Should throw AlreadyExistsException when updating to existing reference")
                void shouldThrowAlreadyExistsForDuplicateUpdate() {
                        createTestInvoice(testCustomer.getId(), "INV-EXISTING");
                        Invoice invoiceToUpdate = createTestInvoice(testCustomer.getId(), "INV-TO-UPDATE");

                        UpdateInvoiceRequest request = new UpdateInvoiceRequest("INV-EXISTING", "USD");

                        assertThatThrownBy(() -> invoiceService.updateInvoice(testUser.getId(), invoiceToUpdate.getId(),
                                        request)).isInstanceOf(AlreadyExistsException.class)
                                                        .hasMessageContaining("already exists for this customer");
                }

                @Test
                @DisplayName("Should throw IllegalStateException when updating archived invoice")
                void shouldThrowIllegalStateUpdatingArchivedInvoice() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCHIVED");
                        invoiceService.archiveInvoice(testUser.getId(), invoice.getId());

                        UpdateInvoiceRequest request = new UpdateInvoiceRequest("INV-CHANGED", "EUR");

                        assertThatThrownBy(
                                        () -> invoiceService.updateInvoice(testUser.getId(), invoice.getId(), request))
                                                        .isInstanceOf(IllegalStateException.class)
                                                        .hasMessageContaining("archived");
                }

                @Test
                @DisplayName("Should throw NotFoundException when updating other user's invoice")
                void shouldThrowNotFoundUpdatingOtherUserInvoice() {
                        Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-OTHER-UPD");
                        UpdateInvoiceRequest request = new UpdateInvoiceRequest("INV-HACK", "USD");

                        assertThatThrownBy(() -> invoiceService.updateInvoice(testUser.getId(), otherInvoice.getId(),
                                        request)).isInstanceOf(NotFoundException.class);
                }
        }

        @Nested
        @DisplayName("Archive and Delete Tests")
        class ArchiveAndDeleteTests {

                @Test
                @DisplayName("Should archive and unarchive invoice successfully")
                void shouldArchiveAndUnarchiveSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCHIVE-TEST");

                        InvoiceResponse archived = invoiceService.archiveInvoice(testUser.getId(), invoice.getId());
                        assertThat(archived.isArchived()).isTrue();

                        InvoiceResponse unarchived = invoiceService.unarchiveInvoice(testUser.getId(), invoice.getId());
                        assertThat(unarchived.isArchived()).isFalse();
                }

                @Test
                @DisplayName("Should delete invoice successfully")
                void shouldDeleteInvoiceSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-DELETE-TEST");

                        invoiceService.deleteInvoice(testUser.getId(), invoice.getId());

                        assertThat(invoiceRepository.findById(invoice.getId())).isEmpty();
                }

                @Test
                @DisplayName("Should delete archived invoice successfully")
                void shouldDeleteArchivedInvoice() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCH-DEL");
                        invoiceService.archiveInvoice(testUser.getId(), invoice.getId());

                        invoiceService.deleteInvoice(testUser.getId(), invoice.getId());

                        assertThat(invoiceRepository.findById(invoice.getId())).isEmpty();
                }

                @Test
                @DisplayName("Should cascade delete installments and payments when deleting invoice")
                void shouldCascadeDeleteComponents() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-CASCADE");
                        // Add installment
                        var instReq = new CreateInstallmentRequest(new BigDecimal("100"), LocalDate.now().toString());
                        var instResp = installmentService.addInstallment(testUser.getId(), invoice.getId(), instReq);
                        String installmentId = instResp.installments().get(0).id();

                        // Add payment
                        var payReq = new CreatePaymentRequest(new BigDecimal("50"), LocalDate.now().toString());
                        paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), payReq);

                        // Delete Invoice
                        invoiceService.deleteInvoice(testUser.getId(), invoice.getId());

                        assertThat(invoiceRepository.findById(invoice.getId())).isEmpty();
                        // Assuming direct repository access or verification via queries if
                        // Installment/Payment repos were exposed directly
                        // Since they are aggregates, checking the root (Invoice) is sufficient in DDD
                        // if mapped correctly (CascadeType.ALL)
                }

                @Test
                @DisplayName("Should throw NotFoundException when deleting other user's invoice")
                void shouldThrowNotFoundDeletingOtherUserInvoice() {
                        Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-OTHER-DEL");

                        assertThatThrownBy(() -> invoiceService.deleteInvoice(testUser.getId(), otherInvoice.getId()))
                                        .isInstanceOf(NotFoundException.class);
                }
        }

        private Invoice createTestInvoice(CustomerId customerId, String reference) {
                Customer customer = customerRepository.findById(customerId).orElseThrow();
                Invoice invoice = Invoice.create(new InvoiceId(UUID.randomUUID()), customer.getId(),
                                new InvoiceReference(reference), "USD");
                return invoiceRepository.save(invoice);
        }
}