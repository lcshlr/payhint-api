package com.payhint.api.domain.crm.repository;

import java.util.List;
import java.util.Optional;

import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.domain.crm.valueobjects.CustomerId;
import com.payhint.api.domain.crm.valueobjects.UserId;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId id);

    List<Customer> findAllByUserId(UserId userId);

    void delete(Customer customer);

    boolean existsById(CustomerId id);

    boolean existsByUserIdAndCompanyName(UserId userId, String companyName);
}