package com.payhint.api.infrastructure.notification.persistence.jpa.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.payhint.api.application.billing.mapper.BillingValueObjectMapperImpl;
import com.payhint.api.application.shared.ValueObjectMapperImpl;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.notification.model.NotificationLog;
import com.payhint.api.domain.notification.model.NotificationStatus;
import com.payhint.api.domain.shared.valueobject.Email;
import com.payhint.api.infrastructure.notification.persistence.jpa.mapper.NotificationLogPersistenceMapperImpl;
import com.payhint.api.infrastructure.notification.persistence.jpa.repository.NotificationLogSpringRepository;

@DataJpaTest
@ActiveProfiles("test")
@Import({ NotificationLogJpaRepositoryAdapter.class, NotificationLogPersistenceMapperImpl.class,
        ValueObjectMapperImpl.class, BillingValueObjectMapperImpl.class })
@DisplayName("NotificationLogJpaRepositoryAdapter Integration Tests")
class NotificationLogJpaRepositoryAdapterIntegrationTest {

    @Autowired
    private NotificationLogJpaRepositoryAdapter adapter;

    @Autowired
    private NotificationLogSpringRepository notificationSpringRepository;

    @AfterEach
    void tearDown() {
        notificationSpringRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        notificationSpringRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save success log and return domain model")
    void shouldSaveSuccessLog() {
        InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
        Email recipient = new Email("test@example.com");
        String subject = "Test Subject";

        NotificationLog log = NotificationLog.createSuccess(installmentId, recipient, subject);

        NotificationLog savedLog = adapter.save(log);

        assertThat(savedLog).isNotNull();
        assertThat(savedLog.getId()).isNotNull();
        assertThat(savedLog.getInstallmentId()).isEqualTo(installmentId);
        assertThat(savedLog.getStatus()).isEqualTo(NotificationStatus.SENT);

        assertThat(notificationSpringRepository.findById(savedLog.getId())).isPresent();
    }

    @Test
    @DisplayName("Should save failure log with error message")
    void shouldSaveFailureLog() {
        InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
        Email recipient = new Email("fail@example.com");
        String error = "SMTP Error";

        NotificationLog log = NotificationLog.createFailure(installmentId, recipient, "Subject", error);

        NotificationLog savedLog = adapter.save(log);

        assertThat(savedLog.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(savedLog.getErrorMessage()).isEqualTo(error);
    }

    @Test
    @DisplayName("Should return true if log exists for installment")
    void shouldReturnTrueIfLogExists() {
        InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
        NotificationLog log = NotificationLog.createSuccess(installmentId, new Email("test@example.com"), "Subject");
        adapter.save(log);

        boolean exists = adapter.existsByInstallmentId(installmentId);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false if log does not exist for installment")
    void shouldReturnFalseIfLogDoesNotExist() {
        InstallmentId installmentId = new InstallmentId(UUID.randomUUID());

        boolean exists = adapter.existsByInstallmentId(installmentId);

        assertThat(exists).isFalse();
    }
}