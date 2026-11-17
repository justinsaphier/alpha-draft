package com.alphadraft.skeleton.auth;

import com.alphadraft.skeleton.model.Role;

// Returned after a successful sign-up or sign-in. Never includes the password or its hash.
public record AuthResponse(String token, String email, Role role) {
}
