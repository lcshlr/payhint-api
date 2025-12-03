package com.payhint.api.application.notification.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.payhint.api.application.notification.repository.MailRepository;
import com.payhint.api.application.shared.exception.NotFoundException;
import com.payhint.api.domain.billing.event.InstallmentOverdueEvent;
import com.payhint.api.domain.billing.exception.InstallmentDoesNotBelongToInvoiceException;
import com.payhint.api.domain.billing.model.Installment;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.domain.notification.model.NotificationLog;
import com.payhint.api.domain.notification.repository.NotificationLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueInstallmentListener {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final MailRepository emailService;
    private final NotificationLogRepository notificationLogRepository;

    @Async
    @EventListener
    @Transactional
    public void handle(InstallmentOverdueEvent event) {
        if (notificationLogRepository.existsByInstallmentId(event.installmentId())) {
            return;
        }

        Invoice invoice = invoiceRepository.findByIdAndOwner(event.invoiceId(), event.userId()).orElse(null);

        if (invoice == null) {
            log.warn("Invoice {} not found for user {} during notification processing", event.invoiceId(),
                    event.userId());
            return;
        }

        try {
            Installment installment = invoice.findInstallmentById(event.installmentId());

            if (!installment.isStrictlyOverdue()) {
                log.info("Skipping notification: Installment {} is not overdue", event.installmentId());
                return;
            }

            sendNotification(event, invoice, installment);

        } catch (InstallmentDoesNotBelongToInvoiceException e) {
            log.warn("Installment {} no longer exists on invoice {}", event.installmentId(), event.invoiceId());
        }
    }

    private void sendNotification(InstallmentOverdueEvent event, Invoice invoice, Installment installment) {
        User user = userRepository.findById(event.userId()).orElseThrow(() -> new NotFoundException("User not found"));

        String subject = "Action Required: Overdue Payment Detected";
        String body = String.format(
                "Hello %s,\n\nThe installment due on %s for invoice %s is overdue.\nPlease check your dashboard.",
                user.getFirstName(), installment.getDueDate(), invoice.getInvoiceReference());

        try {
            emailService.sendEmail(user.getEmail().value(), subject, body);
            NotificationLog log = NotificationLog.createSuccess(event.installmentId(), user.getEmail(), subject);
            notificationLogRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to send overdue notification email", e);
            NotificationLog failureLog = NotificationLog.createFailure(event.installmentId(), user.getEmail(), subject,
                    e.getMessage());
            notificationLogRepository.save(failureLog);
        }
    }
}