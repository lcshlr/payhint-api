package com.payhint.api.domain.crm.model;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.payhint.api.domain.crm.valueobjects.CustomerId;
import com.payhint.api.domain.crm.valueobjects.Email;
import com.payhint.api.domain.crm.valueobjects.UserId;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("Customer Domain Model Tests")
class CustomerTest {

    private static final UserId VALID_USER_ID = new UserId(UUID.randomUUID());
    private static final String VALID_COMPANY_NAME = "Acme Corporation";
    private static final String VALID_EMAIL = "john.doe@example.com";
    private static final String UPDATED_COMPANY_NAME = "Updated Corporation";
    private static final String UPDATED_EMAIL = "jane.smith@example.com";

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
        @DisplayName("Should create customer with valid parameters using simple constructor")
        void shouldCreateCustomerWithValidParameters() {
            Email email = new Email(VALID_EMAIL);

            Customer customer = new Customer(VALID_USER_ID, VALID_COMPANY_NAME, email);

            assertThat(customer).isNotNull();
            assertThat(customer.getId()).isNull();
            assertThat(customer.getUserId()).isEqualTo(VALID_USER_ID);
            assertThat(customer.getCompanyName()).isEqualTo(VALID_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(email);
            assertThat(customer.getCreatedAt()).isNotNull();
            assertThat(customer.getUpdatedAt()).isNotNull();

            Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should create customer with all fields using builder")
        void shouldCreateCustomerUsingBuilder() {
            CustomerId id = new CustomerId(UUID.randomUUID());
            Email email = new Email(VALID_EMAIL);
            LocalDateTime now = LocalDateTime.now();

            Customer customer = Customer.builder().id(id).userId(VALID_USER_ID).companyName(VALID_COMPANY_NAME)
                    .contactEmail(email).createdAt(now).updatedAt(now).build();

            assertThat(customer).isNotNull();
            assertThat(customer.getId()).isEqualTo(id);
            assertThat(customer.getUserId()).isEqualTo(VALID_USER_ID);
            assertThat(customer.getCompanyName()).isEqualTo(VALID_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(email);
            assertThat(customer.getCreatedAt()).isEqualTo(now);
            assertThat(customer.getUpdatedAt()).isEqualTo(now);

            Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Informations Update Tests")
    class InformationsUpdateTests {

        @Test
        @DisplayName("Should update informations with valid company name and contact email")
        void shouldUpdateInformationsWithValidData() {
            Email email = new Email(VALID_EMAIL);
            Customer customer = new Customer(VALID_USER_ID, VALID_COMPANY_NAME, email);
            LocalDateTime beforeUpdate = LocalDateTime.now();

            customer.updateInformation(UPDATED_COMPANY_NAME, new Email(UPDATED_EMAIL));

            assertThat(customer.getCompanyName()).isEqualTo(UPDATED_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(new Email(UPDATED_EMAIL));
            assertThat(customer.getUpdatedAt()).isNotNull();
            assertThat(customer.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);

            Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should update informations and set updatedAt timestamp")
        void shouldSetUpdatedAtTimestamp() {
            Email email = new Email(VALID_EMAIL);
            Customer customer = new Customer(VALID_USER_ID, VALID_COMPANY_NAME, email);
            LocalDateTime beforeUpdate = LocalDateTime.now();

            customer.updateInformation(UPDATED_COMPANY_NAME, new Email(UPDATED_EMAIL));

            assertThat(customer.getCompanyName()).isEqualTo(UPDATED_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(new Email(UPDATED_EMAIL));
            assertThat(customer.getUpdatedAt()).isNotNull();
            assertThat(customer.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);

            Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should update information multiple times with different timestamps")
        void shouldUpdateInformationMultipleTimes() throws InterruptedException {
            Email email = new Email(VALID_EMAIL);
            Customer customer = new Customer(VALID_USER_ID, VALID_COMPANY_NAME, email);

            customer.updateInformation("First Update Corp", new Email("first@example.com"));
            LocalDateTime firstUpdate = customer.getUpdatedAt();

            Thread.sleep(10);

            customer.updateInformation("Second Update Corp", new Email("second@example.com"));
            LocalDateTime secondUpdate = customer.getUpdatedAt();

            assertThat(customer.getCompanyName()).isEqualTo("Second Update Corp");
            assertThat(customer.getContactEmail()).isEqualTo(new Email("second@example.com"));
            assertThat(secondUpdate).isAfter(firstUpdate);
        }

        @Test
        @DisplayName("Should not fail validation when updating information with null company name")
        void shouldNotFailValidationWithNullCompanyName() {
            Email email = new Email(VALID_EMAIL);
            Customer customer = new Customer(VALID_USER_ID, VALID_COMPANY_NAME, email);

            customer.updateInformation(null, new Email(UPDATED_EMAIL));

            Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
            assertThat(violations).isEmpty();
            assertThat(customer.getCompanyName()).isEqualTo(VALID_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(new Email(UPDATED_EMAIL));
        }

        @Test
        @DisplayName("Should not fail validation when updating information with null contact email")
        void shouldNotFailValidationWithNullContactEmail() {
            Email email = new Email(VALID_EMAIL);
            Customer customer = new Customer(VALID_USER_ID, VALID_COMPANY_NAME, email);

            customer.updateInformation(UPDATED_COMPANY_NAME, null);

            Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
            assertThat(violations).isEmpty();
            assertThat(customer.getCompanyName()).isEqualTo(UPDATED_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(new Email(VALID_EMAIL));
        }

        @Test
        @DisplayName("Should update information with same values")
        void shouldUpdateInformationWithSameValues() {
            Email email = new Email(VALID_EMAIL);
            Customer customer = new Customer(VALID_USER_ID, VALID_COMPANY_NAME, email);

            customer.updateInformation(VALID_COMPANY_NAME, email);

            assertThat(customer.getCompanyName()).isEqualTo(VALID_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(email);
            assertThat(customer.getUpdatedAt()).isNotNull();

            Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Timestamp Behavior Tests")
    class TimestampBehaviorTests {

        @Test
        @DisplayName("Should preserve createdAt when updating information")
        void shouldPreserveCreatedAtWhenUpdatingInformation() {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(10);
            Email email = new Email(VALID_EMAIL);
            Customer customer = Customer.builder().userId(VALID_USER_ID).companyName(VALID_COMPANY_NAME)
                    .contactEmail(email).createdAt(createdAt).updatedAt(createdAt).build();

            customer.updateInformation(UPDATED_COMPANY_NAME, new Email(UPDATED_EMAIL));

            assertThat(customer.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("Should update updatedAt when updating information")
        void shouldUpdateUpdatedAtWhenUpdatingInformation() {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
            LocalDateTime oldUpdatedAt = LocalDateTime.now().minusDays(1);
            Email email = new Email(VALID_EMAIL);
            Customer customer = Customer.builder().userId(VALID_USER_ID).companyName(VALID_COMPANY_NAME)
                    .contactEmail(email).createdAt(createdAt).updatedAt(oldUpdatedAt).build();

            LocalDateTime beforeUpdate = LocalDateTime.now();
            customer.updateInformation(UPDATED_COMPANY_NAME, new Email(UPDATED_EMAIL));

            assertThat(customer.getCreatedAt()).isEqualTo(createdAt);
            assertThat(customer.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
            assertThat(customer.getUpdatedAt()).isAfter(oldUpdatedAt);
        }
    }

    @Nested
    @DisplayName("Business Invariant Tests")
    class BusinessInvariantTests {

        @Test
        @DisplayName("Should maintain all properties after information update")
        void shouldMaintainAllPropertiesAfterInformationUpdate() {
            CustomerId id = new CustomerId(UUID.randomUUID());
            Email email = new Email(VALID_EMAIL);
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);

            Customer customer = Customer.builder().id(id).userId(VALID_USER_ID).companyName(VALID_COMPANY_NAME)
                    .contactEmail(email).createdAt(createdAt).updatedAt(createdAt).build();

            customer.updateInformation(UPDATED_COMPANY_NAME, new Email(UPDATED_EMAIL));

            assertThat(customer.getId()).isEqualTo(id);
            assertThat(customer.getUserId()).isEqualTo(VALID_USER_ID);
            assertThat(customer.getCreatedAt()).isEqualTo(createdAt);

            assertThat(customer.getCompanyName()).isEqualTo(UPDATED_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(new Email(UPDATED_EMAIL));
            assertThat(customer.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should verify customer belongs to user")
        void shouldVerifyCustomerBelongsToUser() {
            UserId userId = new UserId(UUID.randomUUID());
            Email email = new Email(VALID_EMAIL);
            Customer customer = new Customer(userId, VALID_COMPANY_NAME, email);

            boolean belongsToUser = customer.belongsToUser(userId);

            assertThat(belongsToUser).isTrue();
        }

        @Test
        @DisplayName("Should verify customer does not belong to different user")
        void shouldVerifyCustomerDoesNotBelongToDifferentUser() {
            UserId userId = new UserId(UUID.randomUUID());
            UserId differentUserId = new UserId(UUID.randomUUID());
            Email email = new Email(VALID_EMAIL);
            Customer customer = new Customer(userId, VALID_COMPANY_NAME, email);

            boolean belongsToUser = customer.belongsToUser(differentUserId);

            assertThat(belongsToUser).isFalse();
        }

        @Test
        @DisplayName("Should return false when checking null user id")
        void shouldReturnFalseWhenCheckingNullUserId() {
            UserId userId = new UserId(UUID.randomUUID());
            Email email = new Email(VALID_EMAIL);
            Customer customer = new Customer(userId, VALID_COMPANY_NAME, email);

            boolean belongsToUser = customer.belongsToUser(null);

            assertThat(belongsToUser).isFalse();
        }

        @Test
        @DisplayName("Should return false when customer has null user id")
        void shouldReturnFalseWhenCustomerHasNullUserId() {
            Email email = new Email(VALID_EMAIL);
            Customer customer = Customer.builder().userId(null).companyName(VALID_COMPANY_NAME).contactEmail(email)
                    .build();

            boolean belongsToUser = customer.belongsToUser(new UserId(UUID.randomUUID()));

            assertThat(belongsToUser).isFalse();
        }
    }
}
