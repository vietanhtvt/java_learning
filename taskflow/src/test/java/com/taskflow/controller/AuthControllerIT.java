package com.taskflow.controller;

import com.taskflow.AbstractIntegrationTest;
import com.taskflow.dto.request.LoginRequest;
import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.response.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthController IT")
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;

    // ---- helpers -----------------------------------------------------------

    AuthResponse register(String username, String email) {
        var body = new RegisterRequest(username, email, "Password1!", "Test User");
        var response = restTemplate.postForEntity(
            "/api/auth/register", body, AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    // ---- tests -------------------------------------------------------------

    @Test
    @DisplayName("register → login → access protected endpoint with JWT")
    void fullAuthFlow() {
        // 1. Register
        var auth = register("alice_it", "alice_it@test.com");
        assertThat(auth).isNotNull();
        assertThat(auth.accessToken()).isNotBlank();
        assertThat(auth.refreshToken()).isNotBlank();
        assertThat(auth.username()).isEqualTo("alice_it");

        // 2. Login
        var loginResp = restTemplate.postForEntity(
            "/api/auth/login",
            new LoginRequest("alice_it", "Password1!"),
            AuthResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var loginAuth = loginResp.getBody();
        assertThat(loginAuth).isNotNull();
        assertThat(loginAuth.accessToken()).isNotBlank();

        // 3. Access protected endpoint with JWT
        var headers = new HttpHeaders();
        headers.setBearerAuth(loginAuth.accessToken());
        var projectsResp = restTemplate.exchange(
            "/api/projects", HttpMethod.GET,
            new HttpEntity<>(headers), String.class);
        assertThat(projectsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("login with wrong password → 400 (BusinessException)")
    void login_wrongPassword() {
        register("bob_it", "bob_it@test.com");

        var resp = restTemplate.postForEntity(
            "/api/auth/login",
            new LoginRequest("bob_it", "wrongpassword"),
            String.class);

        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @DisplayName("access protected endpoint without token → 401")
    void noToken_unauthorized() {
        var resp = restTemplate.getForEntity("/api/projects", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("register duplicate email → 422 Validation Error")
    void register_duplicateEmail() {
        register("charlie_it", "charlie_it@test.com");

        // Try registering again with same email
        var body = new RegisterRequest("charlie2_it", "charlie_it@test.com", "Password1!", null);
        var resp = restTemplate.postForEntity("/api/auth/register", body, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("refresh with valid refresh token → new tokens")
    void refresh_validToken() {
        var auth = register("dave_it", "dave_it@test.com");

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = "{\"refreshToken\": \"" + auth.refreshToken() + "\"}";
        var resp = restTemplate.exchange(
            "/api/auth/refresh", HttpMethod.POST,
            new HttpEntity<>(body, headers), AuthResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().accessToken()).isNotBlank();
    }
}
