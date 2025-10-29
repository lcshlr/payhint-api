package com.payhint.api.infrastructure.utils;

public class Normalize {
    public static String email(String email) {
        if (email != null) {
            email = email.toLowerCase();
        }
        return email;
    }
}
