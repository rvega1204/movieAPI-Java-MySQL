package com.rvg.movieapi.auth.entities;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    // -------------------------------------------------------------------------
    // getAuthorities()
    // -------------------------------------------------------------------------

    @Test
    void getAuthorities_whenRoleIsAssigned_returnsRoleAsAuthority() {
        User user = User.builder()
                .role(UserRole.USER)
                .build();

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("USER");
    }

    @Test
    void getAuthorities_whenRoleIsNull_returnsEmptyList() {
        User user = User.builder()
                .role(null)
                .build();

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertThat(authorities).isEmpty();
    }

    // -------------------------------------------------------------------------
    // getUsername() — should return email, not the username field
    // -------------------------------------------------------------------------

    @Test
    void getUsername_returnsEmail_notUsernameField() {
        User user = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .build();

        assertThat(user.getUsername()).isEqualTo("john@example.com");
        assertThat(user.getUsername()).isNotEqualTo("john_doe");
    }

    // -------------------------------------------------------------------------
    // getPassword()
    // -------------------------------------------------------------------------

    @Test
    void getPassword_returnsPasswordField() {
        User user = User.builder()
                .password("hashed_password")
                .build();

        assertThat(user.getPassword()).isEqualTo("hashed_password");
    }

    // -------------------------------------------------------------------------
    // UserDetails status flags — all hardcoded to true
    // -------------------------------------------------------------------------

    @Test
    void isAccountNonExpired_alwaysReturnsTrue() {
        User user = User.builder().build();
        assertThat(user.isAccountNonExpired()).isTrue();
    }

    @Test
    void isAccountNonLocked_alwaysReturnsTrue() {
        User user = User.builder().build();
        assertThat(user.isAccountNonLocked()).isTrue();
    }

    @Test
    void isCredentialsNonExpired_alwaysReturnsTrue() {
        User user = User.builder().build();
        assertThat(user.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void isEnabled_alwaysReturnsTrue() {
        User user = User.builder().build();
        assertThat(user.isEnabled()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    @Test
    void builder_setsAllFieldsCorrectly() {
        User user = User.builder()
                .userId(1)
                .name("John Doe")
                .username("john_doe")
                .email("john@example.com")
                .password("secret123")
                .role(UserRole.USER)
                .build();

        assertThat(user.getUserId()).isEqualTo(1);
        assertThat(user.getName()).isEqualTo("John Doe");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getPassword()).isEqualTo("secret123");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
    }
}