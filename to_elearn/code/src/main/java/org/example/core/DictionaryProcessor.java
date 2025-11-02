// File: src/main/java/org/example/core/DictionaryProcessor.java
package org.example.core;

import org.example.util.Hasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class DictionaryProcessor {

    private final LongAdder hashesComputed;

    public DictionaryProcessor(LongAdder hashesComputed) {
        this.hashesComputed = hashesComputed;
    }

    public List<String> loadDictionary(String filePath) throws IOException {
        // Safe and tidy resource handling is ensured by Files.readAllLines
        return Files.readAllLines(Paths.get(filePath));
    }

    // Handles the fixed O(D) complexity pre-hashing (The algorithmic fix [cite: 42, 43])
    // Using sequential processing to avoid thread contention on ConcurrentHashMap writes
    public ConcurrentHashMap<String, String> preHashDictionary(
            List<String> dictionaryWords) {

        // Pre-size the map to avoid resizing overhead
        ConcurrentHashMap<String, String> preHashedDictionary = 
            new ConcurrentHashMap<>((int)(dictionaryWords.size() / 0.75) + 1);
        
        // System.out.println("Starting sequential pre-hashing (optimized for memory bandwidth)...");
        
        // Sequential processing is faster for write-heavy operations
        // Avoids ConcurrentHashMap contention and cache line bouncing
        for (String word : dictionaryWords) {
            String hash = Hasher.sha256(word);
            // Hash -> Plaintext mapping
            preHashedDictionary.putIfAbsent(hash, word);
            // Use the shared counter
            hashesComputed.increment();
        }

        return preHashedDictionary;
    }
}