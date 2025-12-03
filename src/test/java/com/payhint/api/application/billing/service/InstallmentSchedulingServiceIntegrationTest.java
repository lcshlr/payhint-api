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

import com.payhint.api.application.billing.dto.request.CreateInstallmentRequest;
import com.payhint.api.application.billing.dto.request.CreatePaymentRequest;
import com.payhint.api.application.billing.dto.request.UpdateInstallmentRequest;
import com.payhint.api.application.billing.dto.response.InstallmentResponse;
import com.payhint.api.application.shared.exception.NotFoundException;
import com.payhint.api.domain.billing.exception.InstallmentDoesNotBelongToInvoiceException;
import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
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
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;
import com.payhint.api.domain.shared.valueobject.Email;
import com.payhint.api.infrastructure.billing.persistence.jpa.repository.InvoiceSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.CustomerSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.UserSpringRepository;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("InstallmentSchedulingService Integration Tests")
class InstallmentSchedulingServiceIntegrationTest {

        @Autowired
        private InvoiceLifecycleService invoiceLifecycleService;

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

        @AfterEach
        void tearDown() {
                invoiceSpringRepository.deleteAll();
                customerSpringRepository.deleteAll();
                userSpringRepository.deleteAll();
        }

        @Nested
        @DisplayName("Installment Management")
        class InstallmentManagementTests {

                @Test
                void shouldAddInstallmentSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-INST-ADD");
                        var request = new CreateInstallmentRequest(new BigDecimal("100.00"),
                                        LocalDate.now().plusDays(10).toString());

                        var response = installmentService.addInstallment(testUser.getId(), invoice.getId(), request);

                        assertThat(response.installments()).hasSize(1);
                        assertThat(response.totalAmount()).isEqualByComparingTo("100.00");
                        assertThat(response.status()).isEqualTo("PENDING");
                }

                @Test
                void shouldThrowExceptionWhenAddingInstallmentToOtherUsersInvoice() {
                        Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-INST-ADD-OTHER");
                        var request = new CreateInstallmentRequest(new BigDecimal("50.00"),
                                        LocalDate.now().plusDays(5).toString());

                        assertThatThrownBy(() -> installmentService.addInstallment(testUser.getId(),
                                        otherInvoice.getId(), request)).isInstanceOf(NotFoundException.class);
                }

                @Test
                void shouldNotAddSecondInstallmentWithSameDueDate() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-INST-DUP-DATE");
                        String dueDate = LocalDate.now().plusDays(7).toString();
                        installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        new CreateInstallmentRequest(new BigDecimal("100"), dueDate));

                        assertThatThrownBy(() -> installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        new CreateInstallmentRequest(new BigDecimal("150"), dueDate)))
                                                        .isInstanceOf(InvalidPropertyException.class);
                }

                @Test
                void shouldUpdateInstallmentSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-INST-UPD");
                        var addRequest = new CreateInstallmentRequest(new BigDecimal("200"),
                                        LocalDate.now().plusDays(8).toString());
                        var addResponse = installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        addRequest);
                        String installmentId = addResponse.installments().get(0).id();

                        var updateRequest = new UpdateInstallmentRequest(new BigDecimal("250"),
                                        LocalDate.now().plusDays(12).toString());
                        var updatedResponse = installmentService.updateInstallment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), updateRequest);

                        assertThat(updatedResponse.installments().get(0).amountDue()).isEqualByComparingTo("250");
                        assertThat(updatedResponse.installments().get(0).dueDate()).isEqualTo(updateRequest.dueDate());
                }

                @Test
                void shouldThrowExceptionWhenUpdatingInstallmentOnOtherUsersInvoice() {
                        Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-INST-UPD-OTHER");
                        var addRequest = new CreateInstallmentRequest(new BigDecimal("120"),
                                        LocalDate.now().plusDays(6).toString());
                        var addResponse = installmentService.addInstallment(otherUser.getId(), otherInvoice.getId(),
                                        addRequest);
                        String installmentId = addResponse.installments().get(0).id();
                        var updateRequest = new UpdateInstallmentRequest(new BigDecimal("130"), null);

                        assertThatThrownBy(() -> installmentService.updateInstallment(testUser.getId(),
                                        otherInvoice.getId(), new InstallmentId(UUID.fromString(installmentId)),
                                        updateRequest)).isInstanceOf(NotFoundException.class);
                }

                @Test
                void shouldRemoveInstallmentSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-INST-REM");
                        var addRequest = new CreateInstallmentRequest(new BigDecimal("90"),
                                        LocalDate.now().plusDays(9).toString());
                        var addResponse = installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        addRequest);
                        String installmentId = addResponse.installments().get(0).id();

                        var response = installmentService.removeInstallment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)));

                        assertThat(response.installments()).isEmpty();
                        assertThat(response.totalAmount()).isEqualByComparingTo("0");
                }

                @Test
                void shouldThrowExceptionWhenRemovingInstallmentOnOtherUsersInvoice() {
                        Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-INST-REM-OTHER");
                        var addRequest = new CreateInstallmentRequest(new BigDecimal("75"),
                                        LocalDate.now().plusDays(4).toString());
                        var addResponse = installmentService.addInstallment(otherUser.getId(), otherInvoice.getId(),
                                        addRequest);
                        String installmentId = addResponse.installments().get(0).id();

                        assertThatThrownBy(() -> installmentService.removeInstallment(testUser.getId(),
                                        otherInvoice.getId(), new InstallmentId(UUID.fromString(installmentId))))
                                                        .isInstanceOf(NotFoundException.class);
                }
        }

        @Nested
        @DisplayName("Edge Case & Invariant Tests")
        class EdgeCaseAndInvariantTests {

                @Test
                void shouldNotAddInstallmentWithNegativeAmount() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-INST-NEG-AMT");
                        var request = new CreateInstallmentRequest(new BigDecimal("-10"),
                                        LocalDate.now().plusDays(2).toString());
                        assertThatThrownBy(() -> installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        request)).isInstanceOf(InvalidMoneyValueException.class);
                }

                @Test
                void shouldNotAddInstallmentWithZeroAmount() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-INST-ZERO-AMT");
                        var request = new CreateInstallmentRequest(new BigDecimal("0"),
                                        LocalDate.now().plusDays(2).toString());
                        assertThatThrownBy(() -> installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        request)).isInstanceOf(InvalidMoneyValueException.class)
                                                        .hasMessage("Installment amountDue must be greater than zero");
                }

                @Test
                void shouldPreventModificationsAfterArchiveForInstallmentUpdate() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCH-INST-UPD");
                        var instReq = new CreateInstallmentRequest(new BigDecimal("70"),
                                        LocalDate.now().plusDays(5).toString());
                        var instResp = installmentService.addInstallment(testUser.getId(), invoice.getId(), instReq);
                        invoiceLifecycleService.archiveInvoice(testUser.getId(), invoice.getId());
                        String installmentId = instResp.installments().get(0).id();
                        var updReq = new UpdateInstallmentRequest(new BigDecimal("80"), null);
                        assertThatThrownBy(() -> installmentService.updateInstallment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), updReq)).isInstanceOfAny(
                                                        IllegalStateException.class,
                                                        InstallmentDoesNotBelongToInvoiceException.class);
                }

                @Test
                void shouldPreventAddingInstallmentAfterArchive() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCH-INST-ADD");
                        invoiceLifecycleService.archiveInvoice(testUser.getId(), invoice.getId());
                        var instReq = new CreateInstallmentRequest(new BigDecimal("30"),
                                        LocalDate.now().plusDays(5).toString());
                        assertThatThrownBy(() -> installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        instReq)).isInstanceOf(IllegalStateException.class);
                }

                @Test
                void shouldReachFullyPaidStatusAcrossMultipleInstallments() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-MULTI-FULLY-PAID");
                        var inst1 = new CreateInstallmentRequest(new BigDecimal("100"),
                                        LocalDate.now().plusDays(5).toString());
                        var inst2 = new CreateInstallmentRequest(new BigDecimal("50"),
                                        LocalDate.now().plusDays(6).toString());
                        installmentService.addInstallment(testUser.getId(), invoice.getId(), inst1);
                        var afterInst2 = installmentService.addInstallment(testUser.getId(), invoice.getId(), inst2);
                        List<InstallmentResponse> insts = afterInst2.installments();
                        String inst100Id = insts.stream()
                                        .filter(i -> i.amountDue().compareTo(new BigDecimal("100")) == 0).findFirst()
                                        .get().id();
                        String inst50Id = insts.stream().filter(i -> i.amountDue().compareTo(new BigDecimal("50")) == 0)
                                        .findFirst().get().id();
                        var pay1 = new CreatePaymentRequest(new BigDecimal("100"), LocalDate.now().toString());
                        var afterPay1 = paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(inst100Id)), pay1);
                        assertThat(afterPay1.status()).isEqualTo("PARTIALLY_PAID");
                        var pay2 = new CreatePaymentRequest(new BigDecimal("50"), LocalDate.now().toString());
                        var afterPay2 = paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(inst50Id)), pay2);
                        assertThat(afterPay2.status()).isEqualTo("PAID");
                        assertThat(afterPay2.remainingAmount()).isEqualByComparingTo("0");
                }
        }

        @Nested
        @DisplayName("Consistency and Flow Tests")
        class ConsistencyAndFlowTests {

                @Test
                @DisplayName("Should verify consistency across invoice properties when manipulating installments")
                void shouldVerifyConsistencyWhenManipulatingInstallments() {
                        // 1. Create Invoice
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-FLOW-INST-001");
                        assertThat(invoice.getStatus().name()).isEqualTo("PENDING");
                        assertThat(invoice.getTotalAmount().amount()).isEqualByComparingTo("0.00");

                        // 2. Add Installment
                        CreateInstallmentRequest instRequest = new CreateInstallmentRequest(new BigDecimal("100.00"),
                                        LocalDate.now().plusDays(10).toString());
                        var addResponse = installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        instRequest);
                        InstallmentId installmentId = new InstallmentId(
                                        UUID.fromString(addResponse.installments().get(0).id()));

                        assertThat(addResponse.totalAmount()).isEqualByComparingTo("100.00");
                        assertThat(addResponse.totalPaid()).isEqualByComparingTo("0.00");
                        assertThat(addResponse.status()).isEqualTo("PENDING");

                        // 3. Update Installment Amount (Increase)
                        UpdateInstallmentRequest updateRequest = new UpdateInstallmentRequest(new BigDecimal("150.00"),
                                        null);
                        var updateResponse = installmentService.updateInstallment(testUser.getId(), invoice.getId(),
                                        installmentId, updateRequest);

                        assertThat(updateResponse.totalAmount()).isEqualByComparingTo("150.00");
                        assertThat(updateResponse.totalPaid()).isEqualByComparingTo("0.00");
                        assertThat(updateResponse.status()).isEqualTo("PENDING");

                        // 4. Remove Installment
                        var removeResponse = installmentService.removeInstallment(testUser.getId(), invoice.getId(),
                                        installmentId);

                        assertThat(removeResponse.totalAmount()).isEqualByComparingTo("0.00");
                        assertThat(removeResponse.installments()).isEmpty();
                }

                @Test
                @DisplayName("Should verify status regression from PAID to PARTIALLY_PAID when adding new installment")
                void shouldVerifyStatusRegression() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-FLOW-INST-002");

                        // 1. Add Installment & Pay it fully
                        var instReq1 = new CreateInstallmentRequest(new BigDecimal("100.00"),
                                        LocalDate.now().plusDays(10).toString());
                        var resp1 = installmentService.addInstallment(testUser.getId(), invoice.getId(), instReq1);
                        InstallmentId instId1 = new InstallmentId(UUID.fromString(resp1.installments().get(0).id()));

                        paymentService.recordPayment(testUser.getId(), invoice.getId(), instId1,
                                        new CreatePaymentRequest(new BigDecimal("100.00"), LocalDate.now().toString()));

                        // Verify PAID
                        var invoiceAfterPay = invoiceLifecycleService.viewInvoice(testUser.getId(), invoice.getId());
                        assertThat(invoiceAfterPay.status()).isEqualTo("PAID");
                        assertThat(invoiceAfterPay.totalAmount()).isEqualByComparingTo("100.00");
                        assertThat(invoiceAfterPay.totalPaid()).isEqualByComparingTo("100.00");

                        // 2. Add New Installment
                        var instReq2 = new CreateInstallmentRequest(new BigDecimal("50.00"),
                                        LocalDate.now().plusDays(20).toString());
                        var resp2 = installmentService.addInstallment(testUser.getId(), invoice.getId(), instReq2);

                        // Verify Regression to PARTIALLY_PAID
                        assertThat(resp2.status()).isEqualTo("PARTIALLY_PAID");
                        assertThat(resp2.totalAmount()).isEqualByComparingTo("150.00"); // 100 + 50
                        assertThat(resp2.totalPaid()).isEqualByComparingTo("100.00"); // Unchanged
                        assertThat(resp2.remainingAmount()).isEqualByComparingTo("50.00");
                }

                @Test
                @DisplayName("Should verify overdue status logic when adding past due installments")
                void shouldVerifyOverdueLogic() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-FLOW-INST-003");

                        // 1. Add Past Due Installment
                        var pastReq = new CreateInstallmentRequest(new BigDecimal("100.00"),
                                        LocalDate.now().minusDays(1).toString());
                        var resp1 = installmentService.addInstallment(testUser.getId(), invoice.getId(), pastReq);
                        InstallmentId instId = new InstallmentId(UUID.fromString(resp1.installments().get(0).id()));

                        assertThat(resp1.isOverdue()).isTrue();

                        // 2. Update to Future Date
                        var updateReq = new UpdateInstallmentRequest(null, LocalDate.now().plusDays(5).toString());
                        var resp2 = installmentService.updateInstallment(testUser.getId(), invoice.getId(), instId,
                                        updateReq);

                        assertThat(resp2.isOverdue()).isFalse();
                }
        }

        private Invoice createTestInvoice(CustomerId customerId, String invoiceReference) {
                Customer customer = customerRepository.findById(customerId).orElseThrow();
                Invoice invoice = Invoice.create(new InvoiceId(UUID.randomUUID()), customer.getId(),
                                new InvoiceReference(invoiceReference), "USD");
                return invoiceRepository.save(invoice);
        }
}