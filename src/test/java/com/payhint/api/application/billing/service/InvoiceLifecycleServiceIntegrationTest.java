package com.payhint.api.application.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.payhint.api.application.billing.dto.request.CreateInstallmentRequest;
import com.payhint.api.application.billing.dto.request.CreateInvoiceRequest;
import com.payhint.api.application.billing.dto.request.CreatePaymentRequest;
import com.payhint.api.application.billing.dto.request.UpdateInvoiceRequest;
import com.payhint.api.application.billing.dto.response.InvoiceResponse;
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
import com.payhint.api.infrastructure.billing.persistence.jpa.repository.InvoiceSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.CustomerSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.UserSpringRepository;

@SpringBootTest
@TestPropertySource(properties = { "spring.datasource.url=jdbc:h2:mem:testdb",
                "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
                "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop" })
@Transactional
@DisplayName("InvoiceService Integration Tests")
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
                                new Email("testuser@example.com"), "encodedPassword", "Test", "User"));

                otherUser = userRepository.register(User.create(new UserId(UUID.randomUUID()),
                                new Email("otheruser@example.com"), "encodedPassword", "Other", "User"));

                testCustomer = customerRepository.save(Customer.create(new CustomerId(UUID.randomUUID()),
                                testUser.getId(), "Test Customer", new Email("customer@example.com")));

                otherCustomer = customerRepository.save(Customer.create(new CustomerId(UUID.randomUUID()),
                                otherUser.getId(), "Other Customer", new Email("othercustomer@example.com")));
        }

        @Nested
        @DisplayName("Invoice Management")
        class InvoiceManagementTests {

                @Test
                void shouldThrowAlreadyExistsWhenCreatingDuplicateReferenceForSameCustomer() {
                        var firstRequest = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-DUP-REF", "USD");
                        invoiceService.createInvoice(testUser.getId(), firstRequest);
                        var duplicateRequest = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-DUP-REF",
                                        "USD");
                        assertThatThrownBy(() -> invoiceService.createInvoice(testUser.getId(), duplicateRequest))
                                        .isInstanceOf(AlreadyExistsException.class);
                }

                @Test
                void shouldAllowSameReferenceForDifferentCustomers() {
                        var req1 = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-SHARED-001", "USD");
                        var req2 = new CreateInvoiceRequest(otherCustomer.getId().value(), "INV-SHARED-001", "USD");
                        var r1 = invoiceService.createInvoice(testUser.getId(), req1);
                        var r2 = invoiceService.createInvoice(otherUser.getId(), req2);
                        assertThat(r1.invoiceReference()).isEqualTo(r2.invoiceReference());
                }

                @Test
                void shouldThrowIllegalStateWhenUpdatingArchivedInvoice() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCH-UPD");
                        invoiceService.archiveInvoice(testUser.getId(), invoice.getId());
                        var updateRequest = new UpdateInvoiceRequest("INV-ARCH-UPD-NEW", "EUR");
                        assertThatThrownBy(() -> invoiceService.updateInvoice(testUser.getId(), invoice.getId(),
                                        updateRequest)).isInstanceOf(IllegalStateException.class);
                }

                @Test
                void shouldViewArchivedInvoiceSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCH-VIEW");
                        invoiceService.archiveInvoice(testUser.getId(), invoice.getId());
                        InvoiceResponse response = invoiceService.viewInvoiceSummary(testUser.getId(), invoice.getId());
                        assertThat(response.isArchived()).isTrue();
                }

                @Test
                void shouldDeleteArchivedInvoice() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCH-DEL");
                        invoiceService.archiveInvoice(testUser.getId(), invoice.getId());
                        invoiceService.deleteInvoice(testUser.getId(), invoice.getId());
                        assertThat(invoiceRepository.findById(invoice.getId())).isEmpty();
                }

                @Test
                void shouldCreateInvoiceSuccessfully() {
                        var request = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-2025-001", "USD");

                        InvoiceResponse response = invoiceService.createInvoice(testUser.getId(), request);

                        assertThat(response).isNotNull();
                        assertThat(response.invoiceReference()).isEqualTo("INV-2025-001");
                        assertThat(response.currency()).isEqualTo("USD");
                        assertThat(response.customerId()).isEqualTo(testCustomer.getId().value().toString());
                }

                @Test
                void shouldThrowExceptionWhenCreatingInvoiceForNonExistentCustomer() {
                        var request = new CreateInvoiceRequest(UUID.randomUUID(), "INV-2025-002", "USD");

                        assertThatThrownBy(() -> invoiceService.createInvoice(testUser.getId(), request))
                                        .isInstanceOf(NotFoundException.class);
                }

                @Test
                void shouldThrowExceptionWhenCreatingInvoiceForOtherUsersCustomer() {
                        var request = new CreateInvoiceRequest(otherCustomer.getId().value(), "INV-2025-003", "USD");

                        assertThatThrownBy(() -> invoiceService.createInvoice(testUser.getId(), request))
                                        .isInstanceOf(PermissionDeniedException.class);
                }

                @Test
                void shouldGetInvoiceById() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-2025-005");

                        InvoiceResponse response = invoiceService.viewInvoiceSummary(testUser.getId(), invoice.getId());

                        assertThat(response).isNotNull();
                        assertThat(response.id()).isEqualTo(invoice.getId().value().toString());
                        assertThat(response.invoiceReference()).isEqualTo("INV-2025-005");
                }

                @Test
                void shouldThrowExceptionWhenAccessingOtherUsersInvoice() {
                        Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-2025-011");

                        assertThatThrownBy(
                                        () -> invoiceService.viewInvoiceSummary(testUser.getId(), otherInvoice.getId()))
                                                        .isInstanceOf(NotFoundException.class);
                }

                @Test
                void shouldUpdateInvoiceSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-TO-UPDATE");
                        var updateRequest = new UpdateInvoiceRequest("INV-UPDATED", "EUR");

                        InvoiceResponse response = invoiceService.updateInvoice(testUser.getId(), invoice.getId(),
                                        updateRequest);

                        assertThat(response.invoiceReference()).isEqualTo("INV-UPDATED");
                        assertThat(response.currency()).isEqualTo("EUR");
                }

                @Test
                void shouldThrowExceptionWhenUpdatingOtherUsersInvoice() {
                        Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-OTHER-USER");
                        var updateRequest = new UpdateInvoiceRequest("INV-FAIL-UPDATE", null);

                        assertThatThrownBy(() -> invoiceService.updateInvoice(testUser.getId(), otherInvoice.getId(),
                                        updateRequest)).isInstanceOf(NotFoundException.class);
                }

                @Test
                void shouldDeleteInvoice() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-TO-DELETE");

                        invoiceService.deleteInvoice(testUser.getId(), invoice.getId());

                        assertThat(invoiceRepository.findById(invoice.getId())).isEmpty();
                }

                @Test
                void shouldThrowExceptionWhenDeletingOtherUsersInvoice() {
                        Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-OTHER-DELETE");

                        assertThatThrownBy(() -> invoiceService.deleteInvoice(testUser.getId(), otherInvoice.getId()))
                                        .isInstanceOf(NotFoundException.class);
                }

                @Test
                void shouldArchiveAndUnarchiveInvoice() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-TO-ARCHIVE");

                        InvoiceResponse archivedResponse = invoiceService.archiveInvoice(testUser.getId(),
                                        invoice.getId());
                        assertThat(archivedResponse.isArchived()).isTrue();

                        Invoice archivedInvoice = invoiceRepository.findById(invoice.getId()).get();
                        assertThat(archivedInvoice.isArchived()).isTrue();

                        InvoiceResponse unarchivedResponse = invoiceService.unarchiveInvoice(testUser.getId(),
                                        invoice.getId());
                        assertThat(unarchivedResponse.isArchived()).isFalse();

                        Invoice unarchivedInvoice = invoiceRepository.findById(invoice.getId()).get();
                        assertThat(unarchivedInvoice.isArchived()).isFalse();
                }

                @Test
                void shouldListInvoicesByCustomer() {
                        createTestInvoice(testCustomer.getId(), "INV-LIST-1");
                        createTestInvoice(testCustomer.getId(), "INV-LIST-2");
                        createTestInvoice(otherCustomer.getId(), "INV-OTHER-LIST");

                        List<InvoiceResponse> responses = invoiceService.listInvoicesByCustomer(testUser.getId(),
                                        testCustomer.getId());

                        assertThat(responses).hasSize(2);
                        assertThat(responses).extracting(InvoiceResponse::invoiceReference)
                                        .containsExactlyInAnyOrder("INV-LIST-1", "INV-LIST-2");
                }

                @Test
                void shouldReturnEmptyListWhenListingInvoicesForCustomerWithNone() {
                        List<InvoiceResponse> responses = invoiceService.listInvoicesByCustomer(testUser.getId(),
                                        testCustomer.getId());

                        assertThat(responses).isEmpty();
                }

                @Test
                void shouldThrowExceptionWhenListingInvoicesForOtherUsersCustomer() {
                        assertThatThrownBy(() -> invoiceService.listInvoicesByCustomer(testUser.getId(),
                                        otherCustomer.getId())).isInstanceOf(PermissionDeniedException.class);
                }
        }

        @Nested
        @DisplayName("Edge Case & Invariant Tests")
        class EdgeCaseAndInvariantTests {
                @Test
                void shouldCascadeDeleteInstallmentsAndPaymentsWhenDeletingInvoice() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-CASCADE-DEL");
                        var inst1 = new CreateInstallmentRequest(new BigDecimal("30"),
                                        LocalDate.now().plusDays(2).toString());
                        var instResp1 = installmentService.addInstallment(testUser.getId(), invoice.getId(), inst1);
                        String installmentId = instResp1.installments().get(0).id();
                        var pay = new CreatePaymentRequest(new BigDecimal("10"), LocalDate.now().toString());
                        paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), pay);
                        invoiceService.deleteInvoice(testUser.getId(), invoice.getId());
                        assertThat(invoiceRepository.findById(invoice.getId())).isEmpty();
                }
        }

        private Invoice createTestInvoice(CustomerId customerId, String invoiceReference) {
                Customer customer = customerRepository.findById(customerId).orElseThrow();
                Invoice invoice = Invoice.create(new InvoiceId(UUID.randomUUID()), customer.getId(),
                                new InvoiceReference(invoiceReference), "USD");
                return invoiceRepository.save(invoice);
        }
}
