package com.payhint.api.infrastructure.notification.persistence.jpa.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.payhint.api.application.notification.dto.OverdueInstallmentDto;
import com.payhint.api.domain.billing.model.PaymentStatus;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InstallmentJpaEntity;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InvoiceJpaEntity;
import com.payhint.api.infrastructure.billing.persistence.jpa.repository.InvoiceSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.entity.CustomerJpaEntity;
import com.payhint.api.infrastructure.crm.persistence.jpa.entity.UserJpaEntity;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.CustomerSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.UserSpringRepository;
import com.payhint.api.infrastructure.notification.persistence.jpa.entity.NotificationLogJpaEntity;
import com.payhint.api.infrastructure.notification.persistence.jpa.repository.NotificationLogSpringRepository;

@DataJpaTest
@ActiveProfiles("test")
@Import(OverdueInstallmentJpaAdapter.class)
@DisplayName("OverdueInstallmentJpaAdapter Integration Tests")
class OverdueInstallmentJpaAdapterIntegrationTest {

    @Autowired
    private OverdueInstallmentJpaAdapter adapter;

    @Autowired
    private InvoiceSpringRepository invoiceRepository;

    @Autowired
    private UserSpringRepository userRepository;

    @Autowired
    private CustomerSpringRepository customerRepository;

    @Autowired
    private NotificationLogSpringRepository notificationLogRepository;

    private UserJpaEntity testUser;
    private CustomerJpaEntity testCustomer;

    private void cleanUpDatabase() {
        notificationLogRepository.deleteAll();
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        cleanUpDatabase();
    }

    @BeforeEach
    void setUp() {
        cleanUpDatabase();

        testUser = UserJpaEntity.builder().id(UUID.randomUUID()).email("test@example.com").password("pass")
                .firstName("Test").lastName("User").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(testUser);

        testCustomer = CustomerJpaEntity.builder().id(UUID.randomUUID()).user(testUser).companyName("Test Co")
                .contactEmail("contact@testco.com").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        customerRepository.save(testCustomer);
    }

    @Test
    @DisplayName("Should find overdue installment not notified")
    void shouldFindOverdueInstallmentNotNotified() {
        createInvoiceWithInstallment(LocalDate.now().minusDays(5), PaymentStatus.PENDING.name());

        List<OverdueInstallmentDto> result = adapter.listOverdueInstallmentsNotNotified();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).dueDate()).isEqualTo(LocalDate.now().minusDays(5));
        assertThat(result.get(0).userId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should NOT find installment if future due date")
    void shouldNotFindFutureInstallment() {
        createInvoiceWithInstallment(LocalDate.now().plusDays(5), PaymentStatus.PENDING.name());

        List<OverdueInstallmentDto> result = adapter.listOverdueInstallmentsNotNotified();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should NOT find installment if paid")
    void shouldNotFindPaidInstallment() {
        createInvoiceWithInstallment(LocalDate.now().minusDays(5), PaymentStatus.PAID.name());

        List<OverdueInstallmentDto> result = adapter.listOverdueInstallmentsNotNotified();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should NOT find installment if already notified")
    void shouldNotFindNotifiedInstallment() {
        InvoiceJpaEntity invoice = createInvoiceWithInstallment(LocalDate.now().minusDays(5),
                PaymentStatus.PENDING.name());
        UUID installmentId = invoice.getInstallments().iterator().next().getId();

        NotificationLogJpaEntity log = NotificationLogJpaEntity.builder().id(UUID.randomUUID())
                .installmentId(installmentId).recipientAddress("test@example.com").status("SENT")
                .sentAt(LocalDateTime.now()).build();
        notificationLogRepository.save(log);

        List<OverdueInstallmentDto> result = adapter.listOverdueInstallmentsNotNotified();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find only relevant installments among many")
    void shouldFilterCorrectly() {
        // 1. Overdue, Pending -> MATCH
        createInvoiceWithInstallment(LocalDate.now().minusDays(10), "PENDING");

        // 2. Overdue, Paid -> SKIP
        createInvoiceWithInstallment(LocalDate.now().minusDays(10), "PAID");

        // 3. Future, Pending -> SKIP
        createInvoiceWithInstallment(LocalDate.now().plusDays(10), "PENDING");

        // 4. Overdue, Pending, Notified -> SKIP
        InvoiceJpaEntity i4 = createInvoiceWithInstallment(LocalDate.now().minusDays(5), "PENDING");
        UUID instId4 = i4.getInstallments().iterator().next().getId();
        notificationLogRepository.save(NotificationLogJpaEntity.builder().id(UUID.randomUUID()).installmentId(instId4)
                .status("SENT").recipientAddress("a").sentAt(LocalDateTime.now()).build());

        List<OverdueInstallmentDto> result = adapter.listOverdueInstallmentsNotNotified();

        assertThat(result).hasSize(1);
    }

    private InvoiceJpaEntity createInvoiceWithInstallment(LocalDate dueDate, String status) {
        InvoiceJpaEntity invoice = InvoiceJpaEntity.builder().id(UUID.randomUUID()).customer(testCustomer)
                .invoiceReference("INV-" + UUID.randomUUID()).currency("USD").totalAmount(BigDecimal.valueOf(100))
                .totalPaid(BigDecimal.ZERO).status(status.equals("PAID") ? "PAID" : "PENDING")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).lastStatusChangeAt(LocalDateTime.now())
                .build();

        InstallmentJpaEntity installment = InstallmentJpaEntity.builder().id(UUID.randomUUID()).invoice(invoice)
                .amountDue(BigDecimal.valueOf(100)).amountPaid(BigDecimal.ZERO).dueDate(dueDate).status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).lastStatusChangeAt(LocalDateTime.now())
                .build();

        invoice.addInstallment(installment);
        return invoiceRepository.save(invoice);
    }
}