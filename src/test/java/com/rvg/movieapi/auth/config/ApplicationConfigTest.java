package com.rvg.movieapi.auth.config;

import com.rvg.movieapi.auth.entities.User;
import com.rvg.movieapi.auth.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private User mockUser;

    @InjectMocks
    private ApplicationConfig applicationConfig;

    // -------------------------------------------------------------------------
    // userDetailsService()
    // -------------------------------------------------------------------------

    @Test
    void userDetailsService_whenUserExists_returnsUserDetails() {
        String email = "user@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        UserDetailsService service = applicationConfig.userDetailsService();
        UserDetails result = service.loadUserByUsername(email);

        assertThat(result).isEqualTo(mockUser);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void userDetailsService_whenUserNotFound_throwsUsernameNotFoundException() {
        String email = "notfound@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        UserDetailsService service = applicationConfig.userDetailsService();

        assertThatThrownBy(() -> service.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(email);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void userDetailsService_returnsDifferentInstanceOnEachCall() {
        UserDetailsService first = applicationConfig.userDetailsService();
        UserDetailsService second = applicationConfig.userDetailsService();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
    }

    // -------------------------------------------------------------------------
    // authenticationProvider()
    // -------------------------------------------------------------------------

    @Test
    void authenticationProvider_returnsNonNullProvider() {
        AuthenticationProvider provider = applicationConfig.authenticationProvider();

        assertThat(provider).isNotNull();
    }

    @Test
    void authenticationProvider_returnsDaoAuthenticationProviderType() {
        AuthenticationProvider provider = applicationConfig.authenticationProvider();

        assertThat(provider).isInstanceOf(DaoAuthenticationProvider.class);
    }

    // -------------------------------------------------------------------------
    // authenticationManager()
    // -------------------------------------------------------------------------

    @Test
    void authenticationManager_returnsManagerFromConfiguration() throws Exception {
        AuthenticationConfiguration config = mock(AuthenticationConfiguration.class);
        AuthenticationManager mockManager = mock(AuthenticationManager.class);
        when(config.getAuthenticationManager()).thenReturn(mockManager);

        AuthenticationManager result = applicationConfig.authenticationManager(config);

        assertThat(result).isEqualTo(mockManager);
        verify(config).getAuthenticationManager();
    }

    @Test
    void authenticationManager_propagatesExceptionFromConfiguration() throws Exception {
        AuthenticationConfiguration config = mock(AuthenticationConfiguration.class);
        when(config.getAuthenticationManager()).thenThrow(new RuntimeException("config error"));

        assertThatThrownBy(() -> applicationConfig.authenticationManager(config))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("config error");
    }

    // -------------------------------------------------------------------------
    // passwordEncoder()
    // -------------------------------------------------------------------------

    @Test
    void passwordEncoder_returnsNonNullEncoder() {
        PasswordEncoder encoder = applicationConfig.passwordEncoder();

        assertThat(encoder).isNotNull();
    }

    @Test
    void passwordEncoder_returnsBCryptPasswordEncoderType() {
        PasswordEncoder encoder = applicationConfig.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoder_encodesPasswordSuccessfully() {
        PasswordEncoder encoder = applicationConfig.passwordEncoder();
        String rawPassword = "securePassword123";

        String encoded = encoder.encode(rawPassword);

        assertThat(encoded).isNotBlank();
        assertThat(encoded).isNotEqualTo(rawPassword);
    }

    @Test
    void passwordEncoder_matchesRawPasswordAgainstEncoded() {
        PasswordEncoder encoder = applicationConfig.passwordEncoder();
        String rawPassword = "securePassword123";
        String encoded = encoder.encode(rawPassword);

        boolean matches = encoder.matches(rawPassword, encoded);

        assertThat(matches).isTrue();
    }

    @Test
    void passwordEncoder_doesNotMatchWrongPassword() {
        PasswordEncoder encoder = applicationConfig.passwordEncoder();
        String encoded = encoder.encode("correctPassword");

        boolean matches = encoder.matches("wrongPassword", encoded);

        assertThat(matches).isFalse();
    }

    @Test
    void passwordEncoder_producesDifferentHashesForSameInput() {
        PasswordEncoder encoder = applicationConfig.passwordEncoder();
        String rawPassword = "samePassword";

        String first = encoder.encode(rawPassword);
        String second = encoder.encode(rawPassword);

        // BCrypt generates a new salt each time
        assertThat(first).isNotEqualTo(second);
    }
}