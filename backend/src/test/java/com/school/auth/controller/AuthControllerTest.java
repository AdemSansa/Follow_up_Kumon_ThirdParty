package com.school.auth.controller;

import tools.jackson.databind.ObjectMapper;
import com.school.auth.dto.AuthResponse;
import com.school.auth.dto.LoginRequest;
import com.school.auth.model.Role;
import com.school.auth.security.SecurityConfig;
import com.school.auth.service.AuthService;
import com.school.auth.service.JwtService;
import com.school.common.exception.GlobalExceptionHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice tests: no DB, no real JWT signing key material beyond
 * what's needed to fabricate a bearer token for the /refresh case.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    // Not exercised directly in these tests, but SecurityConfig's
    // authenticationProvider bean requires one in the application context.
    @MockitoBean
    private UserDetailsService userDetailsService;

    private static AuthResponse sampleResponse() {
        return new AuthResponse("token-value", 3600, UUID.randomUUID(), "teacher@example.com", "Jane", "Doe",
                Role.STAFF);
    }

    @Test
    void loginWithValidCredentialsReturns200AndToken() throws Exception {
        when(authService.login(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("teacher@example.com", "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-value"))
                .andExpect(jsonPath("$.email").value("teacher@example.com"))
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    void loginWithBlankPasswordReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("teacher@example.com", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @Test
    void loginWithBadCredentialsReturns401() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("bad creds"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("teacher@example.com", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void refreshWithoutBearerTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithValidBearerTokenReturns200() throws Exception {
        Claims claims = Jwts.claims().subject("teacher@example.com").add("role", "STAFF").build();
        when(jwtService.parse(eq("valid-token"))).thenReturn(Optional.of(claims));
        when(authService.refresh(eq("teacher@example.com"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("teacher@example.com"));
    }

    @Test
    void refreshWithInvalidBearerTokenReturns401() throws Exception {
        when(jwtService.parse(eq("garbage"))).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }
}
