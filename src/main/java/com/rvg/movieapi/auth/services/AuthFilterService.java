package com.rvg.movieapi.auth.services;

import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter that runs once per request.
 * <p>
 * Intercepts every incoming HTTP request and attempts to authenticate the user
 * based on the JWT found in the {@code Authorization} header. If a valid token
 * is present, the user is authenticated and stored in the {@link SecurityContextHolder}
 * so downstream filters and controllers can access the authenticated principal.
 * <p>
 * Requests without a {@code Bearer} token are passed through without modification,
 * allowing Spring Security to handle them based on the configured authorization rules.
 */
@Service
public class AuthFilterService extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthFilterService(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Processes each request exactly once to validate the JWT and set the authentication.
     * <p>
     * Flow:
     * <ol>
     *   <li>Read the {@code Authorization} header.</li>
     *   <li>If missing or not a Bearer token, skip authentication and continue the chain.</li>
     *   <li>Extract the username from the JWT.</li>
     *   <li>If the username is valid and no authentication exists in the context,
     *       load the user and validate the token.</li>
     *   <li>If the token is valid, create a {@link UsernamePasswordAuthenticationToken}
     *       and store it in the {@link SecurityContextHolder}.</li>
     *   <li>Continue the filter chain regardless of authentication outcome.</li>
     * </ol>
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String jwt;
        String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        username = jwtService.extractUsername(jwt);

        // Only authenticate if username is present and context is not already authenticated
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
                        null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
