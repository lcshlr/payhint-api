package com.payhint.api.domain.billing.repository;

import java.util.List;
import java.util.Optional;

import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.UserId;

public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    void deleteById(InvoiceId id);

    void deleteByCustomerId(CustomerId customerId);

    Optional<Invoice> findById(InvoiceId id);

    Optional<Invoice> findByCustomerIdAndInvoiceReference(CustomerId customerId, InvoiceReference invoiceReference);

    Optional<Invoice> findByIdAndOwner(InvoiceId id, UserId userId);

    List<Invoice> findAllByCustomerId(CustomerId customerId);
}
