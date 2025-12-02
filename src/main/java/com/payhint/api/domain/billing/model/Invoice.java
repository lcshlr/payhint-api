package com.payhint.api.domain.billing.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.payhint.api.domain.billing.exception.InstallmentDoesNotBelongToInvoiceException;
import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.billing.valueobject.PaymentId;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class Invoice {

    private InvoiceId id;

    private CustomerId customerId;

    @NonNull
    private InvoiceReference invoiceReference;

    @NonNull
    private Money totalAmount;

    @NonNull
    private Money totalPaid;

    @NonNull
    private String currency;

    @NonNull
    private PaymentStatus status;

    @NonNull
    private LocalDateTime createdAt;

    @NonNull
    private LocalDateTime updatedAt;

    @NonNull
    private LocalDateTime lastStatusChangeAt;

    private boolean isArchived;

    @Builder.Default
    private List<Installment> installments = new ArrayList<>();

    private Long version;

    public Invoice(@NonNull InvoiceId id, CustomerId customerId, @NonNull InvoiceReference invoiceReference,
            @NonNull Money totalAmount, @NonNull Money totalPaid, @NonNull String currency,
            @NonNull PaymentStatus status, @NonNull LocalDateTime createdAt, @NonNull LocalDateTime updatedAt,
            @NonNull LocalDateTime lastStatusChangeAt, boolean isArchived, List<Installment> installments,
            Long version) {
        if (id == null) {
            throw new InvalidPropertyException("InvoiceId cannot be null");
        }
        this.id = id;
        this.customerId = customerId;
        this.invoiceReference = invoiceReference;
        this.totalAmount = totalAmount;
        this.totalPaid = totalPaid;
        this.status = status;
        this.currency = currency;
        this.isArchived = isArchived;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastStatusChangeAt = lastStatusChangeAt;
        this.version = version;
        this.installments = installments != null ? new ArrayList<>(installments) : new ArrayList<>();
    }

    public static Invoice create(@NonNull InvoiceId id, @NonNull CustomerId customerId,
            @NonNull InvoiceReference invoiceReference, @NonNull String currency) {
        return new Invoice(id, customerId, invoiceReference, Money.ZERO, Money.ZERO, currency, PaymentStatus.PENDING,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), false, new ArrayList<>(), null);
    }

    public List<Installment> getInstallments() {
        return this.installments == null ? List.of() : List.copyOf(installments);
    }

    private void ensureDueDateDoesNotExistInOtherInstallments(@NonNull LocalDate dueDate) {
        boolean dueDateExists = this.installments.stream()
                .anyMatch(installment -> installment.getDueDate().equals(dueDate));
        if (dueDateExists) {
            throw new InvalidPropertyException("An installment with same due date already exists.");
        }
    }

    public boolean isOverdue() {
        return installments.stream().anyMatch(installment -> installment.isOverdue());
    }

    public Money getRemainingAmount() {
        return totalAmount.subtract(totalPaid);
    }

    public boolean isFullyPaid() {
        return getRemainingAmount().compareTo(Money.ZERO) == 0;
    }

    public void unArchive() {
        this.isArchived = false;
    }

    public void archive() {
        this.isArchived = true;
    }

    private void ensureNotArchived() {
        if (this.isArchived) {
            throw new IllegalStateException("Cannot modify an archived invoice.");
        }
    }

    private void updateStatus() {
        PaymentStatus oldStatus = this.status;
        if (totalPaid.compareTo(totalAmount) >= 0) {
            this.status = PaymentStatus.PAID;
        } else if (totalPaid.compareTo(Money.ZERO) > 0) {
            this.status = PaymentStatus.PARTIALLY_PAID;
        } else {
            this.status = PaymentStatus.PENDING;
        }

        if (oldStatus != this.status) {
            this.lastStatusChangeAt = LocalDateTime.now();
        }
    }

    public void updateDetails(InvoiceReference invoiceReference, String currency) {
        ensureNotArchived();
        boolean updated = false;
        if (invoiceReference != null && !invoiceReference.equals(this.invoiceReference)) {
            this.invoiceReference = invoiceReference;
            updated = true;
        }
        if (currency != null && !currency.equals(this.currency)) {
            this.currency = currency;
            updated = true;
        }
        if (updated) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    private void ensureInstallmentCanBeUpdated(@NonNull Installment existingInstallment,
            @NonNull Installment updatedInstallment) {
        if (!existingInstallment.getDueDate().equals(updatedInstallment.getDueDate())) {
            ensureDueDateDoesNotExistInOtherInstallments(updatedInstallment.getDueDate());
        }
    }

    public Installment findInstallmentById(@NonNull InstallmentId installmentId) {
        return this.installments.stream().filter(inst -> inst.getId().equals(installmentId)).findFirst()
                .orElseThrow(() -> new InstallmentDoesNotBelongToInvoiceException(installmentId, this.id));
    }

    public void addInstallments(@NonNull List<Installment> installments) {
        ensureNotArchived();
        if (installments.isEmpty()) {
            return;
        }

        Set<LocalDate> newDueDates = new HashSet<>();
        for (Installment inst : installments) {
            if (!newDueDates.add(inst.getDueDate())) {
                throw new InvalidPropertyException(
                        "The payment schedule contains duplicate due dates: " + inst.getDueDate());
            }
        }

        for (Installment inst : installments) {
            ensureDueDateDoesNotExistInOtherInstallments(inst.getDueDate());
        }

        this.installments.addAll(installments);
        this.totalAmount = this.totalAmount
                .add(installments.stream().map(Installment::getAmountDue).reduce(Money.ZERO, Money::add));
        updateStatus();
        this.updatedAt = LocalDateTime.now();
    }

    public void addInstallment(Money amountDue, LocalDate dueDate) {
        InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
        ensureNotArchived();
        if (this.installments.stream().anyMatch(inst -> inst.getId().equals(installmentId))) {
            throw new InvalidPropertyException("Installment with this id already exists in the invoice.");
        }
        if (amountDue.compareTo(Money.ZERO) <= 0) {
            throw new InvalidMoneyValueException("Installment amountDue must be greater than zero");
        }
        ensureDueDateDoesNotExistInOtherInstallments(dueDate);
        Installment installment = Installment.create(installmentId, amountDue, dueDate);
        this.installments.add(installment);
        this.totalAmount = this.totalAmount.add(amountDue);
        updateStatus();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateInstallment(@NonNull InstallmentId installmentId, Money amountDue, LocalDate dueDate) {
        ensureNotArchived();
        Installment existingInstallment = findInstallmentById(installmentId);
        amountDue = amountDue != null ? amountDue : existingInstallment.getAmountDue();
        dueDate = dueDate != null ? dueDate : existingInstallment.getDueDate();
        Installment updatedInstallment = new Installment(installmentId, amountDue, existingInstallment.getAmountPaid(),
                dueDate, existingInstallment.getStatus(), existingInstallment.getCreatedAt(),
                existingInstallment.getUpdatedAt(), existingInstallment.getLastStatusChangeAt(),
                existingInstallment.getPayments());
        ensureInstallmentCanBeUpdated(existingInstallment, updatedInstallment);
        Money oldAmountDue = existingInstallment.getAmountDue();
        existingInstallment.updateDetails(updatedInstallment.getAmountDue(), updatedInstallment.getDueDate());
        if (amountDue != null) {
            this.totalAmount = this.totalAmount.subtract(oldAmountDue).add(amountDue);
            updateStatus();
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void removeInstallment(@NonNull InstallmentId installmentId) {
        ensureNotArchived();
        Installment installment = findInstallmentById(installmentId);
        if (!this.installments.remove(installment)) {
            throw new InstallmentDoesNotBelongToInvoiceException(installmentId, this.id);
        }
        this.totalAmount = this.totalAmount.subtract(installment.getAmountDue());
        updateStatus();
        this.updatedAt = LocalDateTime.now();
    }

    public void addPayment(@NonNull InstallmentId installmentId, LocalDate paymentDate, Money amount) {
        ensureNotArchived();
        Installment existingInstallment = findInstallmentById(installmentId);
        Payment payment = Payment.create(new PaymentId(UUID.randomUUID()), amount, paymentDate);
        existingInstallment.addPayment(payment);
        this.totalPaid = this.totalPaid.add(amount);
        updateStatus();
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePayment(@NonNull InstallmentId installmentId, @NonNull PaymentId paymentId, LocalDate paymentDate,
            Money amount) {
        ensureNotArchived();
        Installment existingInstallment = findInstallmentById(installmentId);
        Payment existingPayment = existingInstallment.findPaymentById(paymentId);
        paymentDate = paymentDate != null ? paymentDate : existingPayment.getPaymentDate();
        amount = amount != null ? amount : existingPayment.getAmount();
        Payment updatedPayment = new Payment(paymentId, amount, paymentDate, existingPayment.getCreatedAt(),
                existingPayment.getUpdatedAt());
        Money oldAmount = existingPayment.getAmount();
        existingInstallment.updatePayment(updatedPayment);
        if (amount != null) {
            this.totalPaid = this.totalPaid.subtract(oldAmount).add(amount);
            updateStatus();
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void removePayment(@NonNull InstallmentId installmentId, @NonNull PaymentId paymentId) {
        ensureNotArchived();
        Installment existingInstallment = findInstallmentById(installmentId);
        Payment payment = existingInstallment.findPaymentById(paymentId);
        existingInstallment.removePayment(payment);
        this.totalPaid = this.totalPaid.subtract(payment.getAmount());
        updateStatus();
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Invoice invoice = (Invoice) o;

        return id != null && id.equals(invoice.id);
    }
}
