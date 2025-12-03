package com.payhint.api.application.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.payhint.api.application.notification.repository.MailRepository;
import com.payhint.api.domain.billing.event.InstallmentOverdueEvent;
import com.payhint.api.domain.billing.model.Installment;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.model.PaymentStatus;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.domain.notification.model.NotificationStatus;
import com.payhint.api.domain.notification.repository.NotificationLogRepository;
import com.payhint.api.domain.shared.valueobject.Email;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("OverdueInstallmentListener Integration Tests")
class OverdueInstallmentListenerIntegrationTest {

    @Autowired
    private OverdueInstallmentListener listener;

    @MockitoBean
    private InvoiceRepository invoiceRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private MailRepository emailRepository;

    @MockitoBean
    private NotificationLogRepository notificationLogRepository;

    @Test
    @DisplayName("Should send email and log success when valid overdue installment")
    void shouldSendEmailAndLogSuccess() {
        InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
        InvoiceId invoiceId = new InvoiceId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());
        LocalDate dueDate = LocalDate.now().minusDays(5);

        InstallmentOverdueEvent event = new InstallmentOverdueEvent(installmentId, invoiceId, userId, dueDate);

        when(notificationLogRepository.existsByInstallmentId(installmentId)).thenReturn(false);

        Installment installment = Installment.create(installmentId, new Money(BigDecimal.valueOf(100)), dueDate);

        Invoice invoiceSpy = new Invoice(invoiceId, new CustomerId(UUID.randomUUID()), new InvoiceReference("INV-001"),
                new Money(BigDecimal.valueOf(100)), Money.ZERO, "USD", PaymentStatus.PENDING, LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now(), false, List.of(installment), 0L);

        when(invoiceRepository.findByIdAndOwner(invoiceId, userId)).thenReturn(Optional.of(invoiceSpy));

        User user = User.create(userId, new Email("user@example.com"), "pass", "John", "Doe");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        listener.handle(event);

        verify(emailRepository).sendEmail(eq("user@example.com"), anyString(), anyString());
        verify(notificationLogRepository).save(ArgumentMatchers.argThat(
                log -> log.getStatus() == NotificationStatus.SENT && log.getInstallmentId().equals(installmentId)
                        && log.getRecipientAddress().value().equals("user@example.com")));
    }

    @Test
    @DisplayName("Should skip if notification already exists")
    void shouldSkipIfAlreadyNotified() {
        InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
        InstallmentOverdueEvent event = new InstallmentOverdueEvent(installmentId, new InvoiceId(UUID.randomUUID()),
                new UserId(UUID.randomUUID()), LocalDate.now());

        when(notificationLogRepository.existsByInstallmentId(installmentId)).thenReturn(true);

        listener.handle(event);

        verify(invoiceRepository, never()).findByIdAndOwner(any(), any());
        verify(emailRepository, never()).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Should skip if invoice not found")
    void shouldSkipIfInvoiceNotFound() {
        InstallmentOverdueEvent event = new InstallmentOverdueEvent(new InstallmentId(UUID.randomUUID()),
                new InvoiceId(UUID.randomUUID()), new UserId(UUID.randomUUID()), LocalDate.now());

        when(notificationLogRepository.existsByInstallmentId(any())).thenReturn(false);
        when(invoiceRepository.findByIdAndOwner(any(), any())).thenReturn(Optional.empty());

        listener.handle(event);

        verify(emailRepository, never()).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Should skip if installment is actually paid")
    void shouldSkipIfPaid() {
        InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
        InvoiceId invoiceId = new InvoiceId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());

        InstallmentOverdueEvent event = new InstallmentOverdueEvent(installmentId, invoiceId, userId, LocalDate.now());

        // Create paid installment
        Installment paidInstallment = new Installment(installmentId, new Money(BigDecimal.TEN),
                new Money(BigDecimal.TEN), LocalDate.now(), PaymentStatus.PAID, LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now(), List.of());

        Invoice invoiceSpy = new Invoice(invoiceId, new CustomerId(UUID.randomUUID()), new InvoiceReference("INV-001"),
                new Money(BigDecimal.TEN), new Money(BigDecimal.TEN), "USD", PaymentStatus.PAID, LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now(), false, List.of(paidInstallment), 0L);

        when(notificationLogRepository.existsByInstallmentId(installmentId)).thenReturn(false);
        when(invoiceRepository.findByIdAndOwner(invoiceId, userId)).thenReturn(Optional.of(invoiceSpy));

        listener.handle(event);

        verify(emailRepository, never()).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Should log failure if email sending fails")
    void shouldLogFailureOnMailException() {
        InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
        InvoiceId invoiceId = new InvoiceId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());
        LocalDate dueDate = LocalDate.now().minusDays(5);

        InstallmentOverdueEvent event = new InstallmentOverdueEvent(installmentId, invoiceId, userId, dueDate);

        when(notificationLogRepository.existsByInstallmentId(installmentId)).thenReturn(false);

        Installment installment = Installment.create(installmentId, new Money(BigDecimal.valueOf(100)), dueDate);
        Invoice invoiceSpy = new Invoice(invoiceId, new CustomerId(UUID.randomUUID()), new InvoiceReference("INV-001"),
                new Money(BigDecimal.valueOf(100)), Money.ZERO, "USD", PaymentStatus.PENDING, LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now(), false, List.of(installment), 0L);

        when(invoiceRepository.findByIdAndOwner(invoiceId, userId)).thenReturn(Optional.of(invoiceSpy));

        User user = User.create(userId, new Email("user@example.com"), "pass", "John", "Doe");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Mockito.doThrow(new RuntimeException("Mail server down")).when(emailRepository).sendEmail(anyString(),
                anyString(), anyString());

        listener.handle(event);

        verify(notificationLogRepository)
                .save(ArgumentMatchers.argThat(log -> log.getStatus() == NotificationStatus.FAILED
                        && log.getErrorMessage().equals("Mail server down")));
    }
}