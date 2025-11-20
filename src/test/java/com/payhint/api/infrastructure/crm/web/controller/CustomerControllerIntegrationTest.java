package com.payhint.api.infrastructure.crm.web.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payhint.api.application.crm.dto.request.CreateCustomerRequest;
import com.payhint.api.application.crm.dto.request.UpdateCustomerRequest;
import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.CustomerRepository;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.Email;
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InstallmentJpaEntity;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.InvoiceJpaEntity;
import com.payhint.api.infrastructure.billing.persistence.jpa.entity.PaymentJpaEntity;
import com.payhint.api.infrastructure.billing.persistence.jpa.repository.InvoiceSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.CustomerSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.UserSpringRepository;
import com.payhint.api.infrastructure.shared.security.JwtTokenProvider;
import com.payhint.api.infrastructure.shared.security.UserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CustomerController Integration Tests")
class CustomerControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CustomerRepository customerRepository;

        @Autowired
        private UserSpringRepository userSpringRepository;

        @Autowired
        private CustomerSpringRepository customerSpringRepository;

        @Autowired
        private InvoiceSpringRepository invoiceSpringRepository;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        private static final String TEST_USER_EMAIL = "testuser@payhint.com";
        private static final String TEST_USER_PASSWORD = "Password123!";
        private static final String TEST_USER_FIRST_NAME = "John";
        private static final String TEST_USER_LAST_NAME = "Doe";

        private static final String TEST_COMPANY_NAME = "Test Company Inc.";
        private static final String TEST_CONTACT_EMAIL = "contact@testcompany.com";
        private static final String INVALID_JWT_TOKEN = "invalid.jwt.token";

        private User testUser;
        private String jwtToken;

        @BeforeEach
        void setUp() {
                customerSpringRepository.deleteAll();
                userSpringRepository.deleteAll();

                testUser = User.create(new UserId(UUID.randomUUID()), new Email(TEST_USER_EMAIL), TEST_USER_PASSWORD,
                                TEST_USER_FIRST_NAME, TEST_USER_LAST_NAME);
                testUser = userRepository.register(testUser);

                UserPrincipal userPrincipal = new UserPrincipal(testUser.getId().value(), testUser.getEmail().value(),
                                testUser.getPassword(),
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                jwtToken = jwtTokenProvider.generateToken(userPrincipal);
        }

        @Nested
        @DisplayName("POST /api/customers")
        class CreateCustomerEndpoint {

                @Test
                @DisplayName("Should successfully create a new customer with valid data")
                void shouldCreateCustomerWithValidData() throws Exception {
                        CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME,
                                        TEST_CONTACT_EMAIL);

                        mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNotEmpty())
                                        .andExpect(jsonPath("$.companyName").value(TEST_COMPANY_NAME))
                                        .andExpect(jsonPath("$.contactEmail").value(TEST_CONTACT_EMAIL));
                }

                @Test
                @DisplayName("Should reject creation with blank company name")
                void shouldRejectBlankCompanyName() throws Exception {
                        CreateCustomerRequest request = new CreateCustomerRequest("", TEST_CONTACT_EMAIL);

                        mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("companyName"))))
                                        .andExpect(jsonPath("$.instance").value("/api/customers"))
                                        .andExpect(jsonPath("$.timestamp").isNotEmpty());
                }

                @Test
                @DisplayName("Should reject creation with null company name")
                void shouldRejectNullCompanyName() throws Exception {
                        CreateCustomerRequest request = new CreateCustomerRequest(null, TEST_CONTACT_EMAIL);

                        mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("companyName"))));
                }

                @Test
                @DisplayName("Should reject creation with company name exceeding 100 characters")
                void shouldRejectTooLongCompanyName() throws Exception {
                        String longCompanyName = "A".repeat(101);
                        CreateCustomerRequest request = new CreateCustomerRequest(longCompanyName, TEST_CONTACT_EMAIL);

                        mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("companyName"))));
                }

                @Test
                @DisplayName("Should reject creation with blank contact email")
                void shouldRejectBlankContactEmail() throws Exception {
                        CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, "");

                        mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("contactEmail"))));
                }

                @Test
                @DisplayName("Should reject creation with null contact email")
                void shouldRejectNullContactEmail() throws Exception {
                        CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, null);

                        mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("contactEmail"))));
                }

                @Test
                @DisplayName("Should reject creation with invalid email format")
                void shouldRejectInvalidEmailFormat() throws Exception {
                        CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, "invalid-email");

                        mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("contactEmail"))));
                }

                @Test
                @DisplayName("Should reject creation with contact email exceeding 100 characters")
                void shouldRejectTooLongContactEmail() throws Exception {
                        String longEmail = "a".repeat(90) + "@example.com";
                        CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, longEmail);

                        mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("contactEmail"))));
                }

                @Test
                @DisplayName("Should reject creation without authentication token")
                void shouldRejectWithoutAuthenticationToken() throws Exception {
                        CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME,
                                        TEST_CONTACT_EMAIL);

                        mockMvc.perform(post("/api/customers")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("Should reject creation with invalid JWT token")
                void shouldRejectWithInvalidToken() throws Exception {
                        CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME,
                                        TEST_CONTACT_EMAIL);

                        mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + INVALID_JWT_TOKEN)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("Should reject creation with malformed authorization header")
                void shouldRejectWithMalformedAuthorizationHeader() throws Exception {
                        CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME,
                                        TEST_CONTACT_EMAIL);

                        mockMvc.perform(post("/api/customers").header("Authorization", jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isForbidden());
                }
        }

        @Nested
        @DisplayName("GET /api/customers/{id}")
        class GetCustomerByIdEndpoint {

                @Test
                @DisplayName("Should successfully retrieve existing customer")
                void shouldRetrieveExistingCustomer() throws Exception {
                        Customer customer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
                        customer = customerRepository.save(customer);

                        mockMvc.perform(get("/api/customers/{id}", customer.getId()).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                                        .andExpect(jsonPath("$.companyName").value(TEST_COMPANY_NAME))
                                        .andExpect(jsonPath("$.contactEmail").value(TEST_CONTACT_EMAIL));
                }

                @Test
                @DisplayName("Should return 404 when customer does not exist")
                void shouldReturn404WhenCustomerDoesNotExist() throws Exception {
                        UUID nonExistentId = UUID.randomUUID();

                        mockMvc.perform(get("/api/customers/{id}", nonExistentId).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.status").value(404))
                                        .andExpect(jsonPath("$.title").value("Resource Not Found"))
                                        .andExpect(jsonPath("$.detail").isNotEmpty())
                                        .andExpect(jsonPath("$.instance").value("/api/customers/" + nonExistentId))
                                        .andExpect(jsonPath("$.timestamp").isNotEmpty());
                }

                @Test
                @DisplayName("Should return 403 when trying to access another user's customer")
                void shouldReturn403WhenAccessingAnotherUsersCustomer() throws Exception {
                        User anotherUser = User.create(new UserId(UUID.randomUUID()), new Email("another@example.com"),
                                        "Password123!", "Jane", "Smith");
                        anotherUser = userRepository.register(anotherUser);

                        Customer anotherCustomer = Customer.create(new CustomerId(UUID.randomUUID()),
                                        anotherUser.getId(), "Another Company", new Email("contact@another.com"));
                        anotherCustomer = customerRepository.save(anotherCustomer);

                        mockMvc.perform(get("/api/customers/{id}", anotherCustomer.getId()).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isForbidden())
                                        .andExpect(jsonPath("$.status").value(403))
                                        .andExpect(jsonPath("$.title").value("Permission Denied"))
                                        .andExpect(jsonPath("$.detail").isNotEmpty())
                                        .andExpect(jsonPath("$.timestamp").isNotEmpty());
                }

                @Test
                @DisplayName("Should reject request with invalid UUID format")
                void shouldRejectInvalidUuidFormat() throws Exception {
                        mockMvc.perform(get("/api/customers/{id}", "invalid-uuid").header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").isNotEmpty());
                }

                @Test
                @DisplayName("Should reject request without authentication token")
                void shouldRejectWithoutAuthenticationToken() throws Exception {
                        UUID customerId = UUID.randomUUID();

                        mockMvc.perform(get("/api/customers/{id}", customerId)).andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("Should reject request with invalid JWT token")
                void shouldRejectWithInvalidToken() throws Exception {
                        UUID customerId = UUID.randomUUID();

                        mockMvc.perform(get("/api/customers/{id}", customerId).header("Authorization",
                                        "Bearer " + INVALID_JWT_TOKEN)).andExpect(status().isForbidden());
                }
        }

        @Nested
        @DisplayName("GET /api/customers/{id}/invoices")
        class GetInvoicesByCustomerIdEndpoint {

                @Test
                @DisplayName("Should successfully retrieve invoices for a customer with full details")
                void shouldRetrieveInvoicesForCustomer() throws Exception {
                        Customer customer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
                        customer = customerRepository.save(customer);

                        var customerEntity = customerSpringRepository.findById(customer.getId().value()).get();

                        InvoiceJpaEntity invoiceEntity = InvoiceJpaEntity.builder().id(UUID.randomUUID())
                                        .invoiceReference("INV-2025-001").currency("EUR").build();
                        invoiceEntity.setCustomer(customerEntity);

                        InstallmentJpaEntity instEntity = InstallmentJpaEntity.builder().id(UUID.randomUUID())
                                        .amountDue(BigDecimal.valueOf(100)).dueDate(LocalDate.now().plusDays(30))
                                        .build();
                        invoiceEntity.addInstallment(instEntity);

                        PaymentJpaEntity paymentEntity = PaymentJpaEntity.builder().id(UUID.randomUUID())
                                        .amount(BigDecimal.valueOf(50)).paymentDate(LocalDate.now()).build();
                        instEntity.addPayment(paymentEntity);

                        invoiceEntity = invoiceSpringRepository.save(invoiceEntity);

                        mockMvc.perform(get("/api/customers/{id}/invoices", customer.getId()).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isOk())
                                        .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(1))
                                        .andExpect(jsonPath("$[0].id").value(invoiceEntity.getId().toString()))
                                        .andExpect(jsonPath("$[0].invoiceReference").value("INV-2025-001"))
                                        .andExpect(jsonPath("$[0].totalAmount").isNotEmpty())
                                        .andExpect(jsonPath("$[0].installments").isArray())
                                        .andExpect(jsonPath("$[0].installments.length()").value(1))
                                        .andExpect(jsonPath("$[0].installments[0].payments").isArray())
                                        .andExpect(jsonPath("$[0].installments[0].payments.length()").value(1));
                }

                @Test
                @DisplayName("Should return empty array when customer has no invoices")
                void shouldReturnEmptyArrayWhenNoInvoices() throws Exception {
                        Customer customer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
                        customer = customerRepository.save(customer);

                        mockMvc.perform(get("/api/customers/{id}/invoices", customer.getId()).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isOk())
                                        .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(0));
                }

                @Test
                @DisplayName("Should return 403 when trying to access another user's invoices")
                void shouldReturn403WhenAccessingAnotherUsersInvoices() throws Exception {
                        User anotherUser = User.create(new UserId(UUID.randomUUID()), new Email("another@example.com"),
                                        "Password123!", "Jane", "Smith");
                        anotherUser = userRepository.register(anotherUser);

                        Customer anotherCustomer = Customer.create(new CustomerId(UUID.randomUUID()),
                                        anotherUser.getId(), "Another Company", new Email("contact@another.com"));
                        anotherCustomer = customerRepository.save(anotherCustomer);

                        var anotherCustomerEntity = customerSpringRepository.findById(anotherCustomer.getId().value())
                                        .get();
                        InvoiceJpaEntity invoiceEntity = InvoiceJpaEntity.builder().id(UUID.randomUUID())
                                        .invoiceReference("INV-2025-002").currency("EUR").build();
                        invoiceEntity.setCustomer(anotherCustomerEntity);
                        invoiceEntity = invoiceSpringRepository.save(invoiceEntity);

                        mockMvc.perform(get("/api/customers/{id}/invoices", anotherCustomer.getId())
                                        .header("Authorization", "Bearer " + jwtToken))
                                        .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                                        .andExpect(jsonPath("$.title").value("Permission Denied"));
                }

                @Test
                @DisplayName("Should return 404 when customer does not exist")
                void shouldReturn404WhenCustomerDoesNotExist() throws Exception {
                        UUID nonExistentId = UUID.randomUUID();

                        mockMvc.perform(get("/api/customers/{id}/invoices", nonExistentId).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.status").value(404))
                                        .andExpect(jsonPath("$.title").value("Resource Not Found"))
                                        .andExpect(jsonPath("$.instance")
                                                        .value("/api/customers/" + nonExistentId + "/invoices"));
                }

                @Test
                @DisplayName("Should reject request with invalid UUID format")
                void shouldRejectInvalidUuidFormat() throws Exception {
                        mockMvc.perform(get("/api/customers/{id}/invoices", "invalid-uuid").header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").isNotEmpty());
                }

                @Test
                @DisplayName("Should reject request without authentication token")
                void shouldRejectWithoutAuthenticationToken() throws Exception {
                        UUID customerId = UUID.randomUUID();

                        mockMvc.perform(get("/api/customers/{id}/invoices", customerId))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("Should reject request with invalid JWT token")
                void shouldRejectWithInvalidToken() throws Exception {
                        UUID customerId = UUID.randomUUID();

                        mockMvc.perform(get("/api/customers/{id}/invoices", customerId).header("Authorization",
                                        "Bearer " + INVALID_JWT_TOKEN)).andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("Should reject request with malformed authorization header")
                void shouldRejectWithMalformedAuthorizationHeader() throws Exception {
                        UUID customerId = UUID.randomUUID();

                        mockMvc.perform(get("/api/customers/{id}/invoices", customerId).header("Authorization",
                                        jwtToken)).andExpect(status().isForbidden());
                }
        }

        @Nested
        @DisplayName("GET /api/customers")
        class GetAllCustomersEndpoint {

                @Test
                @DisplayName("Should successfully retrieve all customers for authenticated user")
                void shouldRetrieveAllCustomersForUser() throws Exception {
                        Customer customer1 = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        "Company A", new Email("contact@companya.com"));
                        Customer customer2 = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        "Company B", new Email("contact@companyb.com"));
                        customerRepository.save(customer1);
                        customerRepository.save(customer2);

                        mockMvc.perform(get("/api/customers").header("Authorization", "Bearer " + jwtToken))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                                        .andExpect(jsonPath("$.length()").value(2))
                                        .andExpect(jsonPath("$[*].companyName",
                                                        Objects.requireNonNull(
                                                                        containsInAnyOrder("Company A", "Company B"))))
                                        .andExpect(jsonPath("$[*].contactEmail",
                                                        Objects.requireNonNull(
                                                                        containsInAnyOrder("contact@companya.com",
                                                                                        "contact@companyb.com"))));
                }

                @Test
                @DisplayName("Should return empty array when user has no customers")
                void shouldReturnEmptyArrayWhenNoCustomers() throws Exception {
                        mockMvc.perform(get("/api/customers").header("Authorization", "Bearer " + jwtToken))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                                        .andExpect(jsonPath("$.length()").value(0));
                }

                @Test
                @DisplayName("Should only return customers belonging to authenticated user")
                void shouldOnlyReturnAuthenticatedUserCustomers() throws Exception {
                        Customer ownCustomer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        "Own Company", new Email("own@example.com"));
                        customerRepository.save(ownCustomer);

                        User anotherUser = User.create(new UserId(UUID.randomUUID()), new Email("another@example.com"),
                                        "Password123!", "Jane", "Smith");
                        anotherUser = userRepository.register(anotherUser);
                        Customer otherCustomer = Customer.create(new CustomerId(UUID.randomUUID()), anotherUser.getId(),
                                        "Other Company", new Email("other@example.com"));
                        customerRepository.save(otherCustomer);

                        mockMvc.perform(get("/api/customers").header("Authorization", "Bearer " + jwtToken))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                                        .andExpect(jsonPath("$.length()").value(1))
                                        .andExpect(jsonPath("$[0].companyName").value("Own Company"))
                                        .andExpect(jsonPath("$[0].contactEmail").value("own@example.com"));
                }

                @Test
                @DisplayName("Should reject request without authentication token")
                void shouldRejectWithoutAuthenticationToken() throws Exception {
                        mockMvc.perform(get("/api/customers")).andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("Should reject request with invalid JWT token")
                void shouldRejectWithInvalidToken() throws Exception {
                        mockMvc.perform(get("/api/customers").header("Authorization", "Bearer " + INVALID_JWT_TOKEN))
                                        .andExpect(status().isForbidden());
                }
        }

        @Nested
        @DisplayName("PUT /api/customers/{id}")
        class UpdateCustomerEndpoint {

                @Test
                @DisplayName("Should successfully update customer with both fields")
                void shouldUpdateCustomerWithBothFields() throws Exception {
                        Customer customer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
                        customer = customerRepository.save(customer);

                        UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company",
                                        "updated@example.com");

                        mockMvc.perform(put("/api/customers/{id}", customer.getId())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                                        .andExpect(jsonPath("$.companyName").value("Updated Company"))
                                        .andExpect(jsonPath("$.contactEmail").value("updated@example.com"));
                }

                @Test
                @DisplayName("Should successfully update only company name")
                void shouldUpdateOnlyCompanyName() throws Exception {
                        Customer customer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
                        customer = customerRepository.save(customer);

                        UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company", null);

                        mockMvc.perform(put("/api/customers/{id}", customer.getId())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                                        .andExpect(jsonPath("$.companyName").value("Updated Company"))
                                        .andExpect(jsonPath("$.contactEmail").value(TEST_CONTACT_EMAIL));
                }

                @Test
                @DisplayName("Should successfully update only contact email")
                void shouldUpdateOnlyContactEmail() throws Exception {
                        Customer customer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
                        customer = customerRepository.save(customer);

                        UpdateCustomerRequest request = new UpdateCustomerRequest(null, "updated@example.com");

                        mockMvc.perform(put("/api/customers/{id}", customer.getId())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                                        .andExpect(jsonPath("$.companyName").value(TEST_COMPANY_NAME))
                                        .andExpect(jsonPath("$.contactEmail").value("updated@example.com"));
                }

                @Test
                @DisplayName("Should accept update with both fields null")
                void shouldAcceptUpdateWithBothFieldsNull() throws Exception {
                        Customer customer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
                        customer = customerRepository.save(customer);

                        UpdateCustomerRequest request = new UpdateCustomerRequest(null, null);

                        mockMvc.perform(put("/api/customers/{id}", customer.getId())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                                        .andExpect(jsonPath("$.companyName").value(TEST_COMPANY_NAME))
                                        .andExpect(jsonPath("$.contactEmail").value(TEST_CONTACT_EMAIL));
                }

                @Test
                @DisplayName("Should return 404 when updating non-existent customer")
                void shouldReturn404WhenUpdatingNonExistentCustomer() throws Exception {
                        UUID nonExistentId = UUID.randomUUID();
                        UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company",
                                        "updated@example.com");

                        mockMvc.perform(put("/api/customers/{id}", nonExistentId)
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                                        .andExpect(jsonPath("$.title").value("Resource Not Found"))
                                        .andExpect(jsonPath("$.timestamp").isNotEmpty());
                }

                @Test
                @DisplayName("Should return 403 when trying to update another user's customer")
                void shouldReturn403WhenUpdatingAnotherUsersCustomer() throws Exception {
                        User anotherUser = User.create(new UserId(UUID.randomUUID()), new Email("another@example.com"),
                                        "Password123!", "Jane", "Smith");
                        anotherUser = userRepository.register(anotherUser);

                        Customer anotherCustomer = Customer.create(new CustomerId(UUID.randomUUID()),
                                        anotherUser.getId(), "Another Company", new Email("contact@another.com"));
                        anotherCustomer = customerRepository.save(anotherCustomer);

                        UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company",
                                        "updated@example.com");

                        mockMvc.perform(put("/api/customers/{id}", anotherCustomer.getId())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                                        .andExpect(jsonPath("$.title").value("Permission Denied"))
                                        .andExpect(jsonPath("$.timestamp").isNotEmpty());
                }

                @Test
                @DisplayName("Should reject update with company name exceeding 100 characters")
                void shouldRejectTooLongCompanyName() throws Exception {
                        Customer customer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
                        customer = customerRepository.save(customer);

                        String longCompanyName = "A".repeat(101);
                        UpdateCustomerRequest request = new UpdateCustomerRequest(longCompanyName, null);

                        mockMvc.perform(put("/api/customers/{id}", customer.getId())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("companyName"))));
                }

                @Test
                @DisplayName("Should reject update with invalid email format")
                void shouldRejectInvalidEmailFormat() throws Exception {
                        Customer customer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
                        customer = customerRepository.save(customer);

                        UpdateCustomerRequest request = new UpdateCustomerRequest(null, "invalid-email");

                        mockMvc.perform(put("/api/customers/{id}", customer.getId())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("contactEmail"))));
                }

                @Test
                @DisplayName("Should reject update with contact email exceeding 100 characters")
                void shouldRejectTooLongContactEmail() throws Exception {
                        Customer customer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
                        customer = customerRepository.save(customer);

                        String longEmail = "a".repeat(90) + "@example.com";
                        UpdateCustomerRequest request = new UpdateCustomerRequest(null, longEmail);

                        mockMvc.perform(put("/api/customers/{id}", customer.getId())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("contactEmail"))));
                }

                @Test
                @DisplayName("Should reject request with invalid UUID format")
                void shouldRejectInvalidUuidFormat() throws Exception {
                        UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company",
                                        "updated@example.com");

                        mockMvc.perform(put("/api/customers/{id}", "invalid-uuid")
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").isNotEmpty());
                }

                @Test
                @DisplayName("Should reject request without authentication token")
                void shouldRejectWithoutAuthenticationToken() throws Exception {
                        UUID customerId = UUID.randomUUID();
                        UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company",
                                        "updated@example.com");

                        mockMvc.perform(put("/api/customers/{id}", customerId)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("Should reject request with invalid JWT token")
                void shouldRejectWithInvalidToken() throws Exception {
                        UUID customerId = UUID.randomUUID();
                        UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company",
                                        "updated@example.com");

                        mockMvc.perform(put("/api/customers/{id}", customerId)
                                        .header("Authorization", "Bearer " + INVALID_JWT_TOKEN)
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isForbidden());
                }
        }

        @Nested
        @DisplayName("DELETE /api/customers/{id}")
        class DeleteCustomerEndpoint {

                @Test
                @DisplayName("Should successfully delete existing customer")
                void shouldDeleteExistingCustomer() throws Exception {
                        Customer customer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(),
                                        TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
                        customer = customerRepository.save(customer);

                        mockMvc.perform(delete("/api/customers/{id}", customer.getId()).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isNoContent());
                }

                @Test
                @DisplayName("Should return 404 when deleting non-existent customer")
                void shouldReturn404WhenDeletingNonExistentCustomer() throws Exception {
                        UUID nonExistentId = UUID.randomUUID();

                        mockMvc.perform(delete("/api/customers/{id}", nonExistentId).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.status").value(404))
                                        .andExpect(jsonPath("$.title").value("Resource Not Found"))
                                        .andExpect(jsonPath("$.timestamp").isNotEmpty());
                }

                @Test
                @DisplayName("Should return 403 when trying to delete another user's customer")
                void shouldReturn403WhenDeletingAnotherUsersCustomer() throws Exception {
                        User anotherUser = User.create(new UserId(UUID.randomUUID()), new Email("another@example.com"),
                                        "Password123!", "Jane", "Smith");
                        anotherUser = userRepository.register(anotherUser);

                        Customer anotherCustomer = Customer.create(new CustomerId(UUID.randomUUID()),
                                        anotherUser.getId(), "Another Company", new Email("contact@another.com"));
                        anotherCustomer = customerRepository.save(anotherCustomer);

                        mockMvc.perform(delete("/api/customers/{id}", anotherCustomer.getId()).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isForbidden())
                                        .andExpect(jsonPath("$.status").value(403))
                                        .andExpect(jsonPath("$.title").value("Permission Denied"))
                                        .andExpect(jsonPath("$.timestamp").isNotEmpty());
                }

                @Test
                @DisplayName("Should reject request with invalid UUID format")
                void shouldRejectInvalidUuidFormat() throws Exception {
                        mockMvc.perform(delete("/api/customers/{id}", "invalid-uuid").header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").isNotEmpty());
                }

                @Test
                @DisplayName("Should reject request without authentication token")
                void shouldRejectWithoutAuthenticationToken() throws Exception {
                        UUID customerId = UUID.randomUUID();

                        mockMvc.perform(delete("/api/customers/{id}", customerId)).andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("Should reject request with invalid JWT token")
                void shouldRejectWithInvalidToken() throws Exception {
                        UUID customerId = UUID.randomUUID();

                        mockMvc.perform(delete("/api/customers/{id}", customerId).header("Authorization",
                                        "Bearer " + INVALID_JWT_TOKEN)).andExpect(status().isForbidden());
                }
        }
}
