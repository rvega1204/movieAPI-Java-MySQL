package com.rvg.movieapi.controllers;

import com.rvg.movieapi.auth.entities.ForgotPassword;
import com.rvg.movieapi.auth.entities.User;
import com.rvg.movieapi.auth.repositories.ForgotPasswordRepository;
import com.rvg.movieapi.auth.repositories.UserRepository;
import com.rvg.movieapi.auth.utils.ChangePassword;
import com.rvg.movieapi.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private ForgotPasswordRepository forgotPasswordRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ForgotPasswordController forgotPasswordController;

    // -------------------------------------------------------------------------
    // verifyMail()
    // -------------------------------------------------------------------------

    @Test
    void verifyMail_whenUserExists_returns200() {
        User user = buildUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        ResponseEntity<String> response = forgotPasswordController.verifyMail(user.getEmail());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OTP has been sent");
    }

    @Test
    void verifyMail_whenUserExists_sendsEmailAndSavesOtp() {
        User user = buildUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        forgotPasswordController.verifyMail(user.getEmail());

        verify(emailService).sendSimpleMessage(any());
        verify(forgotPasswordRepository).save(any(ForgotPassword.class));
    }

    @Test
    void verifyMail_whenUserNotFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> forgotPasswordController.verifyMail("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void verifyMail_whenUserNotFound_doesNotSendEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        try {
            forgotPasswordController.verifyMail("unknown@example.com");
        } catch (Exception ignored) {
        }

        verifyNoInteractions(emailService, forgotPasswordRepository);
    }

    // -------------------------------------------------------------------------
    // verifyOtp()
    // -------------------------------------------------------------------------

    @Test
    void verifyOtp_whenOtpIsValidAndNotExpired_returns200() {
        User user = buildUser();
        ForgotPassword fp = buildForgotPassword(user, futureDate());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findByOtpAndUser(123456, user)).thenReturn(Optional.of(fp));

        ResponseEntity<String> response = forgotPasswordController.verifyOtp(123456, user.getEmail());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OTP verified");
    }

    @Test
    void verifyOtp_whenOtpIsExpired_returns417AndDeletesRecord() {
        User user = buildUser();
        ForgotPassword fp = buildForgotPassword(user, pastDate());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findByOtpAndUser(123456, user)).thenReturn(Optional.of(fp));

        ResponseEntity<String> response = forgotPasswordController.verifyOtp(123456, user.getEmail());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.EXPECTATION_FAILED);
        assertThat(response.getBody()).isEqualTo("OTP has expired!");
        verify(forgotPasswordRepository).deleteById(fp.getFpid());
    }

    @Test
    void verifyOtp_whenOtpIsExpired_doesNotReturnOk() {
        User user = buildUser();
        ForgotPassword fp = buildForgotPassword(user, pastDate());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findByOtpAndUser(123456, user)).thenReturn(Optional.of(fp));

        ResponseEntity<String> response = forgotPasswordController.verifyOtp(123456, user.getEmail());

        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
    }

    @Test
    void verifyOtp_whenOtpNotFound_throwsRuntimeException() {
        User user = buildUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findByOtpAndUser(999999, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> forgotPasswordController.verifyOtp(999999, user.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(user.getEmail());
    }

    @Test
    void verifyOtp_whenUserNotFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> forgotPasswordController.verifyOtp(123456, "unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // changePassword()
    // -------------------------------------------------------------------------

    @Test
    void changePassword_whenPasswordsMatch_returns200() {
        ChangePassword changePassword = new ChangePassword("newPass123", "newPass123");
        when(passwordEncoder.encode("newPass123")).thenReturn("encoded");

        ResponseEntity<String> response = forgotPasswordController.changePassword(
                changePassword, "john@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Password has been changed!");
    }

    @Test
    void changePassword_whenPasswordsMatch_encodesAndUpdates() {
        ChangePassword changePassword = new ChangePassword("newPass123", "newPass123");
        when(passwordEncoder.encode("newPass123")).thenReturn("encoded");

        forgotPasswordController.changePassword(changePassword, "john@example.com");

        verify(passwordEncoder).encode("newPass123");
        verify(userRepository).updatePassword("john@example.com", "encoded");
    }

    @Test
    void changePassword_whenPasswordsDoNotMatch_returns417() {
        ChangePassword changePassword = new ChangePassword("newPass123", "differentPass");

        ResponseEntity<String> response = forgotPasswordController.changePassword(
                changePassword, "john@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.EXPECTATION_FAILED);
        assertThat(response.getBody()).isEqualTo("Passwords do not match!");
    }

    @Test
    void changePassword_whenPasswordsDoNotMatch_doesNotUpdateRepository() {
        ChangePassword changePassword = new ChangePassword("newPass123", "differentPass");

        forgotPasswordController.changePassword(changePassword, "john@example.com");

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).updatePassword(any(), any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User buildUser() {
        return User.builder()
                .userId(1)
                .name("John Doe")
                .email("john@example.com")
                .username("john_doe")
                .password("encoded_password")
                .build();
    }

    private ForgotPassword buildForgotPassword(User user, Date expirationTime) {
        return ForgotPassword.builder()
                .fpid(1)
                .otp(123456)
                .expirationTime(expirationTime)
                .user(user)
                .build();
    }

    private Date futureDate() {
        return new Date(System.currentTimeMillis() + 70_000);
    }

    private Date pastDate() {
        return new Date(System.currentTimeMillis() - 70_000);
    }
}