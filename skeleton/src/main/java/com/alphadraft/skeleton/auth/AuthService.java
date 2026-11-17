package com.alphadraft.skeleton.auth;

import com.alphadraft.skeleton.model.Role;
import com.alphadraft.skeleton.model.User;
import com.alphadraft.skeleton.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    public AuthResponse signUp(AuthRequest request) {
        Map<String, String> errors = validateSignUp(request);
        if (!errors.isEmpty()) {
            throw new InvalidSignUpException(errors);
        }
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = users.save(new User(email, passwordEncoder.encode(request.password()), Role.PLAYER));
        return respond(user);
    }

    public AuthResponse signIn(AuthRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.password())) {
            throw new InvalidCredentialsException();
        }
        User user = users.findByEmail(normalizeEmail(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return respond(user);
    }

    private AuthResponse respond(User user) {
        return new AuthResponse(tokens.issue(user), user.getEmail(), user.getRole());
    }

    private Map<String, String> validateSignUp(AuthRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (request == null || isBlank(request.email())) {
            errors.put("email", "Email is required.");
        } else if (!normalizeEmail(request.email()).contains("@")) {
            errors.put("email", "Enter a valid email address.");
        }
        if (request == null || isBlank(request.password())) {
            errors.put("password", "Password is required.");
        } else if (request.password().length() < MIN_PASSWORD_LENGTH) {
            errors.put("password", "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        return errors;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
