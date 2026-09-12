package com.daniel.util;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public final class MpesaPasswordGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private MpesaPasswordGenerator() {}

    public static String generateTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    public static String generatePassword(String shortcode, String passkey, String timestamp) {
        String raw = shortcode + passkey + timestamp;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}