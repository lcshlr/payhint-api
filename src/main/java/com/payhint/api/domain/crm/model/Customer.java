package com.payhint.api.domain.crm.model;

import java.time.LocalDateTime;

import com.payhint.api.domain.crm.valueobjects.CustomerId;
import com.payhint.api.domain.crm.valueobjects.Email;
import com.payhint.api.domain.crm.valueobjects.UserId;

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
        this.id = null;
        this.userId = userId;
        this.companyName = companyName;
        this.contactEmail = contactEmail;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateInformation(String companyName, Email contactEmail) {
        if (companyName != null) {
            this.companyName = companyName;
        }
        if (contactEmail != null && contactEmail.value() != null) {
            this.contactEmail = contactEmail;
        }
        if (companyName != null || contactEmail != null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    public boolean belongsToUser(UserId userId) {
        if (this.userId == null || userId == null) {
            return false;
        }
        return this.userId.equals(userId);
    }
}