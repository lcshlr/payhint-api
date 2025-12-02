package com.payhint.api.application.billing.usecase;

import java.util.List;

import com.payhint.api.application.billing.dto.request.CreateInvoiceRequest;
import com.payhint.api.application.billing.dto.request.UpdateInvoiceRequest;
import com.payhint.api.application.billing.dto.response.InvoiceResponse;
import com.payhint.api.application.billing.dto.response.InvoiceSummaryResponse;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.UserId;

public interface InvoiceLifecycleUseCase {

        InvoiceResponse viewInvoice(UserId userId, InvoiceId invoiceId);

        List<InvoiceSummaryResponse> listInvoicesByUser(UserId userId);

        List<InvoiceSummaryResponse> listInvoicesByCustomer(UserId userId, CustomerId customerId);

        InvoiceResponse updateInvoice(UserId userId, InvoiceId invoiceId, UpdateInvoiceRequest request);

        void deleteInvoice(UserId userId, InvoiceId invoiceId);

        InvoiceResponse createInvoice(UserId userId, CreateInvoiceRequest request);

        InvoiceResponse archiveInvoice(UserId userId, InvoiceId invoiceId);

        InvoiceResponse unarchiveInvoice(UserId userId, InvoiceId invoiceId);
}
