package com.payhint.api.infrastructure.persistence.jpa.crm.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.domain.crm.repository.CustomerRepository;
import com.payhint.api.domain.crm.valueobjects.CustomerId;
import com.payhint.api.domain.crm.valueobjects.UserId;
import com.payhint.api.infrastructure.persistence.jpa.crm.entity.CustomerJpaEntity;
import com.payhint.api.infrastructure.persistence.jpa.crm.mapper.CustomerPersistenceMapper;
import com.payhint.api.infrastructure.persistence.jpa.crm.repository.CustomerSpringRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CustomerJpaRepositoryAdapter implements CustomerRepository {

    private final CustomerSpringRepository springDataCustomerRepository;
    private final CustomerPersistenceMapper mapper;

    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity entity = mapper.toEntity(customer);
        CustomerJpaEntity savedEntity = springDataCustomerRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return springDataCustomerRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Customer> findAllByUserId(UserId userId) {
        return springDataCustomerRepository.findAllByUserId(userId.value()).stream().map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Customer customer) {
        springDataCustomerRepository.deleteById(customer.getId().value());
    }

    @Override
    public boolean existsById(CustomerId id) {
        return springDataCustomerRepository.existsById(id.value());
    }

    @Override
    public boolean existsByUserIdAndCompanyName(UserId userId, String companyName) {
        return springDataCustomerRepository.existsByUserIdAndCompanyName(userId.value(), companyName);
    }
}
