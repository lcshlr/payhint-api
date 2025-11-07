package com.payhint.api.domain.billing.repository;

import java.util.List;
import java.util.Optional;

import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.crm.valueobject.CustomerId;

public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(InvoiceId id);

    Optional<Invoice> findByIdWithInstallments(InvoiceId id);

    Optional<Invoice> findByIdWithInstallmentsAndPayments(InvoiceId id);

    List<Invoice> findAllByCustomerId(CustomerId customerId);

    List<Invoice> findAllWithInstallmentsAndPaymentsByCustomerId(CustomerId customerId);

    Optional<Invoice> findByCustomerIdAndInvoiceReference(CustomerId customerId, InvoiceReference invoiceReference);

    void deleteById(InvoiceId id);

    void deleteByCustomerId(CustomerId customerId);
}
