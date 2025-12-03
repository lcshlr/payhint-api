package com.payhint.api.domain.notification.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;
import com.payhint.api.domain.shared.valueobject.Email;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class NotificationLog {

    private final UUID id;
    private final InstallmentId installmentId;
    private final Email recipientAddress;
    private final String subject;
    private final String errorMessage;
    private final NotificationStatus status;
    private final LocalDateTime sentAt;

    @Builder
    private NotificationLog(@NonNull UUID id, @NonNull InstallmentId installmentId, @NonNull Email recipientAddress,
            String subject, String errorMessage, @NonNull NotificationStatus status, @NonNull LocalDateTime sentAt) {
        if (subject == null || subject.isBlank()) {
            throw new InvalidPropertyException("Subject must be provided for notification log");
        }
        if (status == NotificationStatus.FAILED && (errorMessage == null || errorMessage.isBlank())) {
            throw new InvalidPropertyException("Error message must be provided for FAILED notifications");
        }
        this.id = id;
        this.installmentId = installmentId;
        this.recipientAddress = recipientAddress;
        this.subject = subject;
        this.errorMessage = errorMessage;
        this.status = status;
        this.sentAt = sentAt;
    }

    public static NotificationLog createSuccess(InstallmentId installmentId, Email recipientAddress, String subject) {
        return new NotificationLog(UUID.randomUUID(), installmentId, recipientAddress, subject, null,
                NotificationStatus.SENT, LocalDateTime.now());
    }

    public static NotificationLog createFailure(InstallmentId installmentId, Email recipientAddress, String subject,
            String errorMessage) {
        return new NotificationLog(UUID.randomUUID(), installmentId, recipientAddress, subject, errorMessage,
                NotificationStatus.FAILED, LocalDateTime.now());
    }
}