package com.payhint.api.infrastructure.notification.scheduler;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.payhint.api.application.billing.usecase.NotifyOverdueInstallmentsUseCase;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("OverdueInstallmentScheduler Integration Tests")
class OverdueInstallmentSchedulerTest {

    @Autowired
    private OverdueInstallmentScheduler scheduler;

    @MockitoBean
    private NotifyOverdueInstallmentsUseCase useCase;

    @Test
    @DisplayName("Should trigger use case when checkOverdueInstallments is called")
    void shouldTriggerUseCase() {
        scheduler.checkOverdueInstallments();

        verify(useCase).detectAndPublishOverdueEvents();
    }
}