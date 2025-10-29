package com.payhint.api.application.crm.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.payhint.api.application.crm.dto.request.CreateCustomerRequest;
import com.payhint.api.application.crm.dto.request.UpdateCustomerRequest;
import com.payhint.api.application.crm.dto.response.CustomerResponse;
import com.payhint.api.application.shared.exceptions.AlreadyExistException;
import com.payhint.api.application.shared.exceptions.NotFoundException;
import com.payhint.api.application.shared.exceptions.PermissionDeniedException;
import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.CustomerRepository;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.domain.crm.valueobjects.CustomerId;
import com.payhint.api.domain.crm.valueobjects.Email;
import com.payhint.api.domain.crm.valueobjects.UserId;

@SpringBootTest
@TestPropertySource(properties = { "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop" })
@Transactional
@DisplayName("CustomerService Integration Tests")
class CustomerServiceIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String TEST_USER_EMAIL = "testuser@payhint.com";
    private static final String TEST_USER_PASSWORD = "Password123!";
    private static final String TEST_USER_FIRST_NAME = "Test";
    private static final String TEST_USER_LAST_NAME = "User";

    private static final String TEST_COMPANY_NAME = "Test Company Inc.";
    private static final String TEST_CONTACT_EMAIL = "contact@testcompany.com";

    private User testUser;
    private UserId testUserId;

    @BeforeEach
    void setUp() {
        testUser = new User(new Email(TEST_USER_EMAIL), TEST_USER_PASSWORD, TEST_USER_FIRST_NAME, TEST_USER_LAST_NAME);
        testUser = userRepository.register(testUser);
        testUserId = testUser.getId();
    }

    @AfterEach
    void tearDown() {
        userRepository.delete(User.builder().id(testUserId).build());
    }

    @Nested
    @DisplayName("Create Customer Integration Tests")
    class CreateCustomerIntegrationTests {

        @Test
        @DisplayName("Should successfully create a customer and persist to database")
        void shouldCreateCustomerSuccessfully() {
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, TEST_CONTACT_EMAIL);

            CustomerResponse response = customerService.createCustomer(testUserId, request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isNotNull();
            assertThat(response.companyName()).isEqualTo(TEST_COMPANY_NAME);
            assertThat(response.contactEmail()).isEqualTo(TEST_CONTACT_EMAIL);

            Customer savedCustomer = customerRepository.findById(new CustomerId(UUID.fromString(response.id())))
                    .orElseThrow();
            assertThat(savedCustomer.getUserId()).isEqualTo(testUserId);
            assertThat(savedCustomer.getCompanyName()).isEqualTo(TEST_COMPANY_NAME);
            assertThat(savedCustomer.getContactEmail().value()).isEqualTo(TEST_CONTACT_EMAIL);
        }

        @Test
        @DisplayName("Should throw NotFoundException when user does not exist")
        void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
            UUID nonExistentUserId = UUID.randomUUID();
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, TEST_CONTACT_EMAIL);

            assertThatThrownBy(() -> customerService.createCustomer(new UserId(nonExistentUserId), request))
                    .isInstanceOf(NotFoundException.class).hasMessageContaining("User does not exist.");
        }

        @Test
        @DisplayName("Should throw AlreadyExistException when duplicate company name for same user")
        void shouldThrowAlreadyExistExceptionWhenDuplicateCompanyName() {
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, TEST_CONTACT_EMAIL);
            customerService.createCustomer(testUserId, request);

            CreateCustomerRequest duplicateRequest = new CreateCustomerRequest(TEST_COMPANY_NAME, "another@email.com");

            assertThatThrownBy(() -> customerService.createCustomer(testUserId, duplicateRequest))
                    .isInstanceOf(AlreadyExistException.class)
                    .hasMessageContaining("A customer with the same company name already exists for this user.");
        }

        @Test
        @DisplayName("Should allow same company name for different users")
        void shouldAllowSameCompanyNameForDifferentUsers() {
            User anotherUser = new User(new Email("anotheruser@payhint.com"), "Password123!", "Another", "User");
            anotherUser = userRepository.register(anotherUser);

            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, TEST_CONTACT_EMAIL);
            CustomerResponse firstCustomer = customerService.createCustomer(testUserId, request);

            CreateCustomerRequest secondRequest = new CreateCustomerRequest(TEST_COMPANY_NAME, "different@email.com");
            CustomerResponse secondCustomer = customerService.createCustomer(anotherUser.getId(), secondRequest);

            assertThat(firstCustomer.id()).isNotEqualTo(secondCustomer.id());
            assertThat(firstCustomer.companyName()).isEqualTo(secondCustomer.companyName());
            userRepository.delete(anotherUser);
        }

        @Test
        @DisplayName("Should create multiple customers for same user with different company names")
        void shouldCreateMultipleCustomersForSameUser() {
            CreateCustomerRequest firstRequest = new CreateCustomerRequest("Company A", "companya@email.com");
            CreateCustomerRequest secondRequest = new CreateCustomerRequest("Company B", "companyb@email.com");

            CustomerResponse firstCustomer = customerService.createCustomer(testUserId, firstRequest);
            CustomerResponse secondCustomer = customerService.createCustomer(testUserId, secondRequest);

            assertThat(firstCustomer.id()).isNotEqualTo(secondCustomer.id());
            assertThat(customerRepository.findAllByUserId(testUserId)).hasSize(2);
        }

        @Test
        @DisplayName("Should create customer with valid email format")
        void shouldCreateCustomerWithValidEmailFormat() {
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME,
                    "valid.email+tag@example.co.uk");

            CustomerResponse response = customerService.createCustomer(testUserId, request);

            assertThat(response.contactEmail()).isEqualTo("valid.email+tag@example.co.uk");
        }
    }

    @Nested
    @DisplayName("Update Customer Integration Tests")
    class UpdateCustomerIntegrationTests {

        private Customer existingCustomer;

        @BeforeEach
        void setUp() {
            existingCustomer = new Customer(testUserId, TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            existingCustomer = customerRepository.save(existingCustomer);
        }

        @Test
        @DisplayName("Should successfully update customer company name")
        void shouldUpdateCustomerCompanyName() {
            String newCompanyName = "Updated Company Name";
            UpdateCustomerRequest request = new UpdateCustomerRequest(newCompanyName, null);

            CustomerResponse response = customerService.updateCustomerDetails(testUserId, existingCustomer.getId(),
                    request);

            assertThat(response.companyName()).isEqualTo(newCompanyName);
            assertThat(response.contactEmail()).isEqualTo(TEST_CONTACT_EMAIL);

            Customer updatedCustomer = customerRepository
                    .findById(new CustomerId(UUID.fromString(existingCustomer.getId().toString()))).orElseThrow();
            assertThat(updatedCustomer.getCompanyName()).isEqualTo(newCompanyName);
        }

        @Test
        @DisplayName("Should successfully update customer contact email")
        void shouldUpdateCustomerContactEmail() {
            String newEmail = "newemail@company.com";
            UpdateCustomerRequest request = new UpdateCustomerRequest(null, newEmail);

            CustomerResponse response = customerService.updateCustomerDetails(testUserId, existingCustomer.getId(),
                    request);

            assertThat(response.contactEmail()).isEqualTo(newEmail);
            assertThat(response.companyName()).isEqualTo(TEST_COMPANY_NAME);

            Customer updatedCustomer = customerRepository
                    .findById(new CustomerId(UUID.fromString(existingCustomer.getId().toString()))).orElseThrow();
            assertThat(updatedCustomer.getContactEmail().value()).isEqualTo(newEmail);
        }

        @Test
        @DisplayName("Should successfully update both company name and contact email")
        void shouldUpdateBothFields() {
            String newCompanyName = "New Company";
            String newEmail = "newcontact@company.com";
            UpdateCustomerRequest request = new UpdateCustomerRequest(newCompanyName, newEmail);

            CustomerResponse response = customerService.updateCustomerDetails(testUserId, existingCustomer.getId(),
                    request);

            assertThat(response.companyName()).isEqualTo(newCompanyName);
            assertThat(response.contactEmail()).isEqualTo(newEmail);

            Customer updatedCustomer = customerRepository
                    .findById(new CustomerId(UUID.fromString(existingCustomer.getId().toString()))).orElseThrow();
            assertThat(updatedCustomer.getCompanyName()).isEqualTo(newCompanyName);
            assertThat(updatedCustomer.getContactEmail().value()).isEqualTo(newEmail);
        }

        @Test
        @DisplayName("Should not update when both fields are null")
        void shouldNotUpdateWhenBothFieldsNull() {
            UpdateCustomerRequest request = new UpdateCustomerRequest(null, null);

            CustomerResponse response = customerService.updateCustomerDetails(testUserId, existingCustomer.getId(),
                    request);

            assertThat(response.companyName()).isEqualTo(TEST_COMPANY_NAME);
            assertThat(response.contactEmail()).isEqualTo(TEST_CONTACT_EMAIL);
        }

        @Test
        @DisplayName("Should throw NotFoundException when customer does not exist")
        void shouldThrowNotFoundExceptionWhenCustomerDoesNotExist() {
            UUID nonExistentCustomerId = UUID.randomUUID();
            UpdateCustomerRequest request = new UpdateCustomerRequest("New Name", null);

            assertThatThrownBy(() -> customerService.updateCustomerDetails(testUserId,
                    new CustomerId(nonExistentCustomerId), request)).isInstanceOf(NotFoundException.class)
                            .hasMessageContaining("Customer not found.");
        }

        @Test
        @DisplayName("Should throw NotFoundException when user does not exist")
        void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
            UUID nonExistentUserId = UUID.randomUUID();
            UpdateCustomerRequest request = new UpdateCustomerRequest("New Name", null);

            assertThatThrownBy(() -> customerService.updateCustomerDetails(new UserId(nonExistentUserId),
                    new CustomerId(UUID.fromString(existingCustomer.getId().toString())), request))
                            .isInstanceOf(NotFoundException.class).hasMessageContaining("User does not exist.");
        }

        @Test
        @DisplayName("Should throw PermissionDeniedException when customer belongs to different user")
        void shouldThrowPermissionDeniedExceptionWhenCustomerBelongsToDifferentUser() {
            User anotherUser = new User(new Email("anotheruser@payhint.com"), "Password123!", "Another", "User");
            User savedAnotherUser = userRepository.register(anotherUser);

            UpdateCustomerRequest request = new UpdateCustomerRequest("New Name", null);

            assertThatThrownBy(() -> customerService.updateCustomerDetails(savedAnotherUser.getId(),
                    existingCustomer.getId(), request)).isInstanceOf(PermissionDeniedException.class)
                            .hasMessageContaining("User does not have permission to access this customer.");

            userRepository.delete(savedAnotherUser);
        }

        @Test
        @DisplayName("Should update customer with valid email format")
        void shouldUpdateCustomerWithValidEmailFormat() {
            UpdateCustomerRequest request = new UpdateCustomerRequest(null, "new.valid+email@example.org");

            CustomerResponse response = customerService.updateCustomerDetails(testUserId, existingCustomer.getId(),
                    request);

            assertThat(response.contactEmail()).isEqualTo("new.valid+email@example.org");
        }
    }

    @Nested
    @DisplayName("Delete Customer Integration Tests")
    class DeleteCustomerIntegrationTests {

        private Customer existingCustomer;

        @BeforeEach
        void setUp() {
            existingCustomer = new Customer(testUserId, TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            existingCustomer = customerRepository.save(existingCustomer);
        }

        @Test
        @DisplayName("Should successfully delete customer from database")
        void shouldDeleteCustomerSuccessfully() {
            CustomerId customerId = existingCustomer.getId();

            customerService.deleteCustomer(testUserId, customerId);

            assertThat(customerRepository.findById(customerId)).isEmpty();
        }

        @Test
        @DisplayName("Should throw NotFoundException when customer does not exist")
        void shouldThrowNotFoundExceptionWhenCustomerDoesNotExist() {
            UUID nonExistentCustomerId = UUID.randomUUID();

            assertThatThrownBy(() -> customerService.deleteCustomer(testUserId, new CustomerId(nonExistentCustomerId)))
                    .isInstanceOf(NotFoundException.class).hasMessageContaining("Customer not found.");
        }

        @Test
        @DisplayName("Should throw NotFoundException when user does not exist")
        void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
            UUID nonExistentUserId = UUID.randomUUID();

            assertThatThrownBy(
                    () -> customerService.deleteCustomer(new UserId(nonExistentUserId), existingCustomer.getId()))
                            .isInstanceOf(NotFoundException.class).hasMessageContaining("User does not exist.");
        }

        @Test
        @DisplayName("Should throw PermissionDeniedException when customer belongs to different user")
        void shouldThrowPermissionDeniedExceptionWhenCustomerBelongsToDifferentUser() {
            User anotherUser = new User(new Email("anotheruser@payhint.com"), "Password123!", "Another", "User");
            User savedAnotherUser = userRepository.register(anotherUser);

            assertThatThrownBy(() -> customerService.deleteCustomer(savedAnotherUser.getId(), existingCustomer.getId()))
                    .isInstanceOf(PermissionDeniedException.class)
                    .hasMessageContaining("User does not have permission to access this customer.");

            assertThat(customerRepository.findById(existingCustomer.getId())).isPresent();

            userRepository.delete(savedAnotherUser);
        }

        @Test
        @DisplayName("Should not affect other customers when deleting one")
        void shouldNotAffectOtherCustomersWhenDeletingOne() {
            Customer anotherCustomer = new Customer(testUserId, "Another Company", new Email("another@company.com"));
            anotherCustomer = customerRepository.save(anotherCustomer);

            customerService.deleteCustomer(testUserId, existingCustomer.getId());

            assertThat(customerRepository.findById(existingCustomer.getId())).isEmpty();
            assertThat(customerRepository.findById(anotherCustomer.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("Get Customer By ID Integration Tests")
    class GetCustomerByIdIntegrationTests {

        private Customer existingCustomer;

        @BeforeEach
        void setUp() {
            existingCustomer = new Customer(testUserId, TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            existingCustomer = customerRepository.save(existingCustomer);
        }

        @Test
        @DisplayName("Should successfully retrieve customer by ID")
        void shouldGetCustomerByIdSuccessfully() {
            CustomerResponse response = customerService.viewCustomerProfile(testUserId, existingCustomer.getId());

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(existingCustomer.getId().toString());
            assertThat(response.companyName()).isEqualTo(TEST_COMPANY_NAME);
            assertThat(response.contactEmail()).isEqualTo(TEST_CONTACT_EMAIL);
        }

        @Test
        @DisplayName("Should throw NotFoundException when customer does not exist")
        void shouldThrowNotFoundExceptionWhenCustomerDoesNotExist() {
            UUID nonExistentCustomerId = UUID.randomUUID();

            assertThatThrownBy(
                    () -> customerService.viewCustomerProfile(testUserId, new CustomerId(nonExistentCustomerId)))
                            .isInstanceOf(NotFoundException.class).hasMessageContaining("Customer not found.");
        }

        @Test
        @DisplayName("Should throw NotFoundException when user does not exist")
        void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
            UUID nonExistentUserId = UUID.randomUUID();

            assertThatThrownBy(
                    () -> customerService.viewCustomerProfile(new UserId(nonExistentUserId), existingCustomer.getId()))
                            .isInstanceOf(NotFoundException.class).hasMessageContaining("User does not exist.");
        }

        @Test
        @DisplayName("Should throw PermissionDeniedException when customer belongs to different user")
        void shouldThrowPermissionDeniedExceptionWhenCustomerBelongsToDifferentUser() {
            User anotherUser = new User(new Email("anotheruser@payhint.com"), "Password123!", "Another", "User");
            User savedAnotherUser = userRepository.register(anotherUser);

            assertThatThrownBy(
                    () -> customerService.viewCustomerProfile(savedAnotherUser.getId(), existingCustomer.getId()))
                            .isInstanceOf(PermissionDeniedException.class)
                            .hasMessageContaining("User does not have permission to access this customer.");

            userRepository.delete(savedAnotherUser);
        }

        @Test
        @DisplayName("Should retrieve customer with all fields correctly mapped")
        void shouldRetrieveCustomerWithAllFieldsCorrectlyMapped() {
            CustomerResponse response = customerService.viewCustomerProfile(testUserId, existingCustomer.getId());

            Customer repoCustomer = customerRepository.findById(existingCustomer.getId()).orElseThrow();
            assertThat(response.id()).isEqualTo(repoCustomer.getId().toString());
            assertThat(response.companyName()).isEqualTo(repoCustomer.getCompanyName());
            assertThat(response.contactEmail()).isEqualTo(repoCustomer.getContactEmail().value());
        }
    }

    @Nested
    @DisplayName("Get All Customers Integration Tests")
    class GetAllCustomersIntegrationTests {

        @Test
        @DisplayName("Should return empty list when user has no customers")
        void shouldReturnEmptyListWhenUserHasNoCustomers() {
            List<CustomerResponse> customers = customerService.listAllCustomers(testUserId);

            assertThat(customers).isEmpty();
        }

        @Test
        @DisplayName("Should return all customers for user")
        void shouldReturnAllCustomersForUser() {
            Customer customer1 = new Customer(testUserId, "Company A", new Email("companya@email.com"));
            Customer customer2 = new Customer(testUserId, "Company B", new Email("companyb@email.com"));
            Customer customer3 = new Customer(testUserId, "Company C", new Email("companyc@email.com"));

            customerRepository.save(customer1);
            customerRepository.save(customer2);
            customerRepository.save(customer3);

            List<CustomerResponse> customers = customerService.listAllCustomers(testUserId);

            assertThat(customers).hasSize(3);
            assertThat(customers).extracting(CustomerResponse::companyName).containsExactlyInAnyOrder("Company A",
                    "Company B", "Company C");
        }

        @Test
        @DisplayName("Should only return customers belonging to the user")
        void shouldOnlyReturnCustomersBelongingToUser() {
            User anotherUser = new User(new Email("anotheruser@payhint.com"), "Password123!", "Another", "User");
            User savedAnotherUser = userRepository.register(anotherUser);

            Customer userCustomer1 = new Customer(testUserId, "User Company 1", new Email("user1@email.com"));
            Customer userCustomer2 = new Customer(testUserId, "User Company 2", new Email("user2@email.com"));
            Customer otherUserCustomer = new Customer(savedAnotherUser.getId(), "Other User Company",
                    new Email("other@email.com"));

            customerRepository.save(userCustomer1);
            customerRepository.save(userCustomer2);
            customerRepository.save(otherUserCustomer);

            List<CustomerResponse> customers = customerService.listAllCustomers(testUserId);

            assertThat(customers).hasSize(2);
            assertThat(customers).extracting(CustomerResponse::companyName)
                    .containsExactlyInAnyOrder("User Company 1", "User Company 2").doesNotContain("Other User Company");

            userRepository.delete(savedAnotherUser);
        }

        @Test
        @DisplayName("Should throw NotFoundException when user does not exist")
        void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
            UUID nonExistentUserId = UUID.randomUUID();

            assertThatThrownBy(() -> customerService.listAllCustomers(new UserId(nonExistentUserId)))
                    .isInstanceOf(NotFoundException.class).hasMessageContaining("User does not exist.");
        }

        @Test
        @DisplayName("Should return customers with all fields correctly mapped")
        void shouldReturnCustomersWithAllFieldsCorrectlyMapped() {
            Customer customer = new Customer(testUserId, TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            customer = customerRepository.save(customer);

            List<CustomerResponse> customers = customerService.listAllCustomers(testUserId);

            assertThat(customers).hasSize(1);
            CustomerResponse response = customers.get(0);
            assertThat(response.id()).isEqualTo(customer.getId().toString());
            assertThat(response.companyName()).isEqualTo(TEST_COMPANY_NAME);
            assertThat(response.contactEmail()).isEqualTo(TEST_CONTACT_EMAIL);
        }

        @Test
        @DisplayName("Should return multiple customers in consistent order")
        void shouldReturnMultipleCustomersInConsistentOrder() {
            Customer customer1 = new Customer(testUserId, "Alpha Company", new Email("alpha@email.com"));
            Customer customer2 = new Customer(testUserId, "Beta Company", new Email("beta@email.com"));
            Customer customer3 = new Customer(testUserId, "Gamma Company", new Email("gamma@email.com"));

            customerRepository.save(customer1);
            customerRepository.save(customer2);
            customerRepository.save(customer3);

            List<CustomerResponse> firstCall = customerService.listAllCustomers(testUserId);
            List<CustomerResponse> secondCall = customerService.listAllCustomers(testUserId);

            assertThat(firstCall).hasSize(3);
            assertThat(secondCall).hasSize(3);
            assertThat(firstCall).extracting(CustomerResponse::id)
                    .containsExactlyElementsOf(secondCall.stream().map(CustomerResponse::id).toList());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle null UUID gracefully for user ID")
        void shouldHandleNullUserIdGracefully() {
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, TEST_CONTACT_EMAIL);

            assertThatThrownBy(() -> customerService.createCustomer(null, request)).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should handle null UUID gracefully for customer ID")
        void shouldHandleNullCustomerIdGracefully() {
            assertThatThrownBy(() -> customerService.viewCustomerProfile(testUserId, null))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should throw error for very long company name")
        void shouldThrowErrorForVeryLongCompanyName() {
            String longCompanyName = "A".repeat(500);
            CreateCustomerRequest request = new CreateCustomerRequest(longCompanyName, TEST_CONTACT_EMAIL);

            assertThatThrownBy(() -> customerService.createCustomer(testUserId, request)).isInstanceOf(Exception.class)
                    .hasMessageContaining("companyName");
        }

        @Test
        @DisplayName("Should handle special characters in company name")
        void shouldHandleSpecialCharactersInCompanyName() {
            String specialCompanyName = "Company & Co. (Ltd.) - 2024 #1";
            CreateCustomerRequest request = new CreateCustomerRequest(specialCompanyName, TEST_CONTACT_EMAIL);

            CustomerResponse response = customerService.createCustomer(testUserId, request);

            assertThat(response.companyName()).isEqualTo(specialCompanyName);
        }

        @Test
        @DisplayName("Should handle unicode characters in company name")
        void shouldHandleUnicodeCharactersInCompanyName() {
            String unicodeCompanyName = "Société Française 日本語 中文 Русский";
            CreateCustomerRequest request = new CreateCustomerRequest(unicodeCompanyName, TEST_CONTACT_EMAIL);

            CustomerResponse response = customerService.createCustomer(testUserId, request);

            assertThat(response.companyName()).isEqualTo(unicodeCompanyName);
        }

        @Test
        @DisplayName("Should persist timestamps correctly on create")
        void shouldPersistTimestampsCorrectlyOnCreate() throws InterruptedException {
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, TEST_CONTACT_EMAIL);

            CustomerResponse response = customerService.createCustomer(testUserId, request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isNotNull();

            Customer savedCustomer = customerRepository.findById(new CustomerId(UUID.fromString(response.id())))
                    .orElseThrow();
            assertThat(savedCustomer.getCreatedAt()).isNotNull();
            assertThat(savedCustomer.getUpdatedAt()).isNotNull();
            assertThat(savedCustomer.getCreatedAt()).isBeforeOrEqualTo(savedCustomer.getUpdatedAt());
        }

        @Test
        @DisplayName("Should update timestamp on modification")
        void shouldUpdateTimestampOnModification() throws InterruptedException {
            Customer customer = new Customer(testUserId, TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            customer = customerRepository.save(customer);

            Thread.sleep(10);

            UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Name", null);
            CustomerResponse response = customerService.updateCustomerDetails(testUserId, customer.getId(), request);

            assertThat(response).isNotNull();

            Customer updatedCustomer = customerRepository.findById(new CustomerId(UUID.fromString(response.id())))
                    .orElseThrow();
            assertThat(updatedCustomer.getUpdatedAt()).isAfter(updatedCustomer.getCreatedAt());
        }
    }
}
