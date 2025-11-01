package org.example.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal PrehashCache placeholder.
 * Adjust serialization/deserialization as your real cache format requires.
 */
public class PrehashCache {

    // store prehashed dictionary in-memory (simple placeholder)
    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    public PrehashCache() {}

    public void put(String k, String v) {
        map.put(k, v);
    }

    public Map<String, String> asMap() {
        return map;
    }

    /**
     * Load a PrehashCache from disk. This is a minimal implementation that
     * expects each line to be "plainword:hash" — change to match your actual file format.
     */
    public static PrehashCache load(Path p) {
        PrehashCache cache = new PrehashCache();
        if (Files.exists(p)) {
            try {
                for (String line : Files.readAllLines(p)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;
                    int sep = trimmed.indexOf(':');
                    if (sep > 0) {
                        String key = trimmed.substring(0, sep);
                        String value = trimmed.substring(sep + 1);
                        cache.put(key, value);
                    }
                }
            } catch (IOException e) {
                // if reading fails, return empty cache (log or rethrow in real code)
                System.err.println("Warning: couldn't read prehash file: " + e.getMessage());
            }
        }
        return cache;
    }
}
