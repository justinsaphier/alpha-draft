package com.alphadraft.skeleton.auth;

import java.util.Map;

public class InvalidSignUpException extends RuntimeException {

    private final Map<String, String> errors;

    public InvalidSignUpException(Map<String, String> errors) {
        super("Sign-up details are invalid.");
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
