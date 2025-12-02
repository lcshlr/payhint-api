package com.payhint.api.domain.billing.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.billing.valueobject.PaymentId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;

@DisplayName("Installment Domain Model Tests")
class InstallmentTest {

        private static final InstallmentId VALID_INSTALLMENT_ID = new InstallmentId(UUID.randomUUID());
        private static final Money VALID_AMOUNT_DUE = new Money(BigDecimal.valueOf(200.00));
        private static final LocalDate VALID_DUE_DATE = LocalDate.now().plusDays(30);

        private static final PaymentId VALID_PAYMENT_ID = new PaymentId(UUID.randomUUID());
        private static final Money VALID_PAYMENT_AMOUNT = new Money(BigDecimal.valueOf(100.00));
        private static final LocalDate VALID_PAYMENT_DATE = LocalDate.now();

        private Installment installment;
        private Payment payment;

        @BeforeEach
        void setUp() {
                installment = Installment.create(VALID_INSTALLMENT_ID, VALID_AMOUNT_DUE, VALID_DUE_DATE);
                payment = Payment.create(VALID_PAYMENT_ID, VALID_PAYMENT_AMOUNT, VALID_PAYMENT_DATE);
        }

        @Nested
        @DisplayName("Constructor & Factory Tests")
        class ConstructorTests {

                @Test
                @DisplayName("Should create installment with valid parameters using factory")
                void shouldCreateInstallmentWithValidParameters() {
                        assertThat(installment.getId()).isEqualTo(VALID_INSTALLMENT_ID);
                        assertThat(installment.getAmountDue()).isEqualTo(VALID_AMOUNT_DUE);
                        assertThat(installment.getAmountPaid()).isEqualTo(Money.ZERO);
                        assertThat(installment.getDueDate()).isEqualTo(VALID_DUE_DATE);
                        assertThat(installment.getStatus()).isEqualTo(PaymentStatus.PENDING);
                        assertThat(installment.getPayments()).isEmpty();
                        assertThat(installment.getCreatedAt()).isNotNull();
                        assertThat(installment.getUpdatedAt()).isNotNull();
                }

                @Test
                @DisplayName("Should create installment with all parameters using constructor")
                void shouldCreateInstallmentWithAllParameters() {
                        LocalDateTime now = LocalDateTime.now();
                        Installment customInstallment = new Installment(VALID_INSTALLMENT_ID, VALID_AMOUNT_DUE,
                                        Money.ZERO, VALID_DUE_DATE, PaymentStatus.PENDING, now, now, now,
                                        new ArrayList<>());

                        assertThat(customInstallment.getId()).isEqualTo(VALID_INSTALLMENT_ID);
                        assertThat(customInstallment.getAmountDue()).isEqualTo(VALID_AMOUNT_DUE);
                        assertThat(customInstallment.getCreatedAt()).isEqualTo(now);
                }

                @Test
                @DisplayName("Should throw exception when creating installment with null ID")
                void shouldThrowExceptionWhenIdIsNull() {
                        assertThatThrownBy(() -> new Installment(null, VALID_AMOUNT_DUE, Money.ZERO, VALID_DUE_DATE,
                                        PaymentStatus.PENDING, LocalDateTime.now(), LocalDateTime.now(),
                                        LocalDateTime.now(), new ArrayList<>())).isInstanceOfAny(
                                                        NullPointerException.class, InvalidPropertyException.class);
                }
        }

        @Nested
        @DisplayName("Update Details Tests")
        class UpdateDetailsTests {

                @Test
                @DisplayName("Should update amount due and due date successfully")
                void shouldUpdateAmountDueAndDueDate() {
                        Money newAmount = new Money(BigDecimal.valueOf(300.00));
                        LocalDate newDate = VALID_DUE_DATE.plusDays(10);

                        installment.updateDetails(newAmount, newDate);

                        assertThat(installment.getAmountDue()).isEqualTo(newAmount);
                        assertThat(installment.getDueDate()).isEqualTo(newDate);
                }

                @Test
                @DisplayName("Should update status when amount due changes")
                void shouldUpdateStatusWhenAmountDueChanges() {
                        // Pay 100 out of 200 -> Partially Paid
                        installment.addPayment(payment);
                        assertThat(installment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);

                        // Lower amount due to 100 -> Should become Paid
                        installment.updateDetails(VALID_PAYMENT_AMOUNT, null);
                        assertThat(installment.getStatus()).isEqualTo(PaymentStatus.PAID);
                }

                @Test
                @DisplayName("Should throw exception when updating amount due to zero or negative")
                void shouldThrowExceptionWhenAmountDueIsInvalid() {
                        assertThatThrownBy(() -> installment.updateDetails(Money.ZERO, null))
                                        .isInstanceOf(InvalidMoneyValueException.class)
                                        .hasMessageContaining("greater than zero");
                }

                @Test
                @DisplayName("Should throw exception when new amount due is less than amount paid")
                void shouldThrowExceptionWhenAmountDueLessThanPaid() {
                        installment.addPayment(payment); // Paid 100

                        Money invalidAmount = new Money(BigDecimal.valueOf(50.00));

                        assertThatThrownBy(() -> installment.updateDetails(invalidAmount, null))
                                        .isInstanceOf(InvalidMoneyValueException.class)
                                        .hasMessageContaining("cannot be less than amountPaid");
                }

                @Test
                @DisplayName("Should update updated_at timestamp when details change")
                void shouldUpdateTimestampWhenDetailsChange() throws InterruptedException {
                        LocalDateTime initialUpdatedAt = installment.getUpdatedAt();
                        Thread.sleep(10);

                        installment.updateDetails(new Money(BigDecimal.valueOf(250.00)), null);

                        assertThat(installment.getUpdatedAt()).isAfter(initialUpdatedAt);
                }
        }

        @Nested
        @DisplayName("Payment Management - Add Tests")
        class AddPaymentTests {

                @Test
                @DisplayName("Should add payment successfully and update totals")
                void shouldAddPaymentSuccessfully() {
                        installment.addPayment(payment);

                        assertThat(installment.getPayments()).contains(payment);
                        assertThat(installment.getAmountPaid()).isEqualTo(VALID_PAYMENT_AMOUNT);
                        assertThat(installment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
                }

                @Test
                @DisplayName("Should update status to PAID when fully paid")
                void shouldUpdateStatusToPaid() {
                        Payment fullPayment = Payment.create(new PaymentId(UUID.randomUUID()), VALID_AMOUNT_DUE,
                                        LocalDate.now());
                        installment.addPayment(fullPayment);

                        assertThat(installment.getStatus()).isEqualTo(PaymentStatus.PAID);
                }

                @Test
                @DisplayName("Should throw exception when adding duplicate payment ID")
                void shouldThrowExceptionWhenAddingDuplicatePaymentId() {
                        installment.addPayment(payment);

                        assertThatThrownBy(() -> installment.addPayment(payment))
                                        .isInstanceOf(InvalidPropertyException.class)
                                        .hasMessageContaining("already exists");
                }

                @Test
                @DisplayName("Should throw exception when payment amount is zero")
                void shouldThrowExceptionWhenPaymentAmountIsZero() {
                        Payment zeroPayment = Payment.create(new PaymentId(UUID.randomUUID()), Money.ZERO,
                                        LocalDate.now());

                        assertThatThrownBy(() -> installment.addPayment(zeroPayment))
                                        .isInstanceOf(InvalidMoneyValueException.class)
                                        .hasMessageContaining("greater than zero");
                }

                @Test
                @DisplayName("Should throw exception when payment exceeds remaining amount")
                void shouldThrowExceptionWhenPaymentExceedsRemaining() {
                        Money excessiveAmount = VALID_AMOUNT_DUE.add(new Money(BigDecimal.ONE));
                        Payment excessivePayment = Payment.create(new PaymentId(UUID.randomUUID()), excessiveAmount,
                                        LocalDate.now());

                        assertThatThrownBy(() -> installment.addPayment(excessivePayment))
                                        .isInstanceOf(InvalidMoneyValueException.class)
                                        .hasMessageContaining("exceeds remaining");
                }
        }

        @Nested
        @DisplayName("Payment Management - Update Tests")
        class UpdatePaymentTests {

                @BeforeEach
                void addInitialPayment() {
                        installment.addPayment(payment);
                }

                @Test
                @DisplayName("Should update existing payment successfully")
                void shouldUpdateExistingPayment() {
                        Money newAmount = new Money(BigDecimal.valueOf(150.00));
                        Payment updatedPayment = new Payment(VALID_PAYMENT_ID, newAmount, LocalDate.now().plusDays(1),
                                        LocalDateTime.now(), LocalDateTime.now());

                        installment.updatePayment(updatedPayment);

                        Payment storedPayment = installment.findPaymentById(VALID_PAYMENT_ID);
                        assertThat(storedPayment.getAmount()).isEqualTo(newAmount);
                        assertThat(installment.getAmountPaid()).isEqualTo(newAmount);
                }

                @Test
                @DisplayName("Should throw exception when updating non-existent payment")
                void shouldThrowExceptionWhenUpdatingNonExistentPayment() {
                        Payment nonExistent = Payment.create(new PaymentId(UUID.randomUUID()), VALID_PAYMENT_AMOUNT,
                                        LocalDate.now());

                        assertThatThrownBy(() -> installment.updatePayment(nonExistent))
                                        .isInstanceOf(InvalidPropertyException.class).hasMessageContaining("not found");
                }

                @Test
                @DisplayName("Should throw exception when new amount exceeds limit")
                void shouldThrowExceptionWhenNewAmountExceedsLimit() {
                        // Invoice 200, Paid 100. Remaining 100.
                        // Try to update existing payment (100) to 250.
                        // Max allowed for this payment is (Remaining 100 + Old Amount 100) = 200.

                        Money tooBigAmount = new Money(BigDecimal.valueOf(201.00));
                        Payment excessiveUpdate = new Payment(VALID_PAYMENT_ID, tooBigAmount, LocalDate.now(),
                                        LocalDateTime.now(), LocalDateTime.now());

                        assertThatThrownBy(() -> installment.updatePayment(excessiveUpdate))
                                        .isInstanceOf(InvalidMoneyValueException.class)
                                        .hasMessageContaining("exceeds remaining");
                }

                @Test
                @DisplayName("Should throw exception when new amount is zero")
                void shouldThrowExceptionWhenNewAmountIsZero() {
                        Payment zeroUpdate = new Payment(VALID_PAYMENT_ID, Money.ZERO, LocalDate.now(),
                                        LocalDateTime.now(), LocalDateTime.now());

                        assertThatThrownBy(() -> installment.updatePayment(zeroUpdate))
                                        .isInstanceOf(InvalidMoneyValueException.class)
                                        .hasMessageContaining("greater than zero");
                }
        }

        @Nested
        @DisplayName("Payment Management - Remove Tests")
        class RemovePaymentTests {

                @BeforeEach
                void addInitialPayment() {
                        installment.addPayment(payment);
                }

                @Test
                @DisplayName("Should remove payment successfully")
                void shouldRemovePayment() {
                        installment.removePayment(payment);

                        assertThat(installment.getPayments()).isEmpty();
                        assertThat(installment.getAmountPaid()).isEqualTo(Money.ZERO);
                        assertThat(installment.getStatus()).isEqualTo(PaymentStatus.PENDING);
                }

                @Test
                @DisplayName("Should throw exception when removing non-existent payment")
                void shouldThrowExceptionWhenRemovingNonExistentPayment() {
                        Payment nonExistent = Payment.create(new PaymentId(UUID.randomUUID()), VALID_PAYMENT_AMOUNT,
                                        LocalDate.now());

                        assertThatThrownBy(() -> installment.removePayment(nonExistent))
                                        .isInstanceOf(InvalidPropertyException.class).hasMessageContaining("not found");
                }
        }

        @Nested
        @DisplayName("Status & Calculation Tests")
        class CalculationTests {

                @Test
                @DisplayName("Should correctly identify overdue installment")
                void shouldIdentifyOverdue() {
                        Installment overdueInstallment = Installment.create(VALID_INSTALLMENT_ID, VALID_AMOUNT_DUE,
                                        LocalDate.now().minusDays(1));
                        assertThat(overdueInstallment.isOverdue()).isTrue();
                }

                @Test
                @DisplayName("Should not be overdue if paid")
                void shouldNotBeOverdueIfPaid() {
                        Installment overdueInstallment = Installment.create(VALID_INSTALLMENT_ID, VALID_AMOUNT_DUE,
                                        LocalDate.now().minusDays(1));
                        Payment fullPayment = Payment.create(new PaymentId(UUID.randomUUID()), VALID_AMOUNT_DUE,
                                        LocalDate.now());

                        overdueInstallment.addPayment(fullPayment);

                        assertThat(overdueInstallment.isOverdue()).isFalse();
                }

                @Test
                @DisplayName("Should return correct remaining amount")
                void shouldReturnCorrectRemainingAmount() {
                        installment.addPayment(payment); // 100 paid out of 200
                        assertThat(installment.getRemainingAmount()).isEqualTo(new Money(BigDecimal.valueOf(100.00)));
                }

                @Test
                @DisplayName("Should return last payment date")
                void shouldReturnLastPaymentDate() throws InterruptedException {
                        installment.addPayment(payment);
                        Thread.sleep(10);

                        Payment secondPayment = Payment.create(new PaymentId(UUID.randomUUID()),
                                        new Money(BigDecimal.valueOf(50.00)), LocalDate.now());
                        installment.addPayment(secondPayment);

                        assertThat(installment.getLastPaymentDate()).isPresent();
                        assertThat(installment.getLastPaymentDate().get()).isEqualTo(secondPayment.getUpdatedAt());
                }
        }
}