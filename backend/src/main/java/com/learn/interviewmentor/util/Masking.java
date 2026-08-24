package com.learn.interviewmentor.util;

/**
 * Sensitive numbers never leave the server in full.
 *
 * Lives in util/ rather than vo/ because it is a helper, not a payload - vo/ is
 * for classes that ARE a response body, and one that is only used to build one
 * would make the package mean two different things.
 *
 * An admin needs enough to confirm the mentor typed the right thing, not the
 * whole number - so we show the last four digits only. The full value stays in
 * the database.
 */
public final class Masking {

    private Masking() {
    }

    /** "123456789012" -> "XXXXXXXX9012" */
    public static String tail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return "X".repeat(trimmed.length());
        }
        return "X".repeat(trimmed.length() - 4) + trimmed.substring(trimmed.length() - 4);
    }
}
