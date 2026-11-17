package com.alphadraft.skeleton.auth;

// Same message for an unknown email and a wrong password, so sign-in does not reveal which emails have accounts.
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email or password is incorrect.");
    }
}
