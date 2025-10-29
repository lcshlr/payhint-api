package com.payhint.api.domain.billing.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class Installment {

    private UUID id;
    @NotBlank
    private UUID invoiceId;
    @NotBlank
    @Positive
    private BigDecimal amountDue;
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;
    @NotBlank
    private LocalDate dueDate;
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    public Installment(UUID invoiceId, BigDecimal amountDue, LocalDate dueDate) {
        this.invoiceId = invoiceId;
        this.amountDue = amountDue;
        this.dueDate = dueDate;
    }

    private void updateStatus(PaymentStatus newStatus) {
        this.status = newStatus;
    }

    public boolean isOverdue() {
        return LocalDate.now().isAfter(dueDate)
                && (status == PaymentStatus.PENDING || status == PaymentStatus.PARTIALLY_PAID);
    }

    public BigDecimal getRemainingAmount() {
        return amountDue.subtract(amountPaid);
    }

    public void addPayment(Payment payment) {
        if (payment.getAmount().compareTo(getRemainingAmount()) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds remaining installment amount.");
        }
        this.payments.add(payment);
        this.amountPaid = this.amountPaid.add(payment.getAmount());
        updatePaymentStatus();
    }

    private void updatePaymentStatus() {
        BigDecimal remaining = getRemainingAmount();
        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            updateStatus(PaymentStatus.PAID);
        } else if (remaining.compareTo(amountDue) < 0) {
            updateStatus(PaymentStatus.PARTIALLY_PAID);
        } else if (isOverdue()) {
            updateStatus(PaymentStatus.LATE);
        }
    }
}
