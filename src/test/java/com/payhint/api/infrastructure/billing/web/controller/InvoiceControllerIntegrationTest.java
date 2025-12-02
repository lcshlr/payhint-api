package com.payhint.api.infrastructure.billing.web.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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
import com.payhint.api.application.billing.dto.request.CreateInstallmentRequest;
import com.payhint.api.application.billing.dto.request.CreateInvoiceRequest;
import com.payhint.api.application.billing.dto.request.UpdateInvoiceRequest;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.CustomerRepository;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.Email;
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.infrastructure.billing.persistence.jpa.repository.InvoiceSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.CustomerSpringRepository;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.UserSpringRepository;
import com.payhint.api.infrastructure.shared.security.JwtTokenProvider;
import com.payhint.api.infrastructure.shared.security.RateLimitingFilter;
import com.payhint.api.infrastructure.shared.security.UserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("InvoiceController Integration Tests")
class InvoiceControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CustomerRepository customerRepository;

        @Autowired
        private InvoiceRepository invoiceRepository;

        @Autowired
        private UserSpringRepository userSpringRepository;

        @Autowired
        private CustomerSpringRepository customerSpringRepository;

        @Autowired
        private InvoiceSpringRepository invoiceSpringRepository;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        @Autowired
        private RateLimitingFilter rateLimitingFilter;

        private User testUser;
        private Customer testCustomer;
        private String jwtToken;

        @BeforeEach
        void setUp() {
                invoiceSpringRepository.deleteAll();
                customerSpringRepository.deleteAll();
                userSpringRepository.deleteAll();
                rateLimitingFilter.clearBuckets();

                testUser = User.create(new UserId(UUID.randomUUID()), new Email("test@payhint.com"), "Password123!",
                                "Test", "User");
                testUser = userRepository.register(testUser);

                testCustomer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(), "Test Company",
                                new Email("contact@company.com"));
                testCustomer = customerRepository.save(testCustomer);

                UserPrincipal userPrincipal = new UserPrincipal(testUser.getId().value(), testUser.getEmail().value(),
                                testUser.getPassword(),
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                jwtToken = jwtTokenProvider.generateToken(userPrincipal);
        }

        @Nested
        @DisplayName("POST /api/invoices")
        class CreateInvoiceEndpoint {

                @Test
                @DisplayName("Should create invoice successfully")
                void shouldCreateInvoiceSuccessfully() throws Exception {
                        CreateInvoiceRequest request = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-001",
                                        "EUR", null);

                        mockMvc.perform(post("/api/invoices").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNotEmpty())
                                        .andExpect(jsonPath("$.invoiceReference").value("INV-001"))
                                        .andExpect(jsonPath("$.currency").value("EUR"))
                                        .andExpect(jsonPath("$.customerId")
                                                        .value(testCustomer.getId().value().toString()));
                }

                @Test
                @DisplayName("Should create invoice with installments successfully")
                void shouldCreateInvoiceWithInstallments() throws Exception {
                        List<CreateInstallmentRequest> installments = List.of(new CreateInstallmentRequest(
                                        new BigDecimal("100.00"), LocalDate.now().plusDays(30).toString()));
                        CreateInvoiceRequest request = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-002",
                                        "USD", installments);

                        mockMvc.perform(post("/api/invoices").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.totalAmount").value(100.00))
                                        .andExpect(jsonPath("$.installments", hasSize(1)));
                }

                @Test
                @DisplayName("Should return 400 when validation fails")
                void shouldReturn400WhenValidationFails() throws Exception {
                        CreateInvoiceRequest request = new CreateInvoiceRequest(null, "", "", null);

                        mockMvc.perform(post("/api/invoices").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors", containsString("customerId")))
                                        .andExpect(jsonPath("$.errors", containsString("invoiceReference")))
                                        .andExpect(jsonPath("$.errors", containsString("currency")));
                }

                @Test
                @DisplayName("Should return 400 when installment validation fails")
                void shouldReturn400WhenInstallmentValidationFails() throws Exception {
                        List<CreateInstallmentRequest> installments = List
                                        .of(new CreateInstallmentRequest(BigDecimal.ZERO, "invalid-date"));
                        CreateInvoiceRequest request = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-003",
                                        "USD", installments);

                        mockMvc.perform(post("/api/invoices").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors", containsString("amountDue")))
                                        .andExpect(jsonPath("$.errors", containsString("dueDate")));
                }

                @Test
                @DisplayName("Should return 409 when invoice reference already exists")
                void shouldReturn409WhenReferenceExists() throws Exception {
                        Invoice invoice = Invoice.create(new InvoiceId(UUID.randomUUID()), testCustomer.getId(),
                                        new InvoiceReference("INV-DUP"), "EUR");
                        invoiceRepository.save(invoice);

                        CreateInvoiceRequest request = new CreateInvoiceRequest(testCustomer.getId().value(), "INV-DUP",
                                        "EUR", null);

                        mockMvc.perform(post("/api/invoices").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isConflict());
                }

                @Test
                @DisplayName("Should return 403 when creating invoice for other user's customer")
                void shouldReturn403ForOtherUserCustomer() throws Exception {
                        User otherUser = userRepository.register(User.create(new UserId(UUID.randomUUID()),
                                        new Email("other@payhint.com"), "Pass123!", "Other", "User"));
                        Customer otherCustomer = customerRepository
                                        .save(Customer.create(new CustomerId(UUID.randomUUID()), otherUser.getId(),
                                                        "Other Co", new Email("other@co.com")));

                        CreateInvoiceRequest request = new CreateInvoiceRequest(otherCustomer.getId().value(),
                                        "INV-Other", "EUR", null);

                        mockMvc.perform(post("/api/invoices").header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isForbidden());
                }
        }

        @Nested
        @DisplayName("GET /api/invoices/{id}")
        class GetInvoiceByIdEndpoint {

                @Test
                @DisplayName("Should return invoice details successfully")
                void shouldReturnInvoiceDetails() throws Exception {
                        Invoice invoice = Invoice.create(new InvoiceId(UUID.randomUUID()), testCustomer.getId(),
                                        new InvoiceReference("INV-GET"), "EUR");
                        invoiceRepository.save(invoice);

                        mockMvc.perform(get("/api/invoices/{id}", invoice.getId().value()).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(invoice.getId().value().toString()))
                                        .andExpect(jsonPath("$.invoiceReference").value("INV-GET"));
                }

                @Test
                @DisplayName("Should return 404 when invoice not found")
                void shouldReturn404WhenNotFound() throws Exception {
                        mockMvc.perform(get("/api/invoices/{id}", UUID.randomUUID()).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("Should return 404 (or 403 based on implementation) when accessing other user's invoice")
                void shouldReturn404ForOtherUserInvoice() throws Exception {
                        User otherUser = userRepository.register(User.create(new UserId(UUID.randomUUID()),
                                        new Email("other@payhint.com"), "Pass123!", "Other", "User"));
                        Customer otherCustomer = customerRepository
                                        .save(Customer.create(new CustomerId(UUID.randomUUID()), otherUser.getId(),
                                                        "Other Co", new Email("other@co.com")));
                        Invoice invoice = Invoice.create(new InvoiceId(UUID.randomUUID()), otherCustomer.getId(),
                                        new InvoiceReference("INV-OTHER"), "EUR");
                        invoiceRepository.save(invoice);

                        mockMvc.perform(get("/api/invoices/{id}", invoice.getId().value()).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("Should return 400 for invalid UUID")
                void shouldReturn400ForInvalidUUID() throws Exception {
                        mockMvc.perform(get("/api/invoices/{id}", "invalid-uuid").header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isBadRequest());
                }
        }

        @Nested
        @DisplayName("GET /api/invoices")
        class GetAllInvoicesEndpoint {

                @Test
                @DisplayName("Should return list of invoices for user")
                void shouldReturnInvoiceList() throws Exception {
                        Invoice i1 = Invoice.create(new InvoiceId(UUID.randomUUID()), testCustomer.getId(),
                                        new InvoiceReference("INV-L1"), "EUR");
                        Invoice i2 = Invoice.create(new InvoiceId(UUID.randomUUID()), testCustomer.getId(),
                                        new InvoiceReference("INV-L2"), "USD");
                        invoiceRepository.save(i1);
                        invoiceRepository.save(i2);

                        mockMvc.perform(get("/api/invoices").header("Authorization", "Bearer " + jwtToken))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                                        .andExpect(jsonPath("$[*].invoiceReference",
                                                        containsInAnyOrder("INV-L1", "INV-L2")));
                }

                @Test
                @DisplayName("Should return empty list when no invoices")
                void shouldReturnEmptyList() throws Exception {
                        mockMvc.perform(get("/api/invoices").header("Authorization", "Bearer " + jwtToken))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
                }
        }

        @Nested
        @DisplayName("PUT /api/invoices/{id}")
        class UpdateInvoiceEndpoint {

                @Test
                @DisplayName("Should update invoice successfully")
                void shouldUpdateInvoiceSuccessfully() throws Exception {
                        Invoice invoice = Invoice.create(new InvoiceId(UUID.randomUUID()), testCustomer.getId(),
                                        new InvoiceReference("INV-OLD"), "EUR");
                        invoiceRepository.save(invoice);

                        UpdateInvoiceRequest request = new UpdateInvoiceRequest("INV-NEW", "USD");

                        mockMvc.perform(put("/api/invoices/{id}", invoice.getId().value())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
                                        .andExpect(jsonPath("$.invoiceReference").value("INV-NEW"))
                                        .andExpect(jsonPath("$.currency").value("USD"));
                }

                @Test
                @DisplayName("Should return 400 when validation fails")
                void shouldReturn400WhenValidationFails() throws Exception {
                        Invoice invoice = Invoice.create(new InvoiceId(UUID.randomUUID()), testCustomer.getId(),
                                        new InvoiceReference("INV-VAL"), "EUR");
                        invoiceRepository.save(invoice);

                        String longRef = "A".repeat(51);
                        UpdateInvoiceRequest request = new UpdateInvoiceRequest(longRef, "LONG");

                        mockMvc.perform(put("/api/invoices/{id}", invoice.getId().value())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should return 409 when new reference already exists")
                void shouldReturn409WhenReferenceExists() throws Exception {
                        Invoice i1 = Invoice.create(new InvoiceId(UUID.randomUUID()), testCustomer.getId(),
                                        new InvoiceReference("INV-1"), "EUR");
                        Invoice i2 = Invoice.create(new InvoiceId(UUID.randomUUID()), testCustomer.getId(),
                                        new InvoiceReference("INV-2"), "EUR");
                        invoiceRepository.save(i1);
                        invoiceRepository.save(i2);

                        UpdateInvoiceRequest request = new UpdateInvoiceRequest("INV-1", "EUR");

                        mockMvc.perform(put("/api/invoices/{id}", i2.getId().value())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isConflict());
                }

                @Test
                @DisplayName("Should return 500 (IllegalStateException) when updating archived invoice")
                void shouldReturnErrorWhenUpdatingArchivedInvoice() throws Exception {
                        Invoice invoice = Invoice.create(new InvoiceId(UUID.randomUUID()), testCustomer.getId(),
                                        new InvoiceReference("INV-ARCH"), "EUR");
                        invoice.archive();
                        invoiceRepository.save(invoice);

                        UpdateInvoiceRequest request = new UpdateInvoiceRequest("INV-NEW", "EUR");

                        mockMvc.perform(put("/api/invoices/{id}", invoice.getId().value())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isInternalServerError());
                }
        }

        @Nested
        @DisplayName("DELETE /api/invoices/{id}")
        class DeleteInvoiceEndpoint {

                @Test
                @DisplayName("Should delete invoice successfully")
                void shouldDeleteInvoiceSuccessfully() throws Exception {
                        Invoice invoice = Invoice.create(new InvoiceId(UUID.randomUUID()), testCustomer.getId(),
                                        new InvoiceReference("INV-DEL"), "EUR");
                        invoiceRepository.save(invoice);

                        mockMvc.perform(delete("/api/invoices/{id}", invoice.getId().value()).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isNoContent());

                        mockMvc.perform(get("/api/invoices/{id}", invoice.getId().value()).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("Should return 404 when deleting non-existent invoice")
                void shouldReturn404WhenDeletingNonExistent() throws Exception {
                        mockMvc.perform(delete("/api/invoices/{id}", UUID.randomUUID()).header("Authorization",
                                        "Bearer " + jwtToken)).andExpect(status().isNotFound());
                }
        }
}