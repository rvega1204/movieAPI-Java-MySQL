package com.rvg.movieapi.auth.config;

import com.rvg.movieapi.auth.repositories.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Central security configuration class.
 * <p>
 * This class defines the core authentication components used by Spring Security:
 * - UserDetailsService for loading users from the database
 * - AuthenticationProvider responsible for validating credentials
 * - AuthenticationManager used during login authentication
 * - PasswordEncoder used to hash and verify passwords
 * <p>
 * The configuration uses a DAO-based authentication mechanism backed by the UserRepository.
 */
@Configuration
public class ApplicationConfig {

    private final UserRepository userRepository;

    public ApplicationConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Defines the UserDetailsService used by Spring Security.
     * <p>
     * This service loads user-specific data during the authentication process.
     * In this implementation, users are retrieved from the database using their email.
     *
     * @return UserDetailsService implementation
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    /**
     * Defines the AuthenticationProvider responsible for authenticating users.
     * <p>
     * DaoAuthenticationProvider uses:
     * - the custom UserDetailsService
     * - the configured PasswordEncoder
     *
     * @return configured AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService());
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthenticationProvider;
    }

    /**
     * Exposes the AuthenticationManager bean used by the authentication process.
     * <p>
     * This manager delegates authentication requests to the configured providers.
     *
     * @param config AuthenticationConfiguration provided by Spring Security
     * @return AuthenticationManager instance
     * @throws Exception if the manager cannot be created
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Password encoder bean.
     * <p>
     * BCrypt is used because it is adaptive, secure, and recommended by Spring Security.
     *
     * @return PasswordEncoder implementation
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
