package com.payhint.api.application.billing.usecase;

public interface NotifyOverdueInstallmentsUseCase {
    void detectAndPublishOverdueEvents();
}