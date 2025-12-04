package com.payhint.api.infrastructure.notification.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceAdapter Tests")
class EmailServiceAdapterTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailServiceAdapter emailServiceAdapter;

    @Test
    @DisplayName("Should send simple message successfully")
    void shouldSendSimpleMessage() {
        String to = "user@example.com";
        String subject = "Test Subject";
        String body = "Test Body";

        emailServiceAdapter.sendEmail(to, subject, body);

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should propagate exception when sending fails")
    void shouldPropagateException() {
        doThrow(new MailSendException("Mail server unavailable")).when(javaMailSender)
                .send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailServiceAdapter.sendEmail("to", "sub", "txt"))
                .isInstanceOf(MailSendException.class);
    }
}