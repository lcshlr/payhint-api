package com.payhint.api.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payhint.api.application.crm.dto.request.LoginUserRequest;
import com.payhint.api.application.crm.dto.request.RegisterUserRequest;
import com.payhint.api.infrastructure.persistence.jpa.crm.repository.UserSpringRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("john.doe@example.com");
            request.setPassword("SecurePass123");
            request.setFirstName("John");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                    .andExpect(jsonPath("$.firstName").value("John")).andExpect(jsonPath("$.lastName").value("Doe"));
        }

        @Test
        @DisplayName("Should reject registration with invalid email format")
        void shouldRejectInvalidEmailFormat() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("invalid-email");
            request.setPassword("SecurePass123");
            request.setFirstName("John");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("email")))
                    .andExpect(jsonPath("$.path").value("/api/auth/register"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("Should reject registration with blank email")
        void shouldRejectBlankEmail() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("");
            request.setPassword("SecurePass123");
            request.setFirstName("John");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("email")));
        }

        @Test
        @DisplayName("Should reject registration with null email")
        void shouldRejectNullEmail() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail(null);
            request.setPassword("SecurePass123");
            request.setFirstName("John");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("email")));
        }

        @Test
        @DisplayName("Should reject registration with password shorter than 8 characters")
        void shouldRejectShortPassword() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("john.doe@example.com");
            request.setPassword("Short1");
            request.setFirstName("John");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("password")));
        }

        @Test
        @DisplayName("Should reject registration with password longer than 30 characters")
        void shouldRejectLongPassword() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("john.doe@example.com");
            request.setPassword("ThisPasswordIsWayTooLongAndExceedsTheMaximumLengthOf30Characters");
            request.setFirstName("John");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("password")));
        }

        @Test
        @DisplayName("Should reject registration with blank password")
        void shouldRejectBlankPassword() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("john.doe@example.com");
            request.setPassword("");
            request.setFirstName("John");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("password")));
        }

        @Test
        @DisplayName("Should reject registration with null password")
        void shouldRejectNullPassword() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("john.doe@example.com");
            request.setPassword(null);
            request.setFirstName("John");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("password")));
        }

        @Test
        @DisplayName("Should reject registration with blank first name")
        void shouldRejectBlankFirstName() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("john.doe@example.com");
            request.setPassword("SecurePass123");
            request.setFirstName("");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("firstName")));
        }

        @Test
        @DisplayName("Should reject registration with null first name")
        void shouldRejectNullFirstName() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("john.doe@example.com");
            request.setPassword("SecurePass123");
            request.setFirstName(null);
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("firstName")));
        }

        @Test
        @DisplayName("Should reject registration with blank last name")
        void shouldRejectBlankLastName() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("john.doe@example.com");
            request.setPassword("SecurePass123");
            request.setFirstName("John");
            request.setLastName("");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("lastName")));
        }

        @Test
        @DisplayName("Should reject registration with null last name")
        void shouldRejectNullLastName() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("john.doe@example.com");
            request.setPassword("SecurePass123");
            request.setFirstName("John");
            request.setLastName(null);

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("lastName")));
        }

        @Test
        @DisplayName("Should reject registration with empty request body")
        void shouldRejectEmptyRequestBody() throws Exception {
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }

        @Test
        @DisplayName("Should reject registration with duplicate email")
        void shouldRejectDuplicateEmail() throws Exception {
            RegisterUserRequest firstRequest = new RegisterUserRequest();
            firstRequest.setEmail("duplicate@example.com");
            firstRequest.setPassword("SecurePass123");
            firstRequest.setFirstName("John");
            firstRequest.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstRequest))).andExpect(status().isCreated());

            RegisterUserRequest duplicateRequest = new RegisterUserRequest();
            duplicateRequest.setEmail("duplicate@example.com");
            duplicateRequest.setPassword("AnotherPass123");
            duplicateRequest.setFirstName("Jane");
            duplicateRequest.setLastName("Smith");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateRequest))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("already")));
        }

        @Test
        @DisplayName("Should accept registration with minimum valid password length")
        void shouldAcceptMinimumPasswordLength() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("min.pass@example.com");
            request.setPassword("Pass1234");
            request.setFirstName("John");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("min.pass@example.com"));
        }

        @Test
        @DisplayName("Should accept registration with maximum valid password length")
        void shouldAcceptMaximumPasswordLength() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("max.pass@example.com");
            request.setPassword("ThisPasswordIsExactly30Chars!");
            request.setFirstName("John");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("max.pass@example.com"));
        }

        @Test
        @DisplayName("Should accept registration with special characters in name")
        void shouldAcceptSpecialCharactersInName() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("special@example.com");
            request.setPassword("SecurePass123");
            request.setFirstName("Jean-Pierre");
            request.setLastName("O'Connor");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.firstName").value("Jean-Pierre"))
                    .andExpect(jsonPath("$.lastName").value("O'Connor"));
        }

        @Test
        @DisplayName("Should accept registration with various valid email formats")
        void shouldAcceptValidEmailFormats() throws Exception {
            String[] validEmails = { "user@example.com", "user.name@example.com", "user+tag@example.co.uk",
                    "user_name@example-domain.com" };

            for (int i = 0; i < validEmails.length; i++) {
                RegisterUserRequest request = new RegisterUserRequest();
                request.setEmail(validEmails[i]);
                request.setPassword("SecurePass123");
                request.setFirstName("User" + i);
                request.setLastName("Test" + i);

                mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
                        .andExpect(jsonPath("$.email").value(validEmails[i]));
            }
        }

        @Test
        @DisplayName("Should reject registration with whitespace-only firstName")
        void shouldRejectWhitespaceOnlyFirstName() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("whitespace@example.com");
            request.setPassword("SecurePass123");
            request.setFirstName("   ");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("firstName")));
        }

        @Test
        @DisplayName("Should reject registration with whitespace-only lastName")
        void shouldRejectWhitespaceOnlyLastName() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("whitespace2@example.com");
            request.setPassword("SecurePass123");
            request.setFirstName("John");
            request.setLastName("   ");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("lastName")));
        }

        @Test
        @DisplayName("Should reject registration with whitespace-only password")
        void shouldRejectWhitespaceOnlyPassword() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("whitespace3@example.com");
            request.setPassword("        ");
            request.setFirstName("John");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("password")));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("Should successfully login with valid credentials")
        void shouldLoginWithValidCredentials() throws Exception {
            RegisterUserRequest registerRequest = new RegisterUserRequest();
            registerRequest.setEmail("login.test@example.com");
            registerRequest.setPassword("SecurePass123");
            registerRequest.setFirstName("John");
            registerRequest.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))).andExpect(status().isCreated());

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("login.test@example.com");
            loginRequest.setPassword("SecurePass123");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty()).andExpect(jsonPath("$.token").isString());
        }

        @Test
        @DisplayName("Should reject login with incorrect password")
        void shouldRejectIncorrectPassword() throws Exception {
            RegisterUserRequest registerRequest = new RegisterUserRequest();
            registerRequest.setEmail("wrong.pass@example.com");
            registerRequest.setPassword("CorrectPass123");
            registerRequest.setFirstName("John");
            registerRequest.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))).andExpect(status().isCreated());

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("wrong.pass@example.com");
            loginRequest.setPassword("WrongPassword123");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"))
                    .andExpect(jsonPath("$.path").value("/api/auth/login"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("Should reject login with non-existent email")
        void shouldRejectNonExistentEmail() throws Exception {
            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("nonexistent@example.com");
            loginRequest.setPassword("SomePassword123");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("Should reject login with blank email")
        void shouldRejectBlankEmail() throws Exception {
            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("");
            loginRequest.setPassword("SecurePass123");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("email")));
        }

        @Test
        @DisplayName("Should reject login with null email")
        void shouldRejectNullEmail() throws Exception {
            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail(null);
            loginRequest.setPassword("SecurePass123");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("email")));
        }

        @Test
        @DisplayName("Should reject login with invalid email format")
        void shouldRejectInvalidEmailFormat() throws Exception {
            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("not-an-email");
            loginRequest.setPassword("SecurePass123");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("email")));
        }

        @Test
        @DisplayName("Should reject login with blank password")
        void shouldRejectBlankPassword() throws Exception {
            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("test@example.com");
            loginRequest.setPassword("");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("password")));
        }

        @Test
        @DisplayName("Should reject login with null password")
        void shouldRejectNullPassword() throws Exception {
            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("test@example.com");
            loginRequest.setPassword(null);

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("password")));
        }

        @Test
        @DisplayName("Should reject login with password shorter than 8 characters")
        void shouldRejectShortPassword() throws Exception {
            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("test@example.com");
            loginRequest.setPassword("Short1");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("password")));
        }

        @Test
        @DisplayName("Should reject login with password longer than 30 characters")
        void shouldRejectLongPassword() throws Exception {
            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("test@example.com");
            loginRequest.setPassword("ThisPasswordIsWayTooLongAndExceedsTheMaximumLengthOf30Characters");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(containsString("password")));
        }

        @Test
        @DisplayName("Should reject login with empty request body")
        void shouldRejectEmptyRequestBody() throws Exception {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }

        @Test
        @DisplayName("Should reject login with whitespace-only email")
        void shouldRejectWhitespaceOnlyEmail() throws Exception {
            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("   ");
            loginRequest.setPassword("SecurePass123");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("email")));
        }

        @Test
        @DisplayName("Should reject login with whitespace-only password")
        void shouldRejectWhitespaceOnlyPassword() throws Exception {
            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("test@example.com");
            loginRequest.setPassword("        ");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("password")));
        }

        @Test
        @DisplayName("Should handle case sensitivity in email for login")
        void shouldHandleCaseSensitivityInEmail() throws Exception {
            RegisterUserRequest registerRequest = new RegisterUserRequest();
            registerRequest.setEmail("CaseSensitive@Example.COM");
            registerRequest.setPassword("SecurePass123");
            registerRequest.setFirstName("John");
            registerRequest.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))).andExpect(status().isCreated());

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("CaseSensitive@Example.COM");
            loginRequest.setPassword("SecurePass123");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        @DisplayName("Should return different tokens for multiple logins")
        void shouldReturnDifferentTokensForMultipleLogins() throws Exception {
            RegisterUserRequest registerRequest = new RegisterUserRequest();
            registerRequest.setEmail("multi.login@example.com");
            registerRequest.setPassword("SecurePass123");
            registerRequest.setFirstName("John");
            registerRequest.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))).andExpect(status().isCreated());

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("multi.login@example.com");
            loginRequest.setPassword("SecurePass123");

            String firstToken = mockMvc
                    .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty()).andReturn().getResponse()
                    .getContentAsString();

            Thread.sleep(1000);

            String secondToken = mockMvc
                    .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty()).andReturn().getResponse()
                    .getContentAsString();

            org.junit.jupiter.api.Assertions.assertNotEquals(firstToken, secondToken);
        }

        @Test
        @DisplayName("Should accept login with minimum valid password length")
        void shouldAcceptMinimumPasswordLength() throws Exception {
            RegisterUserRequest registerRequest = new RegisterUserRequest();
            registerRequest.setEmail("min.login@example.com");
            registerRequest.setPassword("Pass1234");
            registerRequest.setFirstName("John");
            registerRequest.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))).andExpect(status().isCreated());

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("min.login@example.com");
            loginRequest.setPassword("Pass1234");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        @DisplayName("Should accept login with maximum valid password length")
        void shouldAcceptMaximumPasswordLength() throws Exception {
            RegisterUserRequest registerRequest = new RegisterUserRequest();
            registerRequest.setEmail("max.login@example.com");
            registerRequest.setPassword("ThisPasswordIsExactly30Chars!");
            registerRequest.setFirstName("John");
            registerRequest.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))).andExpect(status().isCreated());

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("max.login@example.com");
            loginRequest.setPassword("ThisPasswordIsExactly30Chars!");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("Endpoint Security")
    class EndpointSecurity {

        @Test
        @DisplayName("Should allow unauthenticated access to register endpoint")
        void shouldAllowUnauthenticatedRegister() throws Exception {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("public.access@example.com");
            request.setPassword("SecurePass123");
            request.setFirstName("John");
            request.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should allow unauthenticated access to login endpoint")
        void shouldAllowUnauthenticatedLogin() throws Exception {
            RegisterUserRequest registerRequest = new RegisterUserRequest();
            registerRequest.setEmail("public.login@example.com");
            registerRequest.setPassword("SecurePass123");
            registerRequest.setFirstName("John");
            registerRequest.setLastName("Doe");

            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))).andExpect(status().isCreated());

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("public.login@example.com");
            loginRequest.setPassword("SecurePass123");

            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isOk());
        }
    }
}
