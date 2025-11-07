package com.payhint.api.infrastructure.shared.utils;

public class Normalize {
    public static String email(String email) {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        return email;
    }
}
