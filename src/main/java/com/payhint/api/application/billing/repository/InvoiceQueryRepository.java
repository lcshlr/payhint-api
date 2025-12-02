package com.payhint.api.application.billing.repository;

import java.util.List;

import com.payhint.api.application.billing.dto.response.InvoiceSummaryResponse;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.UserId;

public interface InvoiceQueryRepository {

    List<InvoiceSummaryResponse> findSummariesByCustomerId(CustomerId customerId);

    List<InvoiceSummaryResponse> findSummariesByUserId(UserId userId);
}
