package com.payhint.api.application.billing.mapper;

import java.math.BigDecimal;

import org.mapstruct.Mapper;

import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.billing.valueobject.Money;
import com.payhint.api.domain.billing.valueobject.PaymentId;

@Mapper(componentModel = "spring")
public interface BillingValueObjectMapper {
    default String map(InvoiceId invoiceId) {
        return invoiceId == null ? null : invoiceId.value().toString();
    }

    default InvoiceId mapToInvoiceId(String invoiceId) {
        return invoiceId == null ? null : InvoiceId.fromString(invoiceId);
    }

    default String map(InstallmentId installmentId) {
        return installmentId == null ? null : installmentId.value().toString();
    }

    default InstallmentId mapToInstallmentId(String installmentId) {
        return installmentId == null ? null : InstallmentId.fromString(installmentId);
    }

    default String map(PaymentId paymentId) {
        return paymentId == null ? null : paymentId.value().toString();
    }

    default PaymentId mapToPaymentId(String paymentId) {
        return paymentId == null ? null : PaymentId.fromString(paymentId);
    }

    default String map(InvoiceReference invoiceReference) {
        return invoiceReference == null ? null : invoiceReference.value();
    }

    default InvoiceReference mapToInvoiceReference(String invoiceReference) {
        return invoiceReference == null ? null : new InvoiceReference(invoiceReference);
    }

    default String map(Money money) {
        return money == null ? "0" : money.amount().toString();
    }

    default BigDecimal mapMoneyToBigDecimal(Money money) {
        return money == null ? BigDecimal.ZERO : money.amount();
    }

    default Money mapToMoney(String amount) {
        return amount == null ? null : new Money(BigDecimal.valueOf(Double.parseDouble(amount)));
    }

    default Money mapToMoney(BigDecimal amount) {
        return amount == null ? null : new Money(amount);
    }
}
