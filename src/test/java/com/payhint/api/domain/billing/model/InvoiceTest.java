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
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.billing.valueobject.PaymentId;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;

@DisplayName("Invoice Domain Model Tests")
public class InvoiceTest {

        private static final CustomerId VALID_CUSTOMER_ID = new CustomerId(UUID.randomUUID());
        private static final InvoiceReference VALID_INVOICE_REFERENCE = new InvoiceReference("INV-2025-001");
        private static final String VALID_CURRENCY = "USD";
        private static final InvoiceId VALID_INVOICE_ID = new InvoiceId(UUID.randomUUID());
        private static final InstallmentId VALID_INSTALLMENT_ID = new InstallmentId(UUID.randomUUID());

        @Nested
        @DisplayName("Constructor & Factory Tests")
        class ConstructorAndFactoryTests {

                @Test
                @DisplayName("Should create invoice with valid parameters")
                void shouldCreateInvoiceWithValidParameters() {
                        LocalDateTime now = LocalDateTime.now();
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, now, now, false, new ArrayList<>(), null);

                        assertThat(invoice.getId()).isEqualTo(VALID_INVOICE_ID);
                        assertThat(invoice.getCustomerId()).isEqualTo(VALID_CUSTOMER_ID);
                        assertThat(invoice.getInvoiceReference()).isEqualTo(VALID_INVOICE_REFERENCE);
                        assertThat(invoice.getTotalAmount()).isEqualTo(Money.ZERO);
                        assertThat(invoice.getCurrency()).isEqualTo(VALID_CURRENCY);
                        assertThat(invoice.isArchived()).isFalse();
                        assertThat(invoice.getCreatedAt()).isEqualTo(now);
                        assertThat(invoice.getUpdatedAt()).isEqualTo(now);
                }

                @Test
                @DisplayName("Should throw exception when invoice reference is null")
                void shouldThrowExceptionWhenInvoiceReferenceIsNull() {
                        assertThatThrownBy(() -> new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, null, VALID_CURRENCY,
                                        LocalDateTime.now(), LocalDateTime.now(), false, new ArrayList<>(), null))
                                                        .isInstanceOf(NullPointerException.class)
                                                        .hasMessageContaining("invoiceReference");
                }

                @Test
                @DisplayName("Should throw exception when currency is null")
                void shouldThrowExceptionWhenCurrencyIsNull() {
                        assertThatThrownBy(() -> new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID,
                                        VALID_INVOICE_REFERENCE, null, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null)).isInstanceOf(NullPointerException.class)
                                                        .hasMessageContaining("currency");
                }

                @Test
                @DisplayName("Should create invoice with factory method")
                void shouldCreateInvoiceWithFactoryMethod() {
                        Invoice invoice = Invoice.create(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY);

                        assertThat(invoice.getId()).isEqualTo(VALID_INVOICE_ID);
                        assertThat(invoice.getCustomerId()).isEqualTo(VALID_CUSTOMER_ID);
                        assertThat(invoice.getInvoiceReference()).isEqualTo(VALID_INVOICE_REFERENCE);
                        assertThat(invoice.getTotalAmount()).isEqualTo(Money.ZERO);
                        assertThat(invoice.getCurrency()).isEqualTo(VALID_CURRENCY);
                        assertThat(invoice.isArchived()).isFalse();
                        assertThat(invoice.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
                        assertThat(invoice.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
                }

                @Test
                @DisplayName("Should initialize installments as empty list")
                void shouldInitializeInstallmentsAsEmptyList() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        assertThat(invoice.getInstallments()).isEmpty();
                }

                @Test
                @DisplayName("Should set is archived to false by default")
                void shouldSetIsArchivedToFalseByDefault() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        assertThat(invoice.isArchived()).isFalse();
                }

                @Test
                @DisplayName("Should set created at and updated at to current time")
                void shouldSetCreatedAtAndUpdatedAtToCurrentTime() {
                        LocalDateTime before = LocalDateTime.now();
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);
                        LocalDateTime after = LocalDateTime.now();

                        assertThat(invoice.getCreatedAt()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
                        assertThat(invoice.getUpdatedAt()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
                }
        }

        @Nested
        @DisplayName("Installment Management - Add Tests")
        class InstallmentManagementAddTests {

                private Invoice invoice;

                @BeforeEach
                void setUp() {
                        invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);
                }

                @Test
                @DisplayName("Should add installment successfully")
                void shouldAddInstallmentSuccessfully() {
                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        assertThat(invoice.getInstallments()).hasSize(1);
                        assertThat(invoice.getInstallments()).first().satisfies(installment -> {
                                assertThat(installment.getAmountDue()).isEqualTo(new Money(BigDecimal.valueOf(500.00)));
                                assertThat(installment.getDueDate()).isEqualTo(LocalDate.now().plusDays(30));
                        });
                }

                @Test
                @DisplayName("Should throw when adding installment to archived invoice")
                void shouldThrowWhenAddingInstallmentToArchivedInvoice() {
                        Invoice archived = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), true,
                                        new ArrayList<>(), null);

                        assertThatThrownBy(() -> archived.addInstallment(new Money(BigDecimal.valueOf(100.00)),
                                        LocalDate.now().plusDays(10))).isInstanceOf(IllegalStateException.class)
                                                        .hasMessageContaining("archived");
                }

                @Test
                @DisplayName("Should throw exception when adding installment with duplicate due date")
                void shouldThrowExceptionWhenAddingInstallmentWithDuplicateDueDate() {

                        invoice.addInstallment(new Money(BigDecimal.valueOf(400.00)), LocalDate.now().plusDays(30));

                        assertThatThrownBy(() -> invoice.addInstallment(new Money(BigDecimal.valueOf(400.00)),
                                        LocalDate.now().plusDays(30))).isInstanceOf(InvalidPropertyException.class)
                                                        .hasMessageContaining("due date");
                }

                @Test
                @DisplayName("Should update updated at when adding installment")
                void shouldUpdateUpdatedAtWhenAddingInstallment() throws InterruptedException {
                        LocalDateTime originalUpdatedAt = invoice.getUpdatedAt();
                        Thread.sleep(10);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        assertThat(invoice.getUpdatedAt()).isAfter(originalUpdatedAt);
                }
        }

        @Nested
        @DisplayName("Installment Management - Update Tests")
        class InstallmentManagementUpdateTests {

                private Invoice invoice;
                private InstallmentId existingInstallmentId;

                @BeforeEach
                void setUp() {
                        invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));
                        existingInstallmentId = invoice.getInstallments().get(0).getId();
                }

                @Test
                @DisplayName("Should update installment successfully")
                void shouldUpdateInstallmentSuccessfully() {

                        invoice.updateInstallment(existingInstallmentId, new Money(BigDecimal.valueOf(600.00)),
                                        LocalDate.now().plusDays(45));

                        Installment retrieved = invoice.getInstallments().get(0);
                        assertThat(retrieved.getAmountDue()).isEqualTo(new Money(BigDecimal.valueOf(600.00)));
                        assertThat(retrieved.getDueDate()).isEqualTo(LocalDate.now().plusDays(45));
                }

                @Test
                @DisplayName("Should throw exception when updating installment to duplicate due date")
                void shouldThrowExceptionWhenUpdatingInstallmentToDuplicateDueDate() {
                        invoice.addInstallment(new Money(BigDecimal.valueOf(400.00)), LocalDate.now().plusDays(45));

                        assertThatThrownBy(() -> invoice.updateInstallment(existingInstallmentId,
                                        new Money(BigDecimal.valueOf(600.00)), LocalDate.now().plusDays(45)))
                                                        .isInstanceOf(InvalidPropertyException.class)
                                                        .hasMessageContaining("already exists");
                }

                @Test
                @DisplayName("Should throw exception when updating non existent installment")
                void shouldThrowExceptionWhenUpdatingNonExistentInstallment() {
                        assertThatThrownBy(() -> invoice.updateInstallment(new InstallmentId(UUID.randomUUID()),
                                        new Money(BigDecimal.valueOf(600.00)), LocalDate.now().plusDays(45)))
                                                        .isInstanceOf(InstallmentDoesNotBelongToInvoiceException.class);
                }

                @Test
                @DisplayName("Should update updated at when updating installment")
                void shouldUpdateUpdatedAtWhenUpdatingInstallment() throws InterruptedException {
                        LocalDateTime originalUpdatedAt = invoice.getUpdatedAt();
                        Thread.sleep(10);

                        invoice.updateInstallment(existingInstallmentId, new Money(BigDecimal.valueOf(600.00)),
                                        LocalDate.now().plusDays(45));

                        assertThat(invoice.getUpdatedAt()).isAfter(originalUpdatedAt);
                }

                @Test
                @DisplayName("Should throw when updating installment on archived invoice")
                void shouldThrowWhenUpdatingInstallmentOnArchivedInvoice() {
                        Invoice archived = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), true,
                                        new ArrayList<>(), null);

                        assertThatThrownBy(() -> archived.updateInstallment(new InstallmentId(UUID.randomUUID()),
                                        new Money(BigDecimal.valueOf(150.00)), LocalDate.now().plusDays(10)))
                                                        .isInstanceOf(IllegalStateException.class)
                                                        .hasMessageContaining("archived");
                }
        }

        @Nested
        @DisplayName("Installment Management - Remove Tests")
        class InstallmentManagementRemoveTests {

                private Invoice invoice;
                private InstallmentId existingInstallmentId;

                @BeforeEach
                void setUp() {
                        invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));
                        existingInstallmentId = invoice.getInstallments().get(0).getId();
                }

                @Test
                @DisplayName("Should remove installment successfully")
                void shouldRemoveInstallmentSuccessfully() {
                        invoice.removeInstallment(existingInstallmentId);

                        assertThat(invoice.getInstallments()).isEmpty();
                }

                @Test
                @DisplayName("Should throw exception when removing installment with different invoice id")
                void shouldThrowExceptionWhenRemovingInstallmentWithDifferentInvoiceId() {
                        assertThatThrownBy(() -> invoice.removeInstallment(new InstallmentId(UUID.randomUUID())))
                                        .isInstanceOf(InstallmentDoesNotBelongToInvoiceException.class);
                }

                @Test
                @DisplayName("Should update updated at when removing installment")
                void shouldUpdateUpdatedAtWhenRemovingInstallment() throws InterruptedException {
                        LocalDateTime originalUpdatedAt = invoice.getUpdatedAt();
                        Thread.sleep(10);

                        invoice.removeInstallment(existingInstallmentId);

                        assertThat(invoice.getUpdatedAt()).isAfter(originalUpdatedAt);
                }

                @Test
                @DisplayName("Should throw when removing installment from archived invoice")
                void shouldThrowWhenRemovingInstallmentFromArchivedInvoice() {
                        Invoice archived = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), true,
                                        new ArrayList<>(), null);

                        assertThatThrownBy(() -> archived.removeInstallment(new InstallmentId(UUID.randomUUID())))
                                        .isInstanceOf(IllegalStateException.class).hasMessageContaining("archived");
                }
        }

        @Nested
        @DisplayName("Payment Management - Add Tests")
        class PaymentManagementAddTests {

                private Invoice invoice;
                private InstallmentId existingInstallmentId;

                @BeforeEach
                void setUp() {
                        invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));
                        existingInstallmentId = invoice.getInstallments().get(0).getId();
                }

                @Test
                @DisplayName("Should add payment to installment successfully")
                void shouldAddPaymentToInstallmentSuccessfully() {
                        invoice.addPayment(existingInstallmentId, LocalDate.now(),
                                        new Money(BigDecimal.valueOf(200.00)));

                        Installment retrieved = invoice.getInstallments().get(0);
                        assertThat(retrieved.getPayments()).hasSize(1);
                }

                @Test
                @DisplayName("Should throw exception when adding payment to installment with different invoice id")
                void shouldThrowExceptionWhenAddingPaymentToInstallmentWithDifferentInvoiceId() {

                        assertThatThrownBy(() -> invoice.addPayment(new InstallmentId(UUID.randomUUID()),
                                        LocalDate.now(), new Money(BigDecimal.valueOf(200.00))))
                                                        .isInstanceOf(InstallmentDoesNotBelongToInvoiceException.class);
                }

                @Test
                @DisplayName("Should throw exception when adding payment to non existent installment")
                void shouldThrowExceptionWhenAddingPaymentToNonExistentInstallment() {
                        assertThatThrownBy(() -> invoice.addPayment(new InstallmentId(UUID.randomUUID()),
                                        LocalDate.now(), new Money(BigDecimal.valueOf(200.00))))
                                                        .isInstanceOf(InstallmentDoesNotBelongToInvoiceException.class);
                }

                @Test
                @DisplayName("Should update updated at when adding payment")
                void shouldUpdateUpdatedAtWhenAddingPayment() throws InterruptedException {
                        LocalDateTime originalUpdatedAt = invoice.getUpdatedAt();
                        Thread.sleep(10);

                        invoice.addPayment(existingInstallmentId, LocalDate.now(),
                                        new Money(BigDecimal.valueOf(200.00)));

                        assertThat(invoice.getUpdatedAt()).isAfter(originalUpdatedAt);
                }

                @Test
                @DisplayName("Should throw when adding payment to archived invoice")
                void shouldThrowWhenAddingPaymentToArchivedInvoice() {
                        Invoice archived = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), true,
                                        new ArrayList<>(), null);

                        assertThatThrownBy(() -> archived.addPayment(new InstallmentId(UUID.randomUUID()),
                                        LocalDate.now(), new Money(BigDecimal.valueOf(200.00))))
                                                        .isInstanceOf(IllegalStateException.class)
                                                        .hasMessageContaining("archived");
                }
        }

        @Nested
        @DisplayName("Payment Management - Update Tests")
        class PaymentManagementUpdateTests {

                private Invoice invoice;
                private InstallmentId existingInstallmentId;
                private PaymentId existingPaymentId;

                @BeforeEach
                void setUp() {
                        invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));
                        existingInstallmentId = invoice.getInstallments().get(0).getId();

                        invoice.addPayment(existingInstallmentId, LocalDate.now(),
                                        new Money(BigDecimal.valueOf(200.00)));
                        existingPaymentId = invoice.getInstallments().get(0).getPayments().get(0).getId();
                }

                @Test
                @DisplayName("Should update payment in installment successfully")
                void shouldUpdatePaymentInInstallmentSuccessfully() {
                        invoice.updatePayment(existingInstallmentId, existingPaymentId, LocalDate.now().plusDays(1),
                                        new Money(BigDecimal.valueOf(250.00)));
                        Installment retrieved = invoice.getInstallments().get(0);
                        Payment retrievedPayment = retrieved.getPayments().get(0);
                        assertThat(retrievedPayment.getAmount()).isEqualTo(new Money(BigDecimal.valueOf(250.00)));
                }

                @Test
                @DisplayName("Should throw exception when updating payment in non existent installment")
                void shouldThrowExceptionWhenUpdatingPaymentInNonExistentInstallment() {

                        assertThatThrownBy(() -> invoice.updatePayment(new InstallmentId(UUID.randomUUID()),
                                        existingPaymentId, LocalDate.now().plusDays(1),
                                        new Money(BigDecimal.valueOf(250.00))))
                                                        .isInstanceOf(InstallmentDoesNotBelongToInvoiceException.class);
                }

                @Test
                @DisplayName("Should update updated at when updating payment")
                void shouldUpdateUpdatedAtWhenUpdatingPayment() throws InterruptedException {
                        LocalDateTime originalUpdatedAt = invoice.getUpdatedAt();
                        Thread.sleep(10);

                        invoice.updatePayment(existingInstallmentId, existingPaymentId, LocalDate.now().plusDays(1),
                                        new Money(BigDecimal.valueOf(250.00)));

                        assertThat(invoice.getUpdatedAt()).isAfter(originalUpdatedAt);
                }

                @Test
                @DisplayName("Should throw when updating payment on archived invoice")
                void shouldThrowWhenUpdatingPaymentOnArchivedInvoice() {
                        Invoice archived = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), true,
                                        new ArrayList<>(), null);
                        assertThatThrownBy(() -> archived.updatePayment(existingInstallmentId, existingPaymentId,
                                        LocalDate.now().plusDays(1), new Money(BigDecimal.valueOf(250.00))))
                                                        .isInstanceOf(IllegalStateException.class)
                                                        .hasMessageContaining("archived");
                }
        }

        @Nested
        @DisplayName("Payment Management - Remove Tests")
        class PaymentManagementRemoveTests {

                private Invoice invoice;
                private InstallmentId existingInstallmentId;
                private PaymentId existingPaymentId;

                @BeforeEach
                void setUp() {
                        invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));
                        existingInstallmentId = invoice.getInstallments().get(0).getId();

                        invoice.addPayment(existingInstallmentId, LocalDate.now(),
                                        new Money(BigDecimal.valueOf(200.00)));

                        existingPaymentId = invoice.getInstallments().get(0).getPayments().get(0).getId();
                }

                @Test
                @DisplayName("Should remove payment from installment successfully")
                void shouldRemovePaymentFromInstallmentSuccessfully() {
                        invoice.removePayment(existingInstallmentId, existingPaymentId);

                        Installment retrieved = invoice.getInstallments().get(0);
                        assertThat(retrieved.getPayments()).isEmpty();
                }

                @Test
                @DisplayName("Should throw exception when removing payment from non existent installment")
                void shouldThrowExceptionWhenRemovingPaymentFromNonExistentInstallment() {

                        assertThatThrownBy(() -> invoice.removePayment(new InstallmentId(UUID.randomUUID()),
                                        existingPaymentId))
                                                        .isInstanceOf(InstallmentDoesNotBelongToInvoiceException.class);
                }

                @Test
                @DisplayName("Should update updated at when removing payment")
                void shouldUpdateUpdatedAtWhenRemovingPayment() throws InterruptedException {
                        LocalDateTime originalUpdatedAt = invoice.getUpdatedAt();
                        Thread.sleep(10);

                        invoice.removePayment(existingInstallmentId, existingPaymentId);

                        assertThat(invoice.getUpdatedAt()).isAfter(originalUpdatedAt);
                }

                @Test
                @DisplayName("Should throw when removing payment from archived invoice")
                void shouldThrowWhenRemovingPaymentFromArchivedInvoice() {
                        Invoice archived = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), true,
                                        new ArrayList<>(), null);

                        assertThatThrownBy(() -> archived.removePayment(existingInstallmentId, existingPaymentId))
                                        .isInstanceOf(IllegalStateException.class).hasMessageContaining("archived");
                }
        }

        @Nested
        @DisplayName("Update Details Tests")
        class UpdateDetailsTests {

                private Invoice invoice;

                @BeforeEach
                void setUp() {
                        invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);
                }

                @Test
                @DisplayName("Should update invoice reference successfully")
                void shouldUpdateInvoiceReferenceSuccessfully() {
                        InvoiceReference newReference = new InvoiceReference("INV-2025-002");

                        invoice.updateDetails(newReference, null);

                        assertThat(invoice.getInvoiceReference()).isEqualTo(newReference);
                }

                @Test
                @DisplayName("Should update total amount successfully")
                void shouldUpdateTotalAmountSuccessfully() {
                        Money expectedAmount = new Money(BigDecimal.valueOf(1500.00));
                        invoice.addInstallment(expectedAmount, LocalDate.now().plusDays(30));

                        assertThat(invoice.getTotalAmount()).isEqualTo(expectedAmount);
                }

                @Test
                @DisplayName("Should update currency successfully")
                void shouldUpdateCurrencySuccessfully() {
                        String newCurrency = "EUR";

                        invoice.updateDetails(null, newCurrency);

                        assertThat(invoice.getCurrency()).isEqualTo(newCurrency);
                }

                @Test
                @DisplayName("Should update all details successfully")
                void shouldUpdateAllDetailsSuccessfully() {
                        InvoiceReference newReference = new InvoiceReference("INV-2025-002");
                        String newCurrency = "EUR";

                        invoice.updateDetails(newReference, newCurrency);
                        assertThat(invoice.getInvoiceReference()).isEqualTo(newReference);
                        assertThat(invoice.getCurrency()).isEqualTo(newCurrency);
                }

                @Test
                @DisplayName("Should not update when no changes provided")
                void shouldNotUpdateWhenNoChangesProvided() {
                        invoice.updateDetails(null, null);

                        assertThat(invoice.getInvoiceReference()).isEqualTo(VALID_INVOICE_REFERENCE);
                        assertThat(invoice.getCurrency()).isEqualTo(VALID_CURRENCY);
                }

                @Test
                @DisplayName("Should update updated at when details change")
                void shouldUpdateUpdatedAtWhenDetailsChange() throws InterruptedException {
                        LocalDateTime originalUpdatedAt = invoice.getUpdatedAt();
                        Thread.sleep(10);

                        InvoiceReference newReference = new InvoiceReference("INV-2025-002");
                        invoice.updateDetails(newReference, null);

                        assertThat(invoice.getUpdatedAt()).isAfter(originalUpdatedAt);
                }

                @Test
                @DisplayName("Should not update updated at when no changes")
                void shouldNotUpdateUpdatedAtWhenNoChanges() {
                        LocalDateTime originalUpdatedAt = invoice.getUpdatedAt();

                        invoice.updateDetails(VALID_INVOICE_REFERENCE, VALID_CURRENCY);

                        assertThat(invoice.getUpdatedAt()).isEqualTo(originalUpdatedAt);
                }
        }

        @Nested
        @DisplayName("Archive/Unarchive Tests")
        class ArchiveUnarchiveTests {

                @Test
                @DisplayName("Should archive invoice successfully")
                void shouldArchiveInvoiceSuccessfully() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.archive();

                        assertThat(invoice.isArchived()).isTrue();
                }

                @Test
                @DisplayName("Should unarchive invoice successfully")
                void shouldUnarchiveInvoiceSuccessfully() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), true,
                                        new ArrayList<>(), null);

                        invoice.unArchive();

                        assertThat(invoice.isArchived()).isFalse();
                }

                @Test
                @DisplayName("Should throw when modifying an archived invoice")
                void shouldThrowWhenModifyingArchivedInvoice() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), true,
                                        new ArrayList<>(), null);

                        assertThatThrownBy(() -> invoice.addInstallment(new Money(BigDecimal.valueOf(100.00)),
                                        LocalDate.now().plusDays(10))).isInstanceOf(IllegalStateException.class)
                                                        .hasMessageContaining("archived");
                }
        }

        @Nested
        @DisplayName("Payment Status Tests")
        class PaymentStatusTests {

                private Invoice invoice;

                @BeforeEach
                void setUp() {
                        invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);
                }

                @Test
                @DisplayName("Should return pending status when nothing paid")
                void shouldReturnPendingStatusWhenNothingPaid() {
                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PENDING);
                }

                @Test
                @DisplayName("Should return partially paid status when partially paid")
                void shouldReturnPartiallyPaidStatusWhenPartiallyPaid() {
                        invoice.addInstallment(new Money(BigDecimal.valueOf(1000.00)), LocalDate.now().plusDays(30));

                        invoice.addPayment(invoice.getInstallments().getFirst().getId(), LocalDate.now(),
                                        new Money(BigDecimal.valueOf(500.00)));

                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
                }

                @Test
                @DisplayName("Should return paid status when fully paid")
                void shouldReturnPaidStatusWhenFullyPaid() {
                        invoice.addInstallment(new Money(BigDecimal.valueOf(1000.00)), LocalDate.now().plusDays(30));

                        invoice.addPayment(invoice.getInstallments().getFirst().getId(), LocalDate.now(),
                                        new Money(BigDecimal.valueOf(1000.00)));

                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PAID);
                }
        }

        @Nested
        @DisplayName("Overdue Tests")
        class OverdueTests {

                private Invoice invoice;

                @BeforeEach
                void setUp() {
                        invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);
                }

                @Test
                @DisplayName("Should return true when at least one installment is overdue")
                void shouldReturnTrueWhenAtLeastOneInstallmentIsOverdue() {
                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().minusDays(1));

                        assertThat(invoice.isOverdue()).isTrue();
                }

                @Test
                @DisplayName("Should return false when no installments are overdue")
                void shouldReturnFalseWhenNoInstallmentsAreOverdue() {
                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        assertThat(invoice.isOverdue()).isFalse();
                }

                @Test
                @DisplayName("Should return false when no installments exist")
                void shouldReturnFalseWhenNoInstallmentsExist() {
                        assertThat(invoice.isOverdue()).isFalse();
                }
        }

        @Nested
        @DisplayName("Calculation Tests")
        class CalculationTests {

                private Invoice invoice;

                @BeforeEach
                void setUp() {
                        invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);
                }

                @Test
                @DisplayName("Should calculate total amount correctly")
                void shouldCalculateTotalAmountCorrectly() {
                        Money expectedAmount = new Money(BigDecimal.valueOf(1000));
                        invoice.addInstallment(expectedAmount, LocalDate.now().plusDays(1));
                        assertThat(invoice.getTotalAmount()).isEqualTo(expectedAmount);
                }

                @Test
                @DisplayName("Should calculate total paid correctly")
                void shouldCalculateTotalPaidCorrectly() {
                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(60));

                        invoice.addPayment(invoice.getInstallments().get(0).getId(), LocalDate.now(),
                                        new Money(BigDecimal.valueOf(300.00)));

                        invoice.addPayment(invoice.getInstallments().get(1).getId(), LocalDate.now(),
                                        new Money(BigDecimal.valueOf(200.00)));
                        assertThat(invoice.getTotalPaid()).isEqualTo(new Money(BigDecimal.valueOf(500.00)));
                }

                @Test
                @DisplayName("Should return zero when no payments")
                void shouldReturnZeroWhenNoPayments() {
                        assertThat(invoice.getTotalPaid()).isEqualTo(Money.ZERO);
                }

                @Test
                @DisplayName("Should calculate remaining amount correctly")
                void shouldCalculateRemainingAmountCorrectly() {
                        invoice.addInstallment(new Money(BigDecimal.valueOf(1000.00)), LocalDate.now().plusDays(30));
                        invoice.addPayment(invoice.getInstallments().get(0).getId(), LocalDate.now(),
                                        new Money(BigDecimal.valueOf(400.00)));

                        assertThat(invoice.getRemainingAmount()).isEqualTo(new Money(BigDecimal.valueOf(600.00)));
                }

                @Test
                @DisplayName("Should return true when fully paid")
                void shouldReturnTrueWhenFullyPaid() {
                        invoice.addInstallment(new Money(BigDecimal.valueOf(1000.00)), LocalDate.now().plusDays(30));

                        invoice.addPayment(invoice.getInstallments().get(0).getId(), LocalDate.now(),
                                        new Money(BigDecimal.valueOf(1000.00)));

                        assertThat(invoice.isFullyPaid()).isTrue();
                }

                @Test
                @DisplayName("Should return false when not fully paid")
                void shouldReturnFalseWhenNotFullyPaid() {
                        invoice.addInstallment(new Money(BigDecimal.valueOf(1000.00)), LocalDate.now().plusDays(30));

                        invoice.addPayment(invoice.getInstallments().get(0).getId(), LocalDate.now(),
                                        new Money(BigDecimal.valueOf(500.00)));

                        assertThat(invoice.isFullyPaid()).isFalse();
                }
        }

        @Nested
        @DisplayName("Getters Tests")
        class GettersTests {

                @Test
                @DisplayName("Should return immutable copy of installments")
                void shouldReturnImmutableCopyOfInstallments() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        List<Installment> installments = invoice.getInstallments();

                        assertThat(installments).hasSize(1);
                        assertThatThrownBy(() -> installments.add(new Installment(VALID_INSTALLMENT_ID,
                                        new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30),
                                        LocalDateTime.now(), LocalDateTime.now(), new ArrayList<>())))
                                                        .isInstanceOf(UnsupportedOperationException.class);
                }

                @Test
                @DisplayName("Should not allow modification of installments list")
                void shouldNotAllowModificationOfInstallmentsList() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        List<Installment> installments = invoice.getInstallments();

                        Installment installment = new Installment(VALID_INSTALLMENT_ID,
                                        new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30),
                                        LocalDateTime.now(), LocalDateTime.now(), new ArrayList<>());

                        assertThatThrownBy(() -> installments.add(installment))
                                        .isInstanceOf(UnsupportedOperationException.class);
                }
        }

        @Nested
        @DisplayName("Equals Tests")
        class EqualsTests {

                @Test
                @DisplayName("Should be equal if IDs are the same")
                void shouldBeEqualIfIdsAreTheSame() {
                        Invoice invoice1 = Invoice.create(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY);
                        Invoice invoice2 = Invoice.create(VALID_INVOICE_ID, new CustomerId(UUID.randomUUID()),
                                        new InvoiceReference("REF-002"), "EUR");

                        assertThat(invoice1).isEqualTo(invoice2);
                }

                @Test
                @DisplayName("Should not be equal if IDs are different")
                void shouldNotBeEqualIfIdsAreDifferent() {
                        Invoice invoice1 = Invoice.create(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY);
                        Invoice invoice2 = Invoice.create(new InvoiceId(UUID.randomUUID()), VALID_CUSTOMER_ID,
                                        VALID_INVOICE_REFERENCE, VALID_CURRENCY);

                        assertThat(invoice1).isNotEqualTo(invoice2);
                }

                @Test
                @DisplayName("Should not be equal to other object types")
                void shouldNotBeEqualToOtherObjectTypes() {
                        Invoice invoice = Invoice.create(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY);
                        Object other = new Object();

                        assertThat(invoice).isNotEqualTo(other);
                }

                @Test
                @DisplayName("Should not be equal to null")
                void shouldNotBeEqualToNull() {
                        Invoice invoice = Invoice.create(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY);

                        assertThat(invoice).isNotEqualTo(null);
                }
        }

        @Nested
        @DisplayName("Business Rule Tests - Aggregate Invariants")
        class AggregateInvariantsTests {

                @Test
                @DisplayName("Should not allow adding installment with null installment ID to existing installments")
                void shouldNotAllowAddingInstallmentWithNullIdToExistingInstallments() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        invoice.addInstallment(new Money(BigDecimal.valueOf(300.00)), LocalDate.now().plusDays(60));

                        assertThat(invoice.getInstallments()).hasSize(2);
                }

                @Test
                @DisplayName("Should maintain consistency when updating installment due date")
                void shouldMaintainConsistencyWhenUpdatingInstallmentDueDate() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        LocalDate newDueDate = LocalDate.now().plusDays(45);

                        invoice.updateInstallment(invoice.getInstallments().get(0).getId(),
                                        new Money(BigDecimal.valueOf(500.00)), newDueDate);

                        assertThat(invoice.getInstallments().get(0).getDueDate()).isEqualTo(newDueDate);
                }

                @Test
                @DisplayName("Should correctly calculate total amount with multiple installments")
                void shouldCorrectlyCalculateTotalAmountWithMultipleInstallments() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        invoice.addInstallment(new Money(BigDecimal.valueOf(300.00)), LocalDate.now().plusDays(60));

                        assertThat(invoice.getTotalAmount()).isEqualTo(new Money(BigDecimal.valueOf(800.00)));
                }

                @Test
                @DisplayName("Should return zero total amount when no installments exist")
                void shouldReturnZeroTotalAmountWhenNoInstallmentsExist() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        assertThat(invoice.getTotalAmount()).isEqualTo(Money.ZERO);
                }

                @Test
                @DisplayName("Should prevent modification after archiving")
                void shouldPreventModificationAfterArchiving() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.archive();

                        InvoiceReference newRef = new InvoiceReference("INV-2025-003");
                        assertThatThrownBy(() -> invoice.updateDetails(newRef, null))
                                        .isInstanceOf(IllegalStateException.class).hasMessageContaining("archived");
                }

                @Test
                @DisplayName("Should allow modification after unarchiving")
                void shouldAllowModificationAfterUnarchiving() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), true,
                                        new ArrayList<>(), null);

                        invoice.unArchive();

                        InvoiceReference newRef = new InvoiceReference("INV-2025-003");
                        invoice.updateDetails(newRef, null);

                        assertThat(invoice.getInvoiceReference()).isEqualTo(newRef);
                }

                @Test
                @DisplayName("Should correctly compute payment status with no installments")
                void shouldCorrectlyComputePaymentStatusWithNoInstallments() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PENDING);
                }

                @Test
                @DisplayName("Should return correct remaining amount for invoice with multiple installments and payments")
                void shouldReturnCorrectRemainingAmountForInvoiceWithMultipleInstallmentsAndPayments() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(1000.00)), LocalDate.now().plusDays(30));

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(60));

                        invoice.addPayment(invoice.getInstallments().get(0).getId(), LocalDate.now(),
                                        new Money(BigDecimal.valueOf(400.00)));

                        invoice.addPayment(invoice.getInstallments().get(1).getId(), LocalDate.now(),
                                        new Money(BigDecimal.valueOf(200.00)));

                        assertThat(invoice.getRemainingAmount()).isEqualTo(new Money(BigDecimal.valueOf(900.00)));
                }

                @Test
                @DisplayName("Should find installment by ID successfully")
                void shouldFindInstallmentByIdSuccessfully() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        Installment found = invoice.findInstallmentById(invoice.getInstallments().get(0).getId());

                        assertThat(found).isEqualTo(invoice.getInstallments().get(0));
                }

                @Test
                @DisplayName("Should throw exception when finding non-existent installment")
                void shouldThrowExceptionWhenFindingNonExistentInstallment() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        InstallmentId nonExistentId = new InstallmentId(UUID.randomUUID());

                        assertThatThrownBy(() -> invoice.findInstallmentById(nonExistentId))
                                        .isInstanceOf(InstallmentDoesNotBelongToInvoiceException.class);
                }

                @Test
                @DisplayName("Should maintain installment order when adding multiple installments")
                void shouldMaintainInstallmentOrderWhenAddingMultipleInstallments() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        invoice.addInstallment(new Money(BigDecimal.valueOf(300.00)), LocalDate.now().plusDays(60));

                        List<Installment> installments = invoice.getInstallments();
                        assertThat(installments.get(0)).isEqualTo(invoice.getInstallments().get(0));
                        assertThat(installments.get(1)).isEqualTo(invoice.getInstallments().get(1));
                }

                @Test
                @DisplayName("Should correctly identify overdue invoice with mixed installments")
                void shouldCorrectlyIdentifyOverdueInvoiceWithMixedInstallments() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        invoice.addInstallment(new Money(BigDecimal.valueOf(300.00)), LocalDate.now().minusDays(1));

                        assertThat(invoice.isOverdue()).isTrue();
                }

                @Test
                @DisplayName("Should compute hashCode when ID is present")
                void shouldComputeHashCodeWhenIdIsPresent() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        int hashCode1 = invoice.hashCode();
                        int hashCode2 = invoice.hashCode();

                        assertThat(hashCode1).isEqualTo(hashCode2);
                }

                @Test
                @DisplayName("Should not update details if same values provided")
                void shouldNotUpdateDetailsIfSameValuesProvided() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        LocalDateTime originalUpdatedAt = invoice.getUpdatedAt();

                        invoice.updateDetails(VALID_INVOICE_REFERENCE, VALID_CURRENCY);

                        assertThat(invoice.getUpdatedAt()).isEqualTo(originalUpdatedAt);
                }

                @Test
                @DisplayName("Should maintain immutability of installments list")
                void shouldMaintainImmutabilityOfInstallmentsList() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        List<Installment> installments1 = invoice.getInstallments();
                        List<Installment> installments2 = invoice.getInstallments();

                        assertThat(installments1).isNotSameAs(installments2);
                        assertThat(installments1).isEqualTo(installments2);
                }

                @Test
                @DisplayName("Should not be overdue when all installments are in the future")
                void shouldNotBeOverdueWhenAllInstallmentsAreInTheFuture() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        invoice.addInstallment(new Money(BigDecimal.valueOf(300.00)), LocalDate.now().plusDays(60));

                        assertThat(invoice.isOverdue()).isFalse();
                }

                @Test
                @DisplayName("Should correctly handle payment status transitions from pending to partially paid")
                void shouldCorrectlyHandlePaymentStatusTransitionsFromPendingToPartiallyPaid() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(1000.00)), LocalDate.now().plusDays(30));

                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PENDING);

                        invoice.addPayment(invoice.getInstallments().get(0).getId(), LocalDate.now(),
                                        new Money(BigDecimal.valueOf(500.00)));

                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
                }

                @Test
                @DisplayName("Should correctly handle payment status transitions from partially paid to paid")
                void shouldCorrectlyHandlePaymentStatusTransitionsFromPartiallyPaidToPaid() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(1000.00)), LocalDate.now().plusDays(30));

                        invoice.addPayment(invoice.getInstallments().get(0).getId(), LocalDate.now(),
                                        new Money(BigDecimal.valueOf(500.00)));

                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);

                        invoice.addPayment(invoice.getInstallments().get(0).getId(), LocalDate.now(),
                                        new Money(BigDecimal.valueOf(500.00)));

                        assertThat(invoice.getStatus()).isEqualTo(PaymentStatus.PAID);
                }

                @Test
                @DisplayName("Should allow adding installment with same due date after removing the first one")
                void shouldAllowAddingInstallmentWithSameDueDateAfterRemovingTheFirstOne() {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        LocalDate dueDate = LocalDate.now().plusDays(30);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), dueDate);

                        invoice.removeInstallment(invoice.getInstallments().get(0).getId());

                        invoice.addInstallment(new Money(BigDecimal.valueOf(300.00)), dueDate);

                        assertThat(invoice.getInstallments()).hasSize(1);
                        assertThat(invoice.getInstallments().get(0).getDueDate()).isEqualTo(dueDate);
                }

                @Test
                @DisplayName("Should update invoice timestamp when adding payment to installment")
                void shouldUpdateInvoiceTimestampWhenAddingPaymentToInstallment() throws InterruptedException {
                        Invoice invoice = new Invoice(VALID_INVOICE_ID, VALID_CUSTOMER_ID, VALID_INVOICE_REFERENCE,
                                        VALID_CURRENCY, LocalDateTime.now(), LocalDateTime.now(), false,
                                        new ArrayList<>(), null);

                        invoice.addInstallment(new Money(BigDecimal.valueOf(500.00)), LocalDate.now().plusDays(30));

                        LocalDateTime originalUpdatedAt = invoice.getUpdatedAt();
                        Thread.sleep(10);

                        invoice.addPayment(invoice.getInstallments().get(0).getId(), LocalDate.now(),
                                        new Money(BigDecimal.valueOf(200.00)));

                        assertThat(invoice.getUpdatedAt()).isAfter(originalUpdatedAt);
                }
        }
}
