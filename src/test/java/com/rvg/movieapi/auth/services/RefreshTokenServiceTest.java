package com.rvg.movieapi.auth.services;

import com.rvg.movieapi.auth.entities.RefreshToken;
import com.rvg.movieapi.auth.entities.User;
import com.rvg.movieapi.auth.repositories.RefreshTokenRepository;
import com.rvg.movieapi.auth.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    // -------------------------------------------------------------------------
    // createRefreshToken()
    // -------------------------------------------------------------------------

    @Test
    void createRefreshToken_whenUserNotFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.createRefreshToken("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown@example.com");
    }

    @Test
    void createRefreshToken_whenUserHasNoToken_createsAndSavesNewToken() {
        User user = buildUser(null);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        RefreshToken result = refreshTokenService.createRefreshToken(user.getEmail());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertThat(result.getRefreshToken()).isNotBlank();
    }

    @Test
    void createRefreshToken_whenUserHasNoToken_setsExpirationInFuture() {
        User user = buildUser(null);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        RefreshToken result = refreshTokenService.createRefreshToken(user.getEmail());

        assertThat(result.getExpirationTime()).isAfter(Instant.now());
    }

    @Test
    void createRefreshToken_whenUserHasNoToken_associatesTokenWithUser() {
        User user = buildUser(null);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        RefreshToken result = refreshTokenService.createRefreshToken(user.getEmail());

        assertThat(result.getUser()).isEqualTo(user);
    }

    @Test
    void createRefreshToken_whenUserAlreadyHasToken_returnsExistingTokenWithoutSaving() {
        RefreshToken existingToken = buildRefreshToken(Instant.now().plusSeconds(3600));
        User user = buildUser(existingToken);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        RefreshToken result = refreshTokenService.createRefreshToken(user.getEmail());

        assertThat(result).isEqualTo(existingToken);
        verifyNoInteractions(refreshTokenRepository);
    }

    // -------------------------------------------------------------------------
    // verifyRefreshToken()
    // -------------------------------------------------------------------------

    @Test
    void verifyRefreshToken_whenTokenNotFound_throwsRuntimeException() {
        when(refreshTokenRepository.findByRefreshToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("invalid-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void verifyRefreshToken_whenTokenIsValid_returnsToken() {
        RefreshToken token = buildRefreshToken(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByRefreshToken(token.getRefreshToken()))
                .thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.verifyRefreshToken(token.getRefreshToken());

        assertThat(result).isEqualTo(token);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verifyRefreshToken_whenTokenIsExpired_deletesTokenAndThrows() {
        RefreshToken expiredToken = buildRefreshToken(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByRefreshToken(expiredToken.getRefreshToken()))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken(expiredToken.getRefreshToken()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    void verifyRefreshToken_whenTokenIsExpired_doesNotReturnToken() {
        RefreshToken expiredToken = buildRefreshToken(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByRefreshToken(expiredToken.getRefreshToken()))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken(expiredToken.getRefreshToken()))
                .isInstanceOf(RuntimeException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User buildUser(RefreshToken refreshToken) {
        return User.builder()
                .userId(1)
                .name("John Doe")
                .email("john@example.com")
                .username("john_doe")
                .password("encoded_password")
                .refreshToken(refreshToken)
                .build();
    }

    private RefreshToken buildRefreshToken(Instant expirationTime) {
        return RefreshToken.builder()
                .id(1)
                .refreshToken("test-refresh-token-uuid")
                .expirationTime(expirationTime)
                .build();
    }
}