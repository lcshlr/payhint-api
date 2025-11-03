package com.payhint.api.domain.crm.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.payhint.api.domain.shared.exception.InvalidPropertyException;

@DisplayName("UserId Value Object Tests")
class UserIdTest {

    private static final UUID VALID_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final String VALID_UUID_STRING = "123e4567-e89b-12d3-a456-426614174000";

    @Nested
    @DisplayName("Valid UserId Creation Tests")
    class ValidUserIdCreationTests {

        @Test
        @DisplayName("Should create UserId with valid UUID")
        void shouldCreateUserIdWithValidUUID() {
            UserId userId = new UserId(VALID_UUID);

            assertThat(userId).isNotNull();
            assertThat(userId.value()).isEqualTo(VALID_UUID);
        }

        @Test
        @DisplayName("Should create UserId from valid string")
        void shouldCreateUserIdFromValidString() {
            UserId userId = UserId.fromString(VALID_UUID_STRING);

            assertThat(userId).isNotNull();
            assertThat(userId.value()).isEqualTo(VALID_UUID);
        }

        @Test
        @DisplayName("Should create UserId from random UUID")
        void shouldCreateUserIdFromRandomUUID() {
            UUID randomUuid = UUID.randomUUID();
            UserId userId = new UserId(randomUuid);

            assertThat(userId.value()).isEqualTo(randomUuid);
        }

        @Test
        @DisplayName("Should create UserId from uppercase UUID string")
        void shouldCreateUserIdFromUppercaseUUIDString() {
            String uppercaseUuid = VALID_UUID_STRING.toUpperCase();
            UserId userId = UserId.fromString(uppercaseUuid);

            assertThat(userId.value()).isEqualTo(VALID_UUID);
        }

        @Test
        @DisplayName("Should create UserId from mixed case UUID string")
        void shouldCreateUserIdFromMixedCaseUUIDString() {
            String mixedCaseUuid = "123E4567-e89B-12D3-A456-426614174000";
            UserId userId = UserId.fromString(mixedCaseUuid);

            assertThat(userId.value()).isEqualTo(VALID_UUID);
        }
    }

    @Nested
    @DisplayName("Invalid UserId Validation Tests")
    class InvalidUserIdValidationTests {

        @Test
        @DisplayName("Should reject null UUID")
        void shouldRejectNullUUID() {
            assertThatThrownBy(() -> new UserId(null)).isInstanceOf(InvalidPropertyException.class)
                    .hasMessageContaining("UserId cannot be null");
        }

        @Test
        @DisplayName("Should reject null string in fromString")
        void shouldRejectNullStringInFromString() {
            assertThatThrownBy(() -> UserId.fromString(null)).isInstanceOf(InvalidPropertyException.class)
                    .hasMessageContaining("Invalid UserId format");
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "   ", "invalid", "123", "not-a-uuid", "123e4567-e89b-12d3-a456",
                "123e4567-e89b-12d3-a456-42661417400g", "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" })
        @DisplayName("Should reject invalid UUID strings")
        void shouldRejectInvalidUUIDStrings(String invalidUuid) {
            assertThatThrownBy(() -> UserId.fromString(invalidUuid)).isInstanceOf(InvalidPropertyException.class)
                    .hasMessageContaining("Invalid UserId format");
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Should be immutable as a record")
        void shouldBeImmutable() {
            UserId userId = new UserId(VALID_UUID);
            UUID originalValue = userId.value();

            assertThat(userId.value()).isEqualTo(originalValue);
        }

        @Test
        @DisplayName("Should maintain value integrity")
        void shouldMaintainValueIntegrity() {
            UserId originalUserId = new UserId(VALID_UUID);
            UserId retrievedUserId = originalUserId;

            assertThat(retrievedUserId).isEqualTo(originalUserId);
            assertThat(retrievedUserId.value()).isEqualTo(VALID_UUID);
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when UUIDs are the same")
        void shouldBeEqualWhenUUIDsAreSame() {
            UserId userId1 = new UserId(VALID_UUID);
            UserId userId2 = new UserId(VALID_UUID);

            assertThat(userId1).isEqualTo(userId2);
            assertThat(userId1.hashCode()).isEqualTo(userId2.hashCode());
        }

        @Test
        @DisplayName("Should be equal when created from same string")
        void shouldBeEqualWhenCreatedFromSameString() {
            UserId userId1 = UserId.fromString(VALID_UUID_STRING);
            UserId userId2 = UserId.fromString(VALID_UUID_STRING);

            assertThat(userId1).isEqualTo(userId2);
            assertThat(userId1.hashCode()).isEqualTo(userId2.hashCode());
        }

        @Test
        @DisplayName("Should be equal regardless of case in string")
        void shouldBeEqualRegardlessOfCaseInString() {
            UserId userId1 = UserId.fromString(VALID_UUID_STRING.toLowerCase());
            UserId userId2 = UserId.fromString(VALID_UUID_STRING.toUpperCase());

            assertThat(userId1).isEqualTo(userId2);
            assertThat(userId1.hashCode()).isEqualTo(userId2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when UUIDs are different")
        void shouldNotBeEqualWhenUUIDsAreDifferent() {
            UserId userId1 = new UserId(UUID.randomUUID());
            UserId userId2 = new UserId(UUID.randomUUID());

            assertThat(userId1).isNotEqualTo(userId2);
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should return UUID string in toString")
        void shouldReturnUUIDStringInToString() {
            UserId userId = new UserId(VALID_UUID);

            assertThat(userId.toString()).isEqualTo(VALID_UUID_STRING);
        }

        @Test
        @DisplayName("Should return lowercase UUID in toString")
        void shouldReturnLowercaseUUIDInToString() {
            UserId userId = UserId.fromString(VALID_UUID_STRING.toUpperCase());

            assertThat(userId.toString()).isEqualTo(VALID_UUID_STRING);
        }

        @Test
        @DisplayName("Should be reversible with fromString")
        void shouldBeReversibleWithFromString() {
            UserId originalUserId = new UserId(VALID_UUID);
            String stringRepresentation = originalUserId.toString();
            UserId recreatedUserId = UserId.fromString(stringRepresentation);

            assertThat(recreatedUserId).isEqualTo(originalUserId);
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create same UserId from constructor and fromString")
        void shouldCreateSameUserIdFromConstructorAndFromString() {
            UserId userIdFromConstructor = new UserId(VALID_UUID);
            UserId userIdFromString = UserId.fromString(VALID_UUID_STRING);

            assertThat(userIdFromString).isEqualTo(userIdFromConstructor);
        }

        @Test
        @DisplayName("Should handle fromString with various UUID formats")
        void shouldHandleFromStringWithVariousUUIDFormats() {
            String[] validFormats = { VALID_UUID_STRING, VALID_UUID_STRING.toUpperCase(),
                    VALID_UUID_STRING.toLowerCase(), "123E4567-E89B-12D3-A456-426614174000" };

            for (String format : validFormats) {
                UserId userId = UserId.fromString(format);
                assertThat(userId.value()).isEqualTo(VALID_UUID);
            }
        }
    }
}
