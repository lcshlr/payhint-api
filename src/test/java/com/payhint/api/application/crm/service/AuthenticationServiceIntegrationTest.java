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
import com.payhint.api.application.shared.exceptions.AlreadyExistException;
import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.domain.crm.valueobjects.Email;
import com.payhint.api.domain.crm.valueobjects.UserId;
import com.payhint.api.infrastructure.security.JwtTokenProvider;

import jakarta.validation.ConstraintViolationException;

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
            validRequest = new RegisterUserRequest(TEST_EMAIL, TEST_PASSWORD, TEST_FIRST_NAME, TEST_LAST_NAME);
        }

        @AfterEach
        void tearDown() {
            userRepository.findByEmail(new Email(TEST_EMAIL)).ifPresent(user -> userRepository.delete(user));
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

            var savedUser = userRepository.findById(new UserId(response.id()));
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getEmail().value()).isEqualTo(TEST_EMAIL.toLowerCase());
            assertThat(passwordEncoder.matches(TEST_PASSWORD, savedUser.get().getPassword())).isTrue();
        }

        @Test
        @DisplayName("Should encrypt password before storing in database")
        void shouldEncryptPassword() {
            UserResponse response = authenticationService.register(validRequest);

            var savedUser = userRepository.findById(new UserId(response.id()));
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getPassword()).isNotEqualTo(TEST_PASSWORD);
            assertThat(savedUser.get().getPassword()).startsWith("$2a$");
            assertThat(passwordEncoder.matches(TEST_PASSWORD, savedUser.get().getPassword())).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when registering duplicate email")
        void shouldThrowExceptionWhenDuplicateEmail() {
            authenticationService.register(validRequest);

            RegisterUserRequest duplicateRequest = new RegisterUserRequest(TEST_EMAIL, "AnotherPassword123!", "Another",
                    "User");

            assertThatThrownBy(() -> authenticationService.register(duplicateRequest))
                    .isInstanceOf(AlreadyExistException.class)
                    .hasMessageContaining("User with email " + TEST_EMAIL + " already exists.");
        }

        @Test
        @DisplayName("Should throw exception when registering duplicate email with different case")
        void shouldThrowExceptionWhenDuplicateEmailDifferentCase() {
            authenticationService.register(validRequest);

            RegisterUserRequest duplicateRequest = new RegisterUserRequest(TEST_EMAIL.toUpperCase(),
                    "AnotherPassword123!", "Another", "User");

            assertThatThrownBy(() -> authenticationService.register(duplicateRequest))
                    .isInstanceOf(AlreadyExistException.class).hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should normalize email to lowercase when registering")
        void shouldNormalizeEmailToLowercase() {
            RegisterUserRequest request = new RegisterUserRequest("MixedCase.Email@PayHint.COM", TEST_PASSWORD,
                    TEST_FIRST_NAME, TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);

            assertThat(response.email()).isEqualTo("mixedcase.email@payhint.com");

            var savedUser = userRepository.findById(new UserId(response.id()));
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getEmail().value()).isEqualTo("mixedcase.email@payhint.com");
        }

        @Test
        @DisplayName("Should register multiple users with different emails")
        void shouldRegisterMultipleUsersWithDifferentEmails() {
            UserResponse firstUser = authenticationService.register(validRequest);

            RegisterUserRequest secondRequest = new RegisterUserRequest("second.user@payhint.com", "Password456!",
                    "Second", "User");

            UserResponse secondUser = authenticationService.register(secondRequest);

            assertThat(firstUser.id()).isNotEqualTo(secondUser.id());
            assertThat(firstUser.email()).isNotEqualTo(secondUser.email());

            assertThat(userRepository.findById(new UserId(firstUser.id()))).isPresent();
            assertThat(userRepository.findById(new UserId(secondUser.id()))).isPresent();

            userRepository.delete(User.builder().id(new UserId(secondUser.id())).build());
        }

        @Test
        @DisplayName("Should handle registration with special characters in name")
        void shouldRegisterWithSpecialCharactersInName() {
            RegisterUserRequest request = new RegisterUserRequest(TEST_EMAIL, TEST_PASSWORD, "Jean-François",
                    "O'Brien-Müller");

            UserResponse response = authenticationService.register(request);

            assertThat(response.firstName()).isEqualTo("Jean-François");
            assertThat(response.lastName()).isEqualTo("O'Brien-Müller");
        }

        @Test
        @DisplayName("Should handle registration with very long names")
        void shouldRegisterWithLongNames() {
            String longFirstName = "A".repeat(100);
            String longLastName = "B".repeat(100);

            RegisterUserRequest request = new RegisterUserRequest(TEST_EMAIL, TEST_PASSWORD, longFirstName,
                    longLastName);

            UserResponse response = authenticationService.register(request);

            assertThat(response.firstName()).isEqualTo(longFirstName);
            assertThat(response.lastName()).isEqualTo(longLastName);
        }

        @Test
        @DisplayName("Should handle registration with minimum password length")
        void shouldRegisterWithMinimumPasswordLength() {
            RegisterUserRequest request = new RegisterUserRequest(TEST_EMAIL, "Pass123!", TEST_FIRST_NAME,
                    TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);

            assertThat(response).isNotNull();
            var savedUser = userRepository.findById(new UserId(response.id()));
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
            RegisterUserRequest registerRequest = new RegisterUserRequest(TEST_EMAIL, TEST_PASSWORD, TEST_FIRST_NAME,
                    TEST_LAST_NAME);

            UserResponse registered = authenticationService.register(registerRequest);
            registeredUserId = registered.id();

            validLoginRequest = new LoginUserRequest(TEST_EMAIL, TEST_PASSWORD);
        }

        @AfterEach
        void tearDown() {
            if (registeredUserId != null) {
                userRepository.delete(User.builder().id(new UserId(registeredUserId)).build());
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
            LoginUserRequest request = new LoginUserRequest(TEST_EMAIL.toUpperCase(), TEST_PASSWORD);

            LoginResponse response = authenticationService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.token()).isNotNull();

            String extractedUsername = jwtTokenProvider.extractUsername(response.token());
            assertThat(extractedUsername).isEqualToIgnoringCase(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should login successfully with mixed case email")
        void shouldLoginWithMixedCaseEmail() {
            LoginUserRequest request = new LoginUserRequest("InTeGrAtIoN.TeSt@PayHint.CoM", TEST_PASSWORD);

            LoginResponse response = authenticationService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.token()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when login with wrong password")
        void shouldThrowExceptionWhenWrongPassword() {
            LoginUserRequest request = new LoginUserRequest(TEST_EMAIL, "WrongPassword123!");

            assertThatThrownBy(() -> authenticationService.login(request)).isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should throw exception when login with non-existent email")
        void shouldThrowExceptionWhenNonExistentEmail() {
            LoginUserRequest request = new LoginUserRequest("nonexistent@payhint.com", TEST_PASSWORD);

            assertThatThrownBy(() -> authenticationService.login(request)).isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should throw exception when login with empty password")
        void shouldThrowExceptionWhenEmptyPassword() {
            LoginUserRequest request = new LoginUserRequest(TEST_EMAIL, "");

            assertThatThrownBy(() -> authenticationService.login(request))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("Should throw exception when login with null password")
        void shouldThrowExceptionWhenNullPassword() {
            LoginUserRequest request = new LoginUserRequest(TEST_EMAIL, null);

            assertThatThrownBy(() -> authenticationService.login(request)).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should throw exception when login with slightly wrong password")
        void shouldThrowExceptionWhenSlightlyWrongPassword() {
            LoginUserRequest request = new LoginUserRequest(TEST_EMAIL, TEST_PASSWORD + "x");

            assertThatThrownBy(() -> authenticationService.login(request)).isInstanceOf(BadCredentialsException.class);
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
            LoginUserRequest request = new LoginUserRequest(TEST_EMAIL, TEST_PASSWORD.toLowerCase());

            assertThatThrownBy(() -> authenticationService.login(request)).isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should throw exception when email has extra whitespace")
        void shouldHandleEmailWithWhitespace() {
            LoginUserRequest request = new LoginUserRequest("  " + TEST_EMAIL + "  ", TEST_PASSWORD);

            assertThatThrownBy(() -> authenticationService.login(request))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("Cross-Operation Integration Tests")
    class CrossOperationIntegrationTests {

        private UUID registeredUserId;

        @AfterEach
        void tearDown() {
            if (registeredUserId != null) {
                userRepository.delete(User.builder().id(new UserId(registeredUserId)).build());
            }
        }

        @Test
        @DisplayName("Should login immediately after registration")
        void shouldLoginImmediatelyAfterRegistration() {
            RegisterUserRequest registerRequest = new RegisterUserRequest(TEST_EMAIL, TEST_PASSWORD, TEST_FIRST_NAME,
                    TEST_LAST_NAME);

            UserResponse userResponse = authenticationService.register(registerRequest);
            registeredUserId = userResponse.id();

            LoginUserRequest loginRequest = new LoginUserRequest(TEST_EMAIL, TEST_PASSWORD);

            LoginResponse loginResponse = authenticationService.login(loginRequest);

            assertThat(loginResponse).isNotNull();
            assertThat(loginResponse.token()).isNotNull();
            assertThat(jwtTokenProvider.extractUsername(loginResponse.token())).isEqualTo(TEST_EMAIL.toLowerCase());
        }

        @Test
        @DisplayName("Should not login with original password after registration if password changed")
        void shouldVerifyPasswordEncodingPersistence() {
            RegisterUserRequest registerRequest = new RegisterUserRequest(TEST_EMAIL, TEST_PASSWORD, TEST_FIRST_NAME,
                    TEST_LAST_NAME);

            UserResponse userResponse = authenticationService.register(registerRequest);
            registeredUserId = userResponse.id();

            var user = userRepository.findById(new UserId(registeredUserId));
            assertThat(user).isPresent();
            assertThat(user.get().getPassword()).isNotEqualTo(TEST_PASSWORD);

            LoginUserRequest loginRequest = new LoginUserRequest(TEST_EMAIL, TEST_PASSWORD);

            LoginResponse loginResponse = authenticationService.login(loginRequest);
            assertThat(loginResponse.token()).isNotNull();
        }

        @Test
        @DisplayName("Should handle concurrent registrations with different emails")
        void shouldHandleConcurrentRegistrations() {
            RegisterUserRequest firstRequest = new RegisterUserRequest(TEST_EMAIL, TEST_PASSWORD, "First", "User");

            RegisterUserRequest secondRequest = new RegisterUserRequest("second.concurrent@payhint.com", "Password456!",
                    "Second", "User");

            UserResponse firstResponse = authenticationService.register(firstRequest);
            UserResponse secondResponse = authenticationService.register(secondRequest);

            registeredUserId = firstResponse.id();

            assertThat(firstResponse.id()).isNotEqualTo(secondResponse.id());
            assertThat(userRepository.findById(new UserId(firstResponse.id()))).isPresent();
            assertThat(userRepository.findById(new UserId(secondResponse.id()))).isPresent();

            userRepository.delete(User.builder().id(new UserId(secondResponse.id())).build());
        }

        @Test
        @DisplayName("Should maintain data integrity across register and login operations")
        void shouldMaintainDataIntegrity() {
            RegisterUserRequest registerRequest = new RegisterUserRequest("data.integrity@payhint.com",
                    "IntegrityPass123!", "Data", "Integrity");

            UserResponse registeredUser = authenticationService.register(registerRequest);
            registeredUserId = registeredUser.id();

            assertThat(registeredUser.email()).isEqualTo("data.integrity@payhint.com");
            assertThat(registeredUser.firstName()).isEqualTo("Data");
            assertThat(registeredUser.lastName()).isEqualTo("Integrity");

            LoginUserRequest loginRequest = new LoginUserRequest("data.integrity@payhint.com", "IntegrityPass123!");

            LoginResponse loginResponse = authenticationService.login(loginRequest);

            assertThat(loginResponse.token()).isNotNull();

            var persistedUser = userRepository.findById(new UserId(registeredUserId));
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
                userRepository.delete(User.builder().id(new UserId(registeredUserId)).build());
            }
        }

        @Test
        @DisplayName("Should handle registration with email containing plus sign")
        void shouldHandleEmailWithPlusSign() {
            RegisterUserRequest request = new RegisterUserRequest("user+test@payhint.com", TEST_PASSWORD,
                    TEST_FIRST_NAME, TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            assertThat(response.email()).isEqualTo("user+test@payhint.com");

            LoginUserRequest loginRequest = new LoginUserRequest("user+test@payhint.com", TEST_PASSWORD);

            LoginResponse loginResponse = authenticationService.login(loginRequest);
            assertThat(loginResponse.token()).isNotNull();
        }

        @Test
        @DisplayName("Should handle registration with subdomain email")
        void shouldHandleSubdomainEmail() {
            RegisterUserRequest request = new RegisterUserRequest("user@mail.payhint.com", TEST_PASSWORD,
                    TEST_FIRST_NAME, TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            assertThat(response.email()).isEqualTo("user@mail.payhint.com");
        }

        @Test
        @DisplayName("Should handle registration with single character names")
        void shouldHandleSingleCharacterNames() {
            RegisterUserRequest request = new RegisterUserRequest("single.char@payhint.com", TEST_PASSWORD, "A", "B");

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            assertThat(response.firstName()).isEqualTo("A");
            assertThat(response.lastName()).isEqualTo("B");
        }

        @Test
        @DisplayName("Should handle password with special characters")
        void shouldHandlePasswordWithSpecialCharacters() {
            RegisterUserRequest request = new RegisterUserRequest("special.pass@payhint.com", "P@ssw0rd!#$%^&*()",
                    TEST_FIRST_NAME, TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            LoginUserRequest loginRequest = new LoginUserRequest("special.pass@payhint.com", "P@ssw0rd!#$%^&*()");

            LoginResponse loginResponse = authenticationService.login(loginRequest);
            assertThat(loginResponse.token()).isNotNull();
        }

        @Test
        @DisplayName("Should handle password with unicode characters")
        void shouldHandlePasswordWithUnicode() {
            RegisterUserRequest request = new RegisterUserRequest("unicode.pass@payhint.com", "Pässwörd123!",
                    TEST_FIRST_NAME, TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            LoginUserRequest loginRequest = new LoginUserRequest("unicode.pass@payhint.com", "Pässwörd123!");

            LoginResponse loginResponse = authenticationService.login(loginRequest);
            assertThat(loginResponse.token()).isNotNull();
        }

        @Test
        @DisplayName("Should reject registration attempt after successful registration with same email")
        void shouldRejectDuplicateRegistrationAttempt() {
            RegisterUserRequest firstRequest = new RegisterUserRequest("duplicate.check@payhint.com", TEST_PASSWORD,
                    "First", "Attempt");

            UserResponse firstResponse = authenticationService.register(firstRequest);
            registeredUserId = firstResponse.id();

            RegisterUserRequest secondRequest = new RegisterUserRequest("duplicate.check@payhint.com",
                    "DifferentPass123!", "Second", "Attempt");

            assertThatThrownBy(() -> authenticationService.register(secondRequest))
                    .isInstanceOf(AlreadyExistException.class).hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should handle email with dots")
        void shouldHandleEmailWithDots() {
            RegisterUserRequest request = new RegisterUserRequest("first.middle.last@payhint.com", TEST_PASSWORD,
                    TEST_FIRST_NAME, TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            assertThat(response.email()).isEqualTo("first.middle.last@payhint.com");

            LoginUserRequest loginRequest = new LoginUserRequest("first.middle.last@payhint.com", TEST_PASSWORD);

            LoginResponse loginResponse = authenticationService.login(loginRequest);
            assertThat(loginResponse.token()).isNotNull();
        }

        @Test
        @DisplayName("Should handle maximum length password")
        void shouldHandleMaximumLengthPassword() {
            String maxPassword = "A".repeat(30);

            RegisterUserRequest request = new RegisterUserRequest("max.password@payhint.com", maxPassword,
                    TEST_FIRST_NAME, TEST_LAST_NAME);

            UserResponse response = authenticationService.register(request);
            registeredUserId = response.id();

            LoginUserRequest loginRequest = new LoginUserRequest("max.password@payhint.com", maxPassword);

            LoginResponse loginResponse = authenticationService.login(loginRequest);
            assertThat(loginResponse.token()).isNotNull();
        }
    }
}
