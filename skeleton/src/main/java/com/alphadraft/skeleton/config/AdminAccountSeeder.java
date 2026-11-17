package com.alphadraft.skeleton.config;

import com.alphadraft.skeleton.model.Role;
import com.alphadraft.skeleton.model.User;
import com.alphadraft.skeleton.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Public sign-up always creates a PLAYER, so the one ADMIN account comes from configuration:
// set ADMIN_EMAIL and ADMIN_PASSWORD and, on startup, that account is created as an ADMIN, or
// promoted to ADMIN if it already signed up as a player. An existing account keeps its
// password. With either variable unset, nothing happens.
@Component
public class AdminAccountSeeder implements CommandLineRunner {

    static final int MIN_PASSWORD_LENGTH = 8;

    private static final Logger log = LoggerFactory.getLogger(AdminAccountSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminAccountSeeder(UserRepository users,
                              PasswordEncoder passwordEncoder,
                              @Value("${app.admin.email:}") String email,
                              @Value("${app.admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim().toLowerCase();
        this.password = password == null ? "" : password;
    }

    @Override
    public void run(String... args) {
        if (email.isEmpty() || password.isEmpty()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set; no admin account was created.");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        users.findByEmail(email).ifPresentOrElse(this::promote, this::create);
    }

    private void promote(User user) {
        if (user.getRole() != Role.ADMIN) {
            user.promoteToAdmin();
            users.save(user);
            log.info("Promoted {} to ADMIN.", email);
        }
    }

    private void create() {
        users.save(new User(email, passwordEncoder.encode(password), Role.ADMIN));
        log.info("Created admin account {}.", email);
    }
}
