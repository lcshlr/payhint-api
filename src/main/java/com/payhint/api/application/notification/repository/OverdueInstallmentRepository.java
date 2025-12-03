package com.payhint.api.application.notification.repository;

import java.util.List;

import com.payhint.api.application.notification.dto.OverdueInstallmentDto;

public interface OverdueInstallmentRepository {
    List<OverdueInstallmentDto> listOverdueInstallmentsNotNotified();
}