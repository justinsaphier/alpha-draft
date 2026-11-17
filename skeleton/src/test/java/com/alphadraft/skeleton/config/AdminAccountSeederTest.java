package com.alphadraft.skeleton.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminAccountSeederTest {

    private UserRepository users;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder();
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AdminAccountSeeder seeder(String email, String password) {
        return new AdminAccountSeeder(users, encoder, email, password);
    }

    @Test
    void createsTheAdminWithAHashedPasswordWhenTheAccountDoesNotExist() {
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.empty());

        seeder("  Admin@Example.com ", "admin-password").run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertEquals("admin@example.com", saved.getValue().getEmail());
        assertEquals(Role.ADMIN, saved.getValue().getRole());
        assertTrue(encoder.matches("admin-password", saved.getValue().getPasswordHash()));
    }

    @Test
    void promotesAnExistingPlayerAndKeepsTheirPassword() {
        User player = new User("admin@example.com", "existing-hash", Role.PLAYER);
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.of(player));

        seeder("admin@example.com", "admin-password").run();

        assertEquals(Role.ADMIN, player.getRole());
        assertEquals("existing-hash", player.getPasswordHash());
        verify(users).save(player);
    }

    @Test
    void leavesAnExistingAdminAlone() {
        when(users.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(new User("admin@example.com", "hash", Role.ADMIN)));

        seeder("admin@example.com", "admin-password").run();

        verify(users, never()).save(any());
    }

    @Test
    void doesNothingWhenEmailOrPasswordIsUnset() {
        seeder("", "admin-password").run();
        seeder("admin@example.com", "").run();
        seeder(null, null).run();

        verify(users, never()).findByEmail(any());
        verify(users, never()).save(any());
    }

    @Test
    void rejectsAShortPassword() {
        assertThrows(IllegalStateException.class, () -> seeder("admin@example.com", "short").run());
        verify(users, never()).save(any());
    }
}
