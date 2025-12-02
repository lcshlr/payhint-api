package com.payhint.api.infrastructure.billing.persistence.jpa.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InvoiceJpaEntity;
import com.payhint.api.infrastructure.billing.persistence.jpa.mapper.InvoicePersistenceMapper;
import com.payhint.api.infrastructure.billing.persistence.jpa.repository.InvoiceSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.CustomerSpringRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InvoiceJpaRepositoryAdapter implements InvoiceRepository {

    private final InvoiceSpringRepository springDataInvoiceRepository;
    private final CustomerSpringRepository springDataCustomerRepository;
    private final InvoicePersistenceMapper mapper;

    @Override
    public Invoice save(@NonNull Invoice invoice) {
        var existingEntityOpt = springDataInvoiceRepository.findById(invoice.getId().value());

        InvoiceJpaEntity entityToSave;

        if (existingEntityOpt.isPresent()) {
            entityToSave = existingEntityOpt.get();
            mapper.updateEntity(invoice, entityToSave);
        } else {
            entityToSave = mapper.toEntity(invoice);

            var customerEntity = springDataCustomerRepository.findById(invoice.getCustomerId().value())
                    .orElseThrow(() -> new DataIntegrityViolationException("Customer not found"));
            entityToSave.setCustomer(customerEntity);
        }

        InvoiceJpaEntity saved = springDataInvoiceRepository.save(entityToSave);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Invoice> findById(InvoiceId id) {
        if (id == null) {
            return Optional.empty();
        }
        return springDataInvoiceRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Invoice> findAllByCustomerId(@NonNull CustomerId customerId) {
        return springDataInvoiceRepository.findAllByCustomerId(customerId.value()).stream().map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Invoice> findByCustomerIdAndInvoiceReference(@NonNull CustomerId customerId,
            @NonNull InvoiceReference invoiceReference) {
        return springDataInvoiceRepository
                .findByCustomerIdAndInvoiceReference(customerId.value(), invoiceReference.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Invoice> findByIdAndOwner(@NonNull InvoiceId id, @NonNull UserId userId) {
        return springDataInvoiceRepository.findByIdAndOwner(id.value(), userId.value()).map(mapper::toDomain);
    }

    @Override
    public void deleteById(InvoiceId id) {
        if (id != null) {
            springDataInvoiceRepository.deleteById(id.value());
        }
    }

    @Override
    public void deleteByCustomerId(CustomerId customerId) {
        if (customerId != null) {
            var invoices = springDataInvoiceRepository.findAllByCustomerId(customerId.value());
            springDataInvoiceRepository.deleteAll(invoices);
        }
    }
}
