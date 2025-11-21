package com.payhint.api.domain.billing.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.billing.valueobject.PaymentId;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class Payment {

    private PaymentId id;
    @NonNull
    private Money amount;
    @NonNull
    private LocalDate paymentDate;
    @NonNull
    private LocalDateTime createdAt;
    @NonNull
    private LocalDateTime updatedAt;

    public Payment(@NonNull PaymentId id, @NonNull Money amount, @NonNull LocalDate paymentDate,
            @NonNull LocalDateTime createdAt, @NonNull LocalDateTime updatedAt) {
        if (id == null) {
            throw new com.payhint.api.domain.shared.exception.InvalidPropertyException("PaymentId cannot be null");
        }
        this.id = id;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment create(@NonNull PaymentId id, @NonNull Money amount, @NonNull LocalDate paymentDate) {
        return new Payment(id, amount, paymentDate, LocalDateTime.now(), LocalDateTime.now());
    }

    void updateDetails(Money amount, LocalDate paymentDate) {

        boolean isUpdated = false;
        if (amount != null && !amount.equals(this.amount)) {
            if (amount.compareTo(Money.ZERO) <= 0) {
                throw new InvalidMoneyValueException("Payment amount must be greater than zero");
            }
            this.amount = amount;
            isUpdated = true;
        }
        if (paymentDate != null && !paymentDate.equals(this.paymentDate)) {
            this.paymentDate = paymentDate;
            isUpdated = true;
        }
        if (isUpdated) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Payment other = (Payment) obj;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
