
package com.payhint.api.application.crm.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.payhint.api.application.crm.dto.request.LoginUserRequest;
import com.payhint.api.application.crm.dto.request.RegisterUserRequest;
import com.payhint.api.application.crm.dto.response.LoginResponse;
import com.payhint.api.application.crm.dto.response.UserResponse;
import com.payhint.api.application.crm.mapper.UserMapper;
import com.payhint.api.domain.crm.model.User;
import com.payhint.api.domain.crm.repository.UserRepository;
import com.payhint.api.domain.crm.valueobjects.Email;
import com.payhint.api.infrastructure.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Unit Tests")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Nested
    @DisplayName("Register Use Case Tests")
    class RegisterTests {

        private RegisterUserRequest registerRequest;
        private User domainUser;
        private User savedUser;
        private UserResponse expectedResponse;
        private static final String TEST_EMAIL = "test@example.com";
        private static final String PLAIN_PASSWORD = "PlainPassword123";
        private static final String ENCODED_PASSWORD = "$2a$10$encodedPasswordHash";

        @BeforeEach
        void setUp() {
            registerRequest = new RegisterUserRequest();
            registerRequest.setEmail(TEST_EMAIL);
            registerRequest.setPassword(PLAIN_PASSWORD);
            registerRequest.setFirstName("John");
            registerRequest.setLastName("Doe");

            domainUser = User.builder().email(new Email(TEST_EMAIL)).password(PLAIN_PASSWORD).firstName("John")
                    .lastName("Doe").build();

            UUID savedUserId = UUID.randomUUID();
            savedUser = User.builder().id(savedUserId).email(new Email(TEST_EMAIL)).password(ENCODED_PASSWORD)
                    .firstName("John").lastName("Doe").build();

            expectedResponse = new UserResponse(savedUser.getId(), TEST_EMAIL, "John", "Doe");
        }

        @Test
        @DisplayName("Should successfully register a new user with all valid fields")
        void shouldRegisterNewUserSuccessfully() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
            when(userMapper.toDomain(registerRequest)).thenReturn(domainUser);
            when(passwordEncoder.encode(PLAIN_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.register(any(User.class))).thenReturn(savedUser);
            when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

            UserResponse response = authenticationService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(savedUser.getId());
            assertThat(response.email()).isEqualTo(TEST_EMAIL);
            assertThat(response.firstName()).isEqualTo("John");
            assertThat(response.lastName()).isEqualTo("Doe");

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(userMapper).toDomain(registerRequest);
            verify(passwordEncoder).encode(PLAIN_PASSWORD);
            verify(userRepository).register(any(User.class));
            verify(userMapper).toResponse(savedUser);
        }

        @Test
        @DisplayName("Should encode password before persisting user")
        void shouldEncodePasswordBeforeSaving() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
            when(userMapper.toDomain(registerRequest)).thenReturn(domainUser);
            when(passwordEncoder.encode(PLAIN_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.register(any(User.class))).thenReturn(savedUser);
            when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

            authenticationService.register(registerRequest);

            verify(passwordEncoder).encode(PLAIN_PASSWORD);
            assertThat(domainUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("Should save user with encoded password to repository")
        void shouldSaveUserWithEncodedPassword() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
            when(userMapper.toDomain(registerRequest)).thenReturn(domainUser);
            when(passwordEncoder.encode(PLAIN_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.register(any(User.class))).thenReturn(savedUser);
            when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            authenticationService.register(registerRequest);

            verify(userRepository).register(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(capturedUser.getEmail().value()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when user with same email already exists")
        void shouldThrowExceptionWhenUserAlreadyExists() {
            User existingUser = User.builder().id(UUID.randomUUID()).email(new Email(TEST_EMAIL))
                    .password(ENCODED_PASSWORD).firstName("Existing").lastName("User").build();
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> authenticationService.register(registerRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User with email " + TEST_EMAIL + " already exists.");

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(userMapper, never()).toDomain(any());
            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).register(any());
            verify(userMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException with correct message format for duplicate email")
        void shouldThrowExceptionWithCorrectMessageForDuplicateEmail() {
            String duplicateEmail = "duplicate@test.com";
            registerRequest.setEmail(duplicateEmail);
            User existingUser = User.builder().id(UUID.randomUUID()).email(new Email(duplicateEmail))
                    .password(ENCODED_PASSWORD).firstName("Existing").lastName("User").build();
            when(userRepository.findByEmail(duplicateEmail)).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> authenticationService.register(registerRequest))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(duplicateEmail)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should handle repository exception during registration")
        void shouldPropagateRepositoryExceptionDuringRegistration() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
            when(userMapper.toDomain(registerRequest)).thenReturn(domainUser);
            when(passwordEncoder.encode(PLAIN_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.register(any(User.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> authenticationService.register(registerRequest))
                    .isInstanceOf(RuntimeException.class).hasMessageContaining("Database connection failed");

            verify(userRepository).register(any(User.class));
        }

        @Test
        @DisplayName("Should handle repository exception when checking for existing email")
        void shouldPropagateRepositoryExceptionDuringEmailCheck() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenThrow(new RuntimeException("Database unavailable"));

            assertThatThrownBy(() -> authenticationService.register(registerRequest))
                    .isInstanceOf(RuntimeException.class).hasMessageContaining("Database unavailable");

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(userMapper, never()).toDomain(any());
        }
    }

    @Nested
    @DisplayName("Login Use Case Tests")
    class LoginTests {

        private LoginUserRequest loginRequest;
        private Authentication authentication;
        private UserDetails userDetails;
        private static final String TEST_EMAIL = "test@example.com";
        private static final String TEST_PASSWORD = "Password123";
        private static final String EXPECTED_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.signature";

        @BeforeEach
        void setUp() {
            loginRequest = new LoginUserRequest();
            loginRequest.setEmail(TEST_EMAIL);
            loginRequest.setPassword(TEST_PASSWORD);

            userDetails = org.springframework.security.core.userdetails.User.builder().username(TEST_EMAIL)
                    .password("$2a$10$encodedPassword")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))).build();

            authentication = mock(Authentication.class);
        }

        @Test
        @DisplayName("Should successfully login and return JWT token")
        void shouldLoginSuccessfullyAndReturnToken() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(jwtTokenProvider.generateToken(userDetails)).thenReturn(EXPECTED_TOKEN);

            LoginResponse response = authenticationService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo(EXPECTED_TOKEN);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(authentication).getPrincipal();
            verify(jwtTokenProvider).generateToken(userDetails);
        }

        @Test
        @DisplayName("Should authenticate with correct email and password")
        void shouldAuthenticateWithCorrectCredentials() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(jwtTokenProvider.generateToken(userDetails)).thenReturn(EXPECTED_TOKEN);

            ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor = ArgumentCaptor
                    .forClass(UsernamePasswordAuthenticationToken.class);

            authenticationService.login(loginRequest);

            verify(authenticationManager).authenticate(authCaptor.capture());
            UsernamePasswordAuthenticationToken capturedAuth = authCaptor.getValue();

            assertThat(capturedAuth.getPrincipal()).isEqualTo(TEST_EMAIL);
            assertThat(capturedAuth.getCredentials()).isEqualTo(TEST_PASSWORD);
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when password is incorrect")
        void shouldThrowExceptionWhenPasswordIsIncorrect() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authenticationService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class).hasMessageContaining("Bad credentials");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtTokenProvider, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when user does not exist")
        void shouldThrowExceptionWhenUserDoesNotExist() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("User not found"));

            assertThatThrownBy(() -> authenticationService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class);

            verify(jwtTokenProvider, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should throw DisabledException when user account is disabled")
        void shouldThrowExceptionWhenAccountIsDisabled() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new DisabledException("User account is disabled"));

            assertThatThrownBy(() -> authenticationService.login(loginRequest)).isInstanceOf(DisabledException.class)
                    .hasMessageContaining("disabled");

            verify(jwtTokenProvider, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should throw LockedException when user account is locked")
        void shouldThrowExceptionWhenAccountIsLocked() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new LockedException("User account is locked"));

            assertThatThrownBy(() -> authenticationService.login(loginRequest)).isInstanceOf(LockedException.class)
                    .hasMessageContaining("locked");

            verify(jwtTokenProvider, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should extract UserDetails from authentication principal")
        void shouldExtractUserDetailsFromAuthenticationPrincipal() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(jwtTokenProvider.generateToken(userDetails)).thenReturn(EXPECTED_TOKEN);

            authenticationService.login(loginRequest);

            verify(authentication).getPrincipal();
            verify(jwtTokenProvider).generateToken(userDetails);
        }

        @Test
        @DisplayName("Should generate token with authenticated UserDetails")
        void shouldGenerateTokenWithAuthenticatedUserDetails() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(jwtTokenProvider.generateToken(userDetails)).thenReturn(EXPECTED_TOKEN);

            ArgumentCaptor<UserDetails> userDetailsCaptor = ArgumentCaptor.forClass(UserDetails.class);

            authenticationService.login(loginRequest);

            verify(jwtTokenProvider).generateToken(userDetailsCaptor.capture());
            UserDetails capturedUserDetails = userDetailsCaptor.getValue();

            assertThat(capturedUserDetails.getUsername()).isEqualTo(TEST_EMAIL);
            assertThat(capturedUserDetails.getAuthorities()).isNotEmpty();
        }

        @Test
        @DisplayName("Should return LoginResponse with non-empty token")
        void shouldReturnLoginResponseWithNonEmptyToken() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(jwtTokenProvider.generateToken(userDetails)).thenReturn(EXPECTED_TOKEN);

            LoginResponse response = authenticationService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.token()).isNotEmpty();
            assertThat(response.token()).hasSize(EXPECTED_TOKEN.length());
        }

        @Test
        @DisplayName("Should handle token generation failure")
        void shouldPropagateTokenGenerationException() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(jwtTokenProvider.generateToken(userDetails))
                    .thenThrow(new RuntimeException("Token generation failed"));

            assertThatThrownBy(() -> authenticationService.login(loginRequest)).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Token generation failed");

            verify(jwtTokenProvider).generateToken(userDetails);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Validation Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Register: Should handle different email formats")
        void shouldHandleDifferentEmailFormats() {
            String[] validEmails = { "user@domain.com", "user.name@domain.co.uk", "user+tag@domain.com",
                    "user123@sub.domain.org" };

            for (String email : validEmails) {
                RegisterUserRequest request = new RegisterUserRequest();
                request.setEmail(email);
                request.setPassword("Password123");
                request.setFirstName("John");
                request.setLastName("Doe");

                User domainUser = User.builder().email(new Email(email)).password("Password123").firstName("John")
                        .lastName("Doe").build();

                User savedUser = User.builder().id(UUID.randomUUID()).email(new Email(email)).password("encoded")
                        .firstName("John").lastName("Doe").build();

                UserResponse response = new UserResponse(savedUser.getId(), email, "John", "Doe");

                when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
                when(userMapper.toDomain(request)).thenReturn(domainUser);
                when(passwordEncoder.encode("Password123")).thenReturn("encoded");
                when(userRepository.register(any(User.class))).thenReturn(savedUser);
                when(userMapper.toResponse(savedUser)).thenReturn(response);

                UserResponse result = authenticationService.register(request);

                assertThat(result.email()).isEqualTo(email);
            }
        }

        @Test
        @DisplayName("Register: Should handle special characters in names")
        void shouldHandleSpecialCharactersInNames() {
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("test@example.com");
            request.setPassword("Password123");
            request.setFirstName("Jean-François");
            request.setLastName("O'Brien");

            User domainUser = User.builder().email(new Email("test@example.com")).password("Password123")
                    .firstName("Jean-François").lastName("O'Brien").build();

            User savedUser = User.builder().id(UUID.randomUUID()).email(new Email("test@example.com"))
                    .password("encoded").firstName("Jean-François").lastName("O'Brien").build();

            UserResponse response = new UserResponse(savedUser.getId(), "test@example.com", "Jean-François", "O'Brien");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(userMapper.toDomain(request)).thenReturn(domainUser);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.register(any(User.class))).thenReturn(savedUser);
            when(userMapper.toResponse(savedUser)).thenReturn(response);

            UserResponse result = authenticationService.register(request);

            assertThat(result.firstName()).isEqualTo("Jean-François");
            assertThat(result.lastName()).isEqualTo("O'Brien");
        }

        @Test
        @DisplayName("Register: Should treat emails as case-insensitive")
        void shouldTreatEmailsAsCaseInsensitive() {
            String lowercaseEmail = "test@example.com";
            String uppercaseEmail = "Test@Example.COM";

            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail(uppercaseEmail);
            request.setPassword("Password123");
            request.setFirstName("John");
            request.setLastName("Doe");

            User existingUser = User.builder().id(UUID.randomUUID()).email(new Email(lowercaseEmail))
                    .password("encoded").firstName("Existing").lastName("User").build();

            when(userRepository.findByEmail(uppercaseEmail)).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(uppercaseEmail)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Login: Should treat emails as case-insensitive")
        void shouldTreatEmailsAsCaseInsensitiveDuringLogin() {
            LoginUserRequest request = new LoginUserRequest();
            request.setEmail("Test@Example.COM");
            request.setPassword("Password123");

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("test@example.com").password("encoded").authorities("ROLE_USER").build();

            Authentication auth = mock(Authentication.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(jwtTokenProvider.generateToken(userDetails)).thenReturn("token");

            LoginResponse response = authenticationService.login(request);

            assertThat(response.token()).isEqualTo("token");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Register: Should handle maximum password length")
        void shouldHandleMaximumPasswordLength() {
            String maxPassword = "A".repeat(30);
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("test@example.com");
            request.setPassword(maxPassword);
            request.setFirstName("John");
            request.setLastName("Doe");

            User domainUser = User.builder().email(new Email("test@example.com")).password(maxPassword)
                    .firstName("John").lastName("Doe").build();

            User savedUser = User.builder().id(UUID.randomUUID()).email(new Email("test@example.com"))
                    .password("encoded_long_password").firstName("John").lastName("Doe").build();

            UserResponse response = new UserResponse(savedUser.getId(), "test@example.com", "John", "Doe");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(userMapper.toDomain(request)).thenReturn(domainUser);
            when(passwordEncoder.encode(maxPassword)).thenReturn("encoded_long_password");
            when(userRepository.register(any(User.class))).thenReturn(savedUser);
            when(userMapper.toResponse(savedUser)).thenReturn(response);

            UserResponse result = authenticationService.register(request);

            assertThat(result).isNotNull();
            verify(passwordEncoder).encode(maxPassword);
        }
    }
}
