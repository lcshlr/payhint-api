package com.payhint.api.domain.crm.model;

import java.time.LocalDateTime;

import com.payhint.api.domain.crm.valueobjects.CustomerId;
import com.payhint.api.domain.crm.valueobjects.Email;
import com.payhint.api.domain.crm.valueobjects.UserId;
import com.payhint.api.domain.shared.exception.InvalidPropertyException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class Customer {
    private CustomerId id;
    private UserId userId;
    private String companyName;
    private Email contactEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Customer(UserId userId, String companyName, Email contactEmail) {
        if (userId == null) {
            throw new InvalidPropertyException("UserId cannot be null");
        }
        if (companyName == null || companyName.isBlank()) {
            throw new InvalidPropertyException("Company name cannot be null or empty");
        }
        if (contactEmail == null) {
            throw new InvalidPropertyException("Contact email cannot be null");
        }
        this.id = null;
        this.userId = userId;
        this.companyName = companyName;
        this.contactEmail = contactEmail;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateInformation(String companyName, Email contactEmail) {
        boolean isUpdated = false;

        if (companyName != null && companyName.isBlank()) {
            throw new InvalidPropertyException("Company name cannot be empty");
        }

        if (companyName != null) {
            this.companyName = companyName;
            isUpdated = true;
        }
        if (contactEmail != null) {
            this.contactEmail = contactEmail;
            isUpdated = true;
        }
        if (isUpdated) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    public boolean belongsToUser(UserId userId) {
        return this.userId != null && this.userId.equals(userId);
    }
}