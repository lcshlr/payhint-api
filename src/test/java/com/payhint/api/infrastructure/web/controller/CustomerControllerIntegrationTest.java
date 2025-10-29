package com.payhint.api.infrastructure.web.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
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
import com.payhint.api.domain.crm.valueobjects.Email;
import com.payhint.api.infrastructure.persistence.jpa.crm.repository.CustomerSpringRepository;
import com.payhint.api.infrastructure.persistence.jpa.crm.repository.UserSpringRepository;
import com.payhint.api.infrastructure.security.JwtTokenProvider;
import com.payhint.api.infrastructure.security.UserPrincipal;

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

        testUser = new User(new Email(TEST_USER_EMAIL), TEST_USER_PASSWORD, TEST_USER_FIRST_NAME, TEST_USER_LAST_NAME);
        testUser = userRepository.register(testUser);

        UserPrincipal userPrincipal = new UserPrincipal(testUser.getId().value(), testUser.getEmail().value(),
                testUser.getPassword(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        jwtToken = jwtTokenProvider.generateToken(userPrincipal);
    }

    @Nested
    @DisplayName("POST /api/customers")
    class CreateCustomerEndpoint {

        @Test
        @DisplayName("Should successfully create a new customer with valid data")
        void shouldCreateCustomerWithValidData() throws Exception {
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, TEST_CONTACT_EMAIL);

            mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.userId").value(testUser.getId().toString()))
                    .andExpect(jsonPath("$.companyName").value(TEST_COMPANY_NAME))
                    .andExpect(jsonPath("$.contactEmail").value(TEST_CONTACT_EMAIL));
        }

        @Test
        @DisplayName("Should reject creation with blank company name")
        void shouldRejectBlankCompanyName() throws Exception {
            CreateCustomerRequest request = new CreateCustomerRequest("", TEST_CONTACT_EMAIL);

            mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("companyName")))
                    .andExpect(jsonPath("$.path").value("/api/customers"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("Should reject creation with null company name")
        void shouldRejectNullCompanyName() throws Exception {
            CreateCustomerRequest request = new CreateCustomerRequest(null, TEST_CONTACT_EMAIL);

            mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("companyName")));
        }

        @Test
        @DisplayName("Should reject creation with company name exceeding 100 characters")
        void shouldRejectTooLongCompanyName() throws Exception {
            String longCompanyName = "A".repeat(101);
            CreateCustomerRequest request = new CreateCustomerRequest(longCompanyName, TEST_CONTACT_EMAIL);

            mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("companyName")));
        }

        @Test
        @DisplayName("Should reject creation with blank contact email")
        void shouldRejectBlankContactEmail() throws Exception {
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, "");

            mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("contactEmail")));
        }

        @Test
        @DisplayName("Should reject creation with null contact email")
        void shouldRejectNullContactEmail() throws Exception {
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, null);

            mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("contactEmail")));
        }

        @Test
        @DisplayName("Should reject creation with invalid email format")
        void shouldRejectInvalidEmailFormat() throws Exception {
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, "invalid-email");

            mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("contactEmail")));
        }

        @Test
        @DisplayName("Should reject creation with contact email exceeding 100 characters")
        void shouldRejectTooLongContactEmail() throws Exception {
            String longEmail = "a".repeat(90) + "@example.com";
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, longEmail);

            mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("contactEmail")));
        }

        @Test
        @DisplayName("Should reject creation without authentication token")
        void shouldRejectWithoutAuthenticationToken() throws Exception {
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, TEST_CONTACT_EMAIL);

            mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject creation with invalid JWT token")
        void shouldRejectWithInvalidToken() throws Exception {
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, TEST_CONTACT_EMAIL);

            mockMvc.perform(post("/api/customers").header("Authorization", "Bearer " + INVALID_JWT_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject creation with malformed authorization header")
        void shouldRejectWithMalformedAuthorizationHeader() throws Exception {
            CreateCustomerRequest request = new CreateCustomerRequest(TEST_COMPANY_NAME, TEST_CONTACT_EMAIL);

            mockMvc.perform(post("/api/customers").header("Authorization", jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/customers/{id}")
    class GetCustomerByIdEndpoint {

        @Test
        @DisplayName("Should successfully retrieve existing customer")
        void shouldRetrieveExistingCustomer() throws Exception {
            Customer customer = new Customer(testUser.getId(), TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            customer = customerRepository.save(customer);

            mockMvc.perform(get("/api/customers/{id}", customer.getId()).header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(customer.getId().toString()))
                    .andExpect(jsonPath("$.userId").value(testUser.getId().toString()))
                    .andExpect(jsonPath("$.companyName").value(TEST_COMPANY_NAME))
                    .andExpect(jsonPath("$.contactEmail").value(TEST_CONTACT_EMAIL));
        }

        @Test
        @DisplayName("Should return 404 when customer does not exist")
        void shouldReturn404WhenCustomerDoesNotExist() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/api/customers/{id}", nonExistentId).header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found")).andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.path").value("/api/customers/" + nonExistentId))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("Should return 403 when trying to access another user's customer")
        void shouldReturn403WhenAccessingAnotherUsersCustomer() throws Exception {
            User anotherUser = new User(new Email("another@example.com"), "Password123!", "Jane", "Smith");
            anotherUser = userRepository.register(anotherUser);

            Customer anotherCustomer = new Customer(anotherUser.getId(), "Another Company",
                    new Email("contact@another.com"));
            anotherCustomer = customerRepository.save(anotherCustomer);

            mockMvc.perform(
                    get("/api/customers/{id}", anotherCustomer.getId()).header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden")).andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("Should reject request with invalid UUID format")
        void shouldRejectInvalidUuidFormat() throws Exception {
            mockMvc.perform(get("/api/customers/{id}", "invalid-uuid").header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"));
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

            mockMvc.perform(
                    get("/api/customers/{id}", customerId).header("Authorization", "Bearer " + INVALID_JWT_TOKEN))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/customers")
    class GetAllCustomersEndpoint {

        @Test
        @DisplayName("Should successfully retrieve all customers for authenticated user")
        void shouldRetrieveAllCustomersForUser() throws Exception {
            Customer customer1 = new Customer(testUser.getId(), "Company A", new Email("contact@companya.com"));
            Customer customer2 = new Customer(testUser.getId(), "Company B", new Email("contact@companyb.com"));
            customerRepository.save(customer1);
            customerRepository.save(customer2);

            mockMvc.perform(get("/api/customers").header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[*].companyName", containsInAnyOrder("Company A", "Company B")))
                    .andExpect(jsonPath("$[*].contactEmail",
                            containsInAnyOrder("contact@companya.com", "contact@companyb.com")));
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
            Customer ownCustomer = new Customer(testUser.getId(), "Own Company", new Email("own@example.com"));
            customerRepository.save(ownCustomer);

            User anotherUser = new User(new Email("another@example.com"), "Password123!", "Jane", "Smith");
            anotherUser = userRepository.register(anotherUser);
            Customer otherCustomer = new Customer(anotherUser.getId(), "Other Company", new Email("other@example.com"));
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
            Customer customer = new Customer(testUser.getId(), TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            customer = customerRepository.save(customer);

            UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company", "updated@example.com");

            mockMvc.perform(put("/api/customers/{id}", customer.getId()).header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(customer.getId().toString()))
                    .andExpect(jsonPath("$.companyName").value("Updated Company"))
                    .andExpect(jsonPath("$.contactEmail").value("updated@example.com"));
        }

        @Test
        @DisplayName("Should successfully update only company name")
        void shouldUpdateOnlyCompanyName() throws Exception {
            Customer customer = new Customer(testUser.getId(), TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            customer = customerRepository.save(customer);

            UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company", null);

            mockMvc.perform(put("/api/customers/{id}", customer.getId()).header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(customer.getId().toString()))
                    .andExpect(jsonPath("$.companyName").value("Updated Company"))
                    .andExpect(jsonPath("$.contactEmail").value(TEST_CONTACT_EMAIL));
        }

        @Test
        @DisplayName("Should successfully update only contact email")
        void shouldUpdateOnlyContactEmail() throws Exception {
            Customer customer = new Customer(testUser.getId(), TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            customer = customerRepository.save(customer);

            UpdateCustomerRequest request = new UpdateCustomerRequest(null, "updated@example.com");

            mockMvc.perform(put("/api/customers/{id}", customer.getId()).header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(customer.getId().toString()))
                    .andExpect(jsonPath("$.companyName").value(TEST_COMPANY_NAME))
                    .andExpect(jsonPath("$.contactEmail").value("updated@example.com"));
        }

        @Test
        @DisplayName("Should accept update with both fields null")
        void shouldAcceptUpdateWithBothFieldsNull() throws Exception {
            Customer customer = new Customer(testUser.getId(), TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            customer = customerRepository.save(customer);

            UpdateCustomerRequest request = new UpdateCustomerRequest(null, null);

            mockMvc.perform(put("/api/customers/{id}", customer.getId()).header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(customer.getId().toString()))
                    .andExpect(jsonPath("$.companyName").value(TEST_COMPANY_NAME))
                    .andExpect(jsonPath("$.contactEmail").value(TEST_CONTACT_EMAIL));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent customer")
        void shouldReturn404WhenUpdatingNonExistentCustomer() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company", "updated@example.com");

            mockMvc.perform(put("/api/customers/{id}", nonExistentId).header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found")).andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("Should return 403 when trying to update another user's customer")
        void shouldReturn403WhenUpdatingAnotherUsersCustomer() throws Exception {
            User anotherUser = new User(new Email("another@example.com"), "Password123!", "Jane", "Smith");
            anotherUser = userRepository.register(anotherUser);

            Customer anotherCustomer = new Customer(anotherUser.getId(), "Another Company",
                    new Email("contact@another.com"));
            anotherCustomer = customerRepository.save(anotherCustomer);

            UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company", "updated@example.com");

            mockMvc.perform(
                    put("/api/customers/{id}", anotherCustomer.getId()).header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden")).andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("Should reject update with company name exceeding 100 characters")
        void shouldRejectTooLongCompanyName() throws Exception {
            Customer customer = new Customer(testUser.getId(), TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            customer = customerRepository.save(customer);

            String longCompanyName = "A".repeat(101);
            UpdateCustomerRequest request = new UpdateCustomerRequest(longCompanyName, null);

            mockMvc.perform(put("/api/customers/{id}", customer.getId()).header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("companyName")));
        }

        @Test
        @DisplayName("Should reject update with invalid email format")
        void shouldRejectInvalidEmailFormat() throws Exception {
            Customer customer = new Customer(testUser.getId(), TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            customer = customerRepository.save(customer);

            UpdateCustomerRequest request = new UpdateCustomerRequest(null, "invalid-email");

            mockMvc.perform(put("/api/customers/{id}", customer.getId()).header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("contactEmail")));
        }

        @Test
        @DisplayName("Should reject update with contact email exceeding 100 characters")
        void shouldRejectTooLongContactEmail() throws Exception {
            Customer customer = new Customer(testUser.getId(), TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            customer = customerRepository.save(customer);

            String longEmail = "a".repeat(90) + "@example.com";
            UpdateCustomerRequest request = new UpdateCustomerRequest(null, longEmail);

            mockMvc.perform(put("/api/customers/{id}", customer.getId()).header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("contactEmail")));
        }

        @Test
        @DisplayName("Should reject request with invalid UUID format")
        void shouldRejectInvalidUuidFormat() throws Exception {
            UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company", "updated@example.com");

            mockMvc.perform(put("/api/customers/{id}", "invalid-uuid").header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }

        @Test
        @DisplayName("Should reject request without authentication token")
        void shouldRejectWithoutAuthenticationToken() throws Exception {
            UUID customerId = UUID.randomUUID();
            UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company", "updated@example.com");

            mockMvc.perform(put("/api/customers/{id}", customerId).contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject request with invalid JWT token")
        void shouldRejectWithInvalidToken() throws Exception {
            UUID customerId = UUID.randomUUID();
            UpdateCustomerRequest request = new UpdateCustomerRequest("Updated Company", "updated@example.com");

            mockMvc.perform(
                    put("/api/customers/{id}", customerId).header("Authorization", "Bearer " + INVALID_JWT_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/customers/{id}")
    class DeleteCustomerEndpoint {

        @Test
        @DisplayName("Should successfully delete existing customer")
        void shouldDeleteExistingCustomer() throws Exception {
            Customer customer = new Customer(testUser.getId(), TEST_COMPANY_NAME, new Email(TEST_CONTACT_EMAIL));
            customer = customerRepository.save(customer);

            mockMvc.perform(
                    delete("/api/customers/{id}", customer.getId()).header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent customer")
        void shouldReturn404WhenDeletingNonExistentCustomer() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(delete("/api/customers/{id}", nonExistentId).header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found")).andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("Should return 403 when trying to delete another user's customer")
        void shouldReturn403WhenDeletingAnotherUsersCustomer() throws Exception {
            User anotherUser = new User(new Email("another@example.com"), "Password123!", "Jane", "Smith");
            anotherUser = userRepository.register(anotherUser);

            Customer anotherCustomer = new Customer(anotherUser.getId(), "Another Company",
                    new Email("contact@another.com"));
            anotherCustomer = customerRepository.save(anotherCustomer);

            mockMvc.perform(delete("/api/customers/{id}", anotherCustomer.getId()).header("Authorization",
                    "Bearer " + jwtToken)).andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden")).andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("Should reject request with invalid UUID format")
        void shouldRejectInvalidUuidFormat() throws Exception {
            mockMvc.perform(delete("/api/customers/{id}", "invalid-uuid").header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"));
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

            mockMvc.perform(
                    delete("/api/customers/{id}", customerId).header("Authorization", "Bearer " + INVALID_JWT_TOKEN))
                    .andExpect(status().isForbidden());
        }
    }
}
