package com.rvg.movieapi.auth.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for JWT generation, parsing, and validation.
 * <p>
 * Uses HMAC-SHA signing with a Base64-encoded secret key configured via
 * {@code app.security.jwt-secret}. Token expiration is controlled by
 * {@code app.security.jwt-expiration-ms}.
 */
@Service
public class JwtService {

    @Value("${app.security.jwt-secret}")
    private String secretKey;

    @Value("${app.security.jwt-expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Extracts the username (subject) from the given JWT.
     *
     * @param token the JWT string
     * @return the subject claim, typically the user's email
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a specific claim from the JWT using the provided resolver function.
     *
     * @param token          the JWT string
     * @param claimsResolver function that maps {@link Claims} to the desired value
     * @param <T>            the type of the claim to extract
     * @return the resolved claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    /**
     * Generates a JWT for the given user with no extra claims.
     *
     * @param userDetails the authenticated user
     * @return signed JWT string
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT for the given user with additional custom claims.
     * <p>
     * Always includes the user's authorities under the {@code role} claim.
     * Sets {@code iat} (issued at) and {@code exp} (expiration) automatically.
     *
     * @param extraClaims additional claims to embed in the token payload
     * @param userDetails the authenticated user
     * @return signed JWT string
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("role", userDetails.getAuthorities());

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Validates the JWT against the given user.
     * <p>
     * A token is valid if its subject matches the user's username
     * and it has not yet expired.
     *
     * @param token       the JWT string
     * @param userDetails the user to validate against
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Parses and returns all claims from the JWT payload.
     * Throws a {@link io.jsonwebtoken.JwtException} if the token is invalid or tampered.
     *
     * @param token the JWT string
     * @return all claims contained in the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks whether the JWT has passed its expiration date.
     *
     * @param token the JWT string
     * @return {@code true} if the token is expired, {@code false} otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Decodes the Base64 secret key and returns an HMAC-SHA {@link Key}.
     *
     * @return the signing key used to sign and verify JWTs
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
