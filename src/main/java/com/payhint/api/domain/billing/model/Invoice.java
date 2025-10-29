package com.payhint.api.domain.billing.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    private UUID id;
    @NotBlank
    private UUID customerId;
    @NotBlank
    private String invoiceReference;
    @NotBlank
    @Positive
    private BigDecimal totalAmount;
    @NotBlank
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<Installment> installments = new ArrayList<>();

    public Invoice(UUID customerId, String invoiceReference, BigDecimal totalAmount, String currency) {
        this.id = null;
        this.customerId = customerId;
        this.invoiceReference = invoiceReference;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.createdAt = null;
        this.updatedAt = null;
        this.installments = new ArrayList<>();
    }

    public void updateInvoice(String invoiceReference, BigDecimal totalAmount, String currency) {
        this.invoiceReference = invoiceReference;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.updatedAt = LocalDateTime.now();
    }

    public void addInstallment(Installment installment) {
        if (!installment.getInvoiceId().equals(this.id)) {
            throw new IllegalArgumentException("Installment invoiceId does not match Invoice id.");
        }

        BigDecimal remainingAmount = getRemainingAmount();
        if (installment.getAmountDue().compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException("Installment amountDue exceeds remaining invoice amount.");
        }

        this.installments.add(installment);
    }

    public BigDecimal getTotalPaid() {
        return installments.stream().map(Installment::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getRemainingAmount() {
        return totalAmount.subtract(getTotalPaid());
    }

    public boolean isFullyPaid() {
        return getRemainingAmount().compareTo(BigDecimal.ZERO) == 0;
    }
}
