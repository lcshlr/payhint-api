package com.payhint.api.infrastructure.persistence.jpa.billing.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.payhint.api.domain.billing.model.Installment;
import com.payhint.api.domain.billing.model.PaymentStatus;
import com.payhint.api.domain.billing.repository.InstallmentRepository;
import com.payhint.api.infrastructure.persistence.jpa.billing.mapper.InstallmentPersistenceMapper;
import com.payhint.api.infrastructure.persistence.jpa.billing.repository.InstallmentSpringRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InstallmentJpaRepositoryAdapter implements InstallmentRepository {

    private final InstallmentSpringRepository springDataInstallmentRepository;
    private final InstallmentPersistenceMapper mapper;

    @Override
    public Installment save(Installment installment) {
        var entity = mapper.toEntity(installment);
        var savedEntity = springDataInstallmentRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Installment> findById(UUID id) {
        return springDataInstallmentRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Installment> findAllByInvoiceId(UUID invoiceId) {
        return springDataInstallmentRepository.findAllByInvoiceId(invoiceId).stream().map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Installment> findAllByStatus(PaymentStatus status) {
        return springDataInstallmentRepository.findAllByStatus(status).stream().map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Installment> findOverdueInstallments(LocalDate currentDate) {
        return springDataInstallmentRepository.findOverdueInstallments(currentDate).stream().map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        springDataInstallmentRepository.deleteById(id);
    }

    @Override
    public boolean existsByIdAndInvoiceId(UUID id, UUID invoiceId) {
        return springDataInstallmentRepository.existsByIdAndInvoiceId(id, invoiceId);
    }
}
