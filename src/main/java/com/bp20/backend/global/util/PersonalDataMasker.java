package com.bp20.backend.global.util;

public final class PersonalDataMasker {

    private PersonalDataMasker() {
    }

    public static String email(String value) {
        if (value == null || value.isBlank() || !value.contains("@")) {
            return value;
        }
        int separator = value.indexOf('@');
        String local = value.substring(0, separator);
        String domain = value.substring(separator);
        int visible = Math.min(2, local.length());
        return local.substring(0, visible) + "*".repeat(Math.max(1, local.length() - visible)) + domain;
    }

    public static String name(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() == 1) {
            return "*";
        }
        if (value.length() == 2) {
            return value.charAt(0) + "*";
        }
        return value.charAt(0) + "*".repeat(value.length() - 2) + value.substring(value.length() - 1);
    }

    public static String phoneNumber(String value) {
        String normalized = PhoneNumberUtils.normalize(value);
        if (normalized == null || normalized.length() < 7) {
            return normalized;
        }
        return normalized.substring(0, 3)
                + "*".repeat(normalized.length() - 7)
                + normalized.substring(normalized.length() - 4);
    }

    public static String businessNumber(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String normalized = value.replaceAll("[^0-9]", "");
        if (normalized.length() != 10) {
            return "***-**-*****";
        }
        return normalized.substring(0, 3) + "-**-*****";
    }

    public static String ipAddress(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.contains(".")) {
            int separator = value.lastIndexOf('.');
            return value.substring(0, separator + 1) + "***";
        }
        int separator = value.lastIndexOf(':');
        return separator > 0 ? value.substring(0, separator + 1) + "****" : "****";
    }
}
