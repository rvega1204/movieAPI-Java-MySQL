package com.rvg.movieapi.auth.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Entity representing a forgot-password request.
 * <p>
 * Stores the one-time password (OTP) and its expiration time
 * associated with a user who requested a password reset.
 * Each user can have at most one active forgot-password record
 * due to the {@code @OneToOne} relationship.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ForgotPassword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer fpid;

    @Column(nullable = false)
    private Integer otp;

    @Column(nullable = false)
    private Date expirationTime;

    @OneToOne
    private User user;
}
