package com.rvg.movieapi.controllers;

import com.rvg.movieapi.auth.entities.RefreshToken;
import com.rvg.movieapi.auth.entities.User;
import com.rvg.movieapi.auth.services.AuthService;
import com.rvg.movieapi.auth.services.JwtService;
import com.rvg.movieapi.auth.services.RefreshTokenService;
import com.rvg.movieapi.auth.utils.AuthResponse;
import com.rvg.movieapi.auth.utils.LoginRequest;
import com.rvg.movieapi.auth.utils.RefreshTokenRequest;
import com.rvg.movieapi.auth.utils.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller handling authentication endpoints.
 * <p>
 * Exposes three public endpoints under {@code /api/v1/auth/}:
 * <ul>
 *   <li>{@code POST /register} — creates a new user account</li>
 *   <li>{@code POST /login} — authenticates an existing user</li>
 *   <li>{@code POST /refresh} — issues a new access token from a valid refresh token</li>
 * </ul>
 * All endpoints return an {@link AuthResponse} containing the access token
 * and the refresh token.
 */
@RestController
@RequestMapping("/api/v1/auth/")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService, JwtService jwtService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user and returns a token pair.
     *
     * @param registerRequest DTO containing name, email, username, and raw password
     * @return 200 OK with {@link AuthResponse} containing accessToken and refreshToken
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    /**
     * Authenticates an existing user and returns a token pair.
     *
     * @param loginRequest DTO containing email and raw password
     * @return 200 OK with {@link AuthResponse} containing accessToken and refreshToken
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    /**
     * Issues a new access token using a valid refresh token.
     * <p>
     * Validates the refresh token, retrieves the associated user, and
     * generates a fresh JWT access token. The refresh token itself is
     * not rotated — the same one is returned alongside the new access token.
     *
     * @param refreshTokenRequest DTO containing the refresh token string
     * @return 200 OK with {@link AuthResponse} containing the new accessToken and the same refreshToken
     * @throws RuntimeException if the refresh token is not found or has expired
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenRequest.getRefreshToken());
        User user = refreshToken.getUser();
        String accessToken = jwtService.generateToken(user);

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRefreshToken())
                .build());
    }
}
