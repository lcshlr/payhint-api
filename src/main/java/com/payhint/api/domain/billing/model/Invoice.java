package com.payhint.api.domain.billing.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.payhint.api.domain.billing.exception.InstallmentDoesNotBelongToInvoiceException;
import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.billing.valueobject.Money;
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
    private String currency;

    @NonNull
    private LocalDateTime createdAt;

    @NonNull
    private LocalDateTime updatedAt;

    private boolean isArchived;

    @Builder.Default
    private List<Installment> installments = new ArrayList<>();

    public Invoice(@NonNull InvoiceId id, CustomerId customerId, @NonNull InvoiceReference invoiceReference,
            @NonNull String currency, @NonNull LocalDateTime createdAt, @NonNull LocalDateTime updatedAt,
            boolean isArchived, List<Installment> installments) {
        if (id == null) {
            throw new InvalidPropertyException("InvoiceId cannot be null");
        }
        this.id = id;
        this.customerId = customerId;
        this.invoiceReference = invoiceReference;
        this.currency = currency;
        this.isArchived = isArchived;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.installments = installments != null ? new ArrayList<>(installments) : new ArrayList<>();
    }

    public static Invoice create(@NonNull InvoiceId id, @NonNull CustomerId customerId,
            @NonNull InvoiceReference invoiceReference, @NonNull String currency) {
        return new Invoice(id, customerId, invoiceReference, currency, LocalDateTime.now(), LocalDateTime.now(), false,
                new ArrayList<>());
    }

    public List<Installment> getInstallments() {
        return this.installments == null ? List.of() : List.copyOf(installments);
    }

    private void validateInstallmentBelonging(Installment installment) {
        if (installment.getInvoiceId() == null || !installment.getInvoiceId().equals(this.id)) {
            throw new InstallmentDoesNotBelongToInvoiceException(installment.getId(), this.id);
        }
    }

    private void ensureDueDateDoesNotExistInOtherInstallments(@NonNull LocalDate dueDate) {
        boolean dueDateExists = this.installments.stream()
                .anyMatch(installment -> installment.getDueDate().equals(dueDate));
        if (dueDateExists) {
            throw new InvalidPropertyException("An installment with due date " + dueDate + " already exists.");
        }
    }

    public PaymentStatus getStatus() {
        Money totalPaid = getTotalPaid();
        if (totalPaid.compareTo(getTotalAmount()) == 0 && getInstallments().size() > 0) {
            return PaymentStatus.PAID;
        } else if (totalPaid.compareTo(Money.ZERO) > 0) {
            return PaymentStatus.PARTIALLY_PAID;
        }
        return PaymentStatus.PENDING;
    }

    public Money getTotalAmount() {
        return installments.stream().map(Installment::getAmountDue).reduce(Money.ZERO, Money::add);
    }

    public boolean isOverdue() {
        return installments.stream().anyMatch(installment -> installment.isOverdue());
    }

    public Money getTotalPaid() {
        return installments.stream().map(Installment::getAmountPaid).reduce(Money.ZERO, Money::add);
    }

    public Money getRemainingAmount() {
        return getTotalAmount().subtract(getTotalPaid());
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

    public void addInstallment(@NonNull Installment installment) {
        ensureNotArchived();
        validateInstallmentBelonging(installment);
        if (this.installments.stream().anyMatch(inst -> inst.getId().equals(installment.getId()))) {
            throw new InvalidPropertyException(
                    "Installment with id " + installment.getId() + " already exists in the invoice.");
        }
        if (installment.getAmountDue().compareTo(Money.ZERO) <= 0) {
            throw new InvalidMoneyValueException("Installment amountDue must be greater than zero");
        }
        ensureDueDateDoesNotExistInOtherInstallments(installment.getDueDate());
        this.installments.add(installment);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateInstallment(@NonNull Installment updatedInstallment) {
        ensureNotArchived();
        Installment existingInstallment = findInstallmentById(updatedInstallment.getId());
        ensureInstallmentCanBeUpdated(existingInstallment, updatedInstallment);
        existingInstallment.updateDetails(updatedInstallment.getAmountDue(), updatedInstallment.getDueDate());
        this.updatedAt = LocalDateTime.now();
    }

    public void removeInstallment(@NonNull Installment installment) {
        ensureNotArchived();
        validateInstallmentBelonging(installment);
        this.installments.remove(installment);
        this.updatedAt = LocalDateTime.now();
    }

    public void addPayment(@NonNull Installment installment, @NonNull Payment payment) {
        ensureNotArchived();
        validateInstallmentBelonging(installment);
        Installment existingInstallment = findInstallmentById(installment.getId());
        existingInstallment.addPayment(payment);
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePayment(@NonNull Installment installment, @NonNull Payment updatedPayment) {
        ensureNotArchived();
        validateInstallmentBelonging(installment);
        Installment existingInstallment = findInstallmentById(installment.getId());
        existingInstallment.updatePayment(updatedPayment);
        this.updatedAt = LocalDateTime.now();
    }

    public void removePayment(@NonNull Installment installment, @NonNull Payment payment) {
        ensureNotArchived();
        validateInstallmentBelonging(installment);
        Installment existingInstallment = findInstallmentById(installment.getId());
        existingInstallment.removePayment(payment);
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
