package com.payhint.api.domain.billing.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.billing.valueobject.PaymentId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;

@DisplayName("Payment Domain Model Tests")
class PaymentTest {

    private static final PaymentId VALID_PAYMENT_ID = new PaymentId(UUID.randomUUID());
    private static final Money VALID_AMOUNT = new Money(BigDecimal.valueOf(100.00));
    private static final LocalDate VALID_PAYMENT_DATE = LocalDate.now();

    @Nested
    @DisplayName("Constructor & Factory Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create payment with valid parameters using factory method")
        void shouldCreatePaymentWithValidParameters() {
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);

            assertThat(payment).isNotNull();
            assertThat(payment.getId()).isEqualTo(VALID_PAYMENT_ID);
            assertThat(payment.getAmount()).isEqualTo(VALID_AMOUNT);
            assertThat(payment.getPaymentDate()).isEqualTo(VALID_PAYMENT_DATE);
            assertThat(payment.getCreatedAt()).isNotNull();
            assertThat(payment.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create payment with all parameters using constructor")
        void shouldCreatePaymentWithAllParameters() {
            LocalDateTime now = LocalDateTime.now();
            Payment payment = new Payment(VALID_PAYMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE, now, now);

            assertThat(payment.getId()).isEqualTo(VALID_PAYMENT_ID);
            assertThat(payment.getAmount()).isEqualTo(VALID_AMOUNT);
            assertThat(payment.getPaymentDate()).isEqualTo(VALID_PAYMENT_DATE);
            assertThat(payment.getCreatedAt()).isEqualTo(now);
            assertThat(payment.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should throw exception when creating payment with null ID")
        void shouldThrowExceptionWhenIdIsNull() {
            assertThatThrownBy(
                    () -> new Payment(null, VALID_AMOUNT, VALID_PAYMENT_DATE, LocalDateTime.now(), LocalDateTime.now()))
                            .isInstanceOfAny(NullPointerException.class, InvalidPropertyException.class);
        }
    }

    @Nested
    @DisplayName("Update Details Tests")
    class UpdateDetailsTests {

        @Test
        @DisplayName("Should update amount and payment date successfully")
        void shouldUpdateAmountAndPaymentDate() {
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);
            Money newAmount = new Money(BigDecimal.valueOf(200.00));
            LocalDate newDate = VALID_PAYMENT_DATE.plusDays(1);

            payment.updateDetails(newAmount, newDate);

            assertThat(payment.getAmount()).isEqualTo(newAmount);
            assertThat(payment.getPaymentDate()).isEqualTo(newDate);
        }

        @Test
        @DisplayName("Should update updated_at timestamp when details change")
        void shouldUpdateTimestampWhenDetailsChange() throws InterruptedException {
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);
            LocalDateTime initialUpdatedAt = payment.getUpdatedAt();

            Thread.sleep(10); // Ensure time difference
            payment.updateDetails(new Money(BigDecimal.valueOf(150.00)), null);

            assertThat(payment.getUpdatedAt()).isAfter(initialUpdatedAt);
        }

        @Test
        @DisplayName("Should not update fields if values are null")
        void shouldNotUpdateFieldsIfValuesAreNull() {
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);
            LocalDateTime initialUpdatedAt = payment.getUpdatedAt();

            payment.updateDetails(null, null);

            assertThat(payment.getAmount()).isEqualTo(VALID_AMOUNT);
            assertThat(payment.getPaymentDate()).isEqualTo(VALID_PAYMENT_DATE);
            assertThat(payment.getUpdatedAt()).isEqualTo(initialUpdatedAt);
        }

        @Test
        @DisplayName("Should not update fields if values are the same")
        void shouldNotUpdateFieldsIfValuesAreSame() {
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);
            LocalDateTime initialUpdatedAt = payment.getUpdatedAt();

            payment.updateDetails(VALID_AMOUNT, VALID_PAYMENT_DATE);

            assertThat(payment.getUpdatedAt()).isEqualTo(initialUpdatedAt);
        }

        @Test
        @DisplayName("Should throw exception when updating amount to zero")
        void shouldThrowExceptionWhenUpdatingAmountToZero() {
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);

            assertThatThrownBy(() -> payment.updateDetails(Money.ZERO, null))
                    .isInstanceOf(InvalidMoneyValueException.class)
                    .hasMessage("Payment amount must be greater than zero");
        }

        @Test
        @DisplayName("Should not update when numeric amount is equal but scale differs")
        void shouldNotUpdateWhenAmountNumericallyEqualButDifferentScale() {
            Money amountWithScale1 = new Money(new BigDecimal("100.0"));
            Payment payment = Payment.create(VALID_PAYMENT_ID, amountWithScale1, VALID_PAYMENT_DATE);
            LocalDateTime originalUpdatedAt = payment.getUpdatedAt();

            Money amountWithScale2 = new Money(new BigDecimal("100.00"));

            payment.updateDetails(amountWithScale2, null);

            assertThat(payment.getAmount()).isEqualTo(amountWithScale1);
            assertThat(payment.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal based on ID")
        void shouldBeEqualBasedOnId() {
            Payment payment1 = Payment.create(VALID_PAYMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);
            Payment payment2 = Payment.create(VALID_PAYMENT_ID, new Money(BigDecimal.valueOf(500)),
                    LocalDate.now().plusDays(10));

            assertThat(payment1).isEqualTo(payment2);
            assertThat(payment1.hashCode()).isEqualTo(payment2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal if IDs differ")
        void shouldNotBeEqualIfIdsDiffer() {
            Payment payment1 = Payment.create(VALID_PAYMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);
            Payment payment2 = Payment.create(new PaymentId(UUID.randomUUID()), VALID_AMOUNT, VALID_PAYMENT_DATE);

            assertThat(payment1).isNotEqualTo(payment2);
        }
    }
}