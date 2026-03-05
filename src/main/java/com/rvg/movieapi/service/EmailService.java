package com.rvg.movieapi.service;

import com.rvg.movieapi.dto.MailBody;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending plain-text emails.
 * <p>
 * Uses Spring's {@link JavaMailSender} to dispatch {@link SimpleMailMessage}
 * instances. The sender address is fixed to the application's configured
 * email account.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final JavaMailSender javaMailSender;

    public EmailService(JavaMailSender mailSender, JavaMailSender javaMailSender) {
        this.mailSender = mailSender;
        this.javaMailSender = javaMailSender;
    }

    /**
     * Sends a plain-text email using the data provided in the {@link MailBody}.
     * <p>
     * Sets the recipient, subject, body text, and a fixed sender address,
     * then dispatches the message via {@link JavaMailSender}.
     *
     * @param mailBody record containing the recipient address, subject, and message text
     */
    public void sendSimpleMessage(MailBody mailBody) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(mailBody.to());
        message.setSubject(mailBody.subject());
        message.setText(mailBody.text());
        message.setFrom("YOUR_EMAIL_COM");

        javaMailSender.send(message);
    }
}
