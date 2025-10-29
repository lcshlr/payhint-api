package com.payhint.api.infrastructure.persistence.jpa.billing.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.infrastructure.persistence.jpa.billing.mapper.InvoicePersistenceMapper;
import com.payhint.api.infrastructure.persistence.jpa.billing.repository.InvoiceSpringRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InvoiceJpaRepositoryAdapter implements InvoiceRepository {

    private final InvoiceSpringRepository springDataInvoiceRepository;
    private final InvoicePersistenceMapper mapper;

    @Override
    public Invoice save(Invoice invoice) {
        var entity = mapper.toEntity(invoice);
        var savedEntity = springDataInvoiceRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return springDataInvoiceRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Invoice> findAllByCustomerId(UUID customerId) {
        return springDataInvoiceRepository.findAllByCustomerId(customerId).stream().map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Invoice> findByCustomerIdAndInvoiceReference(UUID customerId, String invoiceReference) {
        return springDataInvoiceRepository.findByCustomerIdAndInvoiceReference(customerId, invoiceReference)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        springDataInvoiceRepository.deleteById(id);
    }
}
