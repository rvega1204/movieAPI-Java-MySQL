package com.rvg.movieapi.auth.services;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthFilterServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AuthFilterService authFilterService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // No Authorization header
    // -------------------------------------------------------------------------

    @Test
    void doFilterInternal_whenNoAuthHeader_skipAndContinueChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        authFilterService.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_whenAuthHeaderNotBearer_skipAndContinueChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        authFilterService.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // -------------------------------------------------------------------------
    // Valid token — authentication should be set in SecurityContext
    // -------------------------------------------------------------------------

    @Test
    void doFilterInternal_whenValidToken_setsAuthenticationInContext() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        authFilterService.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_whenValidToken_loadsUserByExtractedUsername() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        authFilterService.doFilterInternal(request, response, filterChain);

        verify(userDetailsService).loadUserByUsername("user@example.com");
        verify(jwtService).isTokenValid(token, userDetails);
    }

    // -------------------------------------------------------------------------
    // Invalid token — authentication should NOT be set
    // -------------------------------------------------------------------------

    @Test
    void doFilterInternal_whenTokenIsInvalid_doesNotSetAuthentication() throws Exception {
        String token = "invalid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);

        authFilterService.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // -------------------------------------------------------------------------
    // Username not extractable from token
    // -------------------------------------------------------------------------

    @Test
    void doFilterInternal_whenUsernameIsNull_doesNotLoadUser() throws Exception {
        String token = "unreadable.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(null);

        authFilterService.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(userDetailsService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // -------------------------------------------------------------------------
    // Already authenticated — should not re-authenticate
    // -------------------------------------------------------------------------

    @Test
    void doFilterInternal_whenAlreadyAuthenticated_doesNotReauthenticate() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn("user@example.com");

        // Simulate existing authentication in context
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        authFilterService.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(userDetailsService);
        verify(jwtService, never()).isTokenValid(any(), any());
        verify(filterChain).doFilter(request, response);
    }

    // -------------------------------------------------------------------------
    // Filter chain always continues
    // -------------------------------------------------------------------------

    @Test
    void doFilterInternal_alwaysContinuesFilterChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        authFilterService.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}