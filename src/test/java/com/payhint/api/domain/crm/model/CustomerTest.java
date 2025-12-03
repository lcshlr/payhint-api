package com.payhint.api.domain.crm.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.payhint.api.domain.crm.valueobject.CustomerId;
import com.payhint.api.domain.crm.valueobject.UserId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;
import com.payhint.api.domain.shared.valueobject.Email;

@DisplayName("Customer Domain Model Tests")
class CustomerTest {
    private static final CustomerId VALID_CUSTOMER_ID = new CustomerId(UUID.randomUUID());
    private static final UserId VALID_USER_ID = new UserId(UUID.randomUUID());
    private static final String VALID_COMPANY_NAME = "Acme Corporation";
    private static final String VALID_EMAIL = "john.doe@example.com";
    private static final String UPDATED_COMPANY_NAME = "Updated Corporation";
    private static final String UPDATED_EMAIL = "jane.smith@example.com";

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create customer with valid parameters using simple constructor")
        void shouldCreateCustomerWithValidParameters() {
            Email email = new Email(VALID_EMAIL);

            Customer customer = Customer.create(VALID_CUSTOMER_ID, VALID_USER_ID, VALID_COMPANY_NAME, email);

            assertThat(customer).isNotNull();
            assertThat(customer.getId()).isEqualTo(VALID_CUSTOMER_ID);
            assertThat(customer.getUserId()).isEqualTo(VALID_USER_ID);
            assertThat(customer.getCompanyName()).isEqualTo(VALID_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(email);
            assertThat(customer.getCreatedAt()).isNotNull();
            assertThat(customer.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create customer with all fields")
        void shouldCreateCustomerUsingBuilder() {
            CustomerId id = new CustomerId(UUID.randomUUID());
            Email email = new Email(VALID_EMAIL);
            LocalDateTime now = LocalDateTime.now();

            Customer customer = new Customer(id, VALID_USER_ID, VALID_COMPANY_NAME, email, now, now);

            assertThat(customer).isNotNull();
            assertThat(customer.getId()).isEqualTo(id);
            assertThat(customer.getUserId()).isEqualTo(VALID_USER_ID);
            assertThat(customer.getCompanyName()).isEqualTo(VALID_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(email);
            assertThat(customer.getCreatedAt()).isEqualTo(now);
            assertThat(customer.getUpdatedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("Informations Update Tests")
    class InformationsUpdateTests {

        @Test
        @DisplayName("Should update informations with valid company name and contact email")
        void shouldUpdateInformationsWithValidData() {
            Email email = new Email(VALID_EMAIL);
            Customer customer = Customer.create(VALID_CUSTOMER_ID, VALID_USER_ID, VALID_COMPANY_NAME, email);
            LocalDateTime beforeUpdate = LocalDateTime.now();

            customer.updateInformation(UPDATED_COMPANY_NAME, new Email(UPDATED_EMAIL));

            assertThat(customer.getCompanyName()).isEqualTo(UPDATED_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(new Email(UPDATED_EMAIL));
            assertThat(customer.getUpdatedAt()).isNotNull();
            assertThat(customer.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
        }

        @Test
        @DisplayName("Should update informations and set updatedAt timestamp")
        void shouldSetUpdatedAtTimestamp() {
            Email email = new Email(VALID_EMAIL);
            Customer customer = Customer.create(VALID_CUSTOMER_ID, VALID_USER_ID, VALID_COMPANY_NAME, email);
            LocalDateTime beforeUpdate = LocalDateTime.now();

            customer.updateInformation(UPDATED_COMPANY_NAME, new Email(UPDATED_EMAIL));

            assertThat(customer.getCompanyName()).isEqualTo(UPDATED_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(new Email(UPDATED_EMAIL));
            assertThat(customer.getUpdatedAt()).isNotNull();
            assertThat(customer.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
        }

        @Test
        @DisplayName("Should update information multiple times with different timestamps")
        void shouldUpdateInformationMultipleTimes() throws InterruptedException {
            Email email = new Email(VALID_EMAIL);
            Customer customer = Customer.create(VALID_CUSTOMER_ID, VALID_USER_ID, VALID_COMPANY_NAME, email);

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
            Customer customer = Customer.create(VALID_CUSTOMER_ID, VALID_USER_ID, VALID_COMPANY_NAME, email);

            customer.updateInformation(null, new Email(UPDATED_EMAIL));
            assertThat(customer.getCompanyName()).isEqualTo(VALID_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(new Email(UPDATED_EMAIL));
        }

        @Test
        @DisplayName("Should not fail validation when updating information with null contact email")
        void shouldNotFailValidationWithNullContactEmail() {
            Email email = new Email(VALID_EMAIL);
            Customer customer = Customer.create(VALID_CUSTOMER_ID, VALID_USER_ID, VALID_COMPANY_NAME, email);

            customer.updateInformation(UPDATED_COMPANY_NAME, null);
            assertThat(customer.getCompanyName()).isEqualTo(UPDATED_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(new Email(VALID_EMAIL));
        }

        @Test
        @DisplayName("Should update information with same values")
        void shouldUpdateInformationWithSameValues() {
            Email email = new Email(VALID_EMAIL);
            Customer customer = Customer.create(VALID_CUSTOMER_ID, VALID_USER_ID, VALID_COMPANY_NAME, email);

            customer.updateInformation(VALID_COMPANY_NAME, email);

            assertThat(customer.getCompanyName()).isEqualTo(VALID_COMPANY_NAME);
            assertThat(customer.getContactEmail()).isEqualTo(email);
            assertThat(customer.getUpdatedAt()).isNotNull();
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
            CustomerId id = new CustomerId(UUID.randomUUID());
            Customer customer = new Customer(id, VALID_USER_ID, VALID_COMPANY_NAME, email, createdAt, createdAt);
            customer.updateInformation(UPDATED_COMPANY_NAME, new Email(UPDATED_EMAIL));

            assertThat(customer.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("Should update updatedAt when updating information")
        void shouldUpdateUpdatedAtWhenUpdatingInformation() {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
            LocalDateTime oldUpdatedAt = LocalDateTime.now().minusDays(1);
            CustomerId id = new CustomerId(UUID.randomUUID());
            Email email = new Email(VALID_EMAIL);
            Customer customer = new Customer(id, VALID_USER_ID, VALID_COMPANY_NAME, email, createdAt, oldUpdatedAt);

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

            Customer customer = new Customer(id, VALID_USER_ID, VALID_COMPANY_NAME, email, createdAt, createdAt);

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
            Customer customer = Customer.create(VALID_CUSTOMER_ID, userId, VALID_COMPANY_NAME, email);

            boolean belongsToUser = customer.belongsToUser(userId);

            assertThat(belongsToUser).isTrue();
        }

        @Test
        @DisplayName("Should verify customer does not belong to different user")
        void shouldVerifyCustomerDoesNotBelongToDifferentUser() {
            UserId userId = new UserId(UUID.randomUUID());
            UserId differentUserId = new UserId(UUID.randomUUID());
            Email email = new Email(VALID_EMAIL);
            Customer customer = Customer.create(VALID_CUSTOMER_ID, userId, VALID_COMPANY_NAME, email);

            boolean belongsToUser = customer.belongsToUser(differentUserId);

            assertThat(belongsToUser).isFalse();
        }

        @Test
        @DisplayName("Should return false when checking null user id")
        void shouldReturnFalseWhenCheckingNullUserId() {
            UserId userId = new UserId(UUID.randomUUID());
            Email email = new Email(VALID_EMAIL);
            Customer customer = Customer.create(VALID_CUSTOMER_ID, userId, VALID_COMPANY_NAME, email);

            boolean belongsToUser = customer.belongsToUser(null);

            assertThat(belongsToUser).isFalse();
        }

        @Test
        @DisplayName("Should throw exception when customer has null user id")
        void shouldThrowExceptionWhenCustomerHasNullUserId() {
            Email email = new Email(VALID_EMAIL);
            assertThatThrownBy(() -> Customer.create(VALID_CUSTOMER_ID, new UserId(null), VALID_COMPANY_NAME, email))
                    .isInstanceOf(InvalidPropertyException.class).hasMessage("ID cannot be null");
        }
    }
}
