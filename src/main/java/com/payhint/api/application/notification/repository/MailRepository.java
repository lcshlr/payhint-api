package com.payhint.api.application.notification.repository;

public interface MailRepository {
    void sendEmail(String to, String subject, String body);
}