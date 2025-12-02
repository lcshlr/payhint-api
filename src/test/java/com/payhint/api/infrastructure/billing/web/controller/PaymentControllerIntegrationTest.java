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
import com.payhint.api.application.billing.dto.request.CreatePaymentRequest;
import com.payhint.api.application.billing.dto.request.UpdatePaymentRequest;
import com.payhint.api.domain.billing.model.Invoice;
import com.payhint.api.domain.billing.model.Payment;
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
@Transactional
@DisplayName("PaymentController Integration Tests")
class PaymentControllerIntegrationTest {

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
        private InstallmentId testInstallmentId;
        private String jwtToken;

        @BeforeEach
        void setUp() {
                invoiceSpringRepository.deleteAll();
                customerSpringRepository.deleteAll();
                userSpringRepository.deleteAll();
                rateLimitingFilter.clearBuckets();

                testUser = User.create(new UserId(UUID.randomUUID()), new Email("test.payment@payhint.com"),
                                "Password123!", "Test", "User");
                testUser = userRepository.register(testUser);

                testCustomer = Customer.create(new CustomerId(UUID.randomUUID()), testUser.getId(), "Test Payment Co",
                                new Email("contact@payment.com"));
                testCustomer = customerRepository.save(testCustomer);

                testInvoice = Invoice.create(new InvoiceId(UUID.randomUUID()), testCustomer.getId(),
                                new InvoiceReference("INV-PAY-001"), "USD");
                testInvoice.addInstallment(new Money(new BigDecimal("100.00")), LocalDate.now().plusDays(30));
                testInvoice = invoiceRepository.save(testInvoice);
                testInstallmentId = testInvoice.getInstallments().get(0).getId();

                UserPrincipal userPrincipal = new UserPrincipal(testUser.getId().value(), testUser.getEmail().value(),
                                testUser.getPassword(),
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                jwtToken = jwtTokenProvider.generateToken(userPrincipal);
        }

        @Nested
        @DisplayName("POST /api/invoices/{invoiceId}/installments/{installmentId}/payments")
        class AddPaymentEndpoint {

                @Test
                void shouldAddPaymentSuccessfully() throws Exception {
                        CreatePaymentRequest request = new CreatePaymentRequest(new BigDecimal("50.00"),
                                        LocalDate.now().toString());

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments/{installmentId}/payments",
                                        testInvoice.getId().value(), testInstallmentId.value())
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.id").value(testInvoice.getId().value().toString()))
                                        .andExpect(jsonPath("$.totalPaid").value(50.00))
                                        .andExpect(jsonPath("$.remainingAmount").value(50.00))
                                        .andExpect(jsonPath("$.installments[0].payments", hasSize(1)))
                                        .andExpect(jsonPath("$.installments[0].payments[0].amount").value(50.00));
                }

                @Test
                void shouldFailToAddPaymentWithZeroAmount() throws Exception {
                        CreatePaymentRequest request = new CreatePaymentRequest(BigDecimal.ZERO,
                                        LocalDate.now().toString());

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments/{installmentId}/payments",
                                        testInvoice.getId().value(), testInstallmentId.value())
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors", containsString("amount")));
                }

                @Test
                void shouldFailToAddPaymentWithNegativeAmount() throws Exception {
                        CreatePaymentRequest request = new CreatePaymentRequest(new BigDecimal("-10.00"),
                                        LocalDate.now().toString());

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments/{installmentId}/payments",
                                        testInvoice.getId().value(), testInstallmentId.value())
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors", containsString("amount")));
                }

                @Test
                void shouldFailToAddPaymentExceedingInstallmentAmount() throws Exception {
                        CreatePaymentRequest request = new CreatePaymentRequest(new BigDecimal("101.00"),
                                        LocalDate.now().toString());

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments/{installmentId}/payments",
                                        testInvoice.getId().value(), testInstallmentId.value())
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail")
                                                        .value(containsString("exceeds remaining installment amount")));
                }

                @Test
                void shouldFailToAddPaymentToNonExistentInvoice() throws Exception {
                        CreatePaymentRequest request = new CreatePaymentRequest(new BigDecimal("50.00"),
                                        LocalDate.now().toString());

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments/{installmentId}/payments",
                                        UUID.randomUUID(), testInstallmentId.value())
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void shouldFailToAddPaymentToAnotherUsersInvoice() throws Exception {
                        User otherUser = userRepository.register(User.create(new UserId(UUID.randomUUID()),
                                        new Email("other@payhint.com"), "Pass123!", "Other", "User"));
                        Customer otherCustomer = customerRepository
                                        .save(Customer.create(new CustomerId(UUID.randomUUID()), otherUser.getId(),
                                                        "Other Co", new Email("other@co.com")));
                        Invoice otherInvoice = Invoice.create(new InvoiceId(UUID.randomUUID()), otherCustomer.getId(),
                                        new InvoiceReference("INV-OTHER"), "USD");
                        otherInvoice.addInstallment(new Money(new BigDecimal("100.00")), LocalDate.now());
                        otherInvoice = invoiceRepository.save(otherInvoice);
                        InstallmentId otherInstallmentId = otherInvoice.getInstallments().get(0).getId();

                        CreatePaymentRequest request = new CreatePaymentRequest(new BigDecimal("50.00"),
                                        LocalDate.now().toString());

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments/{installmentId}/payments",
                                        otherInvoice.getId().value(), otherInstallmentId.value())
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void shouldFailToAddPaymentToArchivedInvoice() throws Exception {
                        testInvoice.archive();
                        invoiceRepository.save(testInvoice);

                        CreatePaymentRequest request = new CreatePaymentRequest(new BigDecimal("50.00"),
                                        LocalDate.now().toString());

                        mockMvc.perform(post("/api/invoices/{invoiceId}/installments/{installmentId}/payments",
                                        testInvoice.getId().value(), testInstallmentId.value())
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isInternalServerError());
                }
        }

        @Nested
        @DisplayName("PUT /api/invoices/{invoiceId}/installments/{installmentId}/payments/{paymentId}")
        class UpdatePaymentEndpoint {

                private UUID paymentId;

                @BeforeEach
                void addInitialPayment() {
                        testInvoice.addPayment(testInstallmentId, LocalDate.now(), new Money(new BigDecimal("50.00")));
                        testInvoice = invoiceRepository.save(testInvoice);
                        Payment payment = testInvoice.getInstallments().get(0).getPayments().get(0);
                        paymentId = payment.getId().value();
                }

                @Test
                void shouldUpdatePaymentSuccessfully() throws Exception {
                        UpdatePaymentRequest request = new UpdatePaymentRequest(new BigDecimal("60.00"),
                                        LocalDate.now().plusDays(1).toString());

                        mockMvc.perform(put(
                                        "/api/invoices/{invoiceId}/installments/{installmentId}/payments/{paymentId}",
                                        testInvoice.getId().value(), testInstallmentId.value(), paymentId)
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$.totalPaid").value(60.00))
                                        .andExpect(jsonPath("$.installments[0].payments[0].amount").value(60.00))
                                        .andExpect(jsonPath("$.installments[0].payments[0].paymentDate")
                                                        .value(request.paymentDate()));
                }

                @Test
                void shouldFailToUpdatePaymentWithNegativeAmount() throws Exception {
                        UpdatePaymentRequest request = new UpdatePaymentRequest(new BigDecimal("-5.00"), null);

                        mockMvc.perform(put(
                                        "/api/invoices/{invoiceId}/installments/{installmentId}/payments/{paymentId}",
                                        testInvoice.getId().value(), testInstallmentId.value(), paymentId)
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.errors", containsString("amount")));
                }

                @Test
                void shouldFailToUpdatePaymentExceedingLimit() throws Exception {
                        // Remaining is 50. Current payment is 50. Total installment is 100.
                        // Max new amount for this payment is 100.
                        UpdatePaymentRequest request = new UpdatePaymentRequest(new BigDecimal("101.00"), null);

                        mockMvc.perform(put(
                                        "/api/invoices/{invoiceId}/installments/{installmentId}/payments/{paymentId}",
                                        testInvoice.getId().value(), testInstallmentId.value(), paymentId)
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail")
                                                        .value(containsString("exceeds remaining installment amount")));
                }

                @Test
                void shouldFailToUpdateNonExistentPayment() throws Exception {
                        UpdatePaymentRequest request = new UpdatePaymentRequest(new BigDecimal("60.00"), null);

                        mockMvc.perform(put(
                                        "/api/invoices/{invoiceId}/installments/{installmentId}/payments/{paymentId}",
                                        testInvoice.getId().value(), testInstallmentId.value(), UUID.randomUUID())
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.detail")
                                                        .value(containsString("Payment not found for installment id: "
                                                                        + testInstallmentId.value())));
                }

                @Test
                void shouldFailToUpdatePaymentOnArchivedInvoice() throws Exception {
                        testInvoice.archive();
                        invoiceRepository.save(testInvoice);

                        UpdatePaymentRequest request = new UpdatePaymentRequest(new BigDecimal("60.00"), null);

                        mockMvc.perform(put(
                                        "/api/invoices/{invoiceId}/installments/{installmentId}/payments/{paymentId}",
                                        testInvoice.getId().value(), testInstallmentId.value(), paymentId)
                                                        .header("Authorization", "Bearer " + jwtToken)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isInternalServerError());
                }
        }

        @Nested
        @DisplayName("DELETE /api/invoices/{invoiceId}/installments/{installmentId}/payments/{paymentId}")
        class DeletePaymentEndpoint {

                private UUID paymentId;

                @BeforeEach
                void addInitialPayment() {
                        testInvoice.addPayment(testInstallmentId, LocalDate.now(), new Money(new BigDecimal("50.00")));
                        testInvoice = invoiceRepository.save(testInvoice);
                        Payment payment = testInvoice.getInstallments().get(0).getPayments().get(0);
                        paymentId = payment.getId().value();
                }

                @Test
                void shouldDeletePaymentSuccessfully() throws Exception {
                        mockMvc.perform(delete(
                                        "/api/invoices/{invoiceId}/installments/{installmentId}/payments/{paymentId}",
                                        testInvoice.getId().value(), testInstallmentId.value(), paymentId)
                                                        .header("Authorization", "Bearer " + jwtToken))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$.totalPaid").value(0.00))
                                        .andExpect(jsonPath("$.installments[0].payments", hasSize(0)));
                }

                @Test
                void shouldFailToDeleteNonExistentPayment() throws Exception {
                        mockMvc.perform(delete(
                                        "/api/invoices/{invoiceId}/installments/{installmentId}/payments/{paymentId}",
                                        testInvoice.getId().value(), testInstallmentId.value(), UUID.randomUUID())
                                                        .header("Authorization", "Bearer " + jwtToken))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.detail").value(containsString("Payment not found")));
                }

                @Test
                void shouldFailToDeletePaymentOnArchivedInvoice() throws Exception {
                        testInvoice.archive();
                        invoiceRepository.save(testInvoice);

                        mockMvc.perform(delete(
                                        "/api/invoices/{invoiceId}/installments/{installmentId}/payments/{paymentId}",
                                        testInvoice.getId().value(), testInstallmentId.value(), paymentId)
                                                        .header("Authorization", "Bearer " + jwtToken))
                                        .andExpect(status().isInternalServerError());
                }

                @Test
                void shouldFailToDeletePaymentOnAnotherUsersInvoice() throws Exception {
                        User otherUser = userRepository.register(User.create(new UserId(UUID.randomUUID()),
                                        new Email("other@payhint.com"), "Pass123!", "Other", "User"));
                        Customer otherCustomer = customerRepository
                                        .save(Customer.create(new CustomerId(UUID.randomUUID()), otherUser.getId(),
                                                        "Other Co", new Email("other@co.com")));
                        Invoice otherInvoice = Invoice.create(new InvoiceId(UUID.randomUUID()), otherCustomer.getId(),
                                        new InvoiceReference("INV-OTHER"), "USD");
                        otherInvoice.addInstallment(new Money(new BigDecimal("100.00")), LocalDate.now());
                        otherInvoice = invoiceRepository.save(otherInvoice);
                        InstallmentId otherInstallmentId = otherInvoice.getInstallments().get(0).getId();
                        otherInvoice.addPayment(otherInstallmentId, LocalDate.now(),
                                        new Money(new BigDecimal("50.00")));
                        otherInvoice = invoiceRepository.save(otherInvoice);
                        Payment payment = otherInvoice.getInstallments().get(0).getPayments().get(0);

                        mockMvc.perform(delete(
                                        "/api/invoices/{invoiceId}/installments/{installmentId}/payments/{paymentId}",
                                        otherInvoice.getId().value(), otherInstallmentId.value(),
                                        payment.getId().value()).header("Authorization", "Bearer " + jwtToken))
                                        .andExpect(status().isNotFound());
                }
        }
}