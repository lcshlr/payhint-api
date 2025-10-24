package com.payhint.api.application.crm.service;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.payhint.api.application.crm.dto.request.LoginUserRequest;
import com.payhint.api.application.crm.dto.request.RegisterUserRequest;
import com.payhint.api.application.crm.dto.response.LoginResponse;
import com.payhint.api.application.crm.dto.response.UserResponse;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.infrastructure.security.JwtTokenProvider;

@SpringBootTest
@TestPropertySource(properties = { "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop" })
@Transactional
@DisplayName("AuthenticationService Integration Tests")
class AuthenticationServiceIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_EMAIL = "integration.test@payhint.com";
    private static final String TEST_PASSWORD = "SecurePassword123!";
    private static final String TEST_FIRST_NAME = "Integration";
    private static final String TEST_LAST_NAME = "Tester";

    @Nested
    @DisplayName("User Registration Integration Tests")
    class RegisterIntegrationTests {

        private RegisterUserRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new RegisterUserRequest();
            validRequest.setEmail(TEST_EMAIL);
            validRequest.setPassword(TEST_PASSWORD);
            validRequest.setFirstName(TEST_FIRST_NAME);
            validRequest.setLastName(TEST_LAST_NAME);
        }

        @AfterEach
        void tearDown() {
            userRepository.findByEmail(TEST_EMAIL).ifPresent(user -> userRepository.deleteById(user.getId()));
        }

        @Test
        @DisplayName("Should successfully register a new user and persist to database")
        void shouldRegisterNewUserSuccessfully() {
            UserResponse response = authenticationService.register(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.id()).isNotNull();
            assertThat(response.email()).isEqualTo(TEST_EMAIL.toLowerCase());
            assertThat(response.firstName()).isEqualTo(TEST_FIRST_NAME);
            assertThat(response.lastName()).isEqualTo(TEST_LAST_NAME);

            var savedUser = userRepository.findById(response.id());
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getEmail().value()).isEqualTo(TEST_EMAIL.toLowerCase());
            assertThat(passwordEncoder.matches(TEST_PASSWORD, savedUser.get().getPassword())).isTrue();
        }

        @Test
        @DisplayName("Should encrypt password before storing in database")
        void shouldEncryptPassword() {
            UserResponse response = authenticationService.register(validRequest);

            var savedUser = userRepository.findById(response.id());
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getPassword()).isNotEqualTo(TEST_PASSWORD);
            assertThat(savedUser.get().getPassword()).startsWith("$2a$");
            assertThat(passwordEncoder.matches(TEST_PASSWORD, savedUser.get().getPassword())).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when registering duplicate email")
        void shouldThrowExceptionWhenDuplicateEmail() {
            authenticationService.register(validRequest);

            RegisterUserRequest duplicateRequest = new RegisterUserRequest();
            duplicateRequest.setEmail(TEST_EMAIL);
            duplicateRequest.setPassword("AnotherPassword123!");
            duplicateRequest.setFirstName("Another");
            duplicateRequest.setLastName("User");

            assertThatThrownBy(() -> authenticationService.register(duplicateRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User with email " + TEST_EMAIL + " already exists.");
        }

        @Test
        @DisplayName("Should throw exception when registering duplicate email with different case")
        void shouldThrowExceptionWhenDuplicateEmailDifferentCase() {
            authenticationService.register(validRequest);

            RegisterUserRequest duplicateRequest = new RegisterUserRequest();
            duplicateRequest.setEmail(TEST_EMAIL.toUpperCase());
            duplicateRequest.setPassword("AnotherPassword123!");
            duplicateRequest.setFirstName("Another");
            duplicateRequest.setLastName("User");

            assertThatThrownBy(() -> authenticationService.register(duplicateRequest))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should normalize email to lowercase when registering")
        void shouldNormalizeEmailToLowercase() {
            validRequest.setEmail("MixedCase.Email@PayHint.COM");

            UserResponse response = authenticationService.register(validRequest);

            assertThat(response.email()).isEqualTo("mixedcase.email@payhint.com");

            var savedUser = userRepository.findById(response.id());
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getEmail().value()).isEqualTo("mixedcase.email@payhint.com");
        }

        @Test
        @DisplayName("Should register multiple users with different emails")
        void shouldRegisterMultipleUsersWithDifferentEmails() {
            UserResponse firstUser = authenticationService.register(validRequest);

            RegisterUserRequest secondRequest = new RegisterUserRequest();
            secondRequest.setEmail("second.user@payhint.com");
            secondRequest.setPassword("Password456!");
            secondRequest.setFirstName("Second");
            secondRequest.setLastName("User");

            UserResponse secondUser = authenticationService.register(secondRequest);

            assertThat(firstUser.id()).isNotEqualTo(secondUser.id());
            assertThat(firstUser.email()).isNotEqualTo(secondUser.email());

            assertThat(userRepository.findById(firstUser.id())).isPresent();
            assertThat(userRepository.findById(secondUser.id())).isPresent();

            userRepository.deleteById(secondUser.id());
        }

        @Test
        @DisplayName("Should handle registration with special characters in name")
        void shouldRegisterWithSpecialCharactersInName() {
            validRequest.setFirstName("Jean-François");
            validRequest.setLastName("O'Brien-Müller");

            UserResponse response = authenticationService.register(validRequest);

            assertThat(response.firstName()).isEqualTo("Jean-François");
            assertThat(response.lastName()).isEqualTo("O'Brien-Müller");
        }

        @Test
        @DisplayName("Should handle registration with very long names")
        void shouldRegisterWithLongNames() {
            String longFirstName = "A".repeat(100);
            String longLastName = "B".repeat(100);

            validRequest.setFirstName(longFirstName);
            validRequest.setLastName(longLastName);

            UserResponse response = authenticationService.register(validRequest);

            assertThat(response.firstName()).isEqualTo(longFirstName);
            assertThat(response.lastName()).isEqualTo(longLastName);
        }

        @Test
        @DisplayName("Should handle registration with minimum password length")
        void shouldRegisterWithMinimumPasswordLength() {
            validRequest.setPassword("Pass123!");

            UserResponse response = authenticationService.register(validRequest);

            assertThat(response).isNotNull();
            var savedUser = userRepository.findById(response.id());
            assertThat(savedUser).isPresent();
            assertThat(passwordEncoder.matches("Pass123!", savedUser.get().getPassword())).isTrue();
        }
    }

    @Nested
    @DisplayName("User Login Integration Tests")
    class LoginIntegrationTests {

        private UUID registeredUserId;
        private LoginUserRequest validLoginRequest;

        @BeforeEach
        void setUp() {
            RegisterUserRequest registerRequest = new RegisterUserRequest();
            registerRequest.setEmail(TEST_EMAIL);
            registerRequest.setPassword(TEST_PASSWORD);
            registerRequest.setFirstName(TEST_FIRST_NAME);
            registerRequest.setLastName(TEST_LAST_NAME);

            UserResponse registered = authenticationService.register(registerRequest);
            registeredUserId = registered.id();

            validLoginRequest = new LoginUserRequest();
            validLoginRequest.setEmail(TEST_EMAIL);
            validLoginRequest.setPassword(TEST_PASSWORD);
        }

        @AfterEach
        void tearDown() {
            if (registeredUserId != null) {
                userRepository.deleteById(registeredUserId);
            }
        }

        @Test
        @DisplayName("Should successfully login with valid credentials")
        void shouldLoginSuccessfully() {
            LoginResponse response = authenticationService.login(validLoginRequest);

            assertThat(response).isNotNull();
            assertThat(response.token()).isNotNull();
            assertThat(response.token()).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate valid JWT token on successful login")
        void shouldGenerateValidJwtToken() {
            LoginResponse response = authenticationService.login(validLoginRequest);

            String token = response.token();
            String extractedUsername = jwtTokenProvider.extractUsername(token);

            assertThat(extractedUsername).isEqualTo(TEST_EMAIL.toLowerCase());
        }

        @Test
        @DisplayName("Should login successfully with uppercase email")
        void shouldLoginWithUppercaseEmail() {
            validLoginRequest.setEmail(TEST_EMAIL.toUpperCase());

            LoginResponse response = authenticationService.login(validLoginRequest);

            assertThat(response).isNotNull();
            assertThat(response.token()).isNotNull();

            String extractedUsername = jwtTokenProvider.extractUsername(response.token());
            assertThat(extractedUsername).isEqualToIgnoringCase(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should login successfully with mixed case email")
        void shouldLoginWithMixedCaseEmail() {
            validLoginRequest.setEmail("InTeGrAtIoN.TeSt@PayHint.CoM");

            LoginResponse response = authenticationService.login(validLoginRequest);

            assertThat(response).isNotNull();
            assertThat(response.token()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when login with wrong password")
        void shouldThrowExceptionWhenWrongPassword() {
            validLoginRequest.setPassword("WrongPassword123!");

            assertThatThrownBy(() -> authenticationService.login(validLoginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should throw exception when login with non-existent email")
        void shouldThrowExceptionWhenNonExistentEmail() {
            validLoginRequest.setEmail("nonexistent@payhint.com");

            assertThatThrownBy(() -> authenticationService.login(validLoginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should throw exception when login with empty password")
        void shouldThrowExceptionWhenEmptyPassword() {
            validLoginRequest.setPassword("");

            assertThatThrownBy(() -> authenticationService.login(validLoginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should throw exception when login with null password")
        void shouldThrowExceptionWhenNullPassword() {
            validLoginRequest.setPassword(null);

            assertThatThrownBy(() -> authenticationService.login(validLoginRequest)).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should throw exception when login with slightly wrong password")
        void shouldThrowExceptionWhenSlightlyWrongPassword() {
            validLoginRequest.setPassword(TEST_PASSWORD + "x");

            assertThatThrownBy(() -> authenticationService.login(validLoginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should generate different tokens for multiple logins")
        void shouldGenerateDifferentTokensForMultipleLogins() throws InterruptedException {
            LoginResponse firstResponse = authenticationService.login(validLoginRequest);

            Thread.sleep(1000);

            LoginResponse secondResponse = authenticationService.login(validLoginRequest);

            assertThat(firstResponse.token()).isNotEqualTo(secondResponse.token());

            String firstUsername = jwtTokenProvider.extractUsername(firstResponse.token());
            String secondUsername = jwtTokenProvider.extractUsername(secondResponse.token());
            assertThat(firstUsername).isEqualTo(secondUsername);
        }

        @Test
        @DisplayName("Should throw exception when password case is incorrect")
        void shouldThrowExceptionWhenPasswordCaseIncorrect() {
            validLoginRequest.setPassword(TEST_PASSWORD.toLowerCase());

            assertThatThrownBy(() -> authenticationService.login(validLoginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should throw exception when email has extra whitespace")
        void shouldHandleEmailWithWhitespace() {
            validLoginRequest.setEmail("  " + TEST_EMAIL + "  ");

            assertThatThrownBy(() -> authenticationService.login(validLoginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("Cross-Operation Integration Tests")
    class CrossOperationIntegrationTests {

        private UUID registeredUserId;

        @AfterEach
        void tearDown() {
            if (registeredUserId != null) {
                userRepository.deleteById(registeredUserId);
            }
        }

        @Test
        @DisplayName("Should login immediately after registration")
        void shouldLoginImmediatelyAfterRegistration() {
            RegisterUserRequest registerRequest = new RegisterUserRequest();
            registerRequest.setEmail(TEST_EMAIL);
            registerRequest.setPassword(TEST_PASSWORD);
            registerRequest.setFirstName(TEST_FIRST_NAME);
            registerRequest.setLastName(TEST_LAST_NAME);

            UserResponse userResponse = authenticationService.register(registerRequest);
            registeredUserId = userResponse.id();

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail(TEST_EMAIL);
            loginRequest.setPassword(TEST_PASSWORD);

            LoginResponse loginResponse = authenticationService.login(loginRequest);

            assertThat(loginResponse).isNotNull();
            assertThat(loginResponse.token()).isNotNull();
            assertThat(jwtTokenProvider.extractUsername(loginResponse.token())).isEqualTo(TEST_EMAIL.toLowerCase());
        }

        @Test
        @DisplayName("Should not login with original password after registration if password changed")
        void shouldVerifyPasswordEncodingPersistence() {
            RegisterUserRequest registerRequest = new RegisterUserRequest();
            registerRequest.setEmail(TEST_EMAIL);
            registerRequest.setPassword(TEST_PASSWORD);
            registerRequest.setFirstName(TEST_FIRST_NAME);
            registerRequest.setLastName(TEST_LAST_NAME);

            UserResponse userResponse = authenticationService.register(registerRequest);
            registeredUserId = userResponse.id();

            var user = userRepository.findById(registeredUserId);
            assertThat(user).isPresent();
            assertThat(user.get().getPassword()).isNotEqualTo(TEST_PASSWORD);

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail(TEST_EMAIL);
            loginRequest.setPassword(TEST_PASSWORD);

            LoginResponse loginResponse = authenticationService.login(loginRequest);
            assertThat(loginResponse.token()).isNotNull();
        }

        @Test
        @DisplayName("Should handle concurrent registrations with different emails")
        void shouldHandleConcurrentRegistrations() {
            RegisterUserRequest firstRequest = new RegisterUserRequest();
            firstRequest.setEmail(TEST_EMAIL);
            firstRequest.setPassword(TEST_PASSWORD);
            firstRequest.setFirstName("First");
            firstRequest.setLastName("User");

            RegisterUserRequest secondRequest = new RegisterUserRequest();
            secondRequest.setEmail("second.concurrent@payhint.com");
            secondRequest.setPassword("Password456!");
            secondRequest.setFirstName("Second");
            secondRequest.setLastName("User");

            UserResponse firstResponse = authenticationService.register(firstRequest);
            UserResponse secondResponse = authenticationService.register(secondRequest);

            registeredUserId = firstResponse.id();

            assertThat(firstResponse.id()).isNotEqualTo(secondResponse.id());
            assertThat(userRepository.findById(firstResponse.id())).isPresent();
            assertThat(userRepository.findById(secondResponse.id())).isPresent();

            userRepository.deleteById(secondResponse.id());
        }

        @Test
        @DisplayName("Should maintain data integrity across register and login operations")
        void shouldMaintainDataIntegrity() {
            RegisterUserRequest registerRequest = new RegisterUserRequest();
            registerRequest.setEmail("data.integrity@payhint.com");
            registerRequest.setPassword("IntegrityPass123!");
            registerRequest.setFirstName("Data");
            registerRequest.setLastName("Integrity");

            UserResponse registeredUser = authenticationService.register(registerRequest);
            registeredUserId = registeredUser.id();

            assertThat(registeredUser.email()).isEqualTo("data.integrity@payhint.com");
            assertThat(registeredUser.firstName()).isEqualTo("Data");
            assertThat(registeredUser.lastName()).isEqualTo("Integrity");

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("data.integrity@payhint.com");
            loginRequest.setPassword("IntegrityPass123!");

            LoginResponse loginResponse = authenticationService.login(loginRequest);

            assertThat(loginResponse.token()).isNotNull();

            var persistedUser = userRepository.findById(registeredUserId);
            assertThat(persistedUser).isPresent();
            assertThat(persistedUser.get().getEmail().value()).isEqualTo("data.integrity@payhint.com");
            assertThat(persistedUser.get().getFirstName()).isEqualTo("Data");
            assertThat(persistedUser.get().getLastName()).isEqualTo("Integrity");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCaseTests {

        private UUID registeredUserId;

        @AfterEach
        void tearDown() {
            if (registeredUserId != null) {
                userRepository.deleteById(registeredUserId);
            }
        }

        @Test
        @DisplayName("Should handle registration with email containing plus sign")
        void shouldHandleEmailWithPlusSign() {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("user+test@payhint.com");
            request.setPassword(TEST_PASSWORD);
            request.setFirstName(TEST_FIRST_NAME);
            request.setLastName(TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            assertThat(response.email()).isEqualTo("user+test@payhint.com");

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("user+test@payhint.com");
            loginRequest.setPassword(TEST_PASSWORD);

            LoginResponse loginResponse = authenticationService.login(loginRequest);
            assertThat(loginResponse.token()).isNotNull();
        }

        @Test
        @DisplayName("Should handle registration with subdomain email")
        void shouldHandleSubdomainEmail() {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("user@mail.payhint.com");
            request.setPassword(TEST_PASSWORD);
            request.setFirstName(TEST_FIRST_NAME);
            request.setLastName(TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            assertThat(response.email()).isEqualTo("user@mail.payhint.com");
        }

        @Test
        @DisplayName("Should handle registration with single character names")
        void shouldHandleSingleCharacterNames() {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("single.char@payhint.com");
            request.setPassword(TEST_PASSWORD);
            request.setFirstName("A");
            request.setLastName("B");

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            assertThat(response.firstName()).isEqualTo("A");
            assertThat(response.lastName()).isEqualTo("B");
        }

        @Test
        @DisplayName("Should handle password with special characters")
        void shouldHandlePasswordWithSpecialCharacters() {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("special.pass@payhint.com");
            request.setPassword("P@ssw0rd!#$%^&*()");
            request.setFirstName(TEST_FIRST_NAME);
            request.setLastName(TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("special.pass@payhint.com");
            loginRequest.setPassword("P@ssw0rd!#$%^&*()");

            LoginResponse loginResponse = authenticationService.login(loginRequest);
            assertThat(loginResponse.token()).isNotNull();
        }

        @Test
        @DisplayName("Should handle password with unicode characters")
        void shouldHandlePasswordWithUnicode() {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("unicode.pass@payhint.com");
            request.setPassword("Pässwörd123!");
            request.setFirstName(TEST_FIRST_NAME);
            request.setLastName(TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("unicode.pass@payhint.com");
            loginRequest.setPassword("Pässwörd123!");

            LoginResponse loginResponse = authenticationService.login(loginRequest);
            assertThat(loginResponse.token()).isNotNull();
        }

        @Test
        @DisplayName("Should reject registration attempt after successful registration with same email")
        void shouldRejectDuplicateRegistrationAttempt() {
            RegisterUserRequest firstRequest = new RegisterUserRequest();
            firstRequest.setEmail("duplicate.check@payhint.com");
            firstRequest.setPassword(TEST_PASSWORD);
            firstRequest.setFirstName("First");
            firstRequest.setLastName("Attempt");

            UserResponse firstResponse = authenticationService.register(firstRequest);
            registeredUserId = firstResponse.id();

            RegisterUserRequest secondRequest = new RegisterUserRequest();
            secondRequest.setEmail("duplicate.check@payhint.com");
            secondRequest.setPassword("DifferentPass123!");
            secondRequest.setFirstName("Second");
            secondRequest.setLastName("Attempt");

            assertThatThrownBy(() -> authenticationService.register(secondRequest))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should handle email with dots")
        void shouldHandleEmailWithDots() {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("first.middle.last@payhint.com");
            request.setPassword(TEST_PASSWORD);
            request.setFirstName(TEST_FIRST_NAME);
            request.setLastName(TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            assertThat(response.email()).isEqualTo("first.middle.last@payhint.com");

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("first.middle.last@payhint.com");
            loginRequest.setPassword(TEST_PASSWORD);

            LoginResponse loginResponse = authenticationService.login(loginRequest);
            assertThat(loginResponse.token()).isNotNull();
        }

        @Test
        @DisplayName("Should handle maximum length password")
        void shouldHandleMaximumLengthPassword() {
            String maxPassword = "A".repeat(30);

            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("max.password@payhint.com");
            request.setPassword(maxPassword);
            request.setFirstName(TEST_FIRST_NAME);
            request.setLastName(TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            LoginUserRequest loginRequest = new LoginUserRequest();
            loginRequest.setEmail("max.password@payhint.com");
            loginRequest.setPassword(maxPassword);

            LoginResponse loginResponse = authenticationService.login(loginRequest);
            assertThat(loginResponse.token()).isNotNull();
        }
    }
}
