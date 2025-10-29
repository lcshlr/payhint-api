package com.payhint.api.infrastructure.persistence.jpa.billing.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.payhint.api.domain.billing.model.Payment;
import com.payhint.api.domain.billing.repository.PaymentRepository;
import com.payhint.api.infrastructure.persistence.jpa.billing.mapper.PaymentPersistenceMapper;
import com.payhint.api.infrastructure.persistence.jpa.billing.repository.PaymentSpringRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentJpaRepositoryAdapter implements PaymentRepository {

    private final PaymentSpringRepository springDataPaymentRepository;
    private final PaymentPersistenceMapper mapper;

    @Override
    public Payment save(Payment payment) {
        var entity = mapper.toEntity(payment);
        var savedEntity = springDataPaymentRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return springDataPaymentRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findAllByInstallmentId(UUID installmentId) {
        return springDataPaymentRepository.findAllByInstallmentId(installmentId).stream().map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        springDataPaymentRepository.deleteById(id);
    }
}
