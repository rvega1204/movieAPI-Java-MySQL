package com.rvg.movieapi.auth.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    // 512-bit Base64-encoded key — valid for HMAC-SHA512
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhNQUMtU0hBLTUxMg==";
    private static final long EXPIRATION_MS = 1000 * 60 * 60; // 1 hour

    @InjectMocks
    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", EXPIRATION_MS);
    }

    // -------------------------------------------------------------------------
    // generateToken() + extractUsername()
    // -------------------------------------------------------------------------

    @Test
    void generateToken_embeddedUsernameIsExtractable() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userDetails.getAuthorities()).thenReturn(java.util.List.of());

        String token = jwtService.generateToken(userDetails);
        String extractedUsername = jwtService.extractUsername(token);

        assertThat(extractedUsername).isEqualTo("user@example.com");
    }

    @Test
    void generateToken_withExtraClaims_includesThemInToken() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userDetails.getAuthorities()).thenReturn(java.util.List.of());

        String token = jwtService.generateToken(Map.of("customClaim", "customValue"), userDetails);

        String customClaim = jwtService.extractClaim(token, claims -> claims.get("customClaim", String.class));
        assertThat(customClaim).isEqualTo("customValue");
    }

    @Test
    void generateToken_producesNonBlankToken() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userDetails.getAuthorities()).thenReturn(java.util.List.of());

        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // isTokenValid()
    // -------------------------------------------------------------------------

    @Test
    void isTokenValid_whenTokenMatchesUserAndNotExpired_returnsTrue() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userDetails.getAuthorities()).thenReturn(java.util.List.of());

        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_whenUsernameMismatch_returnsFalse() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userDetails.getAuthorities()).thenReturn(java.util.List.of());

        String token = jwtService.generateToken(userDetails);

        UserDetails otherUser = org.mockito.Mockito.mock(UserDetails.class);
        when(otherUser.getUsername()).thenReturn("other@example.com");

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_whenTokenIsExpired_throwsExpiredJwtException() {
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1000L);
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userDetails.getAuthorities()).thenReturn(java.util.List.of());

        String expiredToken = jwtService.generateToken(userDetails);

        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", EXPIRATION_MS);

        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, userDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    // -------------------------------------------------------------------------
    // extractClaim()
    // -------------------------------------------------------------------------

    @Test
    void extractClaim_extractsExpirationDate() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userDetails.getAuthorities()).thenReturn(java.util.List.of());

        String token = jwtService.generateToken(userDetails);
        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void extractClaim_extractsIssuedAt() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userDetails.getAuthorities()).thenReturn(java.util.List.of());

        String token = jwtService.generateToken(userDetails);
        Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);

        assertThat(issuedAt).isBeforeOrEqualTo(new Date());
    }

    // -------------------------------------------------------------------------
    // Tampered / invalid token
    // -------------------------------------------------------------------------

    @Test
    void extractUsername_whenTokenIsTampered_throwsJwtException() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userDetails.getAuthorities()).thenReturn(java.util.List.of());

        String token = jwtService.generateToken(userDetails);
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtService.extractUsername(tamperedToken))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    void extractUsername_whenTokenSignedWithDifferentKey_throwsJwtException() {
        // Build a token signed with a completely different key
        SecretKey otherKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(
                        "b3RoZXIta2V5LXRoYXQtaXMtYWxzby1sb25nLWVub3VnaC1mb3ItSE1BQy1TSEEtNTEy"));

        String foreignToken = Jwts.builder()
                .subject("user@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(otherKey)
                .compact();

        assertThatThrownBy(() -> jwtService.extractUsername(foreignToken))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}