package com.payhint.api.infrastructure.notification.persistence.jpa.adapter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.payhint.api.application.notification.dto.OverdueInstallmentDto;
import com.payhint.api.application.notification.repository.OverdueInstallmentRepository;
import com.payhint.api.infrastructure.billing.persistence.jpa.repository.InvoiceSpringRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OverdueInstallmentJpaAdapter implements OverdueInstallmentRepository {

    private final InvoiceSpringRepository invoiceSpringRepository;

    @Override
    public List<OverdueInstallmentDto> listOverdueInstallmentsNotNotified() {
        return invoiceSpringRepository.findOverdueInstallmentsNotNotified();
    }
}