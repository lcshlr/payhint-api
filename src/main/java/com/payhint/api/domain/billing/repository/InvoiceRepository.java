package com.payhint.api.domain.billing.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.payhint.api.domain.billing.model.Invoice;

public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(UUID id);

    List<Invoice> findAllByCustomerId(UUID customerId);

    Optional<Invoice> findByCustomerIdAndInvoiceReference(UUID customerId, String invoiceReference);

    void deleteById(UUID id);
}
