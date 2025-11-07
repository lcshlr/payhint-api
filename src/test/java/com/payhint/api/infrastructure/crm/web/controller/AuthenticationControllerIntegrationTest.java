package com.payhint.api.infrastructure.crm.web.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payhint.api.application.crm.dto.request.LoginUserRequest;
import com.payhint.api.application.crm.dto.request.RegisterUserRequest;
import com.payhint.api.infrastructure.crm.persistence.jpa.repository.UserSpringRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthenticationController Integration Tests")
class AuthenticationControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserSpringRepository userSpringRepository;

        @BeforeEach
        void setUp() {
                userSpringRepository.deleteAll();
        }

        @Nested
        @DisplayName("POST /api/auth/register")
        class RegisterEndpoint {

                @Test
                @DisplayName("Should successfully register a new user with valid data")
                void shouldRegisterNewUserWithValidData() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("john.doe@example.com", "SecurePass123",
                                        "John", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNotEmpty())
                                        .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                                        .andExpect(jsonPath("$.firstName").value("John"))
                                        .andExpect(jsonPath("$.lastName").value("Doe"));
                }

                @Test
                @DisplayName("Should reject registration with invalid email format")
                void shouldRejectInvalidEmailFormat() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("invalid-email", "SecurePass123", "John",
                                        "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("email"))))
                                        .andExpect(jsonPath("$.instance").value("/api/auth/register"))
                                        .andExpect(jsonPath("$.timestamp").isNotEmpty());
                }

                @Test
                @DisplayName("Should reject registration with blank email")
                void shouldRejectBlankEmail() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("", "SecurePass123", "John", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("email"))));
                }

                @Test
                @DisplayName("Should reject registration with null email")
                void shouldRejectNullEmail() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest(null, "SecurePass123", "John", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("email"))));
                }

                @Test
                @DisplayName("Should reject registration with password shorter than 8 characters")
                void shouldRejectShortPassword() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("john.doe@example.com", "Short1", "John",
                                        "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("password"))));
                }

                @Test
                @DisplayName("Should reject registration with password longer than 30 characters")
                void shouldRejectLongPassword() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("john.doe@example.com",
                                        "ThisPasswordIsWayTooLongAndExceedsTheMaximumLengthOf30Characters", "John",
                                        "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("password"))));
                }

                @Test
                @DisplayName("Should reject registration with blank password")
                void shouldRejectBlankPassword() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("john.doe@example.com", "", "John",
                                        "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("password"))));
                }

                @Test
                @DisplayName("Should reject registration with null password")
                void shouldRejectNullPassword() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("john.doe@example.com", null, "John",
                                        "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("password"))));
                }

                @Test
                @DisplayName("Should reject registration with blank first name")
                void shouldRejectBlankFirstName() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("john.doe@example.com", "SecurePass123",
                                        "", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("firstName"))));
                }

                @Test
                @DisplayName("Should reject registration with null first name")
                void shouldRejectNullFirstName() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("john.doe@example.com", "SecurePass123",
                                        null, "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("firstName"))));
                }

                @Test
                @DisplayName("Should reject registration with blank last name")
                void shouldRejectBlankLastName() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("john.doe@example.com", "SecurePass123",
                                        "John", "");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(Objects.requireNonNull(Objects
                                                                        .requireNonNull(containsString("lastName"))))));
                }

                @Test
                @DisplayName("Should reject registration with null last name")
                void shouldRejectNullLastName() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("john.doe@example.com", "SecurePass123",
                                        "John", null);

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(Objects.requireNonNull(Objects
                                                                        .requireNonNull(containsString("lastName"))))));
                }

                @Test
                @DisplayName("Should reject registration with empty request body")
                void shouldRejectEmptyRequestBody() throws Exception {
                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)).content("{}"))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"));
                }

                @Test
                @DisplayName("Should reject registration with duplicate email")
                void shouldRejectDuplicateEmail() throws Exception {
                        RegisterUserRequest firstRequest = new RegisterUserRequest("duplicate@example.com",
                                        "SecurePass123", "John", "Doe");
                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(firstRequest))))
                                        .andExpect(status().isCreated());

                        RegisterUserRequest duplicateRequest = new RegisterUserRequest("duplicate@example.com",
                                        "AnotherPass123", "Jane", "Smith");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(
                                                        objectMapper.writeValueAsString(duplicateRequest))))
                                        .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                                        .andExpect(jsonPath("$.title").value("Resource Already Exists"))
                                        .andExpect(jsonPath("$.detail")
                                                        .value(Objects.requireNonNull(containsString("already"))));
                }

                @Test
                @DisplayName("Should accept registration with minimum valid password length")
                void shouldAcceptMinimumPasswordLength() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("min.pass@example.com", "Pass1234",
                                        "John", "Doe");
                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.email").value("min.pass@example.com"));
                }

                @Test
                @DisplayName("Should accept registration with maximum valid password length")
                void shouldAcceptMaximumPasswordLength() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("max.pass@example.com",
                                        "ThisPasswordIsExactly30Chars!", "John", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.email").value("max.pass@example.com"));
                }

                @Test
                @DisplayName("Should accept registration with special characters in name")
                void shouldAcceptSpecialCharactersInName() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("special@example.com", "SecurePass123",
                                        "Jean-Pierre", "O'Connor");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.firstName").value("Jean-Pierre"))
                                        .andExpect(jsonPath("$.lastName").value("O'Connor"));
                }

                @Test
                @DisplayName("Should accept registration with various valid email formats")
                void shouldAcceptValidEmailFormats() throws Exception {
                        String[] validEmails = { "user@example.com", "user.name@example.com", "user+tag@example.co.uk",
                                        "user_name@example-domain.com" };

                        for (int i = 0; i < validEmails.length; i++) {
                                RegisterUserRequest request = new RegisterUserRequest(validEmails[i], "SecurePass123",
                                                "User" + i, "Test" + i);

                                mockMvc.perform(post("/api/auth/register")
                                                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                                .content(Objects.requireNonNull(
                                                                objectMapper.writeValueAsString(request))))
                                                .andExpect(status().isCreated())
                                                .andExpect(jsonPath("$.email").value(validEmails[i]));
                        }
                }

                @Test
                @DisplayName("Should reject registration with whitespace-only firstName")
                void shouldRejectWhitespaceOnlyFirstName() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("whitespace@example.com", "SecurePass123",
                                        "   ", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("firstName"))));
                }

                @Test
                @DisplayName("Should reject registration with whitespace-only lastName")
                void shouldRejectWhitespaceOnlyLastName() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("whitespace2@example.com",
                                        "SecurePass123", "John", "   ");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(Objects.requireNonNull(Objects
                                                                        .requireNonNull(containsString("lastName"))))));
                }

                @Test
                @DisplayName("Should reject registration with whitespace-only password")
                void shouldRejectWhitespaceOnlyPassword() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("whitespace3@example.com", "        ",
                                        "John", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("password"))));
                }
        }

        @Nested
        @DisplayName("POST /api/auth/login")
        class LoginEndpoint {

                @Test
                @DisplayName("Should successfully login with valid credentials")
                void shouldLoginWithValidCredentials() throws Exception {
                        RegisterUserRequest registerRequest = new RegisterUserRequest("login.test@example.com",
                                        "SecurePass123", "John", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(
                                                        objectMapper.writeValueAsString(registerRequest))))
                                        .andExpect(status().isCreated());

                        LoginUserRequest loginRequest = new LoginUserRequest("login.test@example.com", "SecurePass123");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
                                        .andExpect(jsonPath("$.token").isString());
                }

                @Test
                @DisplayName("Should reject login with incorrect password")
                void shouldRejectIncorrectPassword() throws Exception {
                        RegisterUserRequest registerRequest = new RegisterUserRequest("wrong.pass@example.com",
                                        "CorrectPass123", "John", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(
                                                        objectMapper.writeValueAsString(registerRequest))))
                                        .andExpect(status().isCreated());

                        LoginUserRequest loginRequest = new LoginUserRequest("wrong.pass@example.com",
                                        "WrongPassword123");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                                        .andExpect(jsonPath("$.title").value("Authentication Failed"))
                                        .andExpect(jsonPath("$.detail").value("Invalid email or password"))
                                        .andExpect(jsonPath("$.instance").value("/api/auth/login"))
                                        .andExpect(jsonPath("$.timestamp").isNotEmpty());
                }

                @Test
                @DisplayName("Should reject login with non-existent email")
                void shouldRejectNonExistentEmail() throws Exception {
                        LoginUserRequest loginRequest = new LoginUserRequest("nonexistent@example.com",
                                        "SomePassword123");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                                        .andExpect(jsonPath("$.title").value("Authentication Failed"))
                                        .andExpect(jsonPath("$.detail").value("Invalid email or password"));
                }

                @Test
                @DisplayName("Should reject login with blank email")
                void shouldRejectBlankEmail() throws Exception {
                        LoginUserRequest loginRequest = new LoginUserRequest("", "SecurePass123");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("email"))));
                }

                @Test
                @DisplayName("Should reject login with null email")
                void shouldRejectNullEmail() throws Exception {
                        LoginUserRequest loginRequest = new LoginUserRequest(null, "SecurePass123");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("email"))));
                }

                @Test
                @DisplayName("Should reject login with invalid email format")
                void shouldRejectInvalidEmailFormat() throws Exception {
                        LoginUserRequest loginRequest = new LoginUserRequest("not-an-email", "SecurePass123");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("email"))));
                }

                @Test
                @DisplayName("Should reject login with blank password")
                void shouldRejectBlankPassword() throws Exception {
                        LoginUserRequest loginRequest = new LoginUserRequest("test@example.com", "");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("password"))));
                }

                @Test
                @DisplayName("Should reject login with null password")
                void shouldRejectNullPassword() throws Exception {
                        LoginUserRequest loginRequest = new LoginUserRequest("test@example.com", null);

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("password"))));
                }

                @Test
                @DisplayName("Should reject login with password shorter than 8 characters")
                void shouldRejectShortPassword() throws Exception {
                        LoginUserRequest loginRequest = new LoginUserRequest("test@example.com", "Short1");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("password"))));
                }

                @Test
                @DisplayName("Should reject login with password longer than 30 characters")
                void shouldRejectLongPassword() throws Exception {
                        LoginUserRequest loginRequest = new LoginUserRequest("test@example.com",
                                        "ThisPasswordIsWayTooLongAndExceedsTheMaximumLengthOf30Characters");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("password"))));
                }

                @Test
                @DisplayName("Should reject login with empty request body")
                void shouldRejectEmptyRequestBody() throws Exception {
                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)).content("{}"))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"));
                }

                @Test
                @DisplayName("Should reject login with whitespace-only email")
                void shouldRejectWhitespaceOnlyEmail() throws Exception {
                        LoginUserRequest loginRequest = new LoginUserRequest("   ", "SecurePass123");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("email"))));
                }

                @Test
                @DisplayName("Should reject login with whitespace-only password")
                void shouldRejectWhitespaceOnlyPassword() throws Exception {
                        LoginUserRequest loginRequest = new LoginUserRequest("test@example.com", "        ");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                                        .andExpect(jsonPath("$.title").value("Invalid Input"))
                                        .andExpect(jsonPath("$.detail").value("Validation failed"))
                                        .andExpect(jsonPath("$.errors")
                                                        .value(Objects.requireNonNull(containsString("password"))));
                }

                @Test
                @DisplayName("Should handle case sensitivity in email for login")
                void shouldHandleCaseSensitivityInEmail() throws Exception {
                        RegisterUserRequest registerRequest = new RegisterUserRequest("CaseSensitive@Example.COM",
                                        "SecurePass123", "John", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(
                                                        objectMapper.writeValueAsString(registerRequest))))
                                        .andExpect(status().isCreated());

                        LoginUserRequest loginRequest = new LoginUserRequest("CaseSensitive@Example.COM",
                                        "SecurePass123");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
                }

                @Test
                @DisplayName("Should return different tokens for multiple logins")
                void shouldReturnDifferentTokensForMultipleLogins() throws Exception {
                        RegisterUserRequest registerRequest = new RegisterUserRequest("multi.login@example.com",
                                        "SecurePass123", "John", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(
                                                        objectMapper.writeValueAsString(registerRequest))))
                                        .andExpect(status().isCreated());

                        LoginUserRequest loginRequest = new LoginUserRequest("multi.login@example.com",
                                        "SecurePass123");

                        String firstToken = mockMvc
                                        .perform(post("/api/auth/login")
                                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                                        .content(Objects.requireNonNull(
                                                                        objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
                                        .andReturn().getResponse().getContentAsString();

                        Thread.sleep(1000);

                        String secondToken = mockMvc
                                        .perform(post("/api/auth/login")
                                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                                        .content(Objects.requireNonNull(
                                                                        objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
                                        .andReturn().getResponse().getContentAsString();

                        org.junit.jupiter.api.Assertions.assertNotEquals(firstToken, secondToken);
                }

                @Test
                @DisplayName("Should accept login with minimum valid password length")
                void shouldAcceptMinimumPasswordLength() throws Exception {
                        RegisterUserRequest registerRequest = new RegisterUserRequest("min.login@example.com",
                                        "Pass1234", "John", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(
                                                        objectMapper.writeValueAsString(registerRequest))))
                                        .andExpect(status().isCreated());

                        LoginUserRequest loginRequest = new LoginUserRequest("min.login@example.com", "Pass1234");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
                }

                @Test
                @DisplayName("Should accept login with maximum valid password length")
                void shouldAcceptMaximumPasswordLength() throws Exception {
                        RegisterUserRequest registerRequest = new RegisterUserRequest("max.login@example.com",
                                        "ThisPasswordIsExactly30Chars!", "John", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(
                                                        objectMapper.writeValueAsString(registerRequest))))
                                        .andExpect(status().isCreated());

                        LoginUserRequest loginRequest = new LoginUserRequest("max.login@example.com",
                                        "ThisPasswordIsExactly30Chars!");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
                }
        }

        @Nested
        @DisplayName("Endpoint Security")
        class EndpointSecurity {

                @Test
                @DisplayName("Should allow unauthenticated access to register endpoint")
                void shouldAllowUnauthenticatedRegister() throws Exception {
                        RegisterUserRequest request = new RegisterUserRequest("public.access@example.com",
                                        "SecurePass123", "John", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                                        .andExpect(status().isCreated());
                }

                @Test
                @DisplayName("Should allow unauthenticated access to login endpoint")
                void shouldAllowUnauthenticatedLogin() throws Exception {
                        RegisterUserRequest registerRequest = new RegisterUserRequest("public.login@example.com",
                                        "SecurePass123", "John", "Doe");

                        mockMvc.perform(post("/api/auth/register")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(
                                                        objectMapper.writeValueAsString(registerRequest))))
                                        .andExpect(status().isCreated());

                        LoginUserRequest loginRequest = new LoginUserRequest("public.login@example.com",
                                        "SecurePass123");

                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginRequest))))
                                        .andExpect(status().isOk());
                }
        }
}
