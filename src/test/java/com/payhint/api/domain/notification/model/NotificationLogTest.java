package com.payhint.api.domain.notification.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;
import com.payhint.api.domain.shared.valueobject.Email;

@DisplayName("NotificationLog Domain Model Tests")
class NotificationLogTest {

    private static final InstallmentId VALID_INSTALLMENT_ID = new InstallmentId(UUID.randomUUID());
    private static final Email VALID_RECIPIENT = new Email("user@example.com");
    private static final String VALID_SUBJECT = "Payment Overdue";
    private static final String ERROR_MESSAGE = "SMTP Connection Timeout";

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create success log with valid parameters")
        void shouldCreateSuccessLog() {
            NotificationLog log = NotificationLog.createSuccess(VALID_INSTALLMENT_ID, VALID_RECIPIENT, VALID_SUBJECT);

            assertThat(log).isNotNull();
            assertThat(log.getId()).isNotNull();
            assertThat(log.getInstallmentId()).isEqualTo(VALID_INSTALLMENT_ID);
            assertThat(log.getRecipientAddress()).isEqualTo(VALID_RECIPIENT);
            assertThat(log.getSubject()).isEqualTo(VALID_SUBJECT);
            assertThat(log.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(log.getErrorMessage()).isNull();
            assertThat(log.getSentAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create failure log with valid parameters")
        void shouldCreateFailureLog() {
            NotificationLog log = NotificationLog.createFailure(VALID_INSTALLMENT_ID, VALID_RECIPIENT, VALID_SUBJECT,
                    ERROR_MESSAGE);

            assertThat(log).isNotNull();
            assertThat(log.getId()).isNotNull();
            assertThat(log.getInstallmentId()).isEqualTo(VALID_INSTALLMENT_ID);
            assertThat(log.getRecipientAddress()).isEqualTo(VALID_RECIPIENT);
            assertThat(log.getSubject()).isEqualTo(VALID_SUBJECT);
            assertThat(log.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(log.getErrorMessage()).isEqualTo(ERROR_MESSAGE);
            assertThat(log.getSentAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception when creating success log with null InstallmentId")
        void shouldThrowOnNullInstallmentIdSuccess() {
            assertThatThrownBy(() -> NotificationLog.createSuccess(null, VALID_RECIPIENT, VALID_SUBJECT))
                    // Depending on if Lombok/NonNull throws NPE or IllegalArgumentException
                    .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception when creating success log with null Recipient")
        void shouldThrowOnNullRecipientSuccess() {
            assertThatThrownBy(() -> NotificationLog.createSuccess(VALID_INSTALLMENT_ID, null, VALID_SUBJECT))
                    .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   ", "\t", "\n" })
        @DisplayName("Should throw exception when subject is null or blank (Success)")
        void shouldThrowOnInvalidSubjectSuccess(String invalidSubject) {
            assertThatThrownBy(
                    () -> NotificationLog.createSuccess(VALID_INSTALLMENT_ID, VALID_RECIPIENT, invalidSubject))
                            .isInstanceOf(InvalidPropertyException.class)
                            .hasMessageContaining("Subject must be provided");
        }

        @Test
        @DisplayName("Should throw exception when creating failure log with null InstallmentId")
        void shouldThrowOnNullInstallmentIdFailure() {
            assertThatThrownBy(() -> NotificationLog.createFailure(null, VALID_RECIPIENT, VALID_SUBJECT, ERROR_MESSAGE))
                    .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception when creating failure log with null Recipient")
        void shouldThrowOnNullRecipientFailure() {
            assertThatThrownBy(
                    () -> NotificationLog.createFailure(VALID_INSTALLMENT_ID, null, VALID_SUBJECT, ERROR_MESSAGE))
                            .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   ", "\t", "\n" })
        @DisplayName("Should throw exception when subject is null or blank (Failure)")
        void shouldThrowOnInvalidSubjectFailure(String invalidSubject) {
            assertThatThrownBy(() -> NotificationLog.createFailure(VALID_INSTALLMENT_ID, VALID_RECIPIENT,
                    invalidSubject, ERROR_MESSAGE)).isInstanceOf(InvalidPropertyException.class)
                            .hasMessageContaining("Subject must be provided");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   ", "\t", "\n" })
        @DisplayName("Should throw exception when error message is null or blank for FAILED status")
        void shouldThrowOnInvalidErrorMessageFailure(String invalidError) {
            assertThatThrownBy(() -> NotificationLog.createFailure(VALID_INSTALLMENT_ID, VALID_RECIPIENT, VALID_SUBJECT,
                    invalidError)).isInstanceOf(InvalidPropertyException.class)
                            .hasMessageContaining("Error message must be provided");
        }
    }
}