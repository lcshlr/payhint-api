package com.payhint.api.domain.billing.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class Payment {

    private UUID id;
    @NotBlank
    private UUID installmentId;
    @NotBlank
    @Positive
    private BigDecimal amount;
    @NotBlank
    private LocalDate paymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Payment(UUID installmentId, BigDecimal amount, LocalDate paymentDate) {
        this.installmentId = installmentId;
        this.amount = amount;
        this.paymentDate = paymentDate;
    }

    public void updateDetails(BigDecimal amount, LocalDate paymentDate) {
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.updatedAt = LocalDateTime.now();
    }
}
