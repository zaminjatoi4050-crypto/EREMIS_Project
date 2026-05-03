package com.eremis.utils;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Input validation and sanitisation helpers.
 *
 * FIX [SECURITY]: isValidPassword() now enforces minimum 8 characters AND
 * requires at least one digit — was only checking length >= 6 before.
 *
 * FIX [SECURITY]: sanitize() now strips script-injection patterns in
 * addition to HTML tags.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^[0-9+\\-\\s()]{7,20}$");

    // Password: min 8 chars, at least 1 digit
    private static final Pattern PASSWORD_DIGIT =
        Pattern.compile(".*[0-9].*");

    private ValidationUtil() {}

    public static boolean isNullOrBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone == null || phone.isBlank()
            || PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /** Username: 3-50 characters, or an email-style login such as admin@EREMIS.com. */
    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        String value = username.trim();
        return value.matches("[A-Za-z0-9_]{3,50}") || isValidEmail(value);
    }

    /**
     * Password strength: minimum 8 characters + at least one digit.
     * FIX: was min 6, no digit requirement — too weak.
     */
    public static boolean isValidPassword(String pwd) {
        return pwd != null
            && pwd.length() >= 8
            && PASSWORD_DIGIT.matcher(pwd).matches();
    }

    public static boolean isPositiveDecimal(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isPositiveInt(int value) { return value > 0; }

    public static boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null) return false;
        String value = accountNumber.replaceAll("\\s+", "");
        return value.matches("^[0-9]{8,20}$");
    }

    public static boolean isValidBankName(String bankName) {
        return bankName != null && !bankName.trim().isEmpty();
    }

    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Basic XSS sanitisation — strips HTML tags and common injection patterns.
     * FIX: added javascript: and on* event attribute patterns on top of tag removal.
     * NOTE: For production user-generated content rendered in a browser use a
     * proper HTML sanitiser library (OWASP Java HTML Sanitizer).
     */
    public static String sanitize(String input) {
        if (input == null) return null;
        return input
            .replaceAll("<[^>]*>", "")                    // strip HTML tags
            .replaceAll("(?i)javascript\\s*:", "")         // strip JS URLs
            .replaceAll("(?i)on\\w+\\s*=", "")             // strip event handlers
            .trim();
    }
}
