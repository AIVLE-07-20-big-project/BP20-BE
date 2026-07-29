package com.bp20.backend.global.util;

public final class PhoneNumberUtils {

    public static final String OPTIONAL_KOREAN_PHONE_PATTERN =
            "^$|^(?:02-?\\d{3,4}-?\\d{4}|0(?:1[016789]|3[1-3]|4[1-4]|5[1-5]|6[1-4]|70)-?\\d{3,4}-?\\d{4})$";

    private PhoneNumberUtils() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.replaceAll("\\D", "");
    }
}
