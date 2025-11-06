package com.payhint.api.domain.billing.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
import com.payhint.api.domain.billing.exception.PaymentDoesNotBelongToInstallmentException;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.Money;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class Installment {

    private InstallmentId id;
    @NonNull
    private InvoiceId invoiceId;
    @NonNull
    private Money amountDue;
    @NonNull
    private Money amountPaid;
    @NonNull
    private LocalDate dueDate;
    @NonNull
    private PaymentStatus status;
    @NonNull
    private List<Payment> payments;
    @NonNull
    private LocalDateTime createdAt;
    @NonNull
    private LocalDateTime updatedAt;

    public Installment(InstallmentId id, @NonNull InvoiceId invoiceId, @NonNull Money amountDue,
            @NonNull Money amountPaid, @NonNull LocalDate dueDate, @NonNull PaymentStatus status,
            @NonNull List<Payment> payments, @NonNull LocalDateTime createdAt, @NonNull LocalDateTime updatedAt) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.amountDue = amountDue;
        this.amountPaid = amountPaid;
        this.dueDate = dueDate;
        this.status = status;
        this.payments = payments;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Installment create(InvoiceId invoiceId, Money amountDue, LocalDate dueDate) {
        return new Installment(null, invoiceId, amountDue, Money.ZERO, dueDate, PaymentStatus.PENDING,
                new ArrayList<>(), LocalDateTime.now(), LocalDateTime.now());
    }

    public boolean isPaid() {
        return status == PaymentStatus.PAID;
    }

    public boolean isOverdue() {
        return !isPaid() && LocalDate.now().isAfter(dueDate);
    }

    public Money getRemainingAmount() {
        return amountDue.subtract(amountPaid);
    }

    public void updateDetails(Money amountDue, LocalDate dueDate) {
        boolean updated = false;
        if (amountDue != null) {
            this.amountDue = amountDue;
            updated = true;
        }
        if (dueDate != null) {
            this.dueDate = dueDate;
            updated = true;
        }
        if (updated) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    private void updateStatus(@NonNull PaymentStatus newStatus) {
        if (newStatus != this.status) {
            this.status = newStatus;
            this.updatedAt = LocalDateTime.now();
        }
    }

    private void updatePaymentStatus() {
        Money remaining = getRemainingAmount();
        if (remaining.amount().compareTo(BigDecimal.ZERO) == 0) {
            updateStatus(PaymentStatus.PAID);
        } else if (remaining.compareTo(amountDue) < 0) {
            updateStatus(PaymentStatus.PARTIALLY_PAID);
        } else {
            updateStatus(PaymentStatus.PENDING);
        }
    }

    private void validatePaymentUpdate(@NonNull Money oldAmount, @NonNull Money newAmount) {
        Money maxAllowedNewAmount = getRemainingAmount().add(oldAmount);

        if (newAmount.compareTo(maxAllowedNewAmount) > 0) {
            throw new InvalidMoneyValueException("Updated payment amount exceeds remaining installment amount");
        }
    }

    public LocalDateTime getLastPaymentDate() {
        return payments.stream().map(Payment::getUpdatedAt).max(LocalDateTime::compareTo).orElse(null);
    }

    void addPayment(@NonNull Payment payment) {
        if (payment.getInstallmentId() == null || !payment.getInstallmentId().equals(this.id)) {
            throw new PaymentDoesNotBelongToInstallmentException(payment.getInstallmentId());
        }

        if (payment.getAmount().compareTo(getRemainingAmount()) > 0) {
            throw new InvalidMoneyValueException("Payment amount exceeds remaining installment amount");
        }
        this.payments.add(payment);
        this.amountPaid = this.amountPaid.add(payment.getAmount());
        updatePaymentStatus();
    }

    void updatePayment(@NonNull Payment newPayment) {
        if (newPayment.getId() == null) {
            throw new IllegalArgumentException("Payment ID cannot be null when updating a payment");
        }
        Payment existingPayment = this.payments.stream().filter(p -> p.getId().equals(newPayment.getId())).findFirst()
                .orElse(null);

        if (existingPayment == null) {
            throw new IllegalArgumentException("Payment to update not found in installment");
        }

        validatePaymentUpdate(existingPayment.getAmount(), newPayment.getAmount());

        Money existingAmount = existingPayment.getAmount();
        Money newAmount = newPayment.getAmount();

        existingPayment.updateDetails(newAmount, newPayment.getPaymentDate());

        this.amountPaid = this.amountPaid.subtract(existingAmount).add(newAmount);
        updatePaymentStatus();
    }

    void removePayment(@NonNull Payment payment) {
        if (payment.getId() == null) {
            throw new IllegalArgumentException("Payment ID cannot be null when removing a payment");
        }
        if (this.payments.remove(payment)) {
            this.amountPaid = this.amountPaid.subtract(payment.getAmount());
            updatePaymentStatus();
        } else {
            throw new IllegalArgumentException("Payment not found in installment");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Installment other = (Installment) obj;
        return id != null && id.equals(other.id);
    }
}