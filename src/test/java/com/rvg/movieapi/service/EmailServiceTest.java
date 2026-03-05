package com.rvg.movieapi.service;

import com.rvg.movieapi.dto.MailBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Inject manually to guarantee javaMailSender goes to the right field
        emailService = new EmailService(javaMailSender, javaMailSender);
    }

    // -------------------------------------------------------------------------
    // sendSimpleMessage()
    // -------------------------------------------------------------------------

    @Test
    void sendSimpleMessage_sendsToCorrectRecipient() {
        MailBody mailBody = buildMailBody();
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendSimpleMessage(mailBody);

        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("user@example.com");
    }

    @Test
    void sendSimpleMessage_setsCorrectSubject() {
        MailBody mailBody = buildMailBody();
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendSimpleMessage(mailBody);

        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("OTP for Forgot Password request");
    }

    @Test
    void sendSimpleMessage_setsCorrectText() {
        MailBody mailBody = buildMailBody();
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendSimpleMessage(mailBody);

        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).isEqualTo("Your OTP is: 123456");
    }

    @Test
    void sendSimpleMessage_setsFixedSenderAddress() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendSimpleMessage(buildMailBody());

        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo("YOUR_EMAIL_COM");
    }

    @Test
    void sendSimpleMessage_delegatesToJavaMailSender() {
        emailService.sendSimpleMessage(buildMailBody());

        verify(javaMailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MailBody buildMailBody() {
        return MailBody.builder()
                .to("user@example.com")
                .subject("OTP for Forgot Password request")
                .text("Your OTP is: 123456")
                .build();
    }
}