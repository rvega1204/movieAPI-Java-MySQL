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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    // -------------------------------------------------------------------------
    // register()
    // -------------------------------------------------------------------------

    @Test
    void register_returns200WithAuthResponse() {
        RegisterRequest request = buildRegisterRequest();
        AuthResponse expected = buildAuthResponse();
        when(authService.register(request)).thenReturn(expected);

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void register_delegatesToAuthService() {
        RegisterRequest request = buildRegisterRequest();
        when(authService.register(request)).thenReturn(buildAuthResponse());

        authController.register(request);

        verify(authService).register(request);
        verifyNoInteractions(refreshTokenService, jwtService);
    }

    // -------------------------------------------------------------------------
    // login()
    // -------------------------------------------------------------------------

    @Test
    void login_returns200WithAuthResponse() {
        LoginRequest request = buildLoginRequest();
        AuthResponse expected = buildAuthResponse();
        when(authService.login(request)).thenReturn(expected);

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void login_delegatesToAuthService() {
        LoginRequest request = buildLoginRequest();
        when(authService.login(request)).thenReturn(buildAuthResponse());

        authController.login(request);

        verify(authService).login(request);
        verifyNoInteractions(refreshTokenService, jwtService);
    }

    // -------------------------------------------------------------------------
    // refreshToken()
    // -------------------------------------------------------------------------

    @Test
    void refreshToken_returns200WithNewAccessToken() {
        User user = buildUser();
        RefreshToken refreshToken = buildRefreshToken(user);
        RefreshTokenRequest request = buildRefreshTokenRequest(refreshToken.getRefreshToken());

        when(refreshTokenService.verifyRefreshToken(refreshToken.getRefreshToken())).thenReturn(refreshToken);
        when(jwtService.generateToken(user)).thenReturn("new_access_token");

        ResponseEntity<AuthResponse> response = authController.refreshToken(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo("new_access_token");
    }

    @Test
    void refreshToken_returnsExistingRefreshToken() {
        User user = buildUser();
        RefreshToken refreshToken = buildRefreshToken(user);
        RefreshTokenRequest request = buildRefreshTokenRequest(refreshToken.getRefreshToken());

        when(refreshTokenService.verifyRefreshToken(refreshToken.getRefreshToken())).thenReturn(refreshToken);
        when(jwtService.generateToken(user)).thenReturn("new_access_token");

        ResponseEntity<AuthResponse> response = authController.refreshToken(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRefreshToken()).isEqualTo(refreshToken.getRefreshToken());
    }

    @Test
    void refreshToken_generatesTokenForRefreshTokenOwner() {
        User user = buildUser();
        RefreshToken refreshToken = buildRefreshToken(user);
        RefreshTokenRequest request = buildRefreshTokenRequest(refreshToken.getRefreshToken());

        when(refreshTokenService.verifyRefreshToken(refreshToken.getRefreshToken())).thenReturn(refreshToken);
        when(jwtService.generateToken(user)).thenReturn("new_access_token");

        authController.refreshToken(request);

        verify(jwtService).generateToken(user);
    }

    @Test
    void refreshToken_whenTokenIsInvalid_throwsRuntimeException() {
        RefreshTokenRequest request = buildRefreshTokenRequest("expired-token");
        when(refreshTokenService.verifyRefreshToken("expired-token"))
                .thenThrow(new RuntimeException("Refresh token expired"));

        assertThatThrownBy(() -> authController.refreshToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");

        verifyNoInteractions(jwtService);
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

    private RefreshTokenRequest buildRefreshTokenRequest(String token) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(token);
        return request;
    }

    private AuthResponse buildAuthResponse() {
        return AuthResponse.builder()
                .accessToken("access_token")
                .refreshToken("refresh_token_value")
                .build();
    }

    private User buildUser() {
        return User.builder()
                .userId(1)
                .name("John Doe")
                .email("john@example.com")
                .username("john_doe")
                .password("encoded_password")
                .build();
    }

    private RefreshToken buildRefreshToken(User user) {
        return RefreshToken.builder()
                .id(1)
                .refreshToken("test-refresh-token")
                .expirationTime(Instant.now().plusSeconds(3600))
                .user(user)
                .build();
    }
}