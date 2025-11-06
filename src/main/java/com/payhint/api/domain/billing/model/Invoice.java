package com.payhint.api.domain.billing.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.payhint.api.domain.billing.exception.InstallmentDoesNotBelongToInvoiceException;
import com.payhint.api.domain.billing.exception.InvalidMoneyValueException;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.crm.valueobject.CustomerId;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class Invoice {

    private InvoiceId id;
    @NonNull
    private CustomerId customerId;
    @NonNull
    private InvoiceReference invoiceReference;
    @NonNull
    private Money totalAmount;
    @NonNull
    private String currency;
    @NonNull
    private LocalDateTime createdAt;
    @NonNull
    private LocalDateTime updatedAt;

    private List<Installment> installments;

    public Invoice(InvoiceId id, CustomerId customerId, InvoiceReference invoiceReference, Money totalAmount,
            String currency, LocalDateTime createdAt, LocalDateTime updatedAt, List<Installment> installments) {
        this.id = id;
        this.customerId = customerId;
        this.invoiceReference = invoiceReference;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.installments = installments;
    }

    public static Invoice create(CustomerId customerId, InvoiceReference invoiceReference, Money totalAmount,
            String currency) {
        return new Invoice(null, customerId, invoiceReference, totalAmount, currency, LocalDateTime.now(),
                LocalDateTime.now(), new ArrayList<>());
    }

    private void validateInstallmentBelonging(Installment installment) {
        if (!installment.getInvoiceId().equals(this.id)) {
            throw new InstallmentDoesNotBelongToInvoiceException(installment.getId(), this.id);
        }
    }

    public void updateInvoice(InvoiceReference invoiceReference, Money totalAmount, String currency) {
        this.invoiceReference = invoiceReference;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.updatedAt = LocalDateTime.now();
    }

    public void addPaymentToInstallment(Installment installment, Payment payment) {
        validateInstallmentBelonging(installment);
        if (payment.getAmount().compareTo(installment.getRemainingAmount()) > 0) {
            throw new InvalidMoneyValueException("Payment amount exceeds remaining installment amount.");
        }
        installment.addPayment(payment);
    }

    public void addInstallment(Installment installment) {
        validateInstallmentBelonging(installment);

        Money remainingAmount = getRemainingAmount();
        if (installment.getAmountDue().compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException("Installment amountDue exceeds remaining invoice amount.");
        }

        this.installments.add(installment);
    }

    public Money getTotalPaid() {
        return installments.stream().map(Installment::getAmountPaid).reduce(Money.ZERO, Money::add);
    }

    public Money getRemainingAmount() {
        return totalAmount.subtract(getTotalPaid());
    }

    public boolean isFullyPaid() {
        return getRemainingAmount().compareTo(Money.ZERO) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Invoice invoice = (Invoice) o;

        return id != null ? id.equals(invoice.id) : invoice.id == null;
    }
}
