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
import com.payhint.api.application.billing.dto.request.UpdateInstallmentRequest;
import com.payhint.api.application.billing.dto.request.UpdateInvoiceRequest;
import com.payhint.api.application.billing.dto.request.UpdatePaymentRequest;
import com.payhint.api.application.billing.dto.response.InstallmentResponse;
import com.payhint.api.application.billing.dto.response.InvoiceResponse;
import com.payhint.api.application.shared.exception.AlreadyExistsException;
import com.payhint.api.application.shared.exception.NotFoundException;
import com.payhint.api.application.shared.exception.PermissionDeniedException;
import com.payhint.api.domain.billing.exception.InstallmentDoesNotBelongToInvoiceException;
import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.billing.valueobject.PaymentId;
import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.CustomerRepository;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.Email;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;
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
class InvoiceServiceIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

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

        testUser = userRepository
                .register(User.create(new Email("testuser@example.com"), "encodedPassword", "Test", "User"));

        otherUser = userRepository
                .register(User.create(new Email("otheruser@example.com"), "encodedPassword", "Other", "User"));

        testCustomer = customerRepository
                .save(Customer.create(testUser.getId(), "Test Customer", new Email("customer@example.com")));

        otherCustomer = customerRepository
                .save(Customer.create(otherUser.getId(), "Other Customer", new Email("othercustomer@example.com")));
    }

    @Nested
    @DisplayName("Installment & Payment Management")
    class InstallmentManagementTests {

        @Test
        void shouldAddInstallmentSuccessfully() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-INST-ADD");
            var request = new CreateInstallmentRequest(new BigDecimal("100.00"),
                    LocalDate.now().plusDays(10).toString());

            var response = invoiceService.addInstallment(testUser.getId(), invoice.getId(), request);

            assertThat(response.installments()).hasSize(1);
            assertThat(response.totalAmount()).isEqualByComparingTo("100.00");
            assertThat(response.status()).isEqualTo("PENDING");
        }

        @Test
        void shouldThrowExceptionWhenAddingInstallmentToOtherUsersInvoice() {
            Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-INST-ADD-OTHER");
            var request = new CreateInstallmentRequest(new BigDecimal("50.00"), LocalDate.now().plusDays(5).toString());

            assertThatThrownBy(() -> invoiceService.addInstallment(testUser.getId(), otherInvoice.getId(), request))
                    .isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        void shouldNotAddSecondInstallmentWithSameDueDate() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-INST-DUP-DATE");
            String dueDate = LocalDate.now().plusDays(7).toString();
            invoiceService.addInstallment(testUser.getId(), invoice.getId(),
                    new CreateInstallmentRequest(new BigDecimal("100"), dueDate));

            assertThatThrownBy(() -> invoiceService.addInstallment(testUser.getId(), invoice.getId(),
                    new CreateInstallmentRequest(new BigDecimal("150"), dueDate)))
                            .isInstanceOf(InvalidPropertyException.class);
        }

        @Test
        void shouldUpdateInstallmentSuccessfully() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-INST-UPD");
            var addRequest = new CreateInstallmentRequest(new BigDecimal("200"),
                    LocalDate.now().plusDays(8).toString());
            var addResponse = invoiceService.addInstallment(testUser.getId(), invoice.getId(), addRequest);
            String installmentId = addResponse.installments().get(0).id();

            var updateRequest = new UpdateInstallmentRequest(new BigDecimal("250"),
                    LocalDate.now().plusDays(12).toString());
            var updatedResponse = invoiceService.updateInstallment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), updateRequest);

            assertThat(updatedResponse.installments().get(0).amountDue()).isEqualByComparingTo("250");
            assertThat(updatedResponse.installments().get(0).dueDate()).isEqualTo(updateRequest.dueDate());
        }

        @Test
        void shouldThrowExceptionWhenUpdatingInstallmentOnOtherUsersInvoice() {
            Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-INST-UPD-OTHER");
            var addRequest = new CreateInstallmentRequest(new BigDecimal("120"),
                    LocalDate.now().plusDays(6).toString());
            var addResponse = invoiceService.addInstallment(otherUser.getId(), otherInvoice.getId(), addRequest);
            String installmentId = addResponse.installments().get(0).id();
            var updateRequest = new UpdateInstallmentRequest(new BigDecimal("130"), null);

            assertThatThrownBy(() -> invoiceService.updateInstallment(testUser.getId(), otherInvoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), updateRequest))
                            .isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        void shouldRemoveInstallmentSuccessfully() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-INST-REM");
            var addRequest = new CreateInstallmentRequest(new BigDecimal("90"), LocalDate.now().plusDays(9).toString());
            var addResponse = invoiceService.addInstallment(testUser.getId(), invoice.getId(), addRequest);
            String installmentId = addResponse.installments().get(0).id();

            var response = invoiceService.removeInstallment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)));

            assertThat(response.installments()).isEmpty();
            assertThat(response.totalAmount()).isEqualByComparingTo("0");
        }

        @Test
        void shouldThrowExceptionWhenRemovingInstallmentOnOtherUsersInvoice() {
            Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-INST-REM-OTHER");
            var addRequest = new CreateInstallmentRequest(new BigDecimal("75"), LocalDate.now().plusDays(4).toString());
            var addResponse = invoiceService.addInstallment(otherUser.getId(), otherInvoice.getId(), addRequest);
            String installmentId = addResponse.installments().get(0).id();

            assertThatThrownBy(() -> invoiceService.removeInstallment(testUser.getId(), otherInvoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)))).isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        void shouldRecordPaymentSuccessfullyAndUpdateStatus() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-REC");
            var addRequest = new CreateInstallmentRequest(new BigDecimal("150"),
                    LocalDate.now().plusDays(5).toString());
            var addResponse = invoiceService.addInstallment(testUser.getId(), invoice.getId(), addRequest);
            String installmentId = addResponse.installments().get(0).id();

            var paymentRequest = new CreatePaymentRequest(new BigDecimal("150"), LocalDate.now().toString());
            var paymentResponse = invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), paymentRequest);

            assertThat(paymentResponse.totalPaid()).isEqualByComparingTo("150");
            assertThat(paymentResponse.remainingAmount()).isEqualByComparingTo("0");
            assertThat(paymentResponse.status()).isEqualTo("PAID");
            assertThat(paymentResponse.installments().get(0).payments()).hasSize(1);
        }

        @Test
        void shouldThrowExceptionWhenRecordingPaymentOnOtherUsersInvoice() {
            Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-PMT-REC-OTHER");
            var addRequest = new CreateInstallmentRequest(new BigDecimal("60"), LocalDate.now().plusDays(3).toString());
            var addResponse = invoiceService.addInstallment(otherUser.getId(), otherInvoice.getId(), addRequest);
            String installmentId = addResponse.installments().get(0).id();
            var paymentRequest = new CreatePaymentRequest(new BigDecimal("30"), LocalDate.now().toString());

            assertThatThrownBy(() -> invoiceService.recordPayment(testUser.getId(), otherInvoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), paymentRequest))
                            .isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        void shouldUpdatePaymentSuccessfully() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-UPD");
            var addRequest = new CreateInstallmentRequest(new BigDecimal("200"),
                    LocalDate.now().plusDays(11).toString());
            var addResponse = invoiceService.addInstallment(testUser.getId(), invoice.getId(), addRequest);
            String installmentId = addResponse.installments().get(0).id();
            var paymentRequest = new CreatePaymentRequest(new BigDecimal("80"), LocalDate.now().toString());
            var paymentResponse = invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), paymentRequest);
            String paymentId = paymentResponse.installments().get(0).payments().get(0).id();

            var updatePaymentRequest = new UpdatePaymentRequest(new BigDecimal("100"), LocalDate.now().toString());
            var updatedResponse = invoiceService.updatePayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), new PaymentId(UUID.fromString(paymentId)),
                    updatePaymentRequest);

            assertThat(updatedResponse.totalPaid()).isEqualByComparingTo("100");
            assertThat(updatedResponse.remainingAmount()).isEqualByComparingTo("100");
            assertThat(updatedResponse.status()).isEqualTo("PARTIALLY_PAID");
        }

        @Test
        void shouldThrowExceptionWhenUpdatingPaymentBeyondAllowedAmount() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-UPD-EXC");
            var addRequest = new CreateInstallmentRequest(new BigDecimal("100"),
                    LocalDate.now().plusDays(2).toString());
            var addResponse = invoiceService.addInstallment(testUser.getId(), invoice.getId(), addRequest);
            String installmentId = addResponse.installments().get(0).id();
            var paymentRequest = new CreatePaymentRequest(new BigDecimal("60"), LocalDate.now().toString());
            var paymentResponse = invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), paymentRequest);
            String paymentId = paymentResponse.installments().get(0).payments().get(0).id();

            var updatePaymentRequest = new UpdatePaymentRequest(new BigDecimal("110"), null);
            assertThatThrownBy(() -> invoiceService.updatePayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), new PaymentId(UUID.fromString(paymentId)),
                    updatePaymentRequest)).isInstanceOf(InvalidMoneyValueException.class);
        }

        @Test
        void shouldRemovePaymentSuccessfully() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-REM");
            var addRequest = new CreateInstallmentRequest(new BigDecimal("140"),
                    LocalDate.now().plusDays(13).toString());
            var addResponse = invoiceService.addInstallment(testUser.getId(), invoice.getId(), addRequest);
            String installmentId = addResponse.installments().get(0).id();
            var paymentRequest = new CreatePaymentRequest(new BigDecimal("50"), LocalDate.now().toString());
            var paymentResponse = invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), paymentRequest);
            String paymentId = paymentResponse.installments().get(0).payments().get(0).id();

            var afterRemoval = invoiceService.removePayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), new PaymentId(UUID.fromString(paymentId)));

            assertThat(afterRemoval.installments().get(0).payments()).isEmpty();
            assertThat(afterRemoval.totalPaid()).isEqualByComparingTo("0");
        }

        @Test
        void shouldThrowExceptionWhenRemovingPaymentFromOtherUsersInvoice() {
            Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-PMT-REM-OTHER");
            var addRequest = new CreateInstallmentRequest(new BigDecimal("85"), LocalDate.now().plusDays(3).toString());
            var addResponse = invoiceService.addInstallment(otherUser.getId(), otherInvoice.getId(), addRequest);
            String installmentId = addResponse.installments().get(0).id();
            var paymentRequest = new CreatePaymentRequest(new BigDecimal("40"), LocalDate.now().toString());
            var paymentResponse = invoiceService.recordPayment(otherUser.getId(), otherInvoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), paymentRequest);
            String paymentId = paymentResponse.installments().get(0).payments().get(0).id();

            assertThatThrownBy(() -> invoiceService.removePayment(testUser.getId(), otherInvoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), new PaymentId(UUID.fromString(paymentId))))
                            .isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        void shouldPreventModificationOnArchivedInvoice() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCH-MOD");
            invoiceService.archiveInvoice(testUser.getId(), invoice.getId());
            var request = new CreateInstallmentRequest(new BigDecimal("100"), LocalDate.now().plusDays(5).toString());
            assertThatThrownBy(() -> invoiceService.addInstallment(testUser.getId(), invoice.getId(), request))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void shouldReflectStatusTransitionsAcrossPayments() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-STATE-TRANS");
            var inst1 = new CreateInstallmentRequest(new BigDecimal("100"), LocalDate.now().plusDays(5).toString());
            var inst2 = new CreateInstallmentRequest(new BigDecimal("200"), LocalDate.now().plusDays(6).toString());
            var respA = invoiceService.addInstallment(testUser.getId(), invoice.getId(), inst1);
            String installment1Id = respA.installments().get(0).id();
            var respB = invoiceService.addInstallment(testUser.getId(), invoice.getId(), inst2);
            assertThat(respB.status()).isEqualTo("PENDING");
            String installment2Id = respB.installments().stream().filter(i -> !i.id().equals(installment1Id))
                    .findFirst().get().id();

            var pay1 = new CreatePaymentRequest(new BigDecimal("100"), LocalDate.now().toString());
            var afterFirstPayment = invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installment1Id)), pay1);
            assertThat(afterFirstPayment.status()).isEqualTo("PARTIALLY_PAID");
            assertThat(afterFirstPayment.totalPaid()).isEqualByComparingTo("100");
            assertThat(afterFirstPayment.remainingAmount()).isEqualByComparingTo("200");

            var pay2 = new CreatePaymentRequest(new BigDecimal("200"), LocalDate.now().toString());
            var afterSecondPayment = invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installment2Id)), pay2);
            assertThat(afterSecondPayment.status()).isEqualTo("PAID");
            assertThat(afterSecondPayment.totalPaid()).isEqualByComparingTo("300");
            assertThat(afterSecondPayment.remainingAmount()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("Invoice Management")
    class InvoiceManagementTests {

        @Test
        void shouldThrowAlreadyExistsWhenCreatingDuplicateReferenceForSameCustomer() {
            var firstRequest = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-DUP-REF", "USD");
            invoiceService.createInvoice(testUser.getId(), firstRequest);
            var duplicateRequest = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-DUP-REF", "USD");
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
            assertThatThrownBy(() -> invoiceService.updateInvoice(testUser.getId(), invoice.getId(), updateRequest))
                    .isInstanceOf(IllegalStateException.class);
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

            assertThatThrownBy(() -> invoiceService.viewInvoiceSummary(testUser.getId(), otherInvoice.getId()))
                    .isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        void shouldUpdateInvoiceSuccessfully() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-TO-UPDATE");
            var updateRequest = new UpdateInvoiceRequest("INV-UPDATED", "EUR");

            InvoiceResponse response = invoiceService.updateInvoice(testUser.getId(), invoice.getId(), updateRequest);

            assertThat(response.invoiceReference()).isEqualTo("INV-UPDATED");
            assertThat(response.currency()).isEqualTo("EUR");
        }

        @Test
        void shouldThrowExceptionWhenUpdatingOtherUsersInvoice() {
            Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-OTHER-USER");
            var updateRequest = new UpdateInvoiceRequest("INV-FAIL-UPDATE", null);

            assertThatThrownBy(
                    () -> invoiceService.updateInvoice(testUser.getId(), otherInvoice.getId(), updateRequest))
                            .isInstanceOf(PermissionDeniedException.class);
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
                    .isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        void shouldArchiveAndUnarchiveInvoice() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-TO-ARCHIVE");

            InvoiceResponse archivedResponse = invoiceService.archiveInvoice(testUser.getId(), invoice.getId());
            assertThat(archivedResponse.isArchived()).isTrue();

            Invoice archivedInvoice = invoiceRepository.findById(invoice.getId()).get();
            assertThat(archivedInvoice.isArchived()).isTrue();

            InvoiceResponse unarchivedResponse = invoiceService.unarchiveInvoice(testUser.getId(), invoice.getId());
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
            assertThat(responses).extracting(InvoiceResponse::invoiceReference).containsExactlyInAnyOrder("INV-LIST-1",
                    "INV-LIST-2");
        }

        @Test
        void shouldReturnEmptyListWhenListingInvoicesForCustomerWithNone() {
            List<InvoiceResponse> responses = invoiceService.listInvoicesByCustomer(testUser.getId(),
                    testCustomer.getId());

            assertThat(responses).isEmpty();
        }

        @Test
        void shouldThrowExceptionWhenListingInvoicesForOtherUsersCustomer() {
            assertThatThrownBy(() -> invoiceService.listInvoicesByCustomer(testUser.getId(), otherCustomer.getId()))
                    .isInstanceOf(PermissionDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Edge Case & Invariant Tests")
    class EdgeCaseAndInvariantTests {

        @Test
        void shouldNotAddInstallmentWithNegativeAmount() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-INST-NEG-AMT");
            var request = new CreateInstallmentRequest(new BigDecimal("-10"), LocalDate.now().plusDays(2).toString());
            assertThatThrownBy(() -> invoiceService.addInstallment(testUser.getId(), invoice.getId(), request))
                    .isInstanceOf(InvalidMoneyValueException.class);
        }

        @Test
        void shouldNotAddInstallmentWithZeroAmount() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-INST-ZERO-AMT");
            var request = new CreateInstallmentRequest(new BigDecimal("0"), LocalDate.now().plusDays(2).toString());
            assertThatThrownBy(() -> invoiceService.addInstallment(testUser.getId(), invoice.getId(), request))
                    .isInstanceOf(InvalidMoneyValueException.class)
                    .hasMessage("Installment amountDue must be greater than zero");
        }

        @Test
        void shouldNotRecordPaymentExceedingRemainingAmount() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-EXCEED");
            var inst = new CreateInstallmentRequest(new BigDecimal("100"), LocalDate.now().plusDays(3).toString());
            var resp = invoiceService.addInstallment(testUser.getId(), invoice.getId(), inst);
            String installmentId = resp.installments().get(0).id();
            var paymentTooHigh = new CreatePaymentRequest(new BigDecimal("150"), LocalDate.now().toString());
            assertThatThrownBy(() -> invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), paymentTooHigh))
                            .isInstanceOf(InvalidMoneyValueException.class);
        }

        @Test
        void shouldNotRecordPaymentWithZeroAmount() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-ZERO");
            var inst = new CreateInstallmentRequest(new BigDecimal("50"), LocalDate.now().plusDays(3).toString());
            var resp = invoiceService.addInstallment(testUser.getId(), invoice.getId(), inst);
            String installmentId = resp.installments().get(0).id();
            var zeroPayment = new CreatePaymentRequest(new BigDecimal("0"), LocalDate.now().toString());
            assertThatThrownBy(() -> invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), zeroPayment))
                            .isInstanceOf(InvalidMoneyValueException.class);
        }

        @Test
        void shouldNotUpdatePaymentBeyondAllowedAmount() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-UPD-EXCEED-TOTAL");
            var inst = new CreateInstallmentRequest(new BigDecimal("120"), LocalDate.now().plusDays(4).toString());
            var instResp = invoiceService.addInstallment(testUser.getId(), invoice.getId(), inst);
            String installmentId = instResp.installments().get(0).id();
            var pay = new CreatePaymentRequest(new BigDecimal("60"), LocalDate.now().toString());
            var afterPay = invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), pay);
            String paymentId = afterPay.installments().get(0).payments().get(0).id();
            var updateTooHigh = new UpdatePaymentRequest(new BigDecimal("200"), LocalDate.now().toString());
            assertThatThrownBy(() -> invoiceService.updatePayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), new PaymentId(UUID.fromString(paymentId)),
                    updateTooHigh)).isInstanceOf(InvalidMoneyValueException.class);
        }

        @Test
        void shouldRegressStatusAfterRemovingPayment() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-REGRESS");
            var inst = new CreateInstallmentRequest(new BigDecimal("100"), LocalDate.now().plusDays(3).toString());
            var instResp = invoiceService.addInstallment(testUser.getId(), invoice.getId(), inst);
            String installmentId = instResp.installments().get(0).id();
            var pay = new CreatePaymentRequest(new BigDecimal("40"), LocalDate.now().toString());
            var afterPay = invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), pay);
            String paymentId = afterPay.installments().get(0).payments().get(0).id();
            assertThat(afterPay.status()).isEqualTo("PARTIALLY_PAID");
            var afterRemoval = invoiceService.removePayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), new PaymentId(UUID.fromString(paymentId)));
            assertThat(afterRemoval.status()).isEqualTo("PENDING");
            assertThat(afterRemoval.totalPaid()).isEqualByComparingTo("0");
        }

        @Test
        void shouldPreventModificationsAfterArchiveForInstallmentUpdate() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCH-INST-UPD");
            var instReq = new CreateInstallmentRequest(new BigDecimal("70"), LocalDate.now().plusDays(5).toString());
            var instResp = invoiceService.addInstallment(testUser.getId(), invoice.getId(), instReq);
            invoiceService.archiveInvoice(testUser.getId(), invoice.getId());
            String installmentId = instResp.installments().get(0).id();
            var updReq = new UpdateInstallmentRequest(new BigDecimal("80"), null);
            assertThatThrownBy(() -> invoiceService.updateInstallment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), updReq)).isInstanceOfAny(
                            IllegalStateException.class, InstallmentDoesNotBelongToInvoiceException.class);
        }

        @Test
        void shouldPreventAddingInstallmentAfterArchive() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCH-INST-ADD");
            invoiceService.archiveInvoice(testUser.getId(), invoice.getId());
            var instReq = new CreateInstallmentRequest(new BigDecimal("30"), LocalDate.now().plusDays(5).toString());
            assertThatThrownBy(() -> invoiceService.addInstallment(testUser.getId(), invoice.getId(), instReq))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void shouldPreventRecordingPaymentAfterArchive() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCH-PMT-ADD");
            var instReq = new CreateInstallmentRequest(new BigDecimal("30"), LocalDate.now().plusDays(5).toString());
            var instResp = invoiceService.addInstallment(testUser.getId(), invoice.getId(), instReq);
            invoiceService.archiveInvoice(testUser.getId(), invoice.getId());
            String installmentId = instResp.installments().get(0).id();
            var payReq = new CreatePaymentRequest(new BigDecimal("10"), LocalDate.now().toString());
            assertThatThrownBy(() -> invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), payReq)).isInstanceOfAny(
                            IllegalStateException.class, InstallmentDoesNotBelongToInvoiceException.class);
        }

        @Test
        void shouldReachFullyPaidStatusAcrossMultipleInstallments() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-MULTI-FULLY-PAID");
            var inst1 = new CreateInstallmentRequest(new BigDecimal("100"), LocalDate.now().plusDays(5).toString());
            var inst2 = new CreateInstallmentRequest(new BigDecimal("50"), LocalDate.now().plusDays(6).toString());
            invoiceService.addInstallment(testUser.getId(), invoice.getId(), inst1);
            var afterInst2 = invoiceService.addInstallment(testUser.getId(), invoice.getId(), inst2);
            List<InstallmentResponse> insts = afterInst2.installments();
            String inst100Id = insts.stream().filter(i -> i.amountDue().compareTo(new BigDecimal("100")) == 0)
                    .findFirst().get().id();
            String inst50Id = insts.stream().filter(i -> i.amountDue().compareTo(new BigDecimal("50")) == 0).findFirst()
                    .get().id();
            var pay1 = new CreatePaymentRequest(new BigDecimal("100"), LocalDate.now().toString());
            var afterPay1 = invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(inst100Id)), pay1);
            assertThat(afterPay1.status()).isEqualTo("PARTIALLY_PAID");
            var pay2 = new CreatePaymentRequest(new BigDecimal("50"), LocalDate.now().toString());
            var afterPay2 = invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(inst50Id)), pay2);
            assertThat(afterPay2.status()).isEqualTo("PAID");
            assertThat(afterPay2.remainingAmount()).isEqualByComparingTo("0");
        }

        @Test
        void shouldSequentiallyPreventOverpaymentInConcurrentSimulation() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-SEQ-OVERPAY");
            var inst = new CreateInstallmentRequest(new BigDecimal("80"), LocalDate.now().plusDays(3).toString());
            var resp = invoiceService.addInstallment(testUser.getId(), invoice.getId(), inst);
            String installmentId = resp.installments().get(0).id();
            var pay1 = new CreatePaymentRequest(new BigDecimal("50"), LocalDate.now().toString());
            invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), pay1);
            var pay2 = new CreatePaymentRequest(new BigDecimal("40"), LocalDate.now().toString());
            assertThatThrownBy(() -> invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), pay2))
                            .isInstanceOf(InvalidMoneyValueException.class);
        }

        @Test
        void shouldCascadeDeleteInstallmentsAndPaymentsWhenDeletingInvoice() {
            Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-CASCADE-DEL");
            var inst1 = new CreateInstallmentRequest(new BigDecimal("30"), LocalDate.now().plusDays(2).toString());
            var instResp1 = invoiceService.addInstallment(testUser.getId(), invoice.getId(), inst1);
            String installmentId = instResp1.installments().get(0).id();
            var pay = new CreatePaymentRequest(new BigDecimal("10"), LocalDate.now().toString());
            invoiceService.recordPayment(testUser.getId(), invoice.getId(),
                    new InstallmentId(UUID.fromString(installmentId)), pay);
            invoiceService.deleteInvoice(testUser.getId(), invoice.getId());
            assertThat(invoiceRepository.findById(invoice.getId())).isEmpty();
        }
    }

    private Invoice createTestInvoice(CustomerId customerId, String invoiceReference) {
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        Invoice invoice = Invoice.create(customer.getId(), new InvoiceReference(invoiceReference), "USD");
        return invoiceRepository.save(invoice);
    }
}
