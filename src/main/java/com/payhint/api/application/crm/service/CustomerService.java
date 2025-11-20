package com.payhint.api.application.crm.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.payhint.api.application.crm.dto.request.CreateCustomerRequest;
import com.payhint.api.application.crm.dto.request.UpdateCustomerRequest;
import com.payhint.api.application.crm.dto.response.CustomerResponse;
import com.payhint.api.application.crm.mapper.CustomerMapper;
import com.payhint.api.application.crm.usecase.CustomerManagementUseCase;
import com.payhint.api.application.shared.exception.AlreadyExistsException;
import com.payhint.api.application.shared.exception.NotFoundException;
import com.payhint.api.application.shared.exception.PermissionDeniedException;
import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.domain.crm.repository.CustomerRepository;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.Email;
import com.payhint.api.domain.crm.valueobject.UserId;

@Service
public class CustomerService implements CustomerManagementUseCase {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CustomerMapper customerMapper;
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository,
            CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.customerMapper = customerMapper;
    }

    private Customer findCustomerForUser(UserId userId, CustomerId customerId) {
        if (!userRepository.existsById(userId)) {
            var errorMessage = "User with ID " + userId + " does not exist";
            logger.warn(errorMessage);
            throw new NotFoundException(errorMessage);
        }

        Customer existingCustomer = customerRepository.findById(customerId).orElseThrow(() -> {
            var errorMessage = "Customer not found.";
            logger.warn(errorMessage);
            return new NotFoundException(errorMessage);
        });

        if (!existingCustomer.belongsToUser(userId)) {
            var errorMessage = "User with ID " + userId + " does not have permission to access customer with ID "
                    + customerId;
            logger.warn(errorMessage);
            throw new PermissionDeniedException(errorMessage);
        }
        return existingCustomer;
    }

    @Override
    public CustomerResponse createCustomer(UserId userId, CreateCustomerRequest request) {
        if (!userRepository.existsById(userId)) {
            var errorMessage = "User with ID " + userId + " does not exist";
            logger.warn(errorMessage);
            throw new NotFoundException(errorMessage);
        }
        if (customerRepository.existsByUserIdAndCompanyName(userId, request.companyName())) {
            var errorMessage = "A customer with the same company name already exists for this user";
            logger.warn(errorMessage);
            throw new AlreadyExistsException(errorMessage);
        }

        CustomerId customerId = new CustomerId(UUID.randomUUID());
        Customer customer = Customer.create(customerId, userId, request.companyName(),
                new Email(request.contactEmail()));
        Customer savedCustomer = customerRepository.save(customer);
        logger.info("Customer created successfully: " + savedCustomer.getCompanyName() + " for user ID " + userId);
        return customerMapper.toResponse(savedCustomer);
    }

    @Override
    public CustomerResponse updateCustomerDetails(UserId userId, CustomerId customerId, UpdateCustomerRequest request) {
        Customer existingCustomer = findCustomerForUser(userId, customerId);

        var contactEmail = request.contactEmail() == null ? null : new Email(request.contactEmail());
        existingCustomer.updateInformation(request.companyName(), contactEmail);
        Customer savedCustomer = customerRepository.save(existingCustomer);
        logger.info("Customer updated successfully: " + existingCustomer.getCompanyName() + " for user ID " + userId);
        return customerMapper.toResponse(savedCustomer);
    }

    @Override
    public void deleteCustomer(UserId userId, CustomerId customerId) {
        Customer existingCustomer = findCustomerForUser(userId, customerId);
        customerRepository.delete(existingCustomer);
        logger.info("Customer deleted successfully: " + existingCustomer.getCompanyName() + " for user ID " + userId);
    }

    @Override
    public CustomerResponse viewCustomerProfile(UserId userId, CustomerId customerId) {
        Customer existingCustomer = findCustomerForUser(userId, customerId);
        return customerMapper.toResponse(existingCustomer);
    }

    @Override
    public List<CustomerResponse> listAllCustomers(UserId userId) {
        if (!userRepository.existsById(userId)) {
            var errorMessage = "User with ID " + userId + " does not exist";
            logger.warn(errorMessage);
            throw new NotFoundException(errorMessage);
        }
        List<Customer> customers = customerRepository.findAllByUserId(userId);
        return customerMapper.toResponseList(customers);
    }

}
