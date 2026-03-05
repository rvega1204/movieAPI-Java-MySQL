package com.rvg.movieapi.auth.services;

import com.rvg.movieapi.auth.entities.RefreshToken;
import com.rvg.movieapi.auth.entities.User;
import com.rvg.movieapi.auth.entities.UserRole;
import com.rvg.movieapi.auth.repositories.UserRepository;
import com.rvg.movieapi.auth.utils.AuthResponse;
import com.rvg.movieapi.auth.utils.LoginRequest;
import com.rvg.movieapi.auth.utils.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    // -------------------------------------------------------------------------
    // register()
    // -------------------------------------------------------------------------

    @Test
    void register_savesUserWithEncodedPassword() {
        RegisterRequest request = buildRegisterRequest();
        User savedUser = buildUser();
        RefreshToken refreshToken = buildRefreshToken();

        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("access_token");
        when(refreshTokenService.createRefreshToken(savedUser.getEmail())).thenReturn(refreshToken);

        authService.register(request);

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_assignsUserRoleToNewUser() {
        RegisterRequest request = buildRegisterRequest();
        User savedUser = buildUser();
        RefreshToken refreshToken = buildRefreshToken();

        when(passwordEncoder.encode(any())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("access_token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);

        authService.register(request);

        verify(userRepository).save(argThat(user -> user.getRole() == UserRole.USER));
    }

    @Test
    void register_returnsAccessAndRefreshTokens() {
        RegisterRequest request = buildRegisterRequest();
        User savedUser = buildUser();
        RefreshToken refreshToken = buildRefreshToken();

        when(passwordEncoder.encode(any())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("access_token");
        when(refreshTokenService.createRefreshToken(savedUser.getEmail())).thenReturn(refreshToken);

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token_value");
    }

    @Test
    void register_createsRefreshTokenWithSavedUserEmail() {
        RegisterRequest request = buildRegisterRequest();
        User savedUser = buildUser();
        RefreshToken refreshToken = buildRefreshToken();

        when(passwordEncoder.encode(any())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("access_token");
        when(refreshTokenService.createRefreshToken(savedUser.getEmail())).thenReturn(refreshToken);

        authService.register(request);

        verify(refreshTokenService).createRefreshToken(savedUser.getEmail());
    }

    // -------------------------------------------------------------------------
    // login()
    // -------------------------------------------------------------------------

    @Test
    void login_authenticatesWithEmailAndPassword() {
        LoginRequest request = buildLoginRequest();
        User user = buildUser();
        RefreshToken refreshToken = buildRefreshToken();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access_token");
        when(refreshTokenService.createRefreshToken(request.getEmail())).thenReturn(refreshToken);

        authService.login(request);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
    }

    @Test
    void login_returnsAccessAndRefreshTokens() {
        LoginRequest request = buildLoginRequest();
        User user = buildUser();
        RefreshToken refreshToken = buildRefreshToken();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access_token");
        when(refreshTokenService.createRefreshToken(request.getEmail())).thenReturn(refreshToken);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token_value");
    }

    @Test
    void login_whenUserNotFound_throwsUsernameNotFoundException() {
        LoginRequest request = buildLoginRequest();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(request.getEmail());
    }

    @Test
    void login_whenBadCredentials_throwsBadCredentialsException() {
        LoginRequest request = buildLoginRequest();
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(userRepository, jwtService, refreshTokenService);
    }

    @Test
    void login_createsRefreshTokenWithLoginEmail() {
        LoginRequest request = buildLoginRequest();
        User user = buildUser();
        RefreshToken refreshToken = buildRefreshToken();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access_token");
        when(refreshTokenService.createRefreshToken(request.getEmail())).thenReturn(refreshToken);

        authService.login(request);

        verify(refreshTokenService).createRefreshToken(request.getEmail());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RegisterRequest buildRegisterRequest() {
        return RegisterRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .username("john_doe")
                .password("password123")
                .build();
    }

    private LoginRequest buildLoginRequest() {
        return LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();
    }

    private User buildUser() {
        return User.builder()
                .userId(1)
                .name("John Doe")
                .email("john@example.com")
                .username("john_doe")
                .password("encoded_password")
                .role(UserRole.USER)
                .build();
    }

    private RefreshToken buildRefreshToken() {
        return RefreshToken.builder()
                .refreshToken("refresh_token_value")
                .build();
    }
}