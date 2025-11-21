package com.payhint.api.application.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.payhint.api.application.billing.dto.request.CreatePaymentRequest;
import com.payhint.api.application.billing.dto.request.UpdatePaymentRequest;
import com.payhint.api.application.shared.exception.NotFoundException;
import com.payhint.api.domain.billing.exception.InstallmentDoesNotBelongToInvoiceException;
import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
import com.payhint.api.domain.billing.model.Invoice;
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
@Transactional
@DisplayName("InvoiceService Integration Tests")
class PaymentProcessingServiceIntegrationTest {

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
        @DisplayName("Payment Management")
        class PaymentManagementTests {

                @Test
                void shouldRecordPaymentSuccessfullyAndUpdateStatus() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-REC");
                        var addRequest = new CreateInstallmentRequest(new BigDecimal("150"),
                                        LocalDate.now().plusDays(5).toString());
                        var addResponse = installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        addRequest);
                        String installmentId = addResponse.installments().get(0).id();

                        var paymentRequest = new CreatePaymentRequest(new BigDecimal("150"),
                                        LocalDate.now().toString());
                        var paymentResponse = paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), paymentRequest);

                        assertThat(paymentResponse.totalPaid()).isEqualByComparingTo("150");
                        assertThat(paymentResponse.remainingAmount()).isEqualByComparingTo("0");
                        assertThat(paymentResponse.status()).isEqualTo("PAID");
                        assertThat(paymentResponse.installments().get(0).payments()).hasSize(1);
                }

                @Test
                void shouldThrowExceptionWhenRecordingPaymentOnOtherUsersInvoice() {
                        Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-PMT-REC-OTHER");
                        var addRequest = new CreateInstallmentRequest(new BigDecimal("60"),
                                        LocalDate.now().plusDays(3).toString());
                        var addResponse = installmentService.addInstallment(otherUser.getId(), otherInvoice.getId(),
                                        addRequest);
                        String installmentId = addResponse.installments().get(0).id();
                        var paymentRequest = new CreatePaymentRequest(new BigDecimal("30"), LocalDate.now().toString());

                        assertThatThrownBy(() -> paymentService.recordPayment(testUser.getId(), otherInvoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), paymentRequest))
                                                        .isInstanceOf(NotFoundException.class);
                }

                @Test
                void shouldUpdatePaymentSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-UPD");
                        var addRequest = new CreateInstallmentRequest(new BigDecimal("200"),
                                        LocalDate.now().plusDays(11).toString());
                        var addResponse = installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        addRequest);
                        String installmentId = addResponse.installments().get(0).id();
                        var paymentRequest = new CreatePaymentRequest(new BigDecimal("80"), LocalDate.now().toString());
                        var paymentResponse = paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), paymentRequest);
                        String paymentId = paymentResponse.installments().get(0).payments().get(0).id();

                        var updatePaymentRequest = new UpdatePaymentRequest(new BigDecimal("100"),
                                        LocalDate.now().toString());
                        var updatedResponse = paymentService.updatePayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)),
                                        new PaymentId(UUID.fromString(paymentId)), updatePaymentRequest);

                        assertThat(updatedResponse.totalPaid()).isEqualByComparingTo("100");
                        assertThat(updatedResponse.remainingAmount()).isEqualByComparingTo("100");
                        assertThat(updatedResponse.status()).isEqualTo("PARTIALLY_PAID");
                }

                @Test
                void shouldThrowExceptionWhenUpdatingPaymentBeyondAllowedAmount() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-UPD-EXC");
                        var addRequest = new CreateInstallmentRequest(new BigDecimal("100"),
                                        LocalDate.now().plusDays(2).toString());
                        var addResponse = installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        addRequest);
                        String installmentId = addResponse.installments().get(0).id();
                        var paymentRequest = new CreatePaymentRequest(new BigDecimal("60"), LocalDate.now().toString());
                        var paymentResponse = paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), paymentRequest);
                        String paymentId = paymentResponse.installments().get(0).payments().get(0).id();

                        var updatePaymentRequest = new UpdatePaymentRequest(new BigDecimal("110"), null);
                        assertThatThrownBy(() -> paymentService.updatePayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)),
                                        new PaymentId(UUID.fromString(paymentId)), updatePaymentRequest))
                                                        .isInstanceOf(InvalidMoneyValueException.class);
                }

                @Test
                void shouldRemovePaymentSuccessfully() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-REM");
                        var addRequest = new CreateInstallmentRequest(new BigDecimal("140"),
                                        LocalDate.now().plusDays(13).toString());
                        var addResponse = installmentService.addInstallment(testUser.getId(), invoice.getId(),
                                        addRequest);
                        String installmentId = addResponse.installments().get(0).id();
                        var paymentRequest = new CreatePaymentRequest(new BigDecimal("50"), LocalDate.now().toString());
                        var paymentResponse = paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), paymentRequest);
                        String paymentId = paymentResponse.installments().get(0).payments().get(0).id();

                        var afterRemoval = paymentService.removePayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)),
                                        new PaymentId(UUID.fromString(paymentId)));

                        assertThat(afterRemoval.installments().get(0).payments()).isEmpty();
                        assertThat(afterRemoval.totalPaid()).isEqualByComparingTo("0");
                }

                @Test
                void shouldThrowExceptionWhenRemovingPaymentFromOtherUsersInvoice() {
                        Invoice otherInvoice = createTestInvoice(otherCustomer.getId(), "INV-PMT-REM-OTHER");
                        var addRequest = new CreateInstallmentRequest(new BigDecimal("85"),
                                        LocalDate.now().plusDays(3).toString());
                        var addResponse = installmentService.addInstallment(otherUser.getId(), otherInvoice.getId(),
                                        addRequest);
                        String installmentId = addResponse.installments().get(0).id();
                        var paymentRequest = new CreatePaymentRequest(new BigDecimal("40"), LocalDate.now().toString());
                        var paymentResponse = paymentService.recordPayment(otherUser.getId(), otherInvoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), paymentRequest);
                        String paymentId = paymentResponse.installments().get(0).payments().get(0).id();

                        assertThatThrownBy(() -> paymentService.removePayment(testUser.getId(), otherInvoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)),
                                        new PaymentId(UUID.fromString(paymentId))))
                                                        .isInstanceOf(NotFoundException.class);
                }

                @Test
                void shouldReflectStatusTransitionsAcrossPayments() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-STATE-TRANS");
                        var inst1 = new CreateInstallmentRequest(new BigDecimal("100"),
                                        LocalDate.now().plusDays(5).toString());
                        var inst2 = new CreateInstallmentRequest(new BigDecimal("200"),
                                        LocalDate.now().plusDays(6).toString());
                        var respA = installmentService.addInstallment(testUser.getId(), invoice.getId(), inst1);
                        String installment1Id = respA.installments().get(0).id();
                        var respB = installmentService.addInstallment(testUser.getId(), invoice.getId(), inst2);
                        assertThat(respB.status()).isEqualTo("PENDING");
                        String installment2Id = respB.installments().stream()
                                        .filter(i -> !i.id().equals(installment1Id)).findFirst().get().id();

                        var pay1 = new CreatePaymentRequest(new BigDecimal("100"), LocalDate.now().toString());
                        var afterFirstPayment = paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installment1Id)), pay1);
                        assertThat(afterFirstPayment.status()).isEqualTo("PARTIALLY_PAID");
                        assertThat(afterFirstPayment.totalPaid()).isEqualByComparingTo("100");
                        assertThat(afterFirstPayment.remainingAmount()).isEqualByComparingTo("200");

                        var pay2 = new CreatePaymentRequest(new BigDecimal("200"), LocalDate.now().toString());
                        var afterSecondPayment = paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installment2Id)), pay2);
                        assertThat(afterSecondPayment.status()).isEqualTo("PAID");
                        assertThat(afterSecondPayment.totalPaid()).isEqualByComparingTo("300");
                        assertThat(afterSecondPayment.remainingAmount()).isEqualByComparingTo("0");
                }
        }

        @Nested
        @DisplayName("Edge Case & Invariant Tests")
        class EdgeCaseAndInvariantTests {

                @Test
                void shouldNotRecordPaymentExceedingRemainingAmount() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-EXCEED");
                        var inst = new CreateInstallmentRequest(new BigDecimal("100"),
                                        LocalDate.now().plusDays(3).toString());
                        var resp = installmentService.addInstallment(testUser.getId(), invoice.getId(), inst);
                        String installmentId = resp.installments().get(0).id();
                        var paymentTooHigh = new CreatePaymentRequest(new BigDecimal("150"),
                                        LocalDate.now().toString());
                        assertThatThrownBy(() -> paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), paymentTooHigh))
                                                        .isInstanceOf(InvalidMoneyValueException.class);
                }

                @Test
                void shouldNotRecordPaymentWithZeroAmount() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-ZERO");
                        var inst = new CreateInstallmentRequest(new BigDecimal("50"),
                                        LocalDate.now().plusDays(3).toString());
                        var resp = installmentService.addInstallment(testUser.getId(), invoice.getId(), inst);
                        String installmentId = resp.installments().get(0).id();
                        var zeroPayment = new CreatePaymentRequest(new BigDecimal("0"), LocalDate.now().toString());
                        assertThatThrownBy(() -> paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), zeroPayment))
                                                        .isInstanceOf(InvalidMoneyValueException.class);
                }

                @Test
                void shouldNotUpdatePaymentBeyondAllowedAmount() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-UPD-EXCEED-TOTAL");
                        var inst = new CreateInstallmentRequest(new BigDecimal("120"),
                                        LocalDate.now().plusDays(4).toString());
                        var instResp = installmentService.addInstallment(testUser.getId(), invoice.getId(), inst);
                        String installmentId = instResp.installments().get(0).id();
                        var pay = new CreatePaymentRequest(new BigDecimal("60"), LocalDate.now().toString());
                        var afterPay = paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), pay);
                        String paymentId = afterPay.installments().get(0).payments().get(0).id();
                        var updateTooHigh = new UpdatePaymentRequest(new BigDecimal("200"), LocalDate.now().toString());
                        assertThatThrownBy(() -> paymentService.updatePayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)),
                                        new PaymentId(UUID.fromString(paymentId)), updateTooHigh))
                                                        .isInstanceOf(InvalidMoneyValueException.class);
                }

                @Test
                void shouldRegressStatusAfterRemovingPayment() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-PMT-REGRESS");
                        var inst = new CreateInstallmentRequest(new BigDecimal("100"),
                                        LocalDate.now().plusDays(3).toString());
                        var instResp = installmentService.addInstallment(testUser.getId(), invoice.getId(), inst);
                        String installmentId = instResp.installments().get(0).id();
                        var pay = new CreatePaymentRequest(new BigDecimal("40"), LocalDate.now().toString());
                        var afterPay = paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), pay);
                        String paymentId = afterPay.installments().get(0).payments().get(0).id();
                        assertThat(afterPay.status()).isEqualTo("PARTIALLY_PAID");
                        var afterRemoval = paymentService.removePayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)),
                                        new PaymentId(UUID.fromString(paymentId)));
                        assertThat(afterRemoval.status()).isEqualTo("PENDING");
                        assertThat(afterRemoval.totalPaid()).isEqualByComparingTo("0");
                }

                @Test
                void shouldPreventRecordingPaymentAfterArchive() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-ARCH-PMT-ADD");
                        var instReq = new CreateInstallmentRequest(new BigDecimal("30"),
                                        LocalDate.now().plusDays(5).toString());
                        var instResp = installmentService.addInstallment(testUser.getId(), invoice.getId(), instReq);
                        invoiceService.archiveInvoice(testUser.getId(), invoice.getId());
                        String installmentId = instResp.installments().get(0).id();
                        var payReq = new CreatePaymentRequest(new BigDecimal("10"), LocalDate.now().toString());
                        assertThatThrownBy(() -> paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), payReq)).isInstanceOfAny(
                                                        IllegalStateException.class,
                                                        InstallmentDoesNotBelongToInvoiceException.class);
                }

                @Test
                void shouldSequentiallyPreventOverpaymentInConcurrentSimulation() {
                        Invoice invoice = createTestInvoice(testCustomer.getId(), "INV-SEQ-OVERPAY");
                        var inst = new CreateInstallmentRequest(new BigDecimal("80"),
                                        LocalDate.now().plusDays(3).toString());
                        var resp = installmentService.addInstallment(testUser.getId(), invoice.getId(), inst);
                        String installmentId = resp.installments().get(0).id();
                        var pay1 = new CreatePaymentRequest(new BigDecimal("50"), LocalDate.now().toString());
                        paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), pay1);
                        var pay2 = new CreatePaymentRequest(new BigDecimal("40"), LocalDate.now().toString());
                        assertThatThrownBy(() -> paymentService.recordPayment(testUser.getId(), invoice.getId(),
                                        new InstallmentId(UUID.fromString(installmentId)), pay2))
                                                        .isInstanceOf(InvalidMoneyValueException.class);
                }
        }

        private Invoice createTestInvoice(CustomerId customerId, String invoiceReference) {
                Customer customer = customerRepository.findById(customerId).orElseThrow();
                Invoice invoice = Invoice.create(new InvoiceId(UUID.randomUUID()), customer.getId(),
                                new InvoiceReference(invoiceReference), "USD");
                return invoiceRepository.save(invoice);
        }
}
