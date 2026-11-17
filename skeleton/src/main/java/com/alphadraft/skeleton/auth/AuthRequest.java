package com.alphadraft.skeleton.auth;

// Body for POST /auth/signup and POST /auth/signin.
public record AuthRequest(String email, String password) {
}
