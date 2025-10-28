// File: src/main/java/org/example/core/CrackingEngine.java
package org.example.core;

import org.example.model.CrackedCredential;
import org.example.model.User;
import org.example.report.StatusReporter; 
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

// The Core Concurrent Cracking Engine
public class CrackingEngine {

    private final Map<String, User> users;
    private final ConcurrentHashMap<String, String> preHashedDictionary;
    private final AtomicInteger passwordsFound;
    private final AtomicInteger usersChecked;
    private final ConcurrentLinkedQueue<CrackedCredential> crackedQueue;
    private final StatusReporter reporter; 
    private final long totalUsers;  

    public CrackingEngine(
            Map<String, User> users,
            ConcurrentHashMap<String, String> preHashedDictionary,
            AtomicInteger passwordsFound,
            AtomicInteger usersChecked,
            ConcurrentLinkedQueue<CrackedCredential> crackedQueue,
            StatusReporter reporter, 
            long totalUsers) {       
        this.users = users;
        this.preHashedDictionary = preHashedDictionary;
        this.passwordsFound = passwordsFound;
        this.usersChecked = usersChecked;
        this.crackedQueue = crackedQueue;
        this.reporter = reporter;
        this.totalUsers = totalUsers; 
    }

    // Handles the fixed O(U) complexity lookup (High-Performance Concurrency)
    public void startAttack() {
        final int BATCH_SIZE = 1000; // Define the desired batch size

        users.values().parallelStream().forEach(user -> {
            try {
                // O(1) Lookup: Efficient lookup replaces the nested loop.
                String crackedPassword = preHashedDictionary.get(user.hashedPassword());

                if (crackedPassword != null) {
                    // Use AtomicBoolean compareAndSet for lock-free, thread-safe update
                    if (user.isCracked().compareAndSet(false, true)) { 
                        crackedQueue.add(new CrackedCredential(
                            user.username(), user.hashedPassword(), crackedPassword
                        ));
                        passwordsFound.incrementAndGet();
                    }
                }
            } finally {
                int done = usersChecked.incrementAndGet();
                
                // Keep the batch reporting logic
                if (done % BATCH_SIZE == 0 || done == totalUsers) {
                    reporter.reportNow();
                }
            }
        });
        
    }
}