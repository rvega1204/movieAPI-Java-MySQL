package com.rvg.movieapi.controllers;

import com.rvg.movieapi.auth.entities.ForgotPassword;
import com.rvg.movieapi.auth.entities.User;
import com.rvg.movieapi.auth.repositories.ForgotPasswordRepository;
import com.rvg.movieapi.auth.repositories.UserRepository;
import com.rvg.movieapi.auth.utils.ChangePassword;
import com.rvg.movieapi.dto.MailBody;
import com.rvg.movieapi.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

/**
 * REST controller for the forgot-password flow.
 * <p>
 * Exposes three endpoints under {@code /forgotPassword}:
 * <ul>
 *   <li>{@code POST /verifyMail} — sends an OTP to the user's email</li>
 *   <li>{@code POST /verifyOtp/{otp}/{email}} — validates the OTP</li>
 *   <li>{@code POST /changePassword/{email}} — updates the user's password</li>
 * </ul>
 * All endpoints are publicly accessible (configured in {@code SecurityConfiguration}).
 */
@RestController
@RequestMapping("/forgotPassword")
public class ForgotPasswordController {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final PasswordEncoder passwordEncoder;

    public ForgotPasswordController(UserRepository userRepository, EmailService emailService, ForgotPasswordRepository forgotPasswordRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.forgotPasswordRepository = forgotPasswordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Sends a one-time password (OTP) to the given email address.
     * <p>
     * Generates a 6-digit OTP valid for 70 seconds, persists a {@link ForgotPassword}
     * record, and sends the OTP via email.
     *
     * @param email the email address of the user requesting a password reset
     * @return 200 OK if the OTP was sent successfully
     * @throws UsernameNotFoundException if no user is found with the given email
     */
    @PostMapping("/verifyMail")
    public ResponseEntity<String> verifyMail(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Please provide a valid email"));

        int otp = otpGenerator();

        MailBody mailBody = MailBody.builder()
                .to(email)
                .text("This the OTP for your Forgot Password request: " + otp)
                .subject("OTP for Forgot Password request")
                .build();

        ForgotPassword fp = ForgotPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + 70 * 1000))
                .user(user)
                .build();

        emailService.sendSimpleMessage(mailBody);
        forgotPasswordRepository.save(fp);

        return ResponseEntity.ok("OTP has been sent");
    }

    /**
     * Validates the OTP submitted by the user for the given email.
     * <p>
     * If the OTP has expired, it is deleted from the database and a
     * 417 Expectation Failed response is returned. If the OTP is invalid
     * or does not match the user, a {@link RuntimeException} is thrown.
     *
     * @param otp   the one-time password to verify
     * @param email the email address associated with the OTP
     * @return 200 OK if the OTP is valid and not expired,
     * or 417 if the OTP has expired
     * @throws UsernameNotFoundException if no user is found with the given email
     * @throws RuntimeException          if no matching OTP is found for the user
     */
    @PostMapping("/verifyOtp/{otp}/{email}")
    public ResponseEntity<String> verifyOtp(@PathVariable Integer otp, @PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Please provide a valid email"));

        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                .orElseThrow(() -> new RuntimeException("Invalid OTP for email: " + email));

        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteById(fp.getFpid());
            return new ResponseEntity<>("OTP has expired!", HttpStatus.EXPECTATION_FAILED);
        }

        return ResponseEntity.ok("OTP verified");
    }

    /**
     * Updates the password for the given email address.
     * <p>
     * Validates that {@code password} and {@code repeatPassword} match before
     * encoding and persisting the new password. Returns 417 if they do not match.
     *
     * @param changePassword record containing the new password and its confirmation
     * @param email          the email address of the user whose password will be changed
     * @return 200 OK if the password was changed successfully,
     * or 417 if the passwords do not match
     */
    @PostMapping("/changePassword/{email}")
    public ResponseEntity<String> changePassword(@RequestBody ChangePassword changePassword, @PathVariable String email) {
        if (!Objects.equals(changePassword.password(), changePassword.repeatPassword())) {
            return new ResponseEntity<>("Passwords do not match!", HttpStatus.EXPECTATION_FAILED);
        }

        String encodedPassword = passwordEncoder.encode(changePassword.password());
        userRepository.updatePassword(email, encodedPassword);

        return ResponseEntity.ok("Password has been changed!");
    }

    /**
     * Generates a random 6-digit OTP in the range [100000, 999999].
     *
     * @return a random integer OTP
     */
    private int otpGenerator() {
        Random random = new Random();
        return random.nextInt(100_000, 999_999);
    }
}
