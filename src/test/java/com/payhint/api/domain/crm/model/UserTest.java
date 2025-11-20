package com.payhint.api.domain.crm.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.payhint.api.domain.crm.valueobject.Email;
import com.payhint.api.domain.crm.valueobject.UserId;

@DisplayName("User Domain Model Tests")
class UserTest {
    private static final UserId VALID_ID = new UserId(UUID.randomUUID());
    private static final String VALID_EMAIL = "john.doe@example.com";
    private static final String VALID_PASSWORD = "securePassword123";
    private static final String VALID_FIRST_NAME = "John";
    private static final String VALID_LAST_NAME = "Doe";
    private static final String UPDATED_FIRST_NAME = "Jane";
    private static final String UPDATED_LAST_NAME = "Smith";

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create user with valid parameters using simple constructor")
        void shouldCreateUserWithValidParameters() {
            Email email = new Email(VALID_EMAIL);

            User user = User.create(VALID_ID, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(VALID_ID);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getPassword()).isEqualTo(VALID_PASSWORD);
            assertThat(user.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(user.getCreatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isAfterOrEqualTo(user.getCreatedAt());
        }

        @Test
        @DisplayName("Should create user with all fields using builder")
        void shouldCreateUserUsingBuilder() {
            UserId id = new UserId(UUID.randomUUID());
            Email email = new Email(VALID_EMAIL);
            LocalDateTime now = LocalDateTime.now();

            User user = new User(id, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME, now, now);

            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getPassword()).isEqualTo(VALID_PASSWORD);
            assertThat(user.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(user.getCreatedAt()).isEqualTo(now);
            assertThat(user.getUpdatedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("Password Management Tests")
    class PasswordManagementTests {

        @Test
        @DisplayName("Should update password successfully")
        void shouldUpdatePassword() {
            Email email = new Email(VALID_EMAIL);
            User user = User.create(VALID_ID, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);
            String newPassword = "newSecurePassword456";

            user.changePassword(newPassword);

            assertThat(user.getPassword()).isEqualTo(newPassword);
        }
    }

    @Nested
    @DisplayName("Profile Update Tests")
    class ProfileUpdateTests {

        @Test
        @DisplayName("Should update profile with valid first and last name")
        void shouldUpdateProfileWithValidNames() {
            Email email = new Email(VALID_EMAIL);
            User user = User.create(VALID_ID, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);
            LocalDateTime beforeUpdate = LocalDateTime.now();

            user.updateProfile(null, UPDATED_FIRST_NAME, UPDATED_LAST_NAME);

            assertThat(user.getFirstName()).isEqualTo(UPDATED_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(UPDATED_LAST_NAME);
            assertThat(user.getUpdatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
        }

        @Test
        @DisplayName("Should update profile and set updatedAt timestamp")
        void shouldSetUpdatedAtTimestamp() {
            Email email = new Email(VALID_EMAIL);
            User user = User.create(VALID_ID, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);
            LocalDateTime beforeUpdate = LocalDateTime.now();
            user.updateProfile(null, UPDATED_FIRST_NAME, UPDATED_LAST_NAME);
            LocalDateTime afterUpdate = LocalDateTime.now();

            assertThat(user.getUpdatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate).isBeforeOrEqualTo(afterUpdate);
        }

        @Test
        @DisplayName("Should update profile multiple times with different timestamps")
        void shouldUpdateProfileMultipleTimes() throws InterruptedException {
            Email email = new Email(VALID_EMAIL);
            User user = User.create(VALID_ID, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            user.updateProfile(null, "FirstUpdate", "LastUpdate1");
            LocalDateTime firstUpdate = user.getUpdatedAt();

            Thread.sleep(10);

            user.updateProfile(null, "SecondUpdate", "LastUpdate2");
            LocalDateTime secondUpdate = user.getUpdatedAt();

            assertThat(user.getFirstName()).isEqualTo("SecondUpdate");
            assertThat(user.getLastName()).isEqualTo("LastUpdate2");
            assertThat(secondUpdate).isAfter(firstUpdate);
        }

        @Test
        @DisplayName("Should update profile with same values")
        void shouldUpdateProfileWithSameValues() {
            Email email = new Email(VALID_EMAIL);
            User user = User.create(VALID_ID, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);

            user.updateProfile(null, VALID_FIRST_NAME, VALID_LAST_NAME);

            assertThat(user.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(user.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should update email when provided")
        void shouldUpdateEmailWhenProvided() {
            Email email = new Email(VALID_EMAIL);
            User user = User.create(VALID_ID, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);
            Email newEmail = new Email("new.email@example.com");

            user.updateProfile(newEmail, null, null);

            assertThat(user.getEmail()).isEqualTo(newEmail);
            assertThat(user.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(VALID_LAST_NAME);
        }

        @Test
        @DisplayName("Should update all profile fields when all provided")
        void shouldUpdateAllProfileFieldsWhenAllProvided() {
            Email email = new Email(VALID_EMAIL);
            User user = User.create(VALID_ID, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);
            Email newEmail = new Email("updated.email@example.com");

            user.updateProfile(newEmail, UPDATED_FIRST_NAME, UPDATED_LAST_NAME);

            assertThat(user.getEmail()).isEqualTo(newEmail);
            assertThat(user.getFirstName()).isEqualTo(UPDATED_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(UPDATED_LAST_NAME);
        }

        @Test
        @DisplayName("Should not update profile when all values are null")
        void shouldNotUpdateWhenAllValuesAreNull() {
            Email email = new Email(VALID_EMAIL);
            User user = User.create(VALID_ID, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME);
            LocalDateTime originalUpdatedAt = user.getUpdatedAt();

            user.updateProfile(null, null, null);

            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(user.getUpdatedAt()).isEqualTo(originalUpdatedAt);
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
            UserId id = new UserId(UUID.randomUUID());
            User user = new User(id, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME, createdAt, createdAt);

            user.updateProfile(null, UPDATED_FIRST_NAME, UPDATED_LAST_NAME);

            assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("Should not modify createdAt when changing password")
        void shouldNotModifyCreatedAtWhenSettingPassword() {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
            Email email = new Email(VALID_EMAIL);
            UserId id = new UserId(UUID.randomUUID());
            User user = new User(id, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME, createdAt, createdAt);

            user.changePassword("newPassword");

            assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("Business Invariant Tests")
    class BusinessInvariantTests {

        @Test
        @DisplayName("Should maintain all properties after profile update")
        void shouldMaintainAllPropertiesAfterProfileUpdate() {
            UserId id = new UserId(UUID.randomUUID());
            Email email = new Email(VALID_EMAIL);
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);

            User user = new User(id, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME, createdAt, createdAt);

            user.updateProfile(null, UPDATED_FIRST_NAME, UPDATED_LAST_NAME);

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
            UserId id = new UserId(UUID.randomUUID());
            Email email = new Email(VALID_EMAIL);
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
            LocalDateTime updatedAt = LocalDateTime.now().minusHours(1);

            User user = new User(id, email, VALID_PASSWORD, VALID_FIRST_NAME, VALID_LAST_NAME, createdAt, updatedAt);

            String newPassword = "changedPassword";

            user.changePassword(newPassword);

            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getFirstName()).isEqualTo(VALID_FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(VALID_LAST_NAME);
            assertThat(user.getCreatedAt()).isEqualTo(createdAt);
            assertThat(user.getUpdatedAt()).isAfter(updatedAt);

            assertThat(user.getPassword()).isEqualTo(newPassword);
        }
    }
}
