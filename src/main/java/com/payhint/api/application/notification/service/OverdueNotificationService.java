package com.payhint.api.application.notification.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhint.api.application.billing.usecase.NotifyOverdueInstallmentsUseCase;
import com.payhint.api.application.notification.dto.OverdueInstallmentDto;
import com.payhint.api.application.notification.repository.OverdueInstallmentRepository;
import com.payhint.api.domain.billing.event.InstallmentOverdueEvent;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.crm.valueobject.UserId;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OverdueNotificationService implements NotifyOverdueInstallmentsUseCase {

    private final OverdueInstallmentRepository overdueInstallmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public void detectAndPublishOverdueEvents() {
        List<OverdueInstallmentDto> overdueInstallments = overdueInstallmentRepository
                .listOverdueInstallmentsNotNotified();

        for (OverdueInstallmentDto installment : overdueInstallments) {
            eventPublisher.publishEvent(new InstallmentOverdueEvent(new InstallmentId(installment.installmentId()),
                    new InvoiceId(installment.invoiceId()), new UserId(installment.userId()), installment.dueDate()));
        }
    }
}