package com.financialapp.upload.domain.model.importrun;

import com.financialapp.upload.domain.exception.InvalidFileHashException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public record FileHash(String value) {

    private static final Pattern HEX_64_LOWER = Pattern.compile("^[0-9a-f]{64}$");

    public FileHash {
        if (value == null || !HEX_64_LOWER.matcher(value).matches()) {
            throw new InvalidFileHashException(value);
        }
    }

    public static FileHash ofBytes(byte[] bytes) {
        if (bytes == null) {
            throw new InvalidFileHashException("null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return new FileHash(hexString.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
