package com.payhint.api.application.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import com.payhint.api.application.notification.dto.OverdueInstallmentDto;
import com.payhint.api.application.notification.repository.OverdueInstallmentRepository;
import com.payhint.api.domain.billing.event.InstallmentOverdueEvent;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
@DisplayName("OverdueNotificationService Integration Tests")
class OverdueNotificationServiceIntegrationTest {

    @Autowired
    private OverdueNotificationService overdueNotificationService;

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockitoBean
    private OverdueInstallmentRepository overdueInstallmentRepository;

    @Test
    @DisplayName("Should detect overdue installments and publish events")
    void shouldDetectAndPublishEvents() {
        UUID installmentId1 = UUID.randomUUID();
        OverdueInstallmentDto dto1 = new OverdueInstallmentDto(installmentId1, UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().minusDays(5));

        UUID installmentId2 = UUID.randomUUID();
        OverdueInstallmentDto dto2 = new OverdueInstallmentDto(installmentId2, UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().minusDays(10));

        when(overdueInstallmentRepository.listOverdueInstallmentsNotNotified()).thenReturn(List.of(dto1, dto2));

        overdueNotificationService.detectAndPublishOverdueEvents();

        verify(overdueInstallmentRepository).listOverdueInstallmentsNotNotified();

        long eventCount = applicationEvents.stream(InstallmentOverdueEvent.class).count();
        assertThat(eventCount).isEqualTo(2);

        boolean hasEvent1 = applicationEvents.stream(InstallmentOverdueEvent.class)
                .anyMatch(e -> e.installmentId().value().equals(installmentId1));
        assertThat(hasEvent1).isTrue();
    }

    @Test
    @DisplayName("Should do nothing when no overdue installments found")
    void shouldDoNothingWhenNoOverdueItems() {
        when(overdueInstallmentRepository.listOverdueInstallmentsNotNotified()).thenReturn(List.of());

        overdueNotificationService.detectAndPublishOverdueEvents();

        verify(overdueInstallmentRepository).listOverdueInstallmentsNotNotified();

        long eventCount = applicationEvents.stream(InstallmentOverdueEvent.class).count();
        assertThat(eventCount).isEqualTo(0);
    }
}