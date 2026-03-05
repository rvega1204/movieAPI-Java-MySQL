package com.rvg.movieapi.auth.services;

import com.rvg.movieapi.auth.entities.RefreshToken;
import com.rvg.movieapi.auth.entities.User;
import com.rvg.movieapi.auth.repositories.RefreshTokenRepository;
import com.rvg.movieapi.auth.repositories.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service responsible for creating and validating JWT refresh tokens.
 * <p>
 * Refresh tokens are long-lived credentials that allow clients to obtain
 * new access tokens without re-authenticating. Each user holds at most
 * one active refresh token — if one already exists it is reused,
 * otherwise a new one is created and persisted.
 */
@Service
public class RefreshTokenService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Returns the existing refresh token for the given user, or creates a new one.
     * <p>
     * If the user already has a refresh token (via the {@code refreshToken} field
     * on the {@link User} entity), it is returned as-is. Otherwise, a new token
     * is generated with a validity of 50 hours, persisted, and returned.
     *
     * @param username the user's email address used as the lookup key
     * @return the active {@link RefreshToken} for the user
     * @throws UsernameNotFoundException if no user exists with the given email
     */
    public RefreshToken createRefreshToken(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with: " + username));

        RefreshToken refreshToken = user.getRefreshToken();
        if (refreshToken == null) {
            long refreshTokenValidity = 5 * 60 * 60 * 10000;
            refreshToken = RefreshToken.builder()
                    .refreshToken(UUID.randomUUID().toString())
                    .expirationTime(Instant.now().plusMillis(refreshTokenValidity))
                    .user(user)
                    .build();

            refreshTokenRepository.save(refreshToken);
        }

        return refreshToken;
    }

    /**
     * Validates the given refresh token string and returns the corresponding entity.
     * <p>
     * If the token has expired, it is deleted from the database before throwing
     * an exception, ensuring stale tokens are cleaned up immediately.
     *
     * @param refreshToken the raw refresh token string to look up and validate
     * @return the valid {@link RefreshToken} entity
     * @throws RuntimeException if the token is not found or has expired
     */
    public RefreshToken verifyRefreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (token.getExpirationTime().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token expired");
        }

        return token;
    }

}
