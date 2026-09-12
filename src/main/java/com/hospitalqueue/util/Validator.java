package com.hospitalqueue.util;

import java.util.regex.Pattern;

public final class Validator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    // Myanmar phone numbers: starts with 09 followed by 7, 8, or 9 digits (total 9, 10, or 11 digits)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^09[0-9]{7,9}$");
    private static final int PHONE_MIN_LENGTH = 9;
    private static final int PHONE_MAX_LENGTH = 11;

    private static final String[] MYANMAR_REGIONS = {
        "yangon", "mandalay", "naypyidaw", "nay pyi taw", "bago", "ayeyarwady", "irrawaddy",
        "magway", "magwe", "sagaing", "tanintharyi", "tenasserim", "kachin", "kayah",
        "kayin", "karen", "chin", "mon", "rakhine", "arakan", "shan",
        "ရန်ကုန်", "မန္တလေး", "နေပြည်တော်", "ပဲခူး", "ဧရာဝတီ", "မကွေး", "စစ်ကိုင်း",
        "တနင်္သာရီ", "ကချင်", "ကယား", "ကရင်", "ချင်း", "မွန်", "ရခိုင်", "ရှမ်း"
    };

    private Validator() {
    }

    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return !isNotEmpty(email) || EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (!isNotEmpty(phone)) return false;
        String clean = phone.trim().replaceAll("[\\s\\-()]", "");
        return PHONE_PATTERN.matcher(clean).matches();
    }

    /**
     * Returns a specific phone validation error message, or null if valid.
     */
    public static String getPhoneValidationError(String phone) {
        if (!isNotEmpty(phone)) return "Phone number is required.";
        String clean = phone.trim().replaceAll("[\\s\\-()]", "");
        if (!clean.startsWith("09")) {
            return "Phone number must start with 09 (Myanmar format).";
        }
        if (clean.length() < PHONE_MIN_LENGTH || clean.length() > PHONE_MAX_LENGTH) {
            return "Phone number must be " + PHONE_MIN_LENGTH + " to " + PHONE_MAX_LENGTH + " digits long.";
        }
        if (!clean.matches("[0-9]+")) {
            return "Phone number must contain only digits.";
        }
        if (!PHONE_PATTERN.matcher(clean).matches()) {
            return "Invalid phone number format. Must start with 09 and be 9-11 digits.";
        }
        return null;
    }

    public static boolean isValidPassword(String password) {
        if (!isNotEmpty(password) || password.length() < 8) {
            return false;
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    public static String getPasswordValidationError(String password) {
        if (!isNotEmpty(password)) return "Password is required.";
        if (password.length() < 8) return "Password must be at least 8 characters long.";
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        if (!hasUpper) return "Password must contain at least one uppercase letter.";
        if (!hasLower) return "Password must contain at least one lowercase letter.";
        if (!hasDigit) return "Password must contain at least one number.";
        if (!hasSpecial) return "Password must contain at least one special character (!@#$%^&* etc.).";
        return null;
    }

    /**
     * Address is optional. If provided:
     * - Must contain a valid Myanmar region/state.
     * - Must contain at minimum a street, town/township, and region (at least 3 descriptive elements).
     */
    public static boolean isValidMyanmarAddress(String address) {
        if (!isNotEmpty(address)) {
            return true; // Address is optional
        }
        String lower = address.trim().toLowerCase();
        
        // 1. Check if contains a recognized Myanmar region
        boolean hasRegion = false;
        for (String region : MYANMAR_REGIONS) {
            if (lower.contains(region)) {
                hasRegion = true;
                break;
            }
        }
        if (!hasRegion) {
            return false;
        }

        // 2. Check for street, town/township indicators or at least 3 parts
        // Comma, slash, dash separated components
        String[] parts = address.split("[,;/\\n]");
        if (parts.length >= 3) {
            return true;
        }

        // Or contains keywords for street/road and township/town
        boolean hasStreet = lower.contains("street") || lower.contains("st") || lower.contains("road") 
                || lower.contains("rd") || lower.contains("lane") || lower.contains("လမ်း");
        boolean hasTown = lower.contains("town") || lower.contains("township") || lower.contains("tsp") 
                || lower.contains("ts") || lower.contains("city") || lower.contains("မြို့နယ်") || lower.contains("မြို့");

        return (hasStreet && hasTown) || address.trim().length() >= 15;
    }

    public static boolean isMatch(String value1, String value2) {
        return value1 != null && value1.equals(value2);
    }
}
