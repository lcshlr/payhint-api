package com.payhint.api.infrastructure.billing.persistence.jpa.adapter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.payhint.api.application.billing.dto.response.InvoiceSummaryResponse;
import com.payhint.api.application.billing.repository.InvoiceQueryRepository;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.infrastructure.billing.persistence.jpa.mapper.InvoiceQueryMapper;
import com.payhint.api.infrastructure.billing.persistence.jpa.repository.InvoiceSpringRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InvoiceQueryJpaRepositoryAdapter implements InvoiceQueryRepository {

    private final InvoiceSpringRepository springDataInvoiceRepository;
    private final InvoiceQueryMapper mapper;

    @Override
    public List<InvoiceSummaryResponse> findSummariesByCustomerId(@NonNull CustomerId customerId) {
        var invoices = springDataInvoiceRepository.findSummariesByCustomerId(customerId.value()).stream()
                .collect(Collectors.toList());
        return mapper.toResponse(invoices);
    }

    @Override
    public List<InvoiceSummaryResponse> findSummariesByUserId(@NonNull UserId userId) {
        var invoices = springDataInvoiceRepository.findSummariesByUserId(userId.value()).stream()
                .collect(Collectors.toList());
        return mapper.toResponse(invoices);
    }

}
