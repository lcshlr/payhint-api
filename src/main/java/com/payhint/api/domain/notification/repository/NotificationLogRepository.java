package com.payhint.api.domain.notification.repository;

import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.notification.model.NotificationLog;

public interface NotificationLogRepository {
    NotificationLog save(NotificationLog log);

    boolean existsByInstallmentId(InstallmentId installmentId);
}