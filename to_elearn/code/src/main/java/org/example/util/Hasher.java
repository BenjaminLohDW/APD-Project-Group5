// File: src/main/java/org/example/util/Hasher.java
package org.example.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;


public class Hasher {
    private static final ThreadLocal<MessageDigest> DIGEST = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to exist on every Java platform; rethrow as unchecked if not.
            throw new RuntimeException(e);
        }
    });

    public static String sha256(String input) {
        MessageDigest digest = DIGEST.get();
        digest.reset();
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        // Use Java's HexFormat for fast byte[] -> hex conversion
        return HexFormat.of().formatHex(hash);
    }
}