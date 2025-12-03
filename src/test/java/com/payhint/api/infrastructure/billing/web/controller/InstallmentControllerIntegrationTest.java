package com.payhint.api.infrastructure.billing.web.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payhint.api.application.billing.dto.request.CreateInstallmentRequest;
import com.payhint.api.application.billing.dto.request.UpdateInstallmentRequest;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.repository.InvoiceRepository;
import com.payhint.api.domain.billing.valueobject.InstallmentId;
import com.payhint.api.domain.billing.valueobject.InvoiceId;
import com.payhint.api.domain.billing.valueobject.InvoiceReference;
import com.payhint.api.domain.billing.valueobject.Money;
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
@DisplayName("InstallmentController Integration Tests")
class InstallmentControllerIntegrationTest {

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
        private Invoice testInvoice;
        private String jwtToken;

        @BeforeEach
        void setUp() {
                invoiceSpringRepository.deleteAll();
                customerSpringRepository.deleteAll();
                userSpringRepository.deleteAll();
                rateLimitingFilter.clearBuckets();

                testUser = User.create(new UserId(UUID.randomUUID()), new Email("test.installment@payhint.com"),
                                "Password123!", "Test", "User");
                testUser = userRepository.register(testUser);

                testCustomer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(), "Test Company",
                                new Email("contact@company.com"));
                testCustomer = customerRepository.save(testCustomer);

                testInvoice = Invoice.create(new InvoiceId(UUID.randomUUID()), testCustomer.getId(),
                                new InvoiceReference("INV-INST-001"), "EUR");
                testInvoice = invoiceRepository.save(testInvoice);

                UserPrincipal userPrincipal = new UserPrincipal(testUser.getId().value(), testUser.getEmail().value(),
                                testUser.getPassword(),
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                jwtToken = jwtTokenProvider.generateToken(userPrincipal);
        }

        @AfterEach
        void tearDown() {
                invoiceSpringRepository.deleteAll();
                customerSpringRepository.deleteAll();
                userSpringRepository.deleteAll();
                rateLimitingFilter.clearBuckets();
        }

        @Nested
        @DisplayName("POST /api/invoices/{invoiceId}/installments")
        class AddInstallmentEndpoint {

                @Test
                void shouldAddInstallmentSuccessfully() throws Exception {
                        CreateInstallmentRequest request = new CreateInstallmentRequest(new BigDecimal("100.00"),
                                        LocalDate.now().plusDays(30).toString());

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments", testInvoice.getId().value())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.id").value(testInvoice.getId().value().toString()))
                                        .andExpect(jsonPath("$.totalAmount").value(100.00))
                                        .andExpect(jsonPath("$.installments", hasSize(1)))
                                        .andExpect(jsonPath("$.installments[0].amountDue").value(100.00))
                                        .andExpect(jsonPath("$.installments[0].dueDate").value(request.dueDate()));
                }

                @Test
                void shouldFailToAddInstallmentWithZeroAmount() throws Exception {
                        CreateInstallmentRequest request = new CreateInstallmentRequest(BigDecimal.ZERO,
                                        LocalDate.now().plusDays(30).toString());

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments", testInvoice.getId().value())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors", containsString("amountDue")));
                }

                @Test
                @DisplayName("Should fail to add installment with negative amount")
                void shouldFailToAddInstallmentWithNegativeAmount() throws Exception {
                        CreateInstallmentRequest request = new CreateInstallmentRequest(new BigDecimal("-50.00"),
                                        LocalDate.now().plusDays(30).toString());

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments", testInvoice.getId().value())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors", containsString("amountDue")));
                }

                @Test
                void shouldFailToAddInstallmentWithInvalidDateFormat() throws Exception {
                        String jsonRequest = """
                                        {
                                            "amountDue": 100.00,
                                            "dueDate": "30-12-2025"
                                        }
                                        """;

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments", testInvoice.getId().value())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON).content(jsonRequest))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors", containsString("dueDate")));
                }

                @Test
                void shouldFailToAddInstallmentToNonExistentInvoice() throws Exception {
                        CreateInstallmentRequest request = new CreateInstallmentRequest(new BigDecimal("100.00"),
                                        LocalDate.now().plusDays(30).toString());

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments", UUID.randomUUID())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.title").value("Resource Not Found"));
                }

                @Test
                void shouldFailToAddInstallmentToAnotherUsersInvoice() throws Exception {
                        User otherUser = userRepository.register(User.create(new UserId(UUID.randomUUID()),
                                        new Email("other@payhint.com"), "Pass123!", "Other", "User"));
                        Customer otherCustomer = customerRepository
                                        .save(Customer.create(new CustomerId(UUID.randomUUID()), otherUser.getId(),
                                                        "Other Co", new Email("other@co.com")));
                        Invoice otherInvoice = Invoice.create(new InvoiceId(UUID.randomUUID()), otherCustomer.getId(),
                                        new InvoiceReference("INV-OTHER"), "EUR");
                        invoiceRepository.save(otherInvoice);

                        CreateInstallmentRequest request = new CreateInstallmentRequest(new BigDecimal("100.00"),
                                        LocalDate.now().plusDays(30).toString());

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments", otherInvoice.getId().value())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void shouldFailToAddInstallmentWithDuplicateDueDate() throws Exception {
                        String dueDate = LocalDate.now().plusDays(10).toString();
                        testInvoice.addInstallment(new Money(new BigDecimal("100.00")), LocalDate.parse(dueDate));
                        invoiceRepository.save(testInvoice);

                        CreateInstallmentRequest request = new CreateInstallmentRequest(new BigDecimal("50.00"),
                                        dueDate);

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments", testInvoice.getId().value())
                                        .header("Authorization", "Bearer " + jwtToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.detail").value(containsString(
                                                        "An installment with same due date already exists")));
                }
        }

        @Nested
        @DisplayName("PUT /api/invoices/{invoiceId}/installments/{installmentId}")
        class UpdateInstallmentEndpoint {

                private InstallmentId installmentId;

                @BeforeEach
                void addInitialInstallment() {
                        testInvoice.addInstallment(new Money(new BigDecimal("100.00")), LocalDate.now().plusDays(30));
                        testInvoice = invoiceRepository.save(testInvoice);
                        installmentId = testInvoice.getInstallments().get(0).getId();
                }

                @Test
                void shouldUpdateInstallmentSuccessfully() throws Exception {
                        UpdateInstallmentRequest request = new UpdateInstallmentRequest(new BigDecimal("150.00"),
                                        LocalDate.now().plusDays(45).toString());

                        mockMvc.perform(put("/api/invoices/{invoiceId}/installments/{installmentId}",
                                        testInvoice.getId().value(), installmentId.value())
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(150.00))
                                        .andExpect(jsonPath("$.installments[0].amountDue").value(150.00))
                                        .andExpect(jsonPath("$.installments[0].dueDate").value(request.dueDate()));
                }

                @Test
                void shouldFailToUpdateInstallmentWithNegativeAmount() throws Exception {
                        UpdateInstallmentRequest request = new UpdateInstallmentRequest(new BigDecimal("-10.00"), null);

                        mockMvc.perform(put("/api/invoices/{invoiceId}/installments/{installmentId}",
                                        testInvoice.getId().value(), installmentId.value())
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors", containsString("amountDue")));
                }

                @Test
                void shouldFailToUpdateInstallmentToNonExistentInvoice() throws Exception {
                        UpdateInstallmentRequest request = new UpdateInstallmentRequest(new BigDecimal("150.00"), null);

                        mockMvc.perform(put("/api/invoices/{invoiceId}/installments/{installmentId}", UUID.randomUUID(),
                                        installmentId.value()).header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void shouldFailToUpdateNonExistentInstallment() throws Exception {
                        UpdateInstallmentRequest request = new UpdateInstallmentRequest(new BigDecimal("150.00"), null);

                        mockMvc.perform(put("/api/invoices/{invoiceId}/installments/{installmentId}",
                                        testInvoice.getId().value(), UUID.randomUUID())
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(
                                                        containsString("Installment does not belong to invoice")));
                }

                @Test
                void shouldFailToUpdateInstallmentWhenInvoiceIsArchived() throws Exception {
                        testInvoice.archive();
                        invoiceRepository.save(testInvoice);

                        UpdateInstallmentRequest request = new UpdateInstallmentRequest(new BigDecimal("150.00"), null);

                        mockMvc.perform(put("/api/invoices/{invoiceId}/installments/{installmentId}",
                                        testInvoice.getId().value(), installmentId.value())
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isInternalServerError());
                }
        }

        @Nested
        @DisplayName("DELETE /api/invoices/{invoiceId}/installments/{installmentId}")
        class DeleteInstallmentEndpoint {

                private InstallmentId installmentId;

                @BeforeEach
                void addInitialInstallment() {
                        testInvoice.addInstallment(new Money(new BigDecimal("100.00")), LocalDate.now().plusDays(30));
                        testInvoice = invoiceRepository.save(testInvoice);
                        installmentId = testInvoice.getInstallments().get(0).getId();
                }

                @Test
                void shouldDeleteInstallmentSuccessfully() throws Exception {
                        mockMvc.perform(delete("/api/invoices/{invoiceId}/installments/{installmentId}",
                                        testInvoice.getId().value(), installmentId.value()).header("Authorization",
                                                        "Bearer " + jwtToken))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(0.00))
                                        .andExpect(jsonPath("$.installments", hasSize(0)));
                }

                @Test
                void shouldFailToDeleteNonExistentInstallment() throws Exception {
                        mockMvc.perform(delete("/api/invoices/{invoiceId}/installments/{installmentId}",
                                        testInvoice.getId().value(), UUID.randomUUID()).header("Authorization",
                                                        "Bearer " + jwtToken))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(
                                                        containsString("Installment does not belong to invoice")));
                }

                @Test
                void shouldFailToDeleteInstallmentFromNonExistentInvoice() throws Exception {
                        mockMvc.perform(delete("/api/invoices/{invoiceId}/installments/{installmentId}",
                                        UUID.randomUUID(), installmentId.value()).header("Authorization",
                                                        "Bearer " + jwtToken))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void shouldFailToDeleteInstallmentFromOtherUserInvoice() throws Exception {
                        User otherUser = userRepository.register(User.create(new UserId(UUID.randomUUID()),
                                        new Email("other@payhint.com"), "Pass123!", "Other", "User"));
                        Customer otherCustomer = customerRepository
                                        .save(Customer.create(new CustomerId(UUID.randomUUID()), otherUser.getId(),
                                                        "Other Co", new Email("other@co.com")));
                        Invoice otherInvoice = Invoice.create(new InvoiceId(UUID.randomUUID()), otherCustomer.getId(),
                                        new InvoiceReference("INV-OTHER"), "EUR");
                        InstallmentId otherInstId = new InstallmentId(UUID.randomUUID());
                        otherInvoice.addInstallment(new Money(BigDecimal.TEN), LocalDate.now());
                        // Need to save to get ID persistence if we were fetching it, but for delete we
                        // assume knowledge of ID
                        otherInvoice = invoiceRepository.save(otherInvoice);
                        otherInstId = otherInvoice.getInstallments().get(0).getId();

                        mockMvc.perform(delete("/api/invoices/{invoiceId}/installments/{installmentId}",
                                        otherInvoice.getId().value(), otherInstId.value()).header("Authorization",
                                                        "Bearer " + jwtToken))
                                        .andExpect(status().isNotFound());
                }
        }
}