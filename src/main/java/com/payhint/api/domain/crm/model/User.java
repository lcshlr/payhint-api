package com.payhint.api.domain.crm.model;

import java.time.LocalDateTime;

import com.payhint.api.domain.crm.valueobjects.Email;
import com.payhint.api.domain.crm.valueobjects.UserId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class User {

    private UserId id;
    private Email email;
    private String password;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User(Email email, String password, String firstName, String lastName) {
        if (email == null) {
            throw new InvalidPropertyException("Email cannot be null");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new InvalidPropertyException("First name cannot be empty");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new InvalidPropertyException("Last name cannot be empty");
        }
        ensurePasswordIsValid(password);
        this.id = null;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private void ensurePasswordIsValid(String password) {
        if (password == null || password.isBlank()) {
            throw new InvalidPropertyException("Password cannot be empty");
        }
    }

    public void changePassword(String password) {
        ensurePasswordIsValid(password);
        this.password = password;
    }

    public void ensureNotBlankIfProvided(String fieldName, String value) {
        if (value != null && value.isBlank()) {
            throw new InvalidPropertyException(fieldName + " cannot be empty");
        }
    }

    public void updateProfile(Email email, String firstName, String lastName) {
        boolean isUpdated = false;

        ensureNotBlankIfProvided("First name", firstName);
        ensureNotBlankIfProvided("Last name", lastName);

        if (firstName != null) {
            this.firstName = firstName;
            isUpdated = true;
        }

        if (lastName != null) {
            this.lastName = lastName;
            isUpdated = true;
        }

        if (email != null) {
            this.email = email;
            isUpdated = true;
        }
        if (isUpdated) {
            this.updatedAt = LocalDateTime.now();
        }
    }
}
