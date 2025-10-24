package com.payhint.api.domain.crm.model;

import com.payhint.api.domain.crm.valueobjects.Email;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User Domain Model Tests")
class UserTest {

    private static final String VALID_EMAIL = "john.doe@example.com";
    private static final String VALID_PASSWORD = "securePassword123";
    private static final String VALID_FIRST_NAME = "John";
    private static final String VALID_LAST_NAME = "Doe";
    private static final String UPDATED_FIRST_NAME = "Jane";
    private static final String UPDATED_LAST_NAME = "Smith";

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create user with valid parameters using simple constructor")
        void shouldCreateUserWithValidParameters() {
            Email email = new Email(VALID_EMAIL);

            User user = new User(email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            assertThat(user).isNotNull();
            assertThat(user.getId()).isNull();
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getPassword()).isEqualTo(VALID_PASSWORD);
            assertThat(user.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(user.getCreatedAt()).isNull();
            assertThat(user.getUpdatedAt()).isNull();

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should create user with all fields using builder")
        void shouldCreateUserUsingBuilder() {
            UUID id = UUID.randomUUID();
            Email email = new Email(VALID_EMAIL);
            LocalDateTime now = LocalDateTime.now();

            User user = User.builder().id(id).email(email).password(VALID_PASSWORD).firstName(VALID_FIRST_NAME)
                    .lastName(VALID_LAST_NAME).createdAt(now).updatedAt(now).build();

            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getPassword()).isEqualTo(VALID_PASSWORD);
            assertThat(user.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(user.getCreatedAt()).isEqualTo(now);
            assertThat(user.getUpdatedAt()).isEqualTo(now);

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when password is null")
        void shouldFailValidationWhenPasswordIsNull() {
            Email email = new Email(VALID_EMAIL);

            User user = User.builder().email(email).password(null).firstName(VALID_FIRST_NAME).lastName(VALID_LAST_NAME)
                    .build();

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("password");
        }

        @Test
        @DisplayName("Should fail validation when password is empty")
        void shouldFailValidationWhenPasswordIsEmpty() {
            Email email = new Email(VALID_EMAIL);

            User user = User.builder().email(email).password("").firstName(VALID_FIRST_NAME).lastName(VALID_LAST_NAME)
                    .build();

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("password");
        }

        @Test
        @DisplayName("Should fail validation when password is blank")
        void shouldFailValidationWhenPasswordIsBlank() {
            Email email = new Email(VALID_EMAIL);

            User user = User.builder().email(email).password("   ").firstName(VALID_FIRST_NAME)
                    .lastName(VALID_LAST_NAME).build();

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("password");
        }

        @Test
        @DisplayName("Should fail validation when first name is null")
        void shouldFailValidationWhenFirstNameIsNull() {
            Email email = new Email(VALID_EMAIL);

            User user = User.builder().email(email).password(VALID_PASSWORD).firstName(null).lastName(VALID_LAST_NAME)
                    .build();

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("firstName");
        }

        @Test
        @DisplayName("Should fail validation when first name is empty")
        void shouldFailValidationWhenFirstNameIsEmpty() {
            Email email = new Email(VALID_EMAIL);

            User user = User.builder().email(email).password(VALID_PASSWORD).firstName("").lastName(VALID_LAST_NAME)
                    .build();

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("firstName");
        }

        @Test
        @DisplayName("Should fail validation when last name is null")
        void shouldFailValidationWhenLastNameIsNull() {
            Email email = new Email(VALID_EMAIL);

            User user = User.builder().email(email).password(VALID_PASSWORD).firstName(VALID_FIRST_NAME).lastName(null)
                    .build();

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("lastName");
        }

        @Test
        @DisplayName("Should fail validation when last name is empty")
        void shouldFailValidationWhenLastNameIsEmpty() {
            Email email = new Email(VALID_EMAIL);

            User user = User.builder().email(email).password(VALID_PASSWORD).firstName(VALID_FIRST_NAME).lastName("")
                    .build();

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("lastName");
        }

        @Test
        @DisplayName("Should fail validation with multiple constraint violations")
        void shouldFailValidationWithMultipleViolations() {
            Email email = new Email(VALID_EMAIL);

            User user = User.builder().email(email).password("").firstName("").lastName("").build();

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(3);
            assertThat(violations).extracting(ConstraintViolation::getPropertyPath).extracting(Object::toString)
                    .containsExactlyInAnyOrder("password", "firstName", "lastName");
        }
    }

    @Nested
    @DisplayName("Password Management Tests")
    class PasswordManagementTests {

        @Test
        @DisplayName("Should update password successfully")
        void shouldUpdatePassword() {
            Email email = new Email(VALID_EMAIL);
            User user = new User(email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);
            String newPassword = "newSecurePassword456";

            user.setPassword(newPassword);

            assertThat(user.getPassword()).isEqualTo(newPassword);

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation after setting password to null")
        void shouldFailValidationAfterSettingPasswordToNull() {
            Email email = new Email(VALID_EMAIL);
            User user = new User(email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            user.setPassword(null);

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("password");
        }

        @Test
        @DisplayName("Should fail validation after setting password to empty string")
        void shouldFailValidationAfterSettingPasswordToEmpty() {
            Email email = new Email(VALID_EMAIL);
            User user = new User(email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            user.setPassword("");

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("password");
        }

        @Test
        @DisplayName("Should fail validation after setting password to blank string")
        void shouldFailValidationAfterSettingPasswordToBlank() {
            Email email = new Email(VALID_EMAIL);
            User user = new User(email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            user.setPassword("   ");

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("password");
        }
    }

    @Nested
    @DisplayName("Profile Update Tests")
    class ProfileUpdateTests {

        @Test
        @DisplayName("Should update profile with valid first and last name")
        void shouldUpdateProfileWithValidNames() {
            Email email = new Email(VALID_EMAIL);
            User user = new User(email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);
            LocalDateTime beforeUpdate = LocalDateTime.now();

            user.updateProfile(UPDATED_FIRST_NAME, UPDATED_LAST_NAME);

            assertThat(user.getFirstName()).isEqualTo(UPDATED_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(UPDATED_LAST_NAME);
            assertThat(user.getUpdatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should update profile and set updatedAt timestamp")
        void shouldSetUpdatedAtTimestamp() {
            Email email = new Email(VALID_EMAIL);
            User user = User.builder().email(email).password(VALID_PASSWORD).firstName(VALID_FIRST_NAME)
                    .lastName(VALID_LAST_NAME).createdAt(LocalDateTime.now().minusDays(5)).updatedAt(null).build();

            LocalDateTime beforeUpdate = LocalDateTime.now();
            user.updateProfile(UPDATED_FIRST_NAME, UPDATED_LAST_NAME);
            LocalDateTime afterUpdate = LocalDateTime.now();

            assertThat(user.getUpdatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate).isBeforeOrEqualTo(afterUpdate);
        }

        @Test
        @DisplayName("Should update profile multiple times with different timestamps")
        void shouldUpdateProfileMultipleTimes() throws InterruptedException {
            Email email = new Email(VALID_EMAIL);
            User user = new User(email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            user.updateProfile("FirstUpdate", "LastUpdate1");
            LocalDateTime firstUpdate = user.getUpdatedAt();

            Thread.sleep(10);

            user.updateProfile("SecondUpdate", "LastUpdate2");
            LocalDateTime secondUpdate = user.getUpdatedAt();

            assertThat(user.getFirstName()).isEqualTo("SecondUpdate");
            assertThat(user.getLastName()).isEqualTo("LastUpdate2");
            assertThat(secondUpdate).isAfter(firstUpdate);
        }

        @Test
        @DisplayName("Should fail validation when updating profile with null first name")
        void shouldFailValidationWithNullFirstName() {
            Email email = new Email(VALID_EMAIL);
            User user = new User(email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            user.updateProfile(null, UPDATED_LAST_NAME);

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("firstName");
        }

        @Test
        @DisplayName("Should fail validation when updating profile with null last name")
        void shouldFailValidationWithNullLastName() {
            Email email = new Email(VALID_EMAIL);
            User user = new User(email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            user.updateProfile(UPDATED_FIRST_NAME, null);

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("lastName");
        }

        @Test
        @DisplayName("Should fail validation when updating profile with empty first name")
        void shouldFailValidationWithEmptyFirstName() {
            Email email = new Email(VALID_EMAIL);
            User user = new User(email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            user.updateProfile("", UPDATED_LAST_NAME);

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("firstName");
        }

        @Test
        @DisplayName("Should fail validation when updating profile with empty last name")
        void shouldFailValidationWithEmptyLastName() {
            Email email = new Email(VALID_EMAIL);
            User user = new User(email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            user.updateProfile(UPDATED_FIRST_NAME, "");

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("lastName");
        }

        @Test
        @DisplayName("Should update profile with same values")
        void shouldUpdateProfileWithSameValues() {
            Email email = new Email(VALID_EMAIL);
            User user = new User(email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            user.updateProfile(VALID_FIRST_NAME, VALID_LAST_NAME);

            assertThat(user.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(user.getUpdatedAt()).isNotNull();

            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Email Value Object Integration Tests")
    class EmailIntegrationTests {

        @Test
        @DisplayName("Should maintain email immutability")
        void shouldMaintainEmailImmutability() {
            Email originalEmail = new Email(VALID_EMAIL);
            User user = new User(originalEmail, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            Email retrievedEmail = user.getEmail();

            assertThat(retrievedEmail).isEqualTo(originalEmail);
            assertThat(retrievedEmail.value()).isEqualTo(VALID_EMAIL);
        }

        @Test
        @DisplayName("Should handle different email formats")
        void shouldHandleDifferentEmailFormats() {
            String emailWithPlus = "john.doe+test@example.com";
            Email email = new Email(emailWithPlus);

            User user = new User(email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            assertThat(user.getEmail().value()).isEqualTo(emailWithPlus);
        }
    }

    @Nested
    @DisplayName("Timestamp Behavior Tests")
    class TimestampBehaviorTests {

        @Test
        @DisplayName("Should preserve createdAt when updating profile")
        void shouldPreserveCreatedAtWhenUpdatingProfile() {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(10);
            Email email = new Email(VALID_EMAIL);
            User user = User.builder().email(email).password(VALID_PASSWORD).firstName(VALID_FIRST_NAME)
                    .lastName(VALID_LAST_NAME).createdAt(createdAt).build();

            user.updateProfile(UPDATED_FIRST_NAME, UPDATED_LAST_NAME);

            assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("Should not modify createdAt when setting password")
        void shouldNotModifyCreatedAtWhenSettingPassword() {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
            Email email = new Email(VALID_EMAIL);
            User user = User.builder().email(email).password(VALID_PASSWORD).firstName(VALID_FIRST_NAME)
                    .lastName(VALID_LAST_NAME).createdAt(createdAt).build();

            user.setPassword("newPassword");

            assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("Business Invariant Tests")
    class BusinessInvariantTests {

        @Test
        @DisplayName("Should maintain all properties after profile update")
        void shouldMaintainAllPropertiesAfterProfileUpdate() {
            UUID id = UUID.randomUUID();
            Email email = new Email(VALID_EMAIL);
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);

            User user = User.builder().id(id).email(email).password(VALID_PASSWORD).firstName(VALID_FIRST_NAME)
                    .lastName(VALID_LAST_NAME).createdAt(createdAt).build();

            user.updateProfile(UPDATED_FIRST_NAME, UPDATED_LAST_NAME);

            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getPassword()).isEqualTo(VALID_PASSWORD);
            assertThat(user.getCreatedAt()).isEqualTo(createdAt);

            assertThat(user.getFirstName()).isEqualTo(UPDATED_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(UPDATED_LAST_NAME);
            assertThat(user.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should maintain all properties after password change")
        void shouldMaintainAllPropertiesAfterPasswordChange() {
            UUID id = UUID.randomUUID();
            Email email = new Email(VALID_EMAIL);
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
            LocalDateTime updatedAt = LocalDateTime.now().minusHours(1);

            User user = User.builder().id(id).email(email).password(VALID_PASSWORD).firstName(VALID_FIRST_NAME)
                    .lastName(VALID_LAST_NAME).createdAt(createdAt).updatedAt(updatedAt).build();

            String newPassword = "changedPassword";

            user.setPassword(newPassword);

            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(user.getCreatedAt()).isEqualTo(createdAt);
            assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);

            assertThat(user.getPassword()).isEqualTo(newPassword);
        }
    }
}
