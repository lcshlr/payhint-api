package com.payhint.api.domain.billing.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.billing.valueobject.PaymentId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class Installment {

    private InstallmentId id;
    @NonNull
    private Money amountDue;
    @NonNull
    private Money amountPaid;
    @NonNull
    private LocalDate dueDate;
    @NonNull
    private PaymentStatus status;
    @NonNull
    private LocalDateTime createdAt;
    @NonNull
    private LocalDateTime updatedAt;
    @NonNull
    private LocalDateTime lastStatusChangeAt;
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    public Installment(@NonNull InstallmentId id, Money amountDue, @NonNull Money amountPaid, LocalDate dueDate,
            @NonNull PaymentStatus status, @NonNull LocalDateTime createdAt, @NonNull LocalDateTime updatedAt,
            @NonNull LocalDateTime lastStatusChangeAt, @NonNull List<Payment> payments) {
        if (id == null) {
            throw new InvalidPropertyException("InstallmentId cannot be null");
        }
        this.id = id;
        this.amountDue = amountDue;
        this.amountPaid = amountPaid;
        this.dueDate = dueDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastStatusChangeAt = lastStatusChangeAt;
        this.payments = payments != null ? new ArrayList<>(payments) : new ArrayList<>();
    }

    public static Installment create(@NonNull InstallmentId id, @NonNull Money amountDue, @NonNull LocalDate dueDate) {
        return new Installment(id, amountDue, Money.ZERO, dueDate, PaymentStatus.PENDING, LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now(), new ArrayList<>());
    }

    public boolean isOverdue() {
        return this.status != PaymentStatus.PAID && LocalDate.now().isAfter(dueDate);
    }

    public Money getRemainingAmount() {
        return amountDue.subtract(amountPaid);
    }

    public List<Payment> getPayments() {
        return this.payments == null ? List.of() : List.copyOf(this.payments);
    }

    public Payment findPaymentById(@NonNull PaymentId paymentId) {
        return this.payments.stream().filter(pmt -> pmt.getId().equals(paymentId)).findFirst().orElseThrow(
                () -> new InvalidPropertyException("Payment with id " + paymentId + " not found in installment"));
    }

    private void updateStatus() {
        PaymentStatus oldStatus = this.status;
        if (amountPaid.compareTo(amountDue) >= 0) {
            this.status = PaymentStatus.PAID;
        } else if (amountPaid.compareTo(Money.ZERO) > 0) {
            this.status = PaymentStatus.PARTIALLY_PAID;
        } else {
            this.status = PaymentStatus.PENDING;
        }

        if (oldStatus != this.status) {
            this.lastStatusChangeAt = LocalDateTime.now();
        }
    }

    void updateDetails(Money amountDue, LocalDate dueDate) {
        boolean updated = false;
        if (amountDue != null && !amountDue.equals(this.amountDue)) {
            ensureAmountIsValid(amountDue);
            this.amountDue = amountDue;
            updated = true;
        }
        if (dueDate != null && !dueDate.equals(this.dueDate)) {
            this.dueDate = dueDate;
            updated = true;
        }
        if (updated) {
            this.updatedAt = LocalDateTime.now();
            updateStatus();
        }
    }

    private void ensureAmountIsValid(Money newAmountDue) {
        if (newAmountDue.compareTo(Money.ZERO) <= 0) {
            throw new InvalidMoneyValueException("Installment amountDue must be greater than zero");
        }

        if (newAmountDue.compareTo(amountPaid) < 0) {
            throw new InvalidMoneyValueException(
                    "Installment amountDue cannot be less than amountPaid: " + amountPaid.amount());
        }
    }

    private void validatePaymentUpdate(@NonNull Money oldAmount, @NonNull Money newAmount) {
        Money maxAllowedNewAmount = getRemainingAmount().add(oldAmount);

        if (newAmount.compareTo(Money.ZERO) <= 0) {
            throw new InvalidMoneyValueException("Updated payment amount must be greater than zero");
        }

        if (newAmount.compareTo(maxAllowedNewAmount) > 0) {
            throw new InvalidMoneyValueException("Updated payment amount exceeds remaining installment amount");
        }
    }

    public Optional<LocalDateTime> getLastPaymentDate() {
        return payments.stream().map(Payment::getUpdatedAt).max(LocalDateTime::compareTo);
    }

    public void addPayment(@NonNull Payment payment) {
        if (this.payments.stream().anyMatch(pmt -> pmt.getId().equals(payment.getId()))) {
            throw new InvalidPropertyException(
                    "Payment with id " + payment.getId() + " already exists in the installment.");
        }

        if (payment.getAmount().compareTo(Money.ZERO) <= 0) {
            throw new InvalidMoneyValueException("Payment amount must be greater than zero");
        }

        if (payment.getAmount().compareTo(getRemainingAmount()) > 0) {
            throw new InvalidMoneyValueException("Payment amount exceeds remaining installment amount");
        }
        this.payments.add(payment);
        this.amountPaid = this.amountPaid.add(payment.getAmount());
        updateStatus();
        this.updatedAt = LocalDateTime.now();
    }

    void updatePayment(@NonNull Payment updatedPayment) {
        if (updatedPayment.getId() == null) {
            throw new InvalidPropertyException("Payment ID cannot be null when updating a payment");
        }
        Payment existingPayment = this.payments.stream().filter(p -> p.getId().equals(updatedPayment.getId()))
                .findFirst().orElse(null);

        if (existingPayment == null) {
            throw new InvalidPropertyException("Payment to update not found in installment");
        }

        validatePaymentUpdate(existingPayment.getAmount(), updatedPayment.getAmount());

        Money oldAmount = existingPayment.getAmount();
        Money newAmount = updatedPayment.getAmount();

        existingPayment.updateDetails(newAmount, updatedPayment.getPaymentDate());

        if (newAmount != null) {
            this.amountPaid = this.amountPaid.subtract(oldAmount).add(newAmount);
            updateStatus();
        }

        this.updatedAt = LocalDateTime.now();
    }

    void removePayment(@NonNull Payment payment) {
        if (payment.getId() == null) {
            throw new InvalidPropertyException("Payment ID cannot be null when removing a payment");
        }
        if (this.payments.remove(payment)) {
            this.amountPaid = this.amountPaid.subtract(payment.getAmount());
            updateStatus();
            this.updatedAt = LocalDateTime.now();
        } else {
            throw new InvalidPropertyException("Payment not found in installment");
        }
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
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