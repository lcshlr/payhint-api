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

import com.payhint.api.domain.billing.exception.InstallmentDoesNotBelongToInvoiceException;
import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.billing.valueobject.PaymentId;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;

@DisplayName("Invoice Domain Model Tests")
class InvoiceTest {

        private static final InvoiceId VALID_INVOICE_ID = new InvoiceId(UUID.randomUUID());
        private static final CustomerId VALID_CUSTOMER_ID = new CustomerId(UUID.randomUUID());
        private static final InvoiceReference VALID_REFERENCE = new InvoiceReference("INV-001");
        private static final String VALID_CURRENCY = "USD";
        private static final Money AMOUNT_500 = new Money(BigDecimal.valueOf(500.00));
        private static final Money AMOUNT_200 = new Money(BigDecimal.valueOf(200.00));
        private static final LocalDate DUE_DATE_FUTURE = LocalDate.now().plusDays(30);
        private static final LocalDate DUE_DATE_PAST = LocalDate.now().minusDays(1);

        private Invoice invoice;

        @BeforeEach
        void setUp() {
                invoice = Invoice.create(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_REFERENCE, VALID_CURRENCY);
        }

        @Nested
        @DisplayName("Constructor & Factory Tests")
        class ConstructorTests {

                @Test
                @DisplayName("Should create invoice with valid parameters using factory")
                void shouldCreateInvoiceWithValidParameters() {
                        assertThat(invoice.getId()).isEqualTo(VALID_INVOICE_ID);
                        assertThat(invoice.getCustomerId()).isEqualTo(VALID_CUSTOMER_ID);
                        assertThat(invoice.getInvoiceReference()).isEqualTo(VALID_REFERENCE);
                        assertThat(invoice.getCurrency()).isEqualTo(VALID_CURRENCY);
                        assertThat(invoice.getTotalAmount()).isEqualTo(Money.ZERO);
                        assertThat(invoice.getTotalPaid()).isEqualTo(Money.ZERO);
                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PENDING);
                        assertThat(invoice.getInstallments()).isEmpty();
                        assertThat(invoice.isArchived()).isFalse();
                        assertThat(invoice.getCreatedAt()).isNotNull();
                        assertThat(invoice.getUpdatedAt()).isNotNull();
                }

                @Test
                @DisplayName("Should create invoice with all parameters using constructor")
                void shouldCreateInvoiceWithAllParameters() {
                        LocalDateTime now = LocalDateTime.now();
                        Invoice customInvoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_REFERENCE,
                                        AMOUNT_500, Money.ZERO, VALID_CURRENCY, PaymentStatus.PENDING, now, now, now,
                                        false, new ArrayList<>(), 1L);

                        assertThat(customInvoice.getId()).isEqualTo(VALID_INVOICE_ID);
                        assertThat(customInvoice.getTotalAmount()).isEqualTo(AMOUNT_500);
                        assertThat(customInvoice.getCreatedAt()).isEqualTo(now);
                }

                @Test
                @DisplayName("Should throw exception when creating invoice with null mandatory fields")
                void shouldThrowExceptionWhenMandatoryFieldsNull() {
                        assertThatThrownBy(
                                        () -> Invoice.create(null, VALID_CUSTOMER_ID, VALID_REFERENCE, VALID_CURRENCY))
                                                        .isInstanceOfAny(NullPointerException.class,
                                                                        InvalidPropertyException.class);

                        assertThatThrownBy(
                                        () -> Invoice.create(VALID_INVOICE_ID, null, VALID_REFERENCE, VALID_CURRENCY))
                                                        .isInstanceOfAny(NullPointerException.class,
                                                                        InvalidPropertyException.class);

                        assertThatThrownBy(
                                        () -> Invoice.create(VALID_INVOICE_ID, VALID_CUSTOMER_ID, null, VALID_CURRENCY))
                                                        .isInstanceOfAny(NullPointerException.class,
                                                                        InvalidPropertyException.class);
                }
        }

        @Nested
        @DisplayName("Update Details Tests")
        class UpdateDetailsTests {

                @Test
                @DisplayName("Should update reference and currency successfully")
                void shouldUpdateReferenceAndCurrency() {
                        InvoiceReference newRef = new InvoiceReference("INV-002");
                        String newCurrency = "EUR";

                        invoice.updateDetails(newRef, newCurrency);

                        assertThat(invoice.getInvoiceReference()).isEqualTo(newRef);
                        assertThat(invoice.getCurrency()).isEqualTo(newCurrency);
                }

                @Test
                @DisplayName("Should update updated_at timestamp when details change")
                void shouldUpdateTimestampWhenDetailsChange() throws InterruptedException {
                        LocalDateTime initialUpdatedAt = invoice.getUpdatedAt();
                        Thread.sleep(10);

                        invoice.updateDetails(new InvoiceReference("INV-UPDATED"), null);

                        assertThat(invoice.getUpdatedAt()).isAfter(initialUpdatedAt);
                }

                @Test
                @DisplayName("Should throw exception when updating archived invoice")
                void shouldThrowExceptionWhenUpdatingArchivedInvoice() {
                        invoice.archive();
                        assertThatThrownBy(() -> invoice.updateDetails(new InvoiceReference("INV-003"), "GBP"))
                                        .isInstanceOf(IllegalStateException.class).hasMessageContaining("archived");
                }
        }

        @Nested
        @DisplayName("Installment Management Tests")
        class InstallmentManagementTests {

                @Test
                @DisplayName("Should add installment successfully and update total amount")
                void shouldAddInstallmentAndUpdateTotal() {
                        invoice.addInstallment(AMOUNT_500, DUE_DATE_FUTURE);

                        assertThat(invoice.getInstallments()).hasSize(1);
                        assertThat(invoice.getTotalAmount()).isEqualTo(AMOUNT_500);
                        assertThat(invoice.getInstallments().get(0).getAmountDue()).isEqualTo(AMOUNT_500);
                }

                @Test
                @DisplayName("Should update installment successfully and recalculate total")
                void shouldUpdateInstallmentAndRecalculateTotal() {
                        invoice.addInstallment(AMOUNT_200, DUE_DATE_FUTURE); // Total 200
                        InstallmentId installmentId = invoice.getInstallments().get(0).getId();

                        invoice.updateInstallment(installmentId, AMOUNT_500, null); // Change 200 -> 500

                        assertThat(invoice.getTotalAmount()).isEqualTo(AMOUNT_500);
                        assertThat(invoice.getInstallments().get(0).getAmountDue()).isEqualTo(AMOUNT_500);
                }

                @Test
                @DisplayName("Should remove installment successfully and reduce total")
                void shouldRemoveInstallmentAndReduceTotal() {
                        invoice.addInstallment(AMOUNT_500, DUE_DATE_FUTURE);
                        InstallmentId installmentId = invoice.getInstallments().get(0).getId();

                        invoice.removeInstallment(installmentId);

                        assertThat(invoice.getInstallments()).isEmpty();
                        assertThat(invoice.getTotalAmount()).isEqualTo(Money.ZERO);
                }

                @Test
                @DisplayName("Should add multiple installments via list")
                void shouldAddMultipleInstallments() {
                        Installment i1 = Installment.create(new InstallmentId(UUID.randomUUID()), AMOUNT_200,
                                        DUE_DATE_FUTURE);
                        Installment i2 = Installment.create(new InstallmentId(UUID.randomUUID()), AMOUNT_500,
                                        DUE_DATE_FUTURE.plusDays(1));

                        invoice.addInstallments(List.of(i1, i2));

                        assertThat(invoice.getInstallments()).hasSize(2);
                        assertThat(invoice.getTotalAmount()).isEqualTo(new Money(BigDecimal.valueOf(700.00)));
                }

                @Test
                @DisplayName("Should throw exception when adding installment with duplicate due date")
                void shouldThrowExceptionWhenDuplicateDueDate() {
                        invoice.addInstallment(AMOUNT_200, DUE_DATE_FUTURE);

                        assertThatThrownBy(() -> invoice.addInstallment(AMOUNT_500, DUE_DATE_FUTURE))
                                        .isInstanceOf(InvalidPropertyException.class)
                                        .hasMessageContaining("An installment with same due date already exists");
                }

                @Test
                @DisplayName("Should throw exception when adding installment with zero amount")
                void shouldThrowExceptionWhenZeroAmountInstallment() {
                        assertThatThrownBy(() -> invoice.addInstallment(Money.ZERO, DUE_DATE_FUTURE))
                                        .isInstanceOf(InvalidMoneyValueException.class)
                                        .hasMessageContaining("greater than zero");
                }

                @Test
                @DisplayName("Should throw exception when updating non-existent installment")
                void shouldThrowExceptionWhenUpdatingNonExistentInstallment() {
                        assertThatThrownBy(() -> invoice.updateInstallment(new InstallmentId(UUID.randomUUID()),
                                        AMOUNT_500, null))
                                                        .isInstanceOf(InstallmentDoesNotBelongToInvoiceException.class);
                }

                @Test
                @DisplayName("Should throw exception when modifying installments on archived invoice")
                void shouldThrowExceptionWhenModifyingArchivedInvoice() {
                        invoice.addInstallment(AMOUNT_200, DUE_DATE_FUTURE);
                        InstallmentId id = invoice.getInstallments().get(0).getId();
                        invoice.archive();

                        assertThatThrownBy(() -> invoice.addInstallment(AMOUNT_500, DUE_DATE_FUTURE.plusDays(1)))
                                        .isInstanceOf(IllegalStateException.class);

                        assertThatThrownBy(() -> invoice.updateInstallment(id, AMOUNT_500, null))
                                        .isInstanceOf(IllegalStateException.class);

                        assertThatThrownBy(() -> invoice.removeInstallment(id))
                                        .isInstanceOf(IllegalStateException.class);
                }
        }

        @Nested
        @DisplayName("Payment Management Tests")
        class PaymentManagementTests {

                private InstallmentId installmentId;

                @BeforeEach
                void addInstallment() {
                        invoice.addInstallment(AMOUNT_500, DUE_DATE_FUTURE);
                        installmentId = invoice.getInstallments().get(0).getId();
                }

                @Test
                @DisplayName("Should add payment successfully and update invoice total paid")
                void shouldAddPaymentAndUpdateTotalPaid() {
                        invoice.addPayment(installmentId, LocalDate.now(), AMOUNT_200);

                        assertThat(invoice.getTotalPaid()).isEqualTo(AMOUNT_200);
                        assertThat(invoice.getRemainingAmount()).isEqualTo(new Money(BigDecimal.valueOf(300.00)));
                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
                }

                @Test
                @DisplayName("Should update payment successfully and adjust invoice totals")
                void shouldUpdatePaymentAndAdjustTotals() {
                        invoice.addPayment(installmentId, LocalDate.now(), AMOUNT_200); // Paid 200
                        PaymentId paymentId = invoice.getInstallments().get(0).getPayments().get(0).getId();

                        invoice.updatePayment(installmentId, paymentId, null, new Money(BigDecimal.valueOf(300.00))); // Change
                                                                                                                      // to
                                                                                                                      // 300

                        assertThat(invoice.getTotalPaid()).isEqualTo(new Money(BigDecimal.valueOf(300.00)));
                        assertThat(invoice.getRemainingAmount()).isEqualTo(new Money(BigDecimal.valueOf(200.00)));
                }

                @Test
                @DisplayName("Should remove payment successfully and reduce total paid")
                void shouldRemovePaymentAndReduceTotalPaid() {
                        invoice.addPayment(installmentId, LocalDate.now(), AMOUNT_200);
                        PaymentId paymentId = invoice.getInstallments().get(0).getPayments().get(0).getId();

                        invoice.removePayment(installmentId, paymentId);

                        assertThat(invoice.getTotalPaid()).isEqualTo(Money.ZERO);
                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PENDING);
                }

                @Test
                @DisplayName("Should throw exception when adding payment exceeding remaining amount")
                void shouldThrowExceptionWhenPaymentExceedsInvoiceTotal() {
                        // Installment is 500. Try to pay 600.
                        Money excessiveAmount = new Money(BigDecimal.valueOf(600.00));

                        assertThatThrownBy(() -> invoice.addPayment(installmentId, LocalDate.now(), excessiveAmount))
                                        .isInstanceOf(InvalidMoneyValueException.class)
                                        .hasMessageContaining("exceeds remaining");
                }

                @Test
                @DisplayName("Should throw exception when adding payment with zero amount")
                void shouldThrowExceptionWhenPaymentZero() {
                        assertThatThrownBy(() -> invoice.addPayment(installmentId, LocalDate.now(), Money.ZERO))
                                        .isInstanceOf(InvalidMoneyValueException.class);
                }

                @Test
                @DisplayName("Should throw exception when payment operations on archived invoice")
                void shouldThrowExceptionWhenPaymentOpsOnArchivedInvoice() {
                        invoice.addPayment(installmentId, LocalDate.now(), AMOUNT_200);
                        PaymentId pid = invoice.getInstallments().get(0).getPayments().get(0).getId();
                        invoice.archive();

                        assertThatThrownBy(() -> invoice.addPayment(installmentId, LocalDate.now(), AMOUNT_200))
                                        .isInstanceOf(IllegalStateException.class);

                        assertThatThrownBy(() -> invoice.updatePayment(installmentId, pid, null, AMOUNT_500))
                                        .isInstanceOf(IllegalStateException.class);

                        assertThatThrownBy(() -> invoice.removePayment(installmentId, pid))
                                        .isInstanceOf(IllegalStateException.class);
                }

                @Test
                @DisplayName("Should throw exception when operating on payment in non-existent installment")
                void shouldThrowExceptionWhenInstallmentNotFoundForPayment() {
                        InstallmentId randomId = new InstallmentId(UUID.randomUUID());
                        assertThatThrownBy(() -> invoice.addPayment(randomId, LocalDate.now(), AMOUNT_200))
                                        .isInstanceOf(InstallmentDoesNotBelongToInvoiceException.class);
                }
        }

        @Nested
        @DisplayName("Status & Calculation Tests")
        class CalculationTests {

                @Test
                @DisplayName("Should calculate status transitions correctly")
                void shouldCalculateStatusTransitions() {
                        // 1. Pending (created with 500 due)
                        invoice.addInstallment(AMOUNT_500, DUE_DATE_FUTURE);
                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PENDING);

                        // 2. Partially Paid (pay 200)
                        InstallmentId iId = invoice.getInstallments().get(0).getId();
                        invoice.addPayment(iId, LocalDate.now(), AMOUNT_200);
                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);

                        // 3. Paid (pay remaining 300)
                        invoice.addPayment(iId, LocalDate.now(), new Money(BigDecimal.valueOf(300.00)));
                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PAID);
                        assertThat(invoice.isFullyPaid()).isTrue();
                }

                @Test
                @DisplayName("Should identify overdue invoice if any installment is overdue")
                void shouldIdentifyOverdue() {
                        // Add future installment (not overdue)
                        invoice.addInstallment(AMOUNT_200, DUE_DATE_FUTURE);
                        assertThat(invoice.isOverdue()).isFalse();

                        // Add past installment (overdue)
                        invoice.addInstallment(AMOUNT_200, DUE_DATE_PAST);
                        assertThat(invoice.isOverdue()).isTrue();
                }

                @Test
                @DisplayName("Should not be overdue if past installment is fully paid")
                void shouldNotBeOverdueIfPaid() {
                        invoice.addInstallment(AMOUNT_200, DUE_DATE_PAST);
                        InstallmentId id = invoice.getInstallments().get(0).getId();

                        invoice.addPayment(id, LocalDate.now(), AMOUNT_200); // Pay it off

                        assertThat(invoice.isOverdue()).isFalse();
                }
        }

        @Nested
        @DisplayName("Archive Tests")
        class ArchiveTests {

                @Test
                @DisplayName("Should archive and unarchive successfully")
                void shouldArchiveAndUnarchive() {
                        assertThat(invoice.isArchived()).isFalse();

                        invoice.archive();
                        assertThat(invoice.isArchived()).isTrue();

                        invoice.unArchive();
                        assertThat(invoice.isArchived()).isFalse();
                }
        }
}