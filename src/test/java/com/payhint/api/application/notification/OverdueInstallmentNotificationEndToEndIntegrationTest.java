package com.payhint.api.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.payhint.api.domain.notification.model.NotificationStatus;
import com.payhint.api.domain.notification.repository.NotificationLogRepository;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InstallmentJpaEntity;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InvoiceJpaEntity;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.PaymentJpaEntity;
import com.payhint.api.infrastructure.billing.persistence.jpa.repository.InvoiceSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.entity.CustomerJpaEntity;
import com.payhint.api.infrastructure.crm.persistence.jpa.entity.UserJpaEntity;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.CustomerSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.UserSpringRepository;
import com.payhint.api.infrastructure.notification.persistence.jpa.repository.NotificationLogSpringRepository;
import com.payhint.api.infrastructure.notification.scheduler.OverdueInstallmentScheduler;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Overdue Installment Notification End-to-End Tests")
class OverdueInstallmentNotificationEndToEndIntegrationTest {

    @Autowired
    private OverdueInstallmentScheduler scheduler;

    @Autowired
    private InvoiceSpringRepository invoiceRepository;

    @Autowired
    private UserSpringRepository userRepository;

    @Autowired
    private CustomerSpringRepository customerRepository;

    @Autowired
    private NotificationLogSpringRepository notificationLogRepository;

    @MockitoSpyBean
    private NotificationLogRepository domainLogRepository;

    @MockitoBean
    private JavaMailSender javaMailSender;

    private UserJpaEntity testUser;
    private CustomerJpaEntity testCustomer;

    @BeforeEach
    void setUp() {
        notificationLogRepository.deleteAll();
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        testUser = UserJpaEntity.builder().id(UUID.randomUUID()).email("e2e.test@payhint.com").password("Password123!")
                .firstName("EndToEnd").lastName("Tester").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(testUser);

        testCustomer = CustomerJpaEntity.builder().id(UUID.randomUUID()).user(testUser).companyName("E2E Corp")
                .contactEmail("billing@e2ecorp.com").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        customerRepository.save(testCustomer);
    }

    @Test
    @DisplayName("Should detect overdue installment, send email, and log notification")
    void shouldHandleOverdueNotificationFlow() {
        LocalDate dueDate = LocalDate.now().minusDays(5);
        createInvoiceWithInstallment(dueDate, "PENDING", new BigDecimal("100.00")); // overdue installment

        scheduler.checkOverdueInstallments();

        verify(javaMailSender, timeout(5000)).send(any(SimpleMailMessage.class));

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var logs = notificationLogRepository.findAll();
            assertThat(logs).hasSize(1);

            var log = logs.get(0);
            assertThat(log.getRecipientAddress()).isEqualTo("e2e.test@payhint.com");
            assertThat(log.getStatus()).isEqualTo(NotificationStatus.SENT.name());
        });
    }

    @Test
    @DisplayName("Should NOT send email if installment is effectively paid but status is stale")
    void shouldNotSendEmailIfPaidButStatusStale() {
        LocalDate dueDate = LocalDate.now().minusDays(5);

        // create invoice with installment with stale PENDING status whereas payment
        // covers it

        InvoiceJpaEntity invoice = createInvoiceWithInstallment(dueDate, "PENDING", new BigDecimal("100.00"));

        InstallmentJpaEntity installment = invoice.getInstallments().iterator().next();

        PaymentJpaEntity payment = PaymentJpaEntity.builder().id(UUID.randomUUID()).installment(installment)
                .amount(new BigDecimal("100.00")).paymentDate(LocalDate.now()).createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build();

        installment.addPayment(payment);
        invoiceRepository.save(invoice);

        scheduler.checkOverdueInstallments();

        verify(javaMailSender, timeout(1000).times(0)).send(any(SimpleMailMessage.class));
        assertThat(notificationLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should NOT send email if due date is in the future")
    void shouldNotSendEmailIfFuture() {
        createInvoiceWithInstallment(LocalDate.now().plusDays(5), "PENDING", new BigDecimal("100.00"));

        scheduler.checkOverdueInstallments();

        verify(javaMailSender, timeout(1000).times(0)).send(any(SimpleMailMessage.class));
        assertThat(notificationLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should NOT send duplicate email for the same overdue installment")
    void shouldNotSendDuplicateEmailForSameOverdueInstallment() {
        LocalDate dueDate = LocalDate.now().minusDays(10);
        createInvoiceWithInstallment(dueDate, "PENDING", new BigDecimal("150.00")); // overdue installment

        // First run
        scheduler.checkOverdueInstallments();

        verify(javaMailSender, timeout(5000)).send(any(SimpleMailMessage.class));

        // Second run - should not send another email
        scheduler.checkOverdueInstallments();
        verify(javaMailSender, timeout(2000).times(1)).send(any(SimpleMailMessage.class)); // still only 1 email sent

        var notificationLogs = notificationLogRepository.findAll();
        assertThat(notificationLogs).hasSize(1);
    }

    @Test
    @DisplayName("Should handle missing invoice gracefully without sending email")
    void shouldHandleMissingInvoiceGracefully() {
        LocalDate dueDate = LocalDate.now().minusDays(7);
        InvoiceJpaEntity invoice = createInvoiceWithInstallment(dueDate, "PENDING", new BigDecimal("200.00"));

        invoiceRepository.delete(invoice);

        scheduler.checkOverdueInstallments();

        verify(javaMailSender, timeout(2000).times(0)).send(any(SimpleMailMessage.class));
        assertThat(notificationLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should log failure if email sending fails")
    void shouldLogFailureIfEmailSendingFails() {
        LocalDate dueDate = LocalDate.now().minusDays(3);
        createInvoiceWithInstallment(dueDate, "PENDING", new BigDecimal("120.00"));

        doThrow(new RuntimeException("SMTP server not reachable")).when(javaMailSender)
                .send(any(SimpleMailMessage.class));

        scheduler.checkOverdueInstallments();

        verify(javaMailSender, timeout(5000)).send(any(SimpleMailMessage.class));

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var logs = notificationLogRepository.findAll();
            assertThat(logs).hasSize(1);

            var log = logs.get(0);
            assertThat(log.getStatus()).isEqualTo(NotificationStatus.FAILED.name());
            assertThat(log.getErrorMessage()).contains("SMTP server not reachable");
        });
    }

    @Test
    @DisplayName("Should NOT send email if installment is paid after notification event is triggered but before processing")
    void shouldNotSendEmailIfPaidAfterEventButBeforeProcessing() throws InterruptedException {

        // overdue installment
        LocalDate dueDate = LocalDate.now().minusDays(4);
        InvoiceJpaEntity invoice = createInvoiceWithInstallment(dueDate, "PENDING", new BigDecimal("130.00"));

        CountDownLatch listenerStartedLatch = new CountDownLatch(1);
        CountDownLatch paymentAddedLatch = new CountDownLatch(1);

        doAnswer(invocation -> {
            listenerStartedLatch.countDown();

            if (!paymentAddedLatch.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Test timed out waiting for payment update");
            }

            return invocation.callRealMethod();
        }).when(domainLogRepository).existsByInstallmentId(any());

        scheduler.checkOverdueInstallments();

        boolean listenerIsPaused = listenerStartedLatch.await(2, TimeUnit.SECONDS);
        assertThat(listenerIsPaused).as("Listener should have started").isTrue();

        InstallmentJpaEntity installmentEntity = invoice.getInstallments().iterator().next();

        PaymentJpaEntity payment = PaymentJpaEntity.builder().id(UUID.randomUUID()).installment(installmentEntity)
                .amount(new BigDecimal("130.00")).paymentDate(LocalDate.now()).createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build();

        installmentEntity.addPayment(payment);
        invoice.setNew(false);
        invoiceRepository.saveAndFlush(invoice);

        paymentAddedLatch.countDown();

        verify(javaMailSender, timeout(2000).times(0)).send(any(SimpleMailMessage.class));

        assertThat(notificationLogRepository.findAll()).isEmpty();
    }

    private InvoiceJpaEntity createInvoiceWithInstallment(LocalDate dueDate, String status, BigDecimal amount) {
        InvoiceJpaEntity invoice = InvoiceJpaEntity.builder().id(UUID.randomUUID()).customer(testCustomer)
                .invoiceReference("INV-" + UUID.randomUUID()).currency("USD").totalAmount(amount)
                .totalPaid(BigDecimal.ZERO).status(status).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .lastStatusChangeAt(LocalDateTime.now()).build();

        InstallmentJpaEntity installment = InstallmentJpaEntity.builder().id(UUID.randomUUID()).invoice(invoice)
                .amountDue(amount).amountPaid(BigDecimal.ZERO).dueDate(dueDate).status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).lastStatusChangeAt(LocalDateTime.now())
                .build();

        invoice.addInstallment(installment);
        return invoiceRepository.save(invoice);
    }
}