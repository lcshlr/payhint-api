package com.payhint.api.infrastructure.billing.persistence.jpa.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InvoiceJpaEntity;
import com.payhint.api.infrastructure.billing.persistence.jpa.mapper.InvoicePersistenceMapper;
import com.payhint.api.infrastructure.billing.persistence.jpa.repository.InvoiceSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.entity.CustomerJpaEntity;
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
        UUID invoiceId = invoice.getId() != null ? invoice.getId().value() : null;
        UUID customerId = invoice.getCustomerId() != null ? invoice.getCustomerId().value() : null;
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null when saving an invoice.");
        }
        if (invoiceId == null) {
            InvoiceJpaEntity entity = mapper.toEntity(invoice);

            CustomerJpaEntity customerEntity = springDataCustomerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            entity.setCustomer(customerEntity);

            InvoiceJpaEntity saved = springDataInvoiceRepository.save(entity);

            return mapper.toDomainWithDetails(saved);
        } else {
            InvoiceJpaEntity entity = springDataInvoiceRepository.findById(invoiceId)
                    .orElseGet(() -> mapper.toEntity(invoice));

            CustomerJpaEntity customerEntity = springDataCustomerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            entity.updateFromDomain(invoice, mapper, customerEntity);

            InvoiceJpaEntity saved = springDataInvoiceRepository.save(entity);

            return mapper.toDomainWithDetails(saved);
        }
    }

    @Override
    public Optional<Invoice> findById(InvoiceId id) {
        if (id == null) {
            return Optional.empty();
        }
        return springDataInvoiceRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Invoice> findByIdWithInstallments(@NonNull InvoiceId id) {
        return springDataInvoiceRepository.findByIdWithInstallments(id.value()).map(mapper::toDomainWithDetails);
    }

    @Override
    public Optional<Invoice> findByIdWithInstallmentsAndPayments(@NonNull InvoiceId id) {
        return springDataInvoiceRepository.findByIdWithInstallmentsAndPayments(id.value())
                .map(mapper::toDomainWithDetails);
    }

    @Override
    public List<Invoice> findAllWithInstallmentsAndPaymentsByCustomerId(@NonNull CustomerId customerId) {
        return springDataInvoiceRepository.findAllWithInstallmentsAndPaymentsByCustomerId(customerId.value()).stream()
                .map(mapper::toDomainWithDetails).collect(Collectors.toList());
    }

    @Override
    public List<Invoice> findAllByCustomerId(CustomerId customerId) {
        if (customerId == null) {
            return List.of();
        }
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
