// File: src/main/java/org/example/util/Hasher.java
package org.example.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {
    private static final int WARMUP_ITERATIONS = 50000;
private static final String WARMUP_STRING = "password123";

    public static void warmup() {
        System.out.println("Starting JIT Warmup phase...");
        long startTime = System.nanoTime();
        
        try {
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                // Execute the code we want the JIT to optimize: Hasher.sha256()
                // The result is ignored to prevent the JVM from optimizing the call away entirely.
                // Using a constant string ensures the compilation is relevant.
                sha256(WARMUP_STRING); 
            }
        } catch (Exception e) {
            // Handle potential NoSuchAlgorithmException (though unlikely for SHA-256)
            System.err.println("Warmup failed: " + e.getMessage());
        }

        long endTime = System.nanoTime();
        System.out.printf("Warmup complete in %.2f ms.\n", 
                        (endTime - startTime) / 1_000_000.0);
    }

    public static String sha256(String input) throws NoSuchAlgorithmException {
        // Standard Java hashing implementation
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}