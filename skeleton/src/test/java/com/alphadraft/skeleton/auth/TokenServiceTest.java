package com.alphadraft.skeleton.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alphadraft.skeleton.model.Role;
import com.alphadraft.skeleton.model.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-thirty-two-bytes-long";
    private static final User USER = new User("player@example.com", "hash", Role.PLAYER);

    @Test
    void tokenCarriesTheEmailAndRole() {
        TokenService tokens = new TokenService(SECRET, 60);

        var claims = tokens.parse(tokens.issue(USER));

        assertEquals("player@example.com", claims.getSubject());
        assertEquals("PLAYER", claims.get(TokenService.ROLE_CLAIM, String.class));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String forged = new TokenService("another-secret-that-is-also-at-least-32-bytes", 60).issue(USER);

        assertThrows(JwtException.class, () -> new TokenService(SECRET, 60).parse(forged));
    }

    @Test
    void expiredTokenIsRejected() {
        TokenService tokens = new TokenService(SECRET, -1);

        assertThrows(JwtException.class, () -> tokens.parse(tokens.issue(USER)));
    }

    @Test
    void shortSecretFailsFast() {
        assertThrows(IllegalArgumentException.class, () -> new TokenService("too-short", 60));
    }
}
