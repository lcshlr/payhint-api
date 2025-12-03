package com.payhint.api.domain.crm.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.payhint.api.domain.shared.exception.InvalidPropertyException;
import com.payhint.api.domain.shared.valueobject.Email;

@DisplayName("Email Value Object Tests")
class EmailTest {

    private static final String VALID_EMAIL = "john.doe@example.com";

    @Nested
    @DisplayName("Valid Email Creation Tests")
    class ValidEmailCreationTests {

        @Test
        @DisplayName("Should create email with valid format")
        void shouldCreateEmailWithValidFormat() {
            Email email = new Email(VALID_EMAIL);

            assertThat(email).isNotNull();
            assertThat(email.value()).isEqualTo(VALID_EMAIL);
        }

        @Test
        @DisplayName("Should normalize email to lowercase")
        void shouldNormalizeEmailToLowercase() {
            String uppercaseEmail = "JOHN.DOE@EXAMPLE.COM";
            Email email = new Email(uppercaseEmail);

            assertThat(email.value()).isEqualTo(uppercaseEmail.toLowerCase());
        }

        @Test
        @DisplayName("Should trim whitespace from email")
        void shouldTrimWhitespaceFromEmail() {
            String emailWithSpaces = "  john.doe@example.com  ";
            Email email = new Email(emailWithSpaces);

            assertThat(email.value()).isEqualTo(VALID_EMAIL);
        }

        @Test
        @DisplayName("Should handle email with plus sign")
        void shouldHandleEmailWithPlusSign() {
            String emailWithPlus = "john.doe+test@example.com";
            Email email = new Email(emailWithPlus);

            assertThat(email.value()).isEqualTo(emailWithPlus);
        }

        @Test
        @DisplayName("Should handle email with dots in local part")
        void shouldHandleEmailWithDotsInLocalPart() {
            String emailWithDots = "john.doe.test@example.com";
            Email email = new Email(emailWithDots);

            assertThat(email.value()).isEqualTo(emailWithDots);
        }

        @Test
        @DisplayName("Should handle email with hyphen in domain")
        void shouldHandleEmailWithHyphenInDomain() {
            String emailWithHyphen = "john@example-domain.com";
            Email email = new Email(emailWithHyphen);

            assertThat(email.value()).isEqualTo(emailWithHyphen);
        }

        @Test
        @DisplayName("Should handle email with subdomain")
        void shouldHandleEmailWithSubdomain() {
            String emailWithSubdomain = "john@mail.example.com";
            Email email = new Email(emailWithSubdomain);

            assertThat(email.value()).isEqualTo(emailWithSubdomain);
        }

        @Test
        @DisplayName("Should handle email with numbers")
        void shouldHandleEmailWithNumbers() {
            String emailWithNumbers = "john123@example456.com";
            Email email = new Email(emailWithNumbers);

            assertThat(email.value()).isEqualTo(emailWithNumbers);
        }

        @Test
        @DisplayName("Should handle email with underscore")
        void shouldHandleEmailWithUnderscore() {
            String emailWithUnderscore = "john_doe@example.com";
            Email email = new Email(emailWithUnderscore);

            assertThat(email.value()).isEqualTo(emailWithUnderscore);
        }
    }

    @Nested
    @DisplayName("Invalid Email Validation Tests")
    class InvalidEmailValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should reject null or empty email")
        void shouldRejectNullOrEmptyEmail(String invalidEmail) {
            assertThatThrownBy(() -> new Email(invalidEmail)).isInstanceOf(InvalidPropertyException.class)
                    .hasMessageContaining("Email cannot be null or empty");
        }

        @ParameterizedTest
        @ValueSource(strings = { "   ", "\t", "\n" })
        @DisplayName("Should reject blank email")
        void shouldRejectBlankEmail(String blankEmail) {
            assertThatThrownBy(() -> new Email(blankEmail)).isInstanceOf(InvalidPropertyException.class)
                    .hasMessageContaining("Email cannot be null or empty");
        }

        @ParameterizedTest
        @ValueSource(strings = { "invalid", "invalid@", "@example.com", "invalid@.com", "invalid@domain",
                "invalid domain@example.com", "invalid@domain..com", "invalid@@example.com", ".invalid@example.com",
                "invalid.@example.com" })
        @DisplayName("Should reject invalid email formats")
        void shouldRejectInvalidEmailFormats(String invalidEmail) {
            assertThatThrownBy(() -> new Email(invalidEmail)).isInstanceOf(InvalidPropertyException.class)
                    .hasMessageContaining("Invalid email format");
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Should be immutable as a record")
        void shouldBeImmutable() {
            Email email = new Email(VALID_EMAIL);
            String originalValue = email.value();

            assertThat(email.value()).isEqualTo(originalValue);
        }

        @Test
        @DisplayName("Should maintain value integrity")
        void shouldMaintainValueIntegrity() {
            Email originalEmail = new Email(VALID_EMAIL);
            Email retrievedEmail = originalEmail;

            assertThat(retrievedEmail).isEqualTo(originalEmail);
            assertThat(retrievedEmail.value()).isEqualTo(VALID_EMAIL);
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when values are the same")
        void shouldBeEqualWhenValuesAreSame() {
            Email email1 = new Email(VALID_EMAIL);
            Email email2 = new Email(VALID_EMAIL);

            assertThat(email1).isEqualTo(email2);
            assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
        }

        @Test
        @DisplayName("Should be equal after normalization")
        void shouldBeEqualAfterNormalization() {
            Email email1 = new Email("JOHN.DOE@EXAMPLE.COM");
            Email email2 = new Email("john.doe@example.com");

            assertThat(email1).isEqualTo(email2);
            assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when values are different")
        void shouldNotBeEqualWhenValuesAreDifferent() {
            Email email1 = new Email("john.doe@example.com");
            Email email2 = new Email("jane.doe@example.com");

            assertThat(email1).isNotEqualTo(email2);
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should return email value in toString")
        void shouldReturnEmailValueInToString() {
            Email email = new Email(VALID_EMAIL);

            assertThat(email.toString()).isEqualTo(VALID_EMAIL);
        }

        @Test
        @DisplayName("Should return normalized value in toString")
        void shouldReturnNormalizedValueInToString() {
            Email email = new Email("JOHN.DOE@EXAMPLE.COM");

            assertThat(email.toString()).isEqualTo(VALID_EMAIL);
        }
    }
}
