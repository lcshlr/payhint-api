package com.payhint.api.application.notification.dto;

import java.time.LocalDate;
import java.util.UUID;

public record OverdueInstallmentDto(UUID installmentId, UUID invoiceId, UUID userId, LocalDate dueDate) {
}