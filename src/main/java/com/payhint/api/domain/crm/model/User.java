package com.payhint.api.domain.crm.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.payhint.api.domain.crm.valueobjects.Email;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class User {

    private UUID id;
    private Email email;
    @NotBlank
    private String password;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User(Email email, String password, String firstName, String lastName) {
        this.id = null;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = null;
        this.updatedAt = null;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void updateProfile(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.updatedAt = LocalDateTime.now();
    }
}
