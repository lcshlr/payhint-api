package com.payhint.api.application.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import com.payhint.api.application.billing.dto.request.CreateInstallmentRequest;
import com.payhint.api.application.billing.dto.request.CreatePaymentRequest;
import com.payhint.api.application.billing.dto.request.UpdatePaymentRequest;
import com.payhint.api.application.billing.dto.response.InvoiceResponse;
import com.payhint.api.application.billing.dto.response.PaymentResponse;
import com.payhint.api.application.shared.exception.NotFoundException;
import com.payhint.api.domain.billing.exception.InstallmentDoesNotBelongToInvoiceException;
import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.model.PaymentStatus;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.billing.valueobject.PaymentId;
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
@DisplayName("PaymentProcessingService Integration Tests")
class PaymentProcessingServiceIntegrationTest {

        @Autowired
        private TransactionTemplate transactionTemplate;

        @Autowired
        private InstallmentSchedulingService installmentService;

        @Autowired
        private PaymentProcessingService paymentService;

        @Autowired
        private InvoiceLifecycleService invoiceService;

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
                testUser = userRepository.register(User.create(new UserId(UUID.randomUUID()),
                                new Email("payment.test@example.com"), "Password123!", "Payment", "Tester"));

                otherUser = userRepository.register(User.create(new UserId(UUID.randomUUID()),
                                new Email("other.payment@example.com"), "Password123!", "Other", "User"));

                testCustomer = customerRepository.save(Customer.create(new CustomerId(UUID.randomUUID()),
                                testUser.getId(), "Test Inc", new Email("contact@testinc.com")));

                otherCustomer = customerRepository.save(Customer.create(new CustomerId(UUID.randomUUID()),
                                otherUser.getId(), "Other Inc", new Email("contact@otherinc.com")));
        }

        @AfterEach
        void tearDown() {
                invoiceSpringRepository.deleteAll();
                customerSpringRepository.deleteAll();
                userSpringRepository.deleteAll();
        }

        @Nested
        @DisplayName("Record Payment Tests")
        class RecordPaymentTests {

                @Test
                @DisplayName("Should record payment successfully and update status to PARTIALLY_PAID")
                void shouldRecordPaymentSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-001");
                        InstallmentId installmentId = addInstallment(invoice.getId(), "100.00");

                        CreatePaymentRequest request = new CreatePaymentRequest(new BigDecimal("60.00"),
                                        LocalDate.now().toString());

                        InvoiceResponse response = paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        installmentId, request);

                        assertThat(response.totalPaid()).isEqualByComparingTo("60.00");
                        assertThat(response.remainingAmount()).isEqualByComparingTo("40.00");
                        assertThat(response.status()).isEqualTo(PaymentStatus.PARTIALLY_PAID.name());
                        assertThat(response.installments().get(0).payments()).hasSize(1);
                        assertThat(response.installments().get(0).payments().get(0).amount())
                                        .isEqualByComparingTo("60.00");
                }

                @Test
                @DisplayName("Should record full payment and update status to PAID")
                void shouldRecordFullPaymentAndUpdateStatus() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-002");
                        InstallmentId installmentId = addInstallment(invoice.getId(), "100.00");

                        CreatePaymentRequest request = new CreatePaymentRequest(new BigDecimal("100.00"),
                                        LocalDate.now().toString());

                        InvoiceResponse response = paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        installmentId, request);

                        assertThat(response.totalPaid()).isEqualByComparingTo("100.00");
                        assertThat(response.remainingAmount()).isEqualByComparingTo("0.00");
                        assertThat(response.status()).isEqualTo(PaymentStatus.PAID.name());
                }

                @Test
                @DisplayName("Should throw exception when recording payment exceeding installment amount")
                void shouldThrowExceptionWhenPaymentExceedsLimit() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-003");
                        InstallmentId installmentId = addInstallment(invoice.getId(), "100.00");

                        CreatePaymentRequest request = new CreatePaymentRequest(new BigDecimal("101.00"),
                                        LocalDate.now().toString());

                        assertThatThrownBy(() -> paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        installmentId, request)).isInstanceOf(InvalidMoneyValueException.class)
                                                        .hasMessageContaining("exceeds remaining installment amount");
                }

                @Test
                @DisplayName("Should throw exception when recording payment for other user's invoice")
                void shouldThrowExceptionWhenRecordingOnOtherUserInvoice() {
                        Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-OTHER-001");
                        InstallmentId installmentId = addInstallmentForUser(otherUser.getId(), otherInvoice.getId(),
                                        "100.00");

                        CreatePaymentRequest request = new CreatePaymentRequest(new BigDecimal("50.00"),
                                        LocalDate.now().toString());

                        assertThatThrownBy(() -> paymentService.recordPayment(testUser.getId(), otherInvoice.getId(),
                                        installmentId, request)).isInstanceOf(NotFoundException.class);
                }

                @Test
                @DisplayName("Should throw exception when installment does not belong to invoice")
                void shouldThrowExceptionWhenInstallmentIdMismatch() {
                        Invoice invoice1 = createTestInvoice(testCustomer.getId(), "INV-004");
                        Invoice invoice2 = createTestInvoice(testCustomer.getId(), "INV-005");
                        InstallmentId installmentId2 = addInstallment(invoice2.getId(), "100.00");

                        CreatePaymentRequest request = new CreatePaymentRequest(new BigDecimal("50.00"),
                                        LocalDate.now().toString());

                        assertThatThrownBy(() -> paymentService.recordPayment(testUser.getId(), invoice1.getId(),
                                        installmentId2, request))
                                                        .isInstanceOf(InstallmentDoesNotBelongToInvoiceException.class);
                }

                @Test
                @DisplayName("Should throw exception when payment amount is zero")
                void shouldThrowExceptionWhenAmountIsZero() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-006");
                        InstallmentId installmentId = addInstallment(invoice.getId(), "100.00");

                        CreatePaymentRequest request = new CreatePaymentRequest(BigDecimal.ZERO,
                                        LocalDate.now().toString());

                        assertThatThrownBy(() -> paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        installmentId, request)).isInstanceOf(InvalidMoneyValueException.class);
                }
        }

        @Nested
        @DisplayName("Update Payment Tests")
        class UpdatePaymentTests {

                @Test
                @DisplayName("Should update payment amount successfully and recalculate totals")
                void shouldUpdatePaymentAmountSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-UPD-001");
                        InstallmentId installmentId = addInstallment(invoice.getId(), "200.00");
                        PaymentId paymentId = recordPayment(invoice.getId(), installmentId, "100.00");

                        UpdatePaymentRequest updateRequest = new UpdatePaymentRequest(new BigDecimal("150.00"), null);

                        InvoiceResponse response = paymentService.updatePayment(testUser.getId(), invoice.getId(),
                                        installmentId, paymentId, updateRequest);

                        assertThat(response.totalPaid()).isEqualByComparingTo("150.00");
                        assertThat(response.remainingAmount()).isEqualByComparingTo("50.00");

                        PaymentResponse updatedPayment = response.installments().get(0).payments().get(0);
                        assertThat(updatedPayment.amount()).isEqualByComparingTo("150.00");
                }

                @Test
                @DisplayName("Should update payment date successfully")
                void shouldUpdatePaymentDateSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-UPD-002");
                        InstallmentId installmentId = addInstallment(invoice.getId(), "200.00");
                        PaymentId paymentId = recordPayment(invoice.getId(), installmentId, "100.00");

                        String newDate = LocalDate.now().plusDays(1).toString();
                        UpdatePaymentRequest updateRequest = new UpdatePaymentRequest(null, newDate);

                        InvoiceResponse response = paymentService.updatePayment(testUser.getId(), invoice.getId(),
                                        installmentId, paymentId, updateRequest);

                        PaymentResponse updatedPayment = response.installments().get(0).payments().get(0);
                        assertThat(updatedPayment.paymentDate()).isEqualTo(newDate);
                        assertThat(updatedPayment.amount()).isEqualByComparingTo("100.00");
                }

                @Test
                @DisplayName("Should throw exception when updated amount exceeds remaining installment")
                void shouldThrowExceptionWhenUpdateExceedsRemaining() {
                        // Installment 200, Payment 100 -> Remaining 100
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-UPD-003");
                        InstallmentId installmentId = addInstallment(invoice.getId(), "200.00");
                        PaymentId paymentId = recordPayment(invoice.getId(), installmentId, "100.00");

                        // Try update to 250 (Max allowed is 200)
                        UpdatePaymentRequest updateRequest = new UpdatePaymentRequest(new BigDecimal("250.00"), null);

                        assertThatThrownBy(() -> paymentService.updatePayment(testUser.getId(), invoice.getId(),
                                        installmentId, paymentId, updateRequest))
                                                        .isInstanceOf(InvalidMoneyValueException.class)
                                                        .hasMessageContaining("exceeds remaining installment amount");
                }

                @Test
                @DisplayName("Should throw exception when updating other user's payment")
                void shouldThrowExceptionWhenUpdatingOtherUserPayment() {
                        Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-OTHER-UPD");
                        InstallmentId installmentId = addInstallmentForUser(otherUser.getId(), otherInvoice.getId(),
                                        "100.00");
                        PaymentId paymentId = recordPaymentForUser(otherUser.getId(), otherInvoice.getId(),
                                        installmentId, "50.00");

                        UpdatePaymentRequest request = new UpdatePaymentRequest(new BigDecimal("60.00"), null);

                        assertThatThrownBy(() -> paymentService.updatePayment(testUser.getId(), otherInvoice.getId(),
                                        installmentId, paymentId, request)).isInstanceOf(NotFoundException.class);
                }
        }

        @Nested
        @DisplayName("Remove Payment Tests")
        class RemovePaymentTests {

                @Test
                @DisplayName("Should remove payment successfully and revert status")
                void shouldRemovePaymentSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-REM-001");
                        InstallmentId installmentId = addInstallment(invoice.getId(), "100.00");
                        PaymentId paymentId = recordPayment(invoice.getId(), installmentId, "50.00"); // PARTIALLY_PAID

                        InvoiceResponse response = paymentService.removePayment(testUser.getId(), invoice.getId(),
                                        installmentId, paymentId);

                        assertThat(response.totalPaid()).isEqualByComparingTo("0.00");
                        assertThat(response.remainingAmount()).isEqualByComparingTo("100.00");
                        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING.name());
                        assertThat(response.installments().get(0).payments()).isEmpty();
                }

                @Test
                @DisplayName("Should throw exception when removing non-existent payment")
                void shouldThrowExceptionWhenRemovingNonExistentPayment() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-REM-002");
                        InstallmentId installmentId = addInstallment(invoice.getId(), "100.00");
                        PaymentId randomPaymentId = new PaymentId(UUID.randomUUID());

                        assertThatThrownBy(() -> paymentService.removePayment(testUser.getId(), invoice.getId(),
                                        installmentId, randomPaymentId)).isInstanceOf(
                                                        com.payhint.api.domain.shared.exception.InvalidPropertyException.class)
                                                        .hasMessageContaining("Payment not found for installment id: "
                                                                        + installmentId);
                }
        }

        @Nested
        @DisplayName("Edge Case & Invariant Tests")
        class EdgeCaseAndInvariantTests {

                @Test
                @DisplayName("Should prevent payment modification on archived invoice")
                void shouldPreventPaymentOnArchivedInvoice() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCH-001");
                        InstallmentId installmentId = addInstallment(invoice.getId(), "100.00");
                        PaymentId paymentId = recordPayment(invoice.getId(), installmentId, "50.00");

                        invoiceService.archiveInvoice(testUser.getId(), invoice.getId());

                        UpdatePaymentRequest updateRequest = new UpdatePaymentRequest(new BigDecimal("60.00"), null);

                        assertThatThrownBy(() -> paymentService.updatePayment(testUser.getId(), invoice.getId(),
                                        installmentId, paymentId, updateRequest))
                                                        .isInstanceOf(IllegalStateException.class)
                                                        .hasMessageContaining("archived");

                        CreatePaymentRequest createRequest = new CreatePaymentRequest(new BigDecimal("10.00"),
                                        LocalDate.now().toString());
                        assertThatThrownBy(() -> paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        installmentId, createRequest)).isInstanceOf(IllegalStateException.class)
                                                        .hasMessageContaining("archived");

                        assertThatThrownBy(() -> paymentService.removePayment(testUser.getId(), invoice.getId(),
                                        installmentId, paymentId)).isInstanceOf(IllegalStateException.class)
                                                        .hasMessageContaining("archived");
                }

                @Test
                @DisplayName("Should handle multiple payments totaling exact installment amount")
                void shouldHandleMultiplePaymentsForFullAmount() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-MULTI-001");
                        InstallmentId installmentId = addInstallment(invoice.getId(), "100.00");

                        recordPayment(invoice.getId(), installmentId, "30.00");
                        recordPayment(invoice.getId(), installmentId, "30.00");
                        InvoiceResponse finalResponse = paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        installmentId,
                                        new CreatePaymentRequest(new BigDecimal("40.00"), LocalDate.now().toString()));

                        assertThat(finalResponse.status()).isEqualTo(PaymentStatus.PAID.name());
                        assertThat(finalResponse.totalPaid()).isEqualByComparingTo("100.00");
                        assertThat(finalResponse.remainingAmount()).isEqualByComparingTo("0.00");
                }
        }

        @Nested
        @DisplayName("Consistency and Flow Tests")
        class ConsistencyAndFlowTests {

                @Test
                @DisplayName("Should verify consistency across all layers during payment flow")
                void shouldVerifyConsistencyAcrossAllLayers() {
                        // 1. Create Invoice
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-FLOW-001");
                        assertThat(invoice.getStatus().name()).isEqualTo("PENDING");
                        assertThat(invoice.getTotalAmount().amount()).isEqualByComparingTo("0.00");

                        // 2. Add Installment (Due in future)
                        CreateInstallmentRequest instRequest = new CreateInstallmentRequest(new BigDecimal("100.00"),
                                        LocalDate.now().plusDays(10).toString());
                        var instResponse = installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        instRequest);
                        InstallmentId installmentId = new InstallmentId(
                                        UUID.fromString(instResponse.installments().get(0).id()));

                        assertThat(instResponse.status()).isEqualTo("PENDING");
                        assertThat(instResponse.totalAmount()).isEqualByComparingTo("100.00");
                        assertThat(instResponse.isOverdue()).isFalse();

                        // 3. Add Partial Payment
                        CreatePaymentRequest payRequest1 = new CreatePaymentRequest(new BigDecimal("40.00"),
                                        LocalDate.now().toString());
                        var afterPay1 = paymentService.recordPayment(testUser.getId(), invoice.getId(), installmentId,
                                        payRequest1);

                        // Assert Invoice Layer
                        assertThat(afterPay1.status()).isEqualTo("PARTIALLY_PAID");
                        assertThat(afterPay1.totalPaid()).isEqualByComparingTo("40.00");
                        assertThat(afterPay1.remainingAmount()).isEqualByComparingTo("60.00");

                        // Assert Installment Layer
                        var installment1 = afterPay1.installments().get(0);
                        assertThat(installment1.status()).isEqualTo("PARTIALLY_PAID");
                        assertThat(installment1.amountPaid()).isEqualByComparingTo("40.00");
                        assertThat(installment1.amountDue()).isEqualByComparingTo("100.00");

                        // Assert Payment Layer
                        assertThat(installment1.payments()).hasSize(1);
                        assertThat(installment1.payments().get(0).amount()).isEqualByComparingTo("40.00");

                        // 4. Add Remaining Payment
                        CreatePaymentRequest payRequest2 = new CreatePaymentRequest(new BigDecimal("60.00"),
                                        LocalDate.now().toString());
                        var afterPay2 = paymentService.recordPayment(testUser.getId(), invoice.getId(), installmentId,
                                        payRequest2);

                        // Assert Final Consistency
                        assertThat(afterPay2.status()).isEqualTo("PAID");
                        assertThat(afterPay2.totalPaid()).isEqualByComparingTo("100.00");
                        assertThat(afterPay2.remainingAmount()).isEqualByComparingTo("0.00");
                        assertThat(afterPay2.installments().get(0).status()).isEqualTo("PAID");
                        assertThat(afterPay2.installments().get(0).amountPaid()).isEqualByComparingTo("100.00");
                        assertThat(afterPay2.installments().get(0).payments()).hasSize(2);
                }

                @Test
                @DisplayName("Should verify overdue status updates across layers")
                void shouldVerifyOverdueStatusAcrossLayers() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-FLOW-002");

                        // Add OVERDUE installment (Yesterday)
                        CreateInstallmentRequest pastInstReq = new CreateInstallmentRequest(new BigDecimal("100.00"),
                                        LocalDate.now().minusDays(1).toString());
                        var instResponse = installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        pastInstReq);
                        InstallmentId installmentId = new InstallmentId(
                                        UUID.fromString(instResponse.installments().get(0).id()));

                        // Verify Invoice is overdue
                        assertThat(instResponse.isOverdue()).isTrue();
                        assertThat(instResponse.installments().get(0).isOverdue()).isTrue();

                        // Pay it off
                        CreatePaymentRequest fullPay = new CreatePaymentRequest(new BigDecimal("100.00"),
                                        LocalDate.now().toString());
                        var afterPay = paymentService.recordPayment(testUser.getId(), invoice.getId(), installmentId,
                                        fullPay);

                        // Verify NO LONGER overdue
                        assertThat(afterPay.status()).isEqualTo("PAID");
                        assertThat(afterPay.isOverdue()).isFalse();
                        assertThat(afterPay.installments().get(0).isOverdue()).isFalse();
                }
        }

        @Nested
        @DisplayName("Concurrency Tests")
        class ConcurrencyTests {

                @Test
                @DisplayName("Should prevent double payment over total due under concurrent access")
                void shouldPreventOverpaymentConcurrent() throws Exception {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-CONC-001");
                        InstallmentId installmentId = addInstallment(invoice.getId(), "100.00");

                        // Prepare two payments of 60.00. Total 120.00 > 100.00.
                        // One should succeed, one should fail (either optimistic locking or domain
                        // validation).
                        int threads = 2;
                        ExecutorService executor = Executors.newFixedThreadPool(threads);
                        CountDownLatch startLatch = new CountDownLatch(1);
                        CountDownLatch doneLatch = new CountDownLatch(threads);
                        AtomicInteger successCount = new AtomicInteger(0);
                        AtomicInteger failCount = new AtomicInteger(0);

                        for (int i = 0; i < threads; i++) {
                                executor.submit(() -> {
                                        try {
                                                startLatch.await();
                                                paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                                                installmentId,
                                                                new CreatePaymentRequest(new BigDecimal("60.00"),
                                                                                LocalDate.now().toString()));
                                                successCount.incrementAndGet();
                                        } catch (Exception e) {
                                                // Expected: ObjectOptimisticLockingFailureException or
                                                // InvalidMoneyValueException
                                                failCount.incrementAndGet();
                                        } finally {
                                                doneLatch.countDown();
                                        }
                                });
                        }

                        startLatch.countDown();
                        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
                        executor.shutdown();

                        assertThat(finished).isTrue();
                        assertThat(successCount.get()).isEqualTo(1);
                        assertThat(failCount.get()).isEqualTo(1);

                        transactionTemplate.execute(status -> {
                                Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
                                assertThat(updatedInvoice.getTotalPaid().amount()).isEqualByComparingTo("60.00");
                                return null;
                        });
                }
        }

        private Invoice createTestInvoice(CustomerId customerId, String reference) {
                Customer customer = customerRepository.findById(customerId).orElseThrow();
                Invoice invoice = Invoice.create(new InvoiceId(UUID.randomUUID()), customer.getId(),
                                new InvoiceReference(reference), "USD");
                return invoiceRepository.save(invoice);
        }

        private InstallmentId addInstallment(InvoiceId invoiceId, String amount) {
                return addInstallmentForUser(testUser.getId(), invoiceId, amount);
        }

        private InstallmentId addInstallmentForUser(UserId userId, InvoiceId invoiceId, String amount) {
                var request = new CreateInstallmentRequest(new BigDecimal(amount),
                                LocalDate.now().plusDays(30).toString());
                var response = installmentService.addInstallment(userId, invoiceId, request);
                return new InstallmentId(UUID.fromString(response.installments().get(0).id()));
        }

        private PaymentId recordPayment(InvoiceId invoiceId, InstallmentId installmentId, String amount) {
                return recordPaymentForUser(testUser.getId(), invoiceId, installmentId, amount);
        }

        private PaymentId recordPaymentForUser(UserId userId, InvoiceId invoiceId, InstallmentId installmentId,
                        String amount) {
                var request = new CreatePaymentRequest(new BigDecimal(amount), LocalDate.now().toString());
                var response = paymentService.recordPayment(userId, invoiceId, installmentId, request);
                // Find the payment we just added (last one)
                var payments = response.installments().stream().filter(i -> i.id().equals(installmentId.toString()))
                                .findFirst().get().payments();
                String paymentIdStr = payments.get(payments.size() - 1).id();
                return new PaymentId(UUID.fromString(paymentIdStr));
        }
}