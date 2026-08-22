package com.learn.interviewmentor.dto.mentor;

/**
 * Sensitive numbers never leave the server in full.
 *
 * An admin needs enough to confirm the mentor typed the right thing, not the
 * whole number - so we show the last four digits only. The full value stays in
 * the database.
 */
final class Masking {

    private Masking() {
    }

    /** "123456789012" -> "XXXXXXXX9012" */
    static String tail(String value) {
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
