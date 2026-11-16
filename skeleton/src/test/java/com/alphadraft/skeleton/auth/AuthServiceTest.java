package com.alphadraft.skeleton.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphadraft.skeleton.model.Role;
import com.alphadraft.skeleton.model.User;
import com.alphadraft.skeleton.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-thirty-two-bytes-long";

    private UserRepository users;
    private BCryptPasswordEncoder encoder;
    private TokenService tokens;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder();
        tokens = new TokenService(SECRET, 60);
        service = new AuthService(users, encoder, tokens);
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void signUpCreatesAPlayerWithAHashedPasswordAndReturnsAToken() {
        AuthResponse response = service.signUp(new AuthRequest("  Player@Example.com ", "correct horse"));

        org.mockito.ArgumentCaptor<User> saved = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        User user = saved.getValue();
        assertEquals("player@example.com", user.getEmail());
        assertEquals(Role.PLAYER, user.getRole());
        assertNotEquals("correct horse", user.getPasswordHash());
        assertTrue(encoder.matches("correct horse", user.getPasswordHash()));

        assertEquals("player@example.com", response.email());
        assertEquals(Role.PLAYER, response.role());
        assertEquals("player@example.com", tokens.parse(response.token()).getSubject());
    }

    @Test
    void signUpRejectsAnEmailThatIsAlreadyRegistered() {
        when(users.existsByEmail("player@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> service.signUp(new AuthRequest("player@example.com", "correct horse")));
        verify(users, never()).save(any());
    }

    @Test
    void signUpReportsMissingAndShortFieldsTogether() {
        InvalidSignUpException e = assertThrows(InvalidSignUpException.class,
                () -> service.signUp(new AuthRequest(" ", "short")));

        assertEquals(Map.of(
                "email", "Email is required.",
                "password", "Password must be at least 8 characters."), e.getErrors());
        verify(users, never()).save(any());
    }

    @Test
    void signInSucceedsWithTheCorrectPassword() {
        User stored = new User("player@example.com", encoder.encode("correct horse"), Role.ADMIN);
        when(users.findByEmail("player@example.com")).thenReturn(Optional.of(stored));

        AuthResponse response = service.signIn(new AuthRequest("Player@Example.com", "correct horse"));

        assertEquals("player@example.com", response.email());
        assertEquals(Role.ADMIN, response.role());
        assertEquals("ADMIN", tokens.parse(response.token()).get("role", String.class));
    }

    @Test
    void signInRejectsAWrongPassword() {
        User stored = new User("player@example.com", encoder.encode("correct horse"), Role.PLAYER);
        when(users.findByEmail("player@example.com")).thenReturn(Optional.of(stored));

        assertThrows(InvalidCredentialsException.class,
                () -> service.signIn(new AuthRequest("player@example.com", "wrong password")));
    }

    @Test
    void signInRejectsAnUnknownEmailWithTheSameError() {
        when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> service.signIn(new AuthRequest("nobody@example.com", "anything")));
    }
}
