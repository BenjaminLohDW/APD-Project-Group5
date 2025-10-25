// File: src/main/java/org/example/core/CrackingEngine.java
package org.example.core;

import org.example.model.CrackedCredential;
import org.example.model.User;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

// The Core Concurrent Cracking Engine [cite: 40]
public class CrackingEngine {

    private final Map<String, User> users;
    private final ConcurrentHashMap<String, String> preHashedDictionary;
    private final AtomicInteger passwordsFound;
    private final AtomicInteger usersChecked;
    private final ConcurrentLinkedQueue<CrackedCredential> crackedQueue;

    public CrackingEngine(
            Map<String, User> users, 
            ConcurrentHashMap<String, String> preHashedDictionary,
            AtomicInteger passwordsFound,
            AtomicInteger usersChecked,
            ConcurrentLinkedQueue<CrackedCredential> crackedQueue) {
        this.users = users;
        this.preHashedDictionary = preHashedDictionary;
        this.passwordsFound = passwordsFound;
        this.usersChecked = usersChecked;
        this.crackedQueue = crackedQueue;
    }

    // Handles the fixed O(U) complexity lookup (High-Performance Concurrency [cite: 45])
    public void startAttack(ExecutorService executor) {
        for (User user : users.values()) {
            executor.submit(() -> {
                try {
                    // O(1) Lookup: Efficient lookup replaces the nested loop.
                    String crackedPassword = preHashedDictionary.get(user.hashedPassword());

                    if (crackedPassword != null) {
                        // Use AtomicBoolean compareAndSet for lock-free, thread-safe update 
                        // (Eliminate Race Conditions [cite: 48, 49])
                        if (user.isCracked().compareAndSet(false, true)) { 
                            crackedQueue.add(new CrackedCredential(
                                user.username(), user.hashedPassword(), crackedPassword
                            ));
                            passwordsFound.incrementAndGet();
                        }
                    }
                } finally {
                    usersChecked.incrementAndGet();
                }
            });
        }
    }
}