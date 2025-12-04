package com.payhint.api.application.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
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
import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.CustomerRepository;
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
        private CustomerRepository customerRepository;

        @MockitoBean
        private MailRepository mailRepository;

        @MockitoBean
        private NotificationLogRepository notificationLogRepository;

        @Test
        @DisplayName("Should send email and log success when valid overdue installment")
        void shouldSendEmailAndLogSuccess() {
                InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
                InvoiceId invoiceId = new InvoiceId(UUID.randomUUID());
                UserId userId = new UserId(UUID.randomUUID());
                CustomerId customerId = new CustomerId(UUID.randomUUID());
                LocalDate dueDate = LocalDate.now().minusDays(5);

                InstallmentOverdueEvent event = new InstallmentOverdueEvent(installmentId, invoiceId, userId, dueDate);

                when(notificationLogRepository.existsByInstallmentId(eq(installmentId))).thenReturn(false);

                Installment installment = Installment.create(installmentId, new Money(BigDecimal.valueOf(100)),
                                dueDate);
                Invoice invoiceSpy = new Invoice(invoiceId, customerId, new InvoiceReference("INV-001"),
                                new Money(BigDecimal.valueOf(100)), Money.ZERO, "USD", PaymentStatus.PENDING,
                                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), false,
                                List.of(installment), 0L);

                when(invoiceRepository.findByIdAndOwner(eq(invoiceId), eq(userId))).thenReturn(Optional.of(invoiceSpy));

                User user = User.create(userId, new Email("user@example.com"), "pass", "John", "Doe");
                Customer customer = Customer.create(customerId, userId, "Customer Inc.",
                                new Email("customer@example.com"));
                when(userRepository.findById(eq(userId))).thenReturn(Optional.of(user));
                when(customerRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));

                listener.handle(event);

                verify(mailRepository, timeout(2000)).sendEmail(eq("user@example.com"), anyString(), anyString());

                verify(notificationLogRepository, timeout(2000))
                                .save(argThat(log -> log.getStatus() == NotificationStatus.SENT
                                                && log.getInstallmentId().equals(installmentId)));
        }

        @Test
        @DisplayName("Should skip if notification already exists")
        void shouldSkipIfAlreadyNotified() {
                InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
                InstallmentOverdueEvent event = new InstallmentOverdueEvent(installmentId,
                                new InvoiceId(UUID.randomUUID()), new UserId(UUID.randomUUID()), LocalDate.now());

                when(notificationLogRepository.existsByInstallmentId(eq(installmentId))).thenReturn(true);

                listener.handle(event);

                verify(invoiceRepository, after(500).never()).findByIdAndOwner(any(), any());
                verify(mailRepository, never()).sendEmail(any(), any(), any());
        }

        @Test
        @DisplayName("Should skip if invoice not found")
        void shouldSkipIfInvoiceNotFound() {
                InstallmentOverdueEvent event = new InstallmentOverdueEvent(new InstallmentId(UUID.randomUUID()),
                                new InvoiceId(UUID.randomUUID()), new UserId(UUID.randomUUID()), LocalDate.now());

                when(notificationLogRepository.existsByInstallmentId(any())).thenReturn(false);
                when(invoiceRepository.findByIdAndOwner(any(), any())).thenReturn(Optional.empty());

                listener.handle(event);

                verify(mailRepository, after(500).never()).sendEmail(any(), any(), any());
        }

        @Test
        @DisplayName("Should log failure if email sending fails")
        void shouldLogFailureOnMailException() {
                InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
                InvoiceId invoiceId = new InvoiceId(UUID.randomUUID());
                UserId userId = new UserId(UUID.randomUUID());
                CustomerId customerId = new CustomerId(UUID.randomUUID());
                LocalDate dueDate = LocalDate.now().minusDays(5);

                InstallmentOverdueEvent event = new InstallmentOverdueEvent(installmentId, invoiceId, userId, dueDate);

                when(notificationLogRepository.existsByInstallmentId(eq(installmentId))).thenReturn(false);

                Installment installment = Installment.create(installmentId, new Money(BigDecimal.valueOf(100)),
                                dueDate);
                Invoice invoiceSpy = new Invoice(invoiceId, customerId, new InvoiceReference("INV-001"),
                                new Money(BigDecimal.valueOf(100)), Money.ZERO, "USD", PaymentStatus.PENDING,
                                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), false,
                                List.of(installment), 0L);

                when(invoiceRepository.findByIdAndOwner(eq(invoiceId), eq(userId))).thenReturn(Optional.of(invoiceSpy));

                User user = User.create(userId, new Email("user@example.com"), "pass", "John", "Doe");
                Customer customer = Customer.create(customerId, userId, "Customer Inc.",
                                new Email("contact@customerinc.com"));
                when(userRepository.findById(eq(userId))).thenReturn(Optional.of(user));
                when(customerRepository.findById(eq(customer.getId()))).thenReturn(Optional.of(customer));

                doThrow(new RuntimeException("Mail server down")).when(mailRepository).sendEmail(anyString(),
                                anyString(), anyString());

                listener.handle(event);

                verify(notificationLogRepository, timeout(2000))
                                .save(argThat(log -> log.getStatus() == NotificationStatus.FAILED
                                                && log.getErrorMessage().equals("Mail server down")));
        }
}