package com.payhint.api.domain.billing.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.crm.valueobject.UserId;

@DisplayName("InstallmentOverdueEvent Tests")
class InstallmentOverdueEventTest {

    @Test
    @DisplayName("Should create event with correct data")
    void shouldCreateEvent() {
        InstallmentId installmentId = new InstallmentId(UUID.randomUUID());
        InvoiceId invoiceId = new InvoiceId(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());
        LocalDate dueDate = LocalDate.now().minusDays(1);

        InstallmentOverdueEvent event = new InstallmentOverdueEvent(installmentId, invoiceId, userId, dueDate);

        assertThat(event.installmentId()).isEqualTo(installmentId);
        assertThat(event.invoiceId()).isEqualTo(invoiceId);
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.dueDate()).isEqualTo(dueDate);
    }
}