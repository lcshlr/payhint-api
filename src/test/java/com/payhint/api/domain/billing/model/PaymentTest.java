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

import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.billing.valueobject.PaymentId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;

@DisplayName("Payment Domain Model Tests")
public class PaymentTest {
    private static final PaymentId VALID_PAYMENT_ID = new PaymentId(UUID.randomUUID());
    private static final InstallmentId VALID_INSTALLMENT_ID = new InstallmentId(UUID.randomUUID());
    private static final Money VALID_AMOUNT = new Money(BigDecimal.valueOf(100.00));
    private static final LocalDate VALID_PAYMENT_DATE = LocalDate.now();

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        @Test
        @DisplayName("Should create payment with valid parameters using simple constructor")
        void shouldCreatePaymentWithValidParameters() {
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);

            assertThat(payment.getInstallmentId().toString()).isEqualTo(VALID_INSTALLMENT_ID.toString());
            assertThat(payment.getAmount()).isEqualTo(VALID_AMOUNT);
            assertThat(payment.getPaymentDate()).isEqualTo(VALID_PAYMENT_DATE);
            assertThat(payment.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
            assertThat(payment.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("Should create payment with all parameters using all-args constructor")
        void shouldCreatePaymentWithAllParameters() {
            Payment payment = new Payment(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE,
                    LocalDateTime.now(), LocalDateTime.now());

            assertThat(payment.getInstallmentId().toString()).isEqualTo(VALID_INSTALLMENT_ID.toString());
            assertThat(payment.getAmount()).isEqualTo(VALID_AMOUNT);
            assertThat(payment.getPaymentDate()).isEqualTo(VALID_PAYMENT_DATE);
            assertThat(payment.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
            assertThat(payment.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("Should throw error when creating payment with null installmentId")
        void shouldThrowErrorWhenCreatingPaymentWithNullInstallmentId() {
            assertThatThrownBy(
                    () -> Payment.create(VALID_PAYMENT_ID, new InstallmentId(null), VALID_AMOUNT, VALID_PAYMENT_DATE))
                            .isInstanceOf(InvalidPropertyException.class);
        }

        @Test
        @DisplayName("Should throw error when creating payment with null amount")
        void shouldThrowErrorWhenCreatingPaymentWithNullAmount() {
            assertThatThrownBy(() -> Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, null, VALID_PAYMENT_DATE))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw error when creating payment with null paymentDate")
        void shouldThrowErrorWhenCreatingPaymentWithNullPaymentDate() {
            assertThatThrownBy(() -> Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, VALID_AMOUNT, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Update payment Tests")
    class UpdatePaymentTests {

        @Test
        @DisplayName("Should update informations and set updatedAt timestamp")
        void shouldUpdateInformationsAndSetUpdatedAtTimestamp() throws InterruptedException {
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);
            Thread.sleep(100);
            LocalDateTime originalUpdatedAt = payment.getUpdatedAt();

            LocalDate newPaymentDate = VALID_PAYMENT_DATE.plusDays(5);

            payment.updateDetails(null, newPaymentDate);

            assertThat(payment.getAmount()).isEqualTo(VALID_AMOUNT);
            assertThat(payment.getPaymentDate()).isEqualTo(newPaymentDate);
            assertThat(payment.getUpdatedAt()).isAfter(originalUpdatedAt);
        }

        @Test
        @DisplayName("Should update information multiple times with different timestamps")
        void shouldUpdateInformationMultipleTimes() throws InterruptedException {
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);
            LocalDateTime originalUpdatedAt = payment.getUpdatedAt();

            Thread.sleep(100);

            payment.updateDetails(null, VALID_PAYMENT_DATE.plusDays(5));
            LocalDateTime firstUpdate = payment.getUpdatedAt();

            Thread.sleep(100);

            // perform a different update so timestamp will change
            payment.updateDetails(null, VALID_PAYMENT_DATE.plusDays(6));
            LocalDateTime secondUpdate = payment.getUpdatedAt();

            assertThat(payment.getAmount()).isEqualTo(VALID_AMOUNT);
            // final payment date should reflect the second update (plus 6 days)
            assertThat(payment.getPaymentDate()).isEqualTo(VALID_PAYMENT_DATE.plusDays(6));
            assertThat(firstUpdate).isAfter(originalUpdatedAt);
            assertThat(secondUpdate).isAfter(firstUpdate);
        }

        @Test
        @DisplayName("Should update all information if provided")
        void shouldUpdateAllInformationIfProvided() {
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);

            Money newAmount = new Money(BigDecimal.valueOf(150.00));
            LocalDate newPaymentDate = VALID_PAYMENT_DATE.plusDays(5);

            payment.updateDetails(newAmount, newPaymentDate);

            assertThat(payment.getAmount()).isEqualTo(newAmount);
            assertThat(payment.getPaymentDate()).isEqualTo(newPaymentDate);
        }

        @Test
        @DisplayName("Should not update any information if all parameters are null")
        void shouldNotUpdateAnyInformationIfAllParametersAreNull() {
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);
            LocalDateTime originalUpdatedAt = payment.getUpdatedAt();

            payment.updateDetails(null, null);

            assertThat(payment.getAmount()).isEqualTo(VALID_AMOUNT);
            assertThat(payment.getPaymentDate()).isEqualTo(VALID_PAYMENT_DATE);
            assertThat(payment.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        }

        @Test
        @DisplayName("Should update only amount if payment date is null")
        void shouldUpdateOnlyAmountIfPaymentDateIsNull() {
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);

            Money newAmount = new Money(BigDecimal.valueOf(150.00));

            payment.updateDetails(newAmount, null);

            assertThat(payment.getAmount()).isEqualTo(newAmount);
            assertThat(payment.getPaymentDate()).isEqualTo(VALID_PAYMENT_DATE);
        }

    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {
        @Test
        @DisplayName("Should be equal when both payments have the same non-null id")
        void shouldBeEqualWhenSameId() {
            PaymentId id = new PaymentId(UUID.randomUUID());
            Payment a = new Payment(id, VALID_INSTALLMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE, LocalDateTime.now(),
                    LocalDateTime.now());
            Payment b = new Payment(id, VALID_INSTALLMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE, LocalDateTime.now(),
                    LocalDateTime.now());

            assertThat(a).isEqualTo(b);
            assertThat(a.equals(b)).isTrue();
        }

        @Test
        @DisplayName("Should not be equal when ids are different")
        void shouldNotBeEqualWhenDifferentIds() {
            Payment a = new Payment(new PaymentId(UUID.randomUUID()), VALID_INSTALLMENT_ID, VALID_AMOUNT,
                    VALID_PAYMENT_DATE, LocalDateTime.now(), LocalDateTime.now());
            Payment b = new Payment(new PaymentId(UUID.randomUUID()), VALID_INSTALLMENT_ID, VALID_AMOUNT,
                    VALID_PAYMENT_DATE, LocalDateTime.now(), LocalDateTime.now());

            assertThat(a).isNotEqualTo(b);
        }
    }

    @Nested
    @DisplayName("Business Invariants Tests")
    class BusinessInvariantsTests {
        @Test
        @DisplayName("Should maintain all properties after payment update")
        void shouldMaintainAllPropertiesAfterPaymentUpdate() {
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);
            PaymentId originalId = payment.getId();

            payment.updateDetails(new Money(BigDecimal.valueOf(200.00)), VALID_PAYMENT_DATE.plusDays(10));

            assertThat(payment.getInstallmentId()).isEqualTo(VALID_INSTALLMENT_ID);
            assertThat(payment.getId()).isEqualTo(originalId);
        }
    }

    @Nested
    @DisplayName("Additional Equality + Numeric Behavior Tests")
    class AdditionalTests {
        @Test
        @DisplayName("Should have same hashCode when equal (same non-null id)")
        void shouldHaveSameHashCodeWhenEqual() {
            PaymentId id = new PaymentId(UUID.randomUUID());
            Payment a = new Payment(id, VALID_INSTALLMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE, LocalDateTime.now(),
                    LocalDateTime.now());
            Payment b = new Payment(id, VALID_INSTALLMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE, LocalDateTime.now(),
                    LocalDateTime.now());

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when one id is null and the other non-null")
        void shouldNotBeEqualWhenOneIdNullAndOtherNonNull() {
            Payment a = Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, VALID_AMOUNT, VALID_PAYMENT_DATE);
            Payment b = new Payment(new PaymentId(UUID.randomUUID()), VALID_INSTALLMENT_ID, VALID_AMOUNT,
                    VALID_PAYMENT_DATE, LocalDateTime.now(), LocalDateTime.now());
            assertThat(a).isNotEqualTo(b);
            assertThat(b).isNotEqualTo(a);
        }

        @Test
        @DisplayName("Should not update when numeric amount equal but scale differs")
        void shouldNotUpdateWhenAmountNumericallyEqualButDifferentScale() {
            Money amountWithScale1 = new Money(new java.math.BigDecimal("100.0"));
            Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, amountWithScale1,
                    VALID_PAYMENT_DATE);
            LocalDateTime originalUpdatedAt = payment.getUpdatedAt();

            Money amountWithScale2 = new Money(new java.math.BigDecimal("100.00"));

            payment.updateDetails(amountWithScale2, null);

            assertThat(payment.getAmount()).isEqualTo(amountWithScale1);
            assertThat(payment.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        }
    }
}