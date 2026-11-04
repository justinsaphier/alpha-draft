package com.alphadraft.skeleton.validation;

// Shared checks for stock fields, so every rule treats "missing" the same way.
public final class FieldChecks {

    private FieldChecks() {
    }

    // True when a field is null or contains only whitespace.
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
