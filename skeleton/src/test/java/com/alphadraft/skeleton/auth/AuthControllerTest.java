package com.alphadraft.skeleton.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import com.alphadraft.skeleton.model.Role;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.alphadraft.skeleton.config.SecurityConfig;
import org.springframework.context.annotation.Import;


// Loads the real SecurityConfig to show /auth/** is open to anonymous callers.
@WebMvcTest(value = AuthController.class, properties = {
    "app.jwt.secret=test-secret-that-is-at-least-thirty-two-bytes-long",
    "app.jwt.expiration-minutes=60"
})
@Import({SecurityConfig.class, TokenService.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void signUpReturnsCreatedWithTokenAndNoPassword() throws Exception {
        when(authService.signUp(any())).thenReturn(new AuthResponse("token-value", "player@example.com", Role.PLAYER));

        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"player@example.com\",\"password\":\"correct horse\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-value"))
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))));
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        when(authService.signUp(any())).thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"player@example.com\",\"password\":\"correct horse\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.email").value("An account with this email already exists."));
    }

    @Test
    void signInWithWrongPasswordReturnsUnauthorized() throws Exception {
        when(authService.signIn(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"player@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email or password is incorrect."));
    }

    @Test
    void invalidSignUpReturnsFieldErrors() throws Exception {
        when(authService.signUp(any())).thenThrow(new InvalidSignUpException(
                Map.of("password", "Password must be at least 8 characters.")));

        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"player@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Password must be at least 8 characters."));
    }
}
