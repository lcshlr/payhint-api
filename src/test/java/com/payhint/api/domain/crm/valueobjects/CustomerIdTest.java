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

@DisplayName("CustomerId Value Object Tests")
class CustomerIdTest {

    private static final UUID VALID_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final String VALID_UUID_STRING = "123e4567-e89b-12d3-a456-426614174000";

    @Nested
    @DisplayName("Valid CustomerId Creation Tests")
    class ValidCustomerIdCreationTests {

        @Test
        @DisplayName("Should create CustomerId with valid UUID")
        void shouldCreateCustomerIdWithValidUUID() {
            CustomerId customerId = new CustomerId(VALID_UUID);

            assertThat(customerId).isNotNull();
            assertThat(customerId.value()).isEqualTo(VALID_UUID);
        }

        @Test
        @DisplayName("Should create CustomerId from valid string")
        void shouldCreateCustomerIdFromValidString() {
            CustomerId customerId = CustomerId.fromString(VALID_UUID_STRING);

            assertThat(customerId).isNotNull();
            assertThat(customerId.value()).isEqualTo(VALID_UUID);
        }

        @Test
        @DisplayName("Should create CustomerId from random UUID")
        void shouldCreateCustomerIdFromRandomUUID() {
            UUID randomUuid = UUID.randomUUID();
            CustomerId customerId = new CustomerId(randomUuid);

            assertThat(customerId.value()).isEqualTo(randomUuid);
        }

        @Test
        @DisplayName("Should create CustomerId from uppercase UUID string")
        void shouldCreateCustomerIdFromUppercaseUUIDString() {
            String uppercaseUuid = VALID_UUID_STRING.toUpperCase();
            CustomerId customerId = CustomerId.fromString(uppercaseUuid);

            assertThat(customerId.value()).isEqualTo(VALID_UUID);
        }

        @Test
        @DisplayName("Should create CustomerId from mixed case UUID string")
        void shouldCreateCustomerIdFromMixedCaseUUIDString() {
            String mixedCaseUuid = "123E4567-e89B-12D3-A456-426614174000";
            CustomerId customerId = CustomerId.fromString(mixedCaseUuid);

            assertThat(customerId.value()).isEqualTo(VALID_UUID);
        }
    }

    @Nested
    @DisplayName("Invalid CustomerId Validation Tests")
    class InvalidCustomerIdValidationTests {

        @Test
        @DisplayName("Should reject null UUID")
        void shouldRejectNullUUID() {
            assertThatThrownBy(() -> new CustomerId(null)).isInstanceOf(InvalidPropertyException.class)
                    .hasMessageContaining("CustomerId cannot be null");
        }

        @Test
        @DisplayName("Should reject null string in fromString")
        void shouldRejectNullStringInFromString() {
            assertThatThrownBy(() -> CustomerId.fromString(null)).isInstanceOf(InvalidPropertyException.class)
                    .hasMessageContaining("Invalid CustomerId format");
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "   ", "invalid", "123", "not-a-uuid", "123e4567-e89b-12d3-a456",
                "123e4567-e89b-12d3-a456-42661417400g", "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" })
        @DisplayName("Should reject invalid UUID strings")
        void shouldRejectInvalidUUIDStrings(String invalidUuid) {
            assertThatThrownBy(() -> CustomerId.fromString(invalidUuid)).isInstanceOf(InvalidPropertyException.class)
                    .hasMessageContaining("Invalid CustomerId format");
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Should be immutable as a record")
        void shouldBeImmutable() {
            CustomerId customerId = new CustomerId(VALID_UUID);
            UUID originalValue = customerId.value();

            assertThat(customerId.value()).isEqualTo(originalValue);
        }

        @Test
        @DisplayName("Should maintain value integrity")
        void shouldMaintainValueIntegrity() {
            CustomerId originalCustomerId = new CustomerId(VALID_UUID);
            CustomerId retrievedCustomerId = originalCustomerId;

            assertThat(retrievedCustomerId).isEqualTo(originalCustomerId);
            assertThat(retrievedCustomerId.value()).isEqualTo(VALID_UUID);
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when UUIDs are the same")
        void shouldBeEqualWhenUUIDsAreSame() {
            CustomerId customerId1 = new CustomerId(VALID_UUID);
            CustomerId customerId2 = new CustomerId(VALID_UUID);

            assertThat(customerId1).isEqualTo(customerId2);
            assertThat(customerId1.hashCode()).isEqualTo(customerId2.hashCode());
        }

        @Test
        @DisplayName("Should be equal when created from same string")
        void shouldBeEqualWhenCreatedFromSameString() {
            CustomerId customerId1 = CustomerId.fromString(VALID_UUID_STRING);
            CustomerId customerId2 = CustomerId.fromString(VALID_UUID_STRING);

            assertThat(customerId1).isEqualTo(customerId2);
            assertThat(customerId1.hashCode()).isEqualTo(customerId2.hashCode());
        }

        @Test
        @DisplayName("Should be equal regardless of case in string")
        void shouldBeEqualRegardlessOfCaseInString() {
            CustomerId customerId1 = CustomerId.fromString(VALID_UUID_STRING.toLowerCase());
            CustomerId customerId2 = CustomerId.fromString(VALID_UUID_STRING.toUpperCase());

            assertThat(customerId1).isEqualTo(customerId2);
            assertThat(customerId1.hashCode()).isEqualTo(customerId2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when UUIDs are different")
        void shouldNotBeEqualWhenUUIDsAreDifferent() {
            CustomerId customerId1 = new CustomerId(UUID.randomUUID());
            CustomerId customerId2 = new CustomerId(UUID.randomUUID());

            assertThat(customerId1).isNotEqualTo(customerId2);
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should return UUID string in toString")
        void shouldReturnUUIDStringInToString() {
            CustomerId customerId = new CustomerId(VALID_UUID);

            assertThat(customerId.toString()).isEqualTo(VALID_UUID_STRING);
        }

        @Test
        @DisplayName("Should return lowercase UUID in toString")
        void shouldReturnLowercaseUUIDInToString() {
            CustomerId customerId = CustomerId.fromString(VALID_UUID_STRING.toUpperCase());

            assertThat(customerId.toString()).isEqualTo(VALID_UUID_STRING);
        }

        @Test
        @DisplayName("Should be reversible with fromString")
        void shouldBeReversibleWithFromString() {
            CustomerId originalCustomerId = new CustomerId(VALID_UUID);
            String stringRepresentation = originalCustomerId.toString();
            CustomerId recreatedCustomerId = CustomerId.fromString(stringRepresentation);

            assertThat(recreatedCustomerId).isEqualTo(originalCustomerId);
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create same CustomerId from constructor and fromString")
        void shouldCreateSameCustomerIdFromConstructorAndFromString() {
            CustomerId customerIdFromConstructor = new CustomerId(VALID_UUID);
            CustomerId customerIdFromString = CustomerId.fromString(VALID_UUID_STRING);

            assertThat(customerIdFromString).isEqualTo(customerIdFromConstructor);
        }

        @Test
        @DisplayName("Should handle fromString with various UUID formats")
        void shouldHandleFromStringWithVariousUUIDFormats() {
            String[] validFormats = { VALID_UUID_STRING, VALID_UUID_STRING.toUpperCase(),
                    VALID_UUID_STRING.toLowerCase(), "123E4567-E89B-12D3-A456-426614174000" };

            for (String format : validFormats) {
                CustomerId customerId = CustomerId.fromString(format);
                assertThat(customerId.value()).isEqualTo(VALID_UUID);
            }
        }
    }
}
