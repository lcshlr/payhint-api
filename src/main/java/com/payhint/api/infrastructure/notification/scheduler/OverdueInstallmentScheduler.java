package com.payhint.api.infrastructure.notification.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.payhint.api.application.billing.usecase.NotifyOverdueInstallmentsUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OverdueInstallmentScheduler {

    private final NotifyOverdueInstallmentsUseCase useCase;

    // Run every day at 9 AM Paris time
    @Scheduled(cron = "0 0 9 * * *", zone = "Europe/Paris")
    public void checkOverdueInstallments() {
        useCase.detectAndPublishOverdueEvents();
    }
}