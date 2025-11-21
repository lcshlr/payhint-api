package com.payhint.api.domain.billing.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
import com.payhint.api.domain.billing.exception.PaymentDoesNotBelongToInstallmentException;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.billing.valueobject.PaymentId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;

@DisplayName("Installment Domain Model Tests")
public class InstallmentTest {
        private static final PaymentId VALID_PAYMENT_ID = new PaymentId(UUID.randomUUID());
        private static final InstallmentId VALID_INSTALLMENT_ID = new InstallmentId(UUID.randomUUID());
        private static final InvoiceId VALID_INVOICE_ID = new InvoiceId(UUID.randomUUID());
        private static final Money VALID_MONEY_100_AMOUNT = new Money(BigDecimal.valueOf(100.00));
        private static final LocalDate VALID_PAYMENT_DATE = LocalDate.now();
        private static final LocalDate VALID_INVOICE_DUE_DATE = LocalDate.now().plusDays(30);
        private static final Money VALID_INVOICE_200_DUE_AMOUNT = new Money(BigDecimal.valueOf(200.00));
        private static Installment VALID_INSTALLMENT = null;
        private static Payment VALID_PAYMENT_100_AMOUNT = null;

        @BeforeEach
        void setup() {
                VALID_INSTALLMENT = new Installment(VALID_INSTALLMENT_ID, VALID_INVOICE_ID,
                                VALID_INVOICE_200_DUE_AMOUNT, VALID_INVOICE_DUE_DATE, LocalDateTime.now(),
                                LocalDateTime.now(), new ArrayList<>());
                VALID_PAYMENT_100_AMOUNT = new Payment(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, VALID_MONEY_100_AMOUNT,
                                VALID_PAYMENT_DATE, LocalDateTime.now(), LocalDateTime.now());
        }

        @Nested
        @DisplayName("Constructor Tests")
        class ConstructorTests {
                @Test
                @DisplayName("Should create installment with valid parameters using simple constructor")
                void shouldCreateInstallmentWithValidParameters() {

                        assertThat(VALID_INSTALLMENT.getInvoiceId().toString()).isEqualTo(VALID_INVOICE_ID.toString());
                        assertThat(VALID_INSTALLMENT.getAmountDue()).isEqualTo(VALID_INVOICE_200_DUE_AMOUNT);
                        assertThat(VALID_INSTALLMENT.getAmountPaid()).isEqualTo(Money.ZERO);
                        assertThat(VALID_INSTALLMENT.getDueDate()).isEqualTo(VALID_INVOICE_DUE_DATE);
                        assertThat(VALID_INSTALLMENT.getStatus()).isEqualTo(PaymentStatus.PENDING);
                        assertThat(VALID_INSTALLMENT.getPayments()).isEmpty();
                        assertThat(VALID_INSTALLMENT.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
                        assertThat(VALID_INSTALLMENT.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
                }

                @Test
                @DisplayName("Should create installment with all parameters using all-args constructor")
                void shouldCreateInstallmentWithAllParameters() {
                        Installment installment = new Installment(VALID_INSTALLMENT_ID, VALID_INVOICE_ID,
                                        VALID_INVOICE_200_DUE_AMOUNT, VALID_INVOICE_DUE_DATE, LocalDateTime.now(),
                                        LocalDateTime.now(), new ArrayList<>());

                        assertThat(installment.getId().toString()).isEqualTo(VALID_INSTALLMENT_ID.toString());
                        assertThat(installment.getInvoiceId().toString()).isEqualTo(VALID_INVOICE_ID.toString());
                        assertThat(installment.getAmountDue()).isEqualTo(VALID_INVOICE_200_DUE_AMOUNT);
                        assertThat(installment.getAmountPaid()).isEqualTo(Money.ZERO);
                        assertThat(installment.getDueDate()).isEqualTo(VALID_INVOICE_DUE_DATE);
                        assertThat(installment.getStatus()).isEqualTo(PaymentStatus.PENDING);
                        assertThat(installment.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
                        assertThat(installment.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
                        assertThat(installment.getPayments()).isEmpty();
                }

                @Test
                @DisplayName("Should throw error when creating installment with null invoiceId")
                void shouldThrowErrorWhenCreatingInstallmentWithNullInvoiceId() {
                        assertThatThrownBy(() -> Installment.create(VALID_INSTALLMENT_ID, null,
                                        VALID_INVOICE_200_DUE_AMOUNT, VALID_INVOICE_DUE_DATE))
                                                        .isInstanceOf(NullPointerException.class)
                                                        .hasMessageContaining("invoiceId");
                }

                @Test
                @DisplayName("Should throw error when creating installment with null amountDue")
                void shouldThrowErrorWhenCreatingInstallmentWithNullAmountDue() {
                        assertThatThrownBy(() -> Installment.create(VALID_INSTALLMENT_ID, VALID_INVOICE_ID, null,
                                        VALID_INVOICE_DUE_DATE)).isInstanceOf(NullPointerException.class)
                                                        .hasMessageContaining("amountDue");
                }

                @Test
                @DisplayName("Should throw error when creating installment with null dueDate")
                void shouldThrowErrorWhenCreatingInstallmentWithNullDueDate() {
                        assertThatThrownBy(() -> Installment.create(VALID_INSTALLMENT_ID, VALID_INVOICE_ID,
                                        VALID_INVOICE_200_DUE_AMOUNT, null)).isInstanceOf(NullPointerException.class)
                                                        .hasMessageContaining("dueDate");
                }
        }

        @Nested
        @DisplayName("Behavioral Tests")
        class BehavioralTests {
                @Test
                @DisplayName("isPaid returns true when status is PAID")
                void isPaidReturnsTrueWhenPaid() {
                        Installment installment = new Installment(VALID_INSTALLMENT_ID, VALID_INVOICE_ID,
                                        VALID_INVOICE_200_DUE_AMOUNT, VALID_INVOICE_DUE_DATE, LocalDateTime.now(),
                                        LocalDateTime.now(), new ArrayList<>());
                        Payment paid = new Payment(new PaymentId(UUID.randomUUID()), installment.getId(),
                                        VALID_INVOICE_200_DUE_AMOUNT, LocalDate.now(), LocalDateTime.now(),
                                        LocalDateTime.now());
                        installment.addPayment(paid);

                        assertThat(installment.isPaid()).isTrue();
                }

                @Test
                @DisplayName("isOverdue returns true when past due and not paid")
                void isOverdueWhenPastDueAndNotPaid() {
                        Installment installment = Installment.create(VALID_INSTALLMENT_ID, VALID_INVOICE_ID,
                                        VALID_INVOICE_200_DUE_AMOUNT, LocalDate.now().minusDays(1));

                        assertThat(installment.isOverdue()).isTrue();
                }

                @Test
                @DisplayName("getRemainingAmount returns correct difference")
                void remainingAmountCalculation() {
                        Installment installment = new Installment(VALID_INSTALLMENT_ID, VALID_INVOICE_ID,
                                        VALID_INVOICE_200_DUE_AMOUNT, VALID_INVOICE_DUE_DATE, LocalDateTime.now(),
                                        LocalDateTime.now(), new ArrayList<>());
                        Payment partial = new Payment(new PaymentId(UUID.randomUUID()), installment.getId(),
                                        VALID_MONEY_100_AMOUNT, LocalDate.now(), LocalDateTime.now(),
                                        LocalDateTime.now());
                        installment.addPayment(partial);

                        assertThat(installment.getRemainingAmount()).isEqualTo(new Money(BigDecimal.valueOf(100.00)));
                }

                @Test
                @DisplayName("getLastPaymentDate returns null when no payments")
                void getLastPaymentDateReturnsNullWhenNoPayments() {
                        Installment installment = Installment.create(VALID_INSTALLMENT_ID, VALID_INVOICE_ID,
                                        VALID_INVOICE_200_DUE_AMOUNT, VALID_INVOICE_DUE_DATE);

                        assertThat(installment.getPayments()).isEmpty();
                        assertThat(installment.getLastPaymentDate()).isEmpty();
                }

                @Test
                @DisplayName("getLastPaymentDate returns latest payment updatedAt when payments exist")
                void getLastPaymentDateReturnsLatestPaymentDate() throws InterruptedException {
                        InstallmentId instId = new InstallmentId(UUID.randomUUID());
                        Installment installment = new Installment(instId, VALID_INVOICE_ID,
                                        VALID_INVOICE_200_DUE_AMOUNT, VALID_INVOICE_DUE_DATE, LocalDateTime.now(),
                                        LocalDateTime.now(), new ArrayList<>());

                        Payment p1 = new Payment(new PaymentId(UUID.randomUUID()), installment.getId(),
                                        new Money(BigDecimal.valueOf(50.00)), LocalDate.now(),
                                        LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusMinutes(1));

                        Thread.sleep(5);

                        Payment p2 = new Payment(new PaymentId(UUID.randomUUID()), installment.getId(),
                                        new Money(BigDecimal.valueOf(75.00)), LocalDate.now(), LocalDateTime.now(),
                                        LocalDateTime.now());

                        installment.addPayment(p1);

                        assertThat(installment.getPayments()).hasSize(1);
                        assertThat(installment.getLastPaymentDate().get()).isEqualTo(p1.getUpdatedAt());

                        installment.addPayment(p2);

                        assertThat(installment.getPayments()).hasSize(2);
                        assertThat(installment.getLastPaymentDate().get()).isEqualTo(p2.getUpdatedAt());

                        Thread.sleep(5);

                        Payment p3 = new Payment(p1.getId(), installment.getId(), new Money(BigDecimal.valueOf(100.00)),
                                        LocalDate.now(), LocalDateTime.now(), LocalDateTime.now().plusMinutes(1));

                        installment.updatePayment(p3);

                        assertThat(installment.getLastPaymentDate().get()).isAfter(p2.getUpdatedAt());
                        assertThat(installment.getPayments().size()).isEqualTo(2);
                        assertThat(installment.getPayments()).containsExactlyInAnyOrder(p2, p3);
                        Payment newPayment = installment.getPayments().stream()
                                        .filter(p -> p.getId().equals(p3.getId())).findFirst().get();
                        assertThat(newPayment.getAmount()).isEqualTo(p3.getAmount());
                        assertThat(newPayment.getPaymentDate()).isEqualTo(p3.getPaymentDate());
                        assertThat(newPayment.getCreatedAt()).isEqualTo(p1.getCreatedAt());
                        assertThat(newPayment.getCreatedAt()).isNotEqualTo(p3.getCreatedAt());
                        assertThat(newPayment.getUpdatedAt()).isNotEqualTo(p3.getUpdatedAt());

                }

                @Test
                @DisplayName("updateDetails updates amountDue when non-null and not dueDate when null")
                void updateDetailsUpdatesAmountDueWhenNonNullAndNotDueDateWhenNull() throws InterruptedException {
                        Money newAmount = new Money(BigDecimal.valueOf(150.00));
                        Thread.sleep(10);
                        VALID_INSTALLMENT.updateDetails(newAmount, null);

                        assertThat(VALID_INSTALLMENT.getAmountDue()).isEqualTo(newAmount);
                        assertThat(VALID_INSTALLMENT.getDueDate()).isEqualTo(VALID_INVOICE_DUE_DATE);
                        assertThat(VALID_INSTALLMENT.getUpdatedAt()).isAfter(VALID_INSTALLMENT.getCreatedAt());
                }

                @Test
                @DisplayName("updateDetails updates amountDue and dueDate when non-null")
                void updateDetailsUpdatesFields() throws InterruptedException {
                        LocalDate newDue = VALID_INVOICE_DUE_DATE.plusDays(5);
                        Money newAmount = new Money(BigDecimal.valueOf(150.00));
                        Thread.sleep(10);
                        VALID_INSTALLMENT.updateDetails(newAmount, newDue);

                        assertThat(VALID_INSTALLMENT.getAmountDue()).isEqualTo(newAmount);
                        assertThat(VALID_INSTALLMENT.getDueDate()).isEqualTo(newDue);
                        assertThat(VALID_INSTALLMENT.getUpdatedAt()).isAfter(VALID_INSTALLMENT.getCreatedAt());
                }

                @Test
                @DisplayName("updateDetails with nulls does not change fields but updates nothing")
                void updateDetailsWithNullsNoChange() {
                        VALID_INSTALLMENT.updateDetails(null, null);

                        assertThat(VALID_INSTALLMENT.getAmountDue()).isEqualTo(VALID_INSTALLMENT.getAmountDue());
                        assertThat(VALID_INSTALLMENT.getDueDate()).isEqualTo(VALID_INSTALLMENT.getDueDate());
                        assertThat(VALID_INSTALLMENT.getUpdatedAt()).isEqualTo(VALID_INSTALLMENT.getUpdatedAt());
                }

                @Test
                @DisplayName("Status transitions correctly through partial, paid, and pending states")
                void statusTransitions() {
                        InstallmentId instId = new InstallmentId(UUID.randomUUID());
                        Installment installment = new Installment(instId, VALID_INVOICE_ID,
                                        VALID_INVOICE_200_DUE_AMOUNT, VALID_INVOICE_DUE_DATE, LocalDateTime.now(),
                                        LocalDateTime.now(), new ArrayList<>());
                        Payment p1 = new Payment(new PaymentId(UUID.randomUUID()), installment.getId(),
                                        new Money(BigDecimal.valueOf(100)), LocalDate.now(), LocalDateTime.now(),
                                        LocalDateTime.now());
                        Payment p2 = new Payment(new PaymentId(UUID.randomUUID()), installment.getId(),
                                        new Money(BigDecimal.valueOf(100)), LocalDate.now(), LocalDateTime.now(),
                                        LocalDateTime.now());

                        installment.addPayment(p1);
                        assertThat(installment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);

                        installment.addPayment(p2);
                        assertThat(installment.getStatus()).isEqualTo(PaymentStatus.PAID);

                        installment.removePayment(p2);
                        assertThat(installment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);

                        installment.removePayment(p1);
                        assertThat(installment.getStatus()).isEqualTo(PaymentStatus.PENDING);
                }

                @Test
                @DisplayName("Reapplying same status does not modify timestamps")
                void reapplySameStatusNoTimestampChange() {
                        InstallmentId instId = new InstallmentId(UUID.randomUUID());
                        Installment installment = new Installment(instId, VALID_INVOICE_ID,
                                        VALID_INVOICE_200_DUE_AMOUNT, VALID_INVOICE_DUE_DATE, LocalDateTime.now(),
                                        LocalDateTime.now(), new ArrayList<>());
                        LocalDateTime initialUpdatedAt = installment.getUpdatedAt();

                        try {
                                Thread.sleep(10);
                        } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                        }

                        installment.addPayment(Payment.create(VALID_PAYMENT_ID, installment.getId(),
                                        new Money(BigDecimal.valueOf(1)), LocalDate.now()));

                        assertThat(installment.getUpdatedAt()).isAfter(initialUpdatedAt);
                }

                @Test
                @DisplayName("updateDetails throws when new amountDue is less than already paid")
                void updateDetailsThrowsWhenAmountLessThanPaid() {
                        InstallmentId id = new InstallmentId(UUID.randomUUID());
                        Installment inst = new Installment(id, VALID_INVOICE_ID, new Money(BigDecimal.valueOf(200)),
                                        LocalDate.now().plusDays(5), LocalDateTime.now(), LocalDateTime.now(),
                                        new ArrayList<>());

                        Payment p = new Payment(new PaymentId(UUID.randomUUID()), inst.getId(),
                                        new Money(BigDecimal.valueOf(150)), LocalDate.now(), LocalDateTime.now(),
                                        LocalDateTime.now());
                        inst.addPayment(p);

                        assertThatThrownBy(() -> inst.updateDetails(new Money(BigDecimal.valueOf(100)), null))
                                        .isInstanceOf(InvalidMoneyValueException.class);
                }

                @Test
                @DisplayName("getPayments returns an unmodifiable copy")
                void getPaymentsReturnsUnmodifiableCopy() {
                        InstallmentId id = new InstallmentId(UUID.randomUUID());
                        Installment inst = new Installment(id, VALID_INVOICE_ID, new Money(BigDecimal.valueOf(100)),
                                        LocalDate.now().plusDays(5), LocalDateTime.now(), LocalDateTime.now(),
                                        new ArrayList<>());

                        Payment p = new Payment(new PaymentId(UUID.randomUUID()), inst.getId(),
                                        new Money(BigDecimal.valueOf(50)), LocalDate.now(), LocalDateTime.now(),
                                        LocalDateTime.now());
                        inst.addPayment(p);

                        List<Payment> paymentsView = inst.getPayments();
                        assertThat(paymentsView).hasSize(1);

                        assertThatThrownBy(() -> paymentsView.add(p)).isInstanceOf(UnsupportedOperationException.class);
                        assertThat(inst.getPayments()).hasSize(1);
                }
        }

        @Nested
        @DisplayName("Payment Operations Tests")
        class PaymentOperationsTests {
                @Test
                @DisplayName("Should add payment successfully")
                void shouldAddPaymentSuccessfully() {
                        Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID, VALID_MONEY_100_AMOUNT,
                                        VALID_PAYMENT_DATE);
                        VALID_INSTALLMENT.addPayment(payment);

                        assertThat(VALID_INSTALLMENT.getPayments()).hasSize(1);
                        assertThat(VALID_INSTALLMENT.getAmountPaid()).isEqualTo(VALID_MONEY_100_AMOUNT);
                }

                @Test
                @DisplayName("Should throw error when adding payment with null")
                void shouldThrowErrorWhenAddingPaymentWithNull() {
                        assertThatThrownBy(() -> VALID_INSTALLMENT.addPayment(null))
                                        .isInstanceOf(NullPointerException.class);
                }

                @Test
                @DisplayName("Should throw error when adding payment with amount exceeding remaining amount")
                void shouldThrowErrorWhenAddingPaymentWithAmountExceedingRemainingAmount() {
                        Payment payment = Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT_ID,
                                        new Money(BigDecimal.valueOf(300.00)), VALID_PAYMENT_DATE);

                        assertThatThrownBy(() -> VALID_INSTALLMENT.addPayment(payment))
                                        .isInstanceOf(InvalidMoneyValueException.class)
                                        .hasMessageContaining("exceeds remaining installment amount");
                }

                @Test
                @DisplayName("Should throw error when adding payment with zero amount")
                void shouldThrowErrorWhenAddingPaymentWithZeroAmount() {
                        assertThatThrownBy(() -> {
                                VALID_INSTALLMENT.addPayment(Payment.create(VALID_PAYMENT_ID, VALID_INSTALLMENT.getId(),
                                                Money.ZERO, LocalDate.now()));
                        }).isInstanceOf(InvalidMoneyValueException.class)
                                        .hasMessageContaining("Payment amount must be greater than zero");
                }

                @Test
                @DisplayName("addPayment increases amountPaid and changes status to PARTIALLY_PAID and update timestamps")
                void addPaymentValid() throws InterruptedException {
                        Thread.sleep(10);
                        VALID_INSTALLMENT.addPayment(VALID_PAYMENT_100_AMOUNT);

                        assertThat(VALID_INSTALLMENT.getAmountPaid()).isEqualTo(VALID_MONEY_100_AMOUNT);
                        assertThat(VALID_INSTALLMENT.getPayments()).contains(VALID_PAYMENT_100_AMOUNT);
                        assertThat(VALID_INSTALLMENT.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
                        assertThat(VALID_INSTALLMENT.getUpdatedAt()).isAfter(VALID_INSTALLMENT.getCreatedAt());
                }

                @Test
                @DisplayName("addPayment throws when payment exceeds remaining amount")
                void addPaymentExceedsRemaining() {
                        Payment bigPayment = new Payment(VALID_PAYMENT_ID, VALID_INSTALLMENT.getId(),
                                        new Money(BigDecimal.valueOf(300.00)), VALID_PAYMENT_DATE, LocalDateTime.now(),
                                        LocalDateTime.now());

                        assertThatThrownBy(() -> VALID_INSTALLMENT.addPayment(bigPayment))
                                        .isInstanceOf(InvalidMoneyValueException.class);
                        assertThat(VALID_INSTALLMENT.getAmountPaid()).isEqualTo(Money.ZERO);
                        assertThat(VALID_INSTALLMENT.getPayments()).isEmpty();
                        assertThat(VALID_INSTALLMENT.getStatus()).isEqualTo(PaymentStatus.PENDING);
                        assertThat(VALID_INSTALLMENT.getUpdatedAt()).isEqualTo(VALID_INSTALLMENT.getCreatedAt());
                }

                @Test
                @DisplayName("addPayment throws when payment does not belong to installment")
                void addPaymentDoesNotBelongToInstallment() {
                        InstallmentId otherInstId = new InstallmentId(UUID.randomUUID());
                        Payment wrongInstallmentPayment = new Payment(VALID_PAYMENT_ID, otherInstId,
                                        VALID_MONEY_100_AMOUNT, VALID_PAYMENT_DATE, LocalDateTime.now(),
                                        LocalDateTime.now());

                        assertThatThrownBy(() -> VALID_INSTALLMENT.addPayment(wrongInstallmentPayment))
                                        .isInstanceOf(PaymentDoesNotBelongToInstallmentException.class);
                        assertThat(VALID_INSTALLMENT.getAmountPaid()).isEqualTo(Money.ZERO);
                        assertThat(VALID_INSTALLMENT.getPayments()).isEmpty();
                        assertThat(VALID_INSTALLMENT.getStatus()).isEqualTo(PaymentStatus.PENDING);
                        assertThat(VALID_INSTALLMENT.getUpdatedAt()).isEqualTo(VALID_INSTALLMENT.getCreatedAt());
                }

                @Test
                @DisplayName("updatePayment replaces payment and updates amountPaid and status to PAID")
                void updatePaymentSuccess() throws InterruptedException {
                        Payment p2 = new Payment(VALID_PAYMENT_100_AMOUNT.getId(), VALID_INSTALLMENT.getId(),
                                        new Money(BigDecimal.valueOf(200.00)), VALID_PAYMENT_DATE, LocalDateTime.now(),
                                        LocalDateTime.now());

                        VALID_INSTALLMENT.addPayment(VALID_PAYMENT_100_AMOUNT);
                        Thread.sleep(10);
                        VALID_INSTALLMENT.updatePayment(p2);

                        assertThat(VALID_INSTALLMENT.getAmountPaid()).isEqualTo(new Money(BigDecimal.valueOf(200.00)));
                        assertThat(VALID_INSTALLMENT.getStatus()).isEqualTo(PaymentStatus.PAID);
                        assertThat(VALID_PAYMENT_100_AMOUNT.getId()).isEqualTo(p2.getId());
                        assertThat(VALID_INSTALLMENT.getPayments()).contains(p2);
                        assertThat(VALID_INSTALLMENT.getUpdatedAt()).isAfter(VALID_INSTALLMENT.getCreatedAt());
                }

                @Test
                @DisplayName("updatePayment throws when existing payment not found")
                void updatePaymentExistingNotFound() {
                        InstallmentId instId = new InstallmentId(UUID.randomUUID());
                        Payment replacement = new Payment(VALID_PAYMENT_ID, instId, VALID_MONEY_100_AMOUNT,
                                        VALID_PAYMENT_DATE, LocalDateTime.now(), LocalDateTime.now());

                        assertThatThrownBy(() -> VALID_INSTALLMENT.updatePayment(replacement))
                                        .isInstanceOf(InvalidPropertyException.class);
                        assertThat(VALID_INSTALLMENT.getUpdatedAt()).isEqualTo(VALID_INSTALLMENT.getCreatedAt());
                        assertThat(VALID_INSTALLMENT.getAmountPaid()).isEqualTo(Money.ZERO);
                        assertThat(VALID_INSTALLMENT.getPayments()).isEmpty();
                        assertThat(VALID_INSTALLMENT.getStatus()).isEqualTo(PaymentStatus.PENDING);
                }

                @Test
                @DisplayName("removePayment decreases amountPaid and updates status and timestamps")
                void removePaymentSuccess() {
                        Payment p = new Payment(VALID_PAYMENT_ID, VALID_INSTALLMENT.getId(), VALID_MONEY_100_AMOUNT,
                                        VALID_PAYMENT_DATE, LocalDateTime.now(), LocalDateTime.now());

                        Payment pToRemove = new Payment(VALID_PAYMENT_ID, VALID_INSTALLMENT.getId(),
                                        VALID_MONEY_100_AMOUNT, VALID_PAYMENT_DATE, LocalDateTime.now(),
                                        LocalDateTime.now());
                        VALID_INSTALLMENT.addPayment(p);
                        VALID_INSTALLMENT.removePayment(pToRemove);

                        assertThat(VALID_INSTALLMENT.getAmountPaid()).isEqualTo(Money.ZERO);
                        assertThat(VALID_INSTALLMENT.getPayments()).doesNotContain(p);
                        assertThat(VALID_INSTALLMENT.getStatus()).isEqualTo(PaymentStatus.PENDING);
                }

                @Test
                @DisplayName("removePayment throws when payment not found")
                void removePaymentNotFound() {
                        InstallmentId instId = new InstallmentId(UUID.randomUUID());
                        Payment p = new Payment(VALID_PAYMENT_ID, instId, VALID_MONEY_100_AMOUNT, VALID_PAYMENT_DATE,
                                        LocalDateTime.now(), LocalDateTime.now());

                        assertThatThrownBy(() -> VALID_INSTALLMENT.removePayment(p))
                                        .isInstanceOf(InvalidPropertyException.class);
                }

                @Test
                @DisplayName("addPayment with duplicate payment id throws")
                void addPaymentDuplicateIdThrows() {
                        InstallmentId id = new InstallmentId(UUID.randomUUID());
                        Installment inst = new Installment(id, VALID_INVOICE_ID, new Money(BigDecimal.valueOf(200)),
                                        LocalDate.now().plusDays(5), LocalDateTime.now(), LocalDateTime.now(),
                                        new ArrayList<>());

                        PaymentId pid = new PaymentId(UUID.randomUUID());
                        Payment p1 = new Payment(pid, inst.getId(), new Money(BigDecimal.valueOf(50)), LocalDate.now(),
                                        LocalDateTime.now(), LocalDateTime.now());

                        inst.addPayment(p1);

                        Payment duplicate = new Payment(pid, inst.getId(), new Money(BigDecimal.valueOf(50)),
                                        LocalDate.now(), LocalDateTime.now(), LocalDateTime.now());

                        assertThatThrownBy(() -> inst.addPayment(duplicate))
                                        .isInstanceOf(InvalidPropertyException.class);
                }

                @Test
                @DisplayName("updatePayment throws when updated amount would exceed installment total")
                void updatePaymentExceedsInstallmentThrows() {
                        InstallmentId id = new InstallmentId(UUID.randomUUID());
                        Installment inst = new Installment(id, VALID_INVOICE_ID, new Money(BigDecimal.valueOf(200)),
                                        LocalDate.now().plusDays(5), LocalDateTime.now(), LocalDateTime.now(),
                                        new ArrayList<>());

                        PaymentId pid = new PaymentId(UUID.randomUUID());
                        Payment p = new Payment(pid, inst.getId(), new Money(BigDecimal.valueOf(100)), LocalDate.now(),
                                        LocalDateTime.now(), LocalDateTime.now());
                        inst.addPayment(p);

                        Payment updated = new Payment(pid, inst.getId(), new Money(BigDecimal.valueOf(201)),
                                        LocalDate.now(), LocalDateTime.now(), LocalDateTime.now());

                        assertThatThrownBy(() -> inst.updatePayment(updated))
                                        .isInstanceOf(InvalidMoneyValueException.class);
                }
        }

        @Nested
        @DisplayName("Edge Case Tests")
        class EdgeCaseTests {
                @Test
                @DisplayName("Should handle multiple payments correctly")
                void shouldHandleMultiplePaymentsCorrectly() {
                        Installment installment = Installment.create(VALID_INSTALLMENT_ID, VALID_INVOICE_ID,
                                        new Money(BigDecimal.valueOf(500.00)), VALID_INVOICE_DUE_DATE);
                        Payment p1 = Payment.create(new PaymentId(UUID.randomUUID()), installment.getId(),
                                        new Money(BigDecimal.valueOf(100.00)), LocalDate.now());
                        Payment p2 = Payment.create(new PaymentId(UUID.randomUUID()), installment.getId(),
                                        new Money(BigDecimal.valueOf(150.00)), LocalDate.now().plusDays(1));
                        installment.addPayment(p1);
                        installment.addPayment(p2);

                        assertThat(installment.getAmountPaid()).isEqualTo(new Money(BigDecimal.valueOf(250.00)));
                        assertThat(installment.getRemainingAmount()).isEqualTo(new Money(BigDecimal.valueOf(250.00)));
                }

                @Test
                @DisplayName("Should have status PAID when fully paid")
                void shouldHaveStatusPaidWhenFullyPaid() {
                        Installment installment = Installment.create(VALID_INSTALLMENT_ID, VALID_INVOICE_ID,
                                        VALID_INVOICE_200_DUE_AMOUNT, VALID_INVOICE_DUE_DATE);
                        Payment payment = Payment.create(new PaymentId(UUID.randomUUID()), installment.getId(),
                                        VALID_INVOICE_200_DUE_AMOUNT, LocalDate.now());
                        installment.addPayment(payment);

                        assertThat(installment.getStatus()).isEqualTo(PaymentStatus.PAID);
                }

                @Test
                @DisplayName("Should have status PARTIALLY_PAID when partially paid")
                void shouldHaveStatusPartiallyPaidWhenPartiallyPaid() {
                        Installment installment = Installment.create(VALID_INSTALLMENT_ID, VALID_INVOICE_ID,
                                        VALID_INVOICE_200_DUE_AMOUNT, VALID_INVOICE_DUE_DATE);
                        Payment payment = Payment.create(new PaymentId(UUID.randomUUID()), installment.getId(),
                                        VALID_MONEY_100_AMOUNT, LocalDate.now());
                        installment.addPayment(payment);

                        assertThat(installment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
                }

                @Test
                @DisplayName("Equals and hashCode contract")
                void equalsAndHashCodeContract() {
                        Installment a = Installment.create(VALID_INSTALLMENT_ID, VALID_INVOICE_ID,
                                        new Money(BigDecimal.valueOf(100)), LocalDate.now());
                        Installment b = new Installment(VALID_INSTALLMENT_ID, VALID_INVOICE_ID,
                                        new Money(BigDecimal.valueOf(200)), LocalDate.now().plusDays(1),
                                        LocalDateTime.now(), LocalDateTime.now(), new ArrayList<>());

                        Installment c = Installment.create(new InstallmentId(UUID.randomUUID()), VALID_INVOICE_ID,
                                        new Money(BigDecimal.valueOf(100)), LocalDate.now());

                        assertThat(a).isEqualTo(b);
                        assertThat(a).isNotEqualTo(c);
                        assertThat(a.hashCode()).isEqualTo(b.hashCode());
                }
        }
}