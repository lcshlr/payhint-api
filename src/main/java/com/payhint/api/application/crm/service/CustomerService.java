package com.payhint.api.application.crm.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.payhint.api.application.crm.dto.request.CreateCustomerRequest;
import com.payhint.api.application.crm.dto.request.UpdateCustomerRequest;
import com.payhint.api.application.crm.dto.response.CustomerResponse;
import com.payhint.api.application.crm.mapper.CustomerMapper;
import com.payhint.api.application.crm.usecases.CustomerManagementUseCase;
import com.payhint.api.application.shared.exceptions.AlreadyExistsException;
import com.payhint.api.application.shared.exceptions.NotFoundException;
import com.payhint.api.application.shared.exceptions.PermissionDeniedException;
import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.domain.crm.repository.CustomerRepository;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.domain.crm.valueobjects.CustomerId;
import com.payhint.api.domain.crm.valueobjects.Email;
import com.payhint.api.domain.crm.valueobjects.UserId;

@Service
@Validated
public class CustomerService implements CustomerManagementUseCase {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository,
            CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.customerMapper = customerMapper;
    }

    private Customer findCustomerForUser(UserId userId, CustomerId customerId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User does not exist.");
        }

        Customer existingCustomer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found."));

        if (!existingCustomer.belongsToUser(userId)) {
            throw new PermissionDeniedException("User does not have permission to access this customer.");
        }
        return existingCustomer;
    }

    @Override
    public CustomerResponse createCustomer(UserId userId, CreateCustomerRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User does not exist.");
        }
        if (customerRepository.existsByUserIdAndCompanyName(userId, request.companyName())) {
            throw new AlreadyExistsException("A customer with the same company name already exists for this user.");
        }

        Customer customer = customerMapper.toDomain(userId, request);
        Customer savedCustomer = customerRepository.save(customer);
        return customerMapper.toResponse(savedCustomer);
    }

    @Override
    public CustomerResponse updateCustomerDetails(UserId userId, CustomerId customerId, UpdateCustomerRequest request) {
        Customer existingCustomer = findCustomerForUser(userId, customerId);

        var contactEmail = request.contactEmail() == null ? null : new Email(request.contactEmail());
        existingCustomer.updateInformation(request.companyName(), contactEmail);
        return customerMapper.toResponse(customerRepository.save(existingCustomer));
    }

    @Override
    public void deleteCustomer(UserId userId, CustomerId customerId) {
        Customer existingCustomer = findCustomerForUser(userId, customerId);
        customerRepository.delete(existingCustomer);
    }

    @Override
    public CustomerResponse viewCustomerProfile(UserId userId, CustomerId customerId) {
        Customer existingCustomer = findCustomerForUser(userId, customerId);
        return customerMapper.toResponse(existingCustomer);
    }

    @Override
    public List<CustomerResponse> listAllCustomers(UserId userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User does not exist.");
        }
        List<Customer> customers = customerRepository.findAllByUserId(userId);
        return customerMapper.toResponseList(customers);
    }

}
