package com.rvg.movieapi.auth.services;

import com.rvg.movieapi.auth.entities.RefreshToken;
import com.rvg.movieapi.auth.entities.User;
import com.rvg.movieapi.auth.entities.UserRole;
import com.rvg.movieapi.auth.repositories.UserRepository;
import com.rvg.movieapi.auth.utils.AuthResponse;
import com.rvg.movieapi.auth.utils.LoginRequest;
import com.rvg.movieapi.auth.utils.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service responsible for user authentication and registration.
 * <p>
 * Handles two core flows:
 * <ul>
 *   <li><b>Register:</b> creates a new user, encodes their password, generates
 *       a JWT access token and a refresh token.</li>
 *   <li><b>Login:</b> validates credentials via Spring Security's
 *       {@link AuthenticationManager}, then issues a new access token and
 *       refresh token for the authenticated user.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user and returns a token pair.
     * <p>
     * Steps:
     * <ol>
     *   <li>Build a {@link User} entity from the request, encoding the password with BCrypt.</li>
     *   <li>Persist the user via {@link UserRepository}.</li>
     *   <li>Generate a JWT access token for the saved user.</li>
     *   <li>Create a refresh token associated with the user's email.</li>
     *   <li>Return both tokens in an {@link AuthResponse}.</li>
     * </ol>
     *
     * @param registerRequest DTO containing name, email, username, and raw password
     * @return {@link AuthResponse} with accessToken and refreshToken
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        var user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(UserRole.USER)
                .build();

        User savedUser = userRepository.save(user);
        String accessToken = jwtService.generateToken(savedUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRefreshToken())
                .build();
    }

    /**
     * Authenticates an existing user and returns a token pair.
     * <p>
     * Steps:
     * <ol>
     *   <li>Delegate credential validation to {@link AuthenticationManager}.
     *       Throws {@link org.springframework.security.core.AuthenticationException}
     *       if credentials are invalid.</li>
     *   <li>Load the user from the database by email.</li>
     *   <li>Generate a new JWT access token.</li>
     *   <li>Create a new refresh token for the user.</li>
     *   <li>Return both tokens in an {@link AuthResponse}.</li>
     * </ol>
     *
     * @param loginRequest DTO containing email and raw password
     * @return {@link AuthResponse} with accessToken and refreshToken
     * @throws UsernameNotFoundException if the email does not exist in the database
     */
    public AuthResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        var user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with: " + loginRequest.getEmail()));
        var accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(loginRequest.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRefreshToken())
                .build();
    }
}
