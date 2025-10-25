// File: src/main/java/org/example/core/DictionaryProcessor.java
package org.example.core;

import org.example.util.Hasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DictionaryProcessor {

    private final AtomicInteger hashesComputed;

    public DictionaryProcessor(AtomicInteger hashesComputed) {
        this.hashesComputed = hashesComputed;
    }

    public List<String> loadDictionary(String filePath) throws IOException {
        // Safe and tidy resource handling is ensured by Files.readAllLines
        return Files.readAllLines(Paths.get(filePath));
    }

    // Handles the fixed O(D) complexity pre-hashing (The algorithmic fix [cite: 42, 43])
    public ConcurrentHashMap<String, String> preHashDictionary(
            List<String> dictionaryWords, 
            ExecutorService executor) throws InterruptedException {

        ConcurrentHashMap<String, String> preHashedDictionary = new ConcurrentHashMap<>();
        
        System.out.println("Starting parallel pre-hashing...");
        
        for (String word : dictionaryWords) {
            executor.submit(() -> {
                try {
                    String hash = Hasher.sha256(word);
                    // Hash -> Plaintext mapping.
                    preHashedDictionary.putIfAbsent(hash, word);
                    hashesComputed.incrementAndGet();
                } catch (NoSuchAlgorithmException ignored) {
                }
            });
        }
        
        // Wait for all pre-hashing tasks to complete before proceeding to the attack phase
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        
        return preHashedDictionary;
    }
}