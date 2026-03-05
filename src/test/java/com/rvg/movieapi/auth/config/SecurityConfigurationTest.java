package com.rvg.movieapi.auth.config;

import com.rvg.movieapi.auth.services.AuthService;
import com.rvg.movieapi.auth.services.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SecurityConfiguration.
 * <p>
 * Verifies that the security filter chain correctly permits public endpoints
 * and blocks protected ones. Uses @SpringBootTest to load the real filter chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    // -------------------------------------------------------------------------
    // Public endpoints — security must let them through (no 401 / 403)
    // We don't assert the controller's response, only that Spring Security
    // did not block the request.
    // -------------------------------------------------------------------------

    @Test
    void authLoginEndpoint_isPublic_notBlockedBySecurity() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login"))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertThat(status)
                .as("Expected security to allow /auth/login (no 401 or 403)")
                .isNotEqualTo(401)
                .isNotEqualTo(403);
    }

    @Test
    void authRegisterEndpoint_isPublic_notBlockedBySecurity() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register"))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertThat(status)
                .as("Expected security to allow /auth/register (no 401 or 403)")
                .isNotEqualTo(401)
                .isNotEqualTo(403);
    }

    @Test
    void forgotPasswordEndpoint_isPublic_notBlockedBySecurity() throws Exception {
        MvcResult result = mockMvc.perform(post("/forgotPassword/verify"))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertThat(status)
                .as("Expected security to allow /forgotPassword/** (no 401 or 403)")
                .isNotEqualTo(401)
                .isNotEqualTo(403);
    }

    // -------------------------------------------------------------------------
    // Protected endpoints — must be blocked by security (403 for anonymous users)
    // Spring Security returns 403 by default when no AuthenticationEntryPoint
    // is configured, even for unauthenticated requests.
    // -------------------------------------------------------------------------

    @Test
    void protectedGetEndpoint_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedPostEndpoint_withoutToken_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/movies"))
                .andExpect(status().isForbidden());
    }
}