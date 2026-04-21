package com.company.sqlforge.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SQL fingerprint generator that normalizes whitespace before hashing.
 */
public final class SqlFingerprintUtils {

    private SqlFingerprintUtils() {
    }

    public static String fingerprint(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }
        String normalized = sql.trim().replaceAll("\\s+", " ").toLowerCase();
        return md5Hex(normalized);
    }

    private static String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 algorithm is not available", ex);
        }
    }
}
