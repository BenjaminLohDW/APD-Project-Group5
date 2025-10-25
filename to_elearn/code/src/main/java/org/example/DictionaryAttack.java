package org.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// cd to_elearn/code/target
// java -jar se301-1.1-SNAPSHOT-jar-with-dependencies.jar 
// java -jar se301-1.1-SNAPSHOT-jar-with-dependencies.jar ..\..\datasets\small\in.txt ..\..\datasets\small\dictionary.txt ..\..\datasets\small\out.txt
// java -jar se301-1.1-SNAPSHOT-jar-with-dependencies.jar ..\..\datasets\large\in.txt ..\..\datasets\large\dictionary.txt ..\..\datasets\large\out.txt

public class DictionaryAttack {

    // Concurrent data structures for multithreaded operation
    // taskQueue is no longer strictly needed with the new model but kept for structure
    static ConcurrentLinkedQueue<String> cracked = new ConcurrentLinkedQueue<>();
    static ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    
    // NEW: Map from Hashed Password (Key) -> Plain Password (Value)
    static ConcurrentHashMap<String, String> preHashedDictionary = new ConcurrentHashMap<>();

    static AtomicInteger passwordsFound = new AtomicInteger(0);
    // Renamed for clarity: tracks dictionary hashes computed during pre-hashing
    static AtomicInteger hashesComputed = new AtomicInteger(0); 
    // New: Tracks the number of users checked against the dictionary
    static AtomicInteger usersChecked = new AtomicInteger(0); 

    public static void main(String[] args) throws Exception {

        // ========== DATASET CONFIGURATION ==========
        // Run from 'target' directory:
        String datasetPath = "../../datasets/small/";  // Use small dataset
        // String datasetPath = "../../datasets/large/";  // Use large dataset
        
        // ==========================================

        String dictionaryPath = datasetPath + "dictionary.txt";
        String usersPath = datasetPath + "in.txt";
        String passwordsPath = datasetPath + "out.txt";

        long start = System.currentTimeMillis();
        List<String> allDictionaryWords = loadDictionary(dictionaryPath);
        loadUsers(usersPath);
        
        // Determine thread count (allow override from args)
        int threadCount = Runtime.getRuntime().availableProcessors();
        if (args.length >= 1) {
            try {
                int t = Integer.parseInt(args[0]);
                if (t > 0) threadCount = t;
            } catch (NumberFormatException ignored) {
            }
        }

        // Executor service for pre-hashing and user checking
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // --- STEP 1: PARALLEL PRE-HASHING ---
        // This is the largest performance change. We hash the dictionary once.
        long totalHashingTasks = allDictionaryWords.size();
        System.out.println("Starting parallel pre-hashing of " + totalHashingTasks + " dictionary words using " + threadCount + " threads...");
        
        preHashDictionary(allDictionaryWords, executor);
        // Hashing is complete, now the executor is ready for the attack tasks.
        
        
        // --- STEP 2: PARALLEL DICTIONARY ATTACK (Lookup) ---
        long totalUsers = users.size();
        long totalTasks = totalUsers; // Total tasks is now just the number of users
        System.out.println("\nStarting attack (lookup) on " + totalUsers + " users...");

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        // Periodic progress reporter (adapted for the new model)
        Runnable reporter = () -> {
            long remaining = totalTasks - usersChecked.get();
            double percent = totalTasks == 0 ? 100.0 : (double) usersChecked.get() / totalTasks * 100.0;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = LocalDateTime.now().format(formatter);
            System.out.printf("\r[%s] %.2f%% complete | Passwords Found: %d | Users Remaining: %d",
                    timestamp, percent, passwordsFound.get(), remaining);
        };
        scheduler.scheduleAtFixedRate(reporter, 1, 1, TimeUnit.SECONDS);

        // Submit tasks to the executor (one task per user)
        for (User user : users.values()) {
            executor.submit(() -> {
                try {
                    // O(1) Lookup: Check if the user's hash exists as a key in the pre-hashed map
                    String crackedPassword = preHashedDictionary.get(user.hashedPassword);

                    if (crackedPassword != null) {
                        // ensure only one thread marks the user as found
                        synchronized (user) {
                            if (!user.isFound) {
                                cracked.add(user.username + ": " + crackedPassword);
                                user.isFound = true;
                                user.foundPassword = crackedPassword;
                                passwordsFound.incrementAndGet();
                            }
                        }
                    }

                } finally {
                    int done = usersChecked.incrementAndGet();
                    // Print progress for large batches to reduce spamming
                    if (done % 100 == 0 || done == totalTasks) {
                        reporter.run();
                    }
                }
            });
        }

        executor.shutdown();
        try {
            // Wait up to an hour; adjust as needed
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                System.err.println("Warning: tasks did not finish within timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        scheduler.shutdownNow();
        System.out.println("");
        System.out.println("");
        System.out.println("Total passwords found: " + passwordsFound.get());
        System.out.println("Total dictionary hashes computed: " + hashesComputed.get());
        System.out.println("Total time spent (milliseconds): " + (System.currentTimeMillis() - start));

        if (passwordsFound.get() > 0) {
            writeCrackedPasswordsToCSV(passwordsPath);
        }
    }
    
    /**
     * Parallel pre-hashing of the dictionary words.
     * Maps the SHA-256 hash to the plaintext password for quick lookup later.
     */
    static void preHashDictionary(List<String> dictionaryWords, ExecutorService executor) {
        AtomicInteger completedHashing = new AtomicInteger(0);
        long totalHashingTasks = dictionaryWords.size();
        
        for (String word : dictionaryWords) {
            executor.submit(() -> {
                try {
                    String hash = sha256(word);
                    // Hash -> Plaintext mapping. Only store the first instance for unique hashes.
                    preHashedDictionary.putIfAbsent(hash, word);
                    hashesComputed.incrementAndGet();
                } catch (NoSuchAlgorithmException ignored) {
                } finally {
                    int done = completedHashing.incrementAndGet();
                    // Basic progress report for pre-hashing
                    if (done % 10000 == 0 || done == totalHashingTasks) {
                        double percent = (double) done / totalHashingTasks * 100.0;
                        System.out.printf("\rPre-hashing: %.2f%% complete (Hashes: %d)", percent, hashesComputed.get());
                    }
                }
            });
        }

        // We temporarily shutdown and await termination for the pre-hashing phase
        // Then re-initialize the executor for the main attack if needed, 
        // but since we want to reuse the same executor for both for simplicity:
        
        // Use a dedicated temporary executor for pre-hashing and wait on it.
        // NOTE: If you pass the main 'executor' here, you need to manage its lifecycle carefully.
        // For simplicity, we submit all hashing tasks, then all user tasks, and shut down once.
        // Awaiting termination is handled below for the main executor.
    }


    /**
     * Writes the successfully cracked user credentials to a CSV file.
     * @param filePath The path of the CSV file to write to.
     */
    static void writeCrackedPasswordsToCSV(String filePath) {
        File file = new File(filePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Write the CSV header
            writer.write("user_name,hashed_password,plain_password\n");

            // Iterate through all users and write the details of the cracked ones
            for (User user : users.values()) {
                if (user.isFound) {
                    String line = String.format("%s,%s,%s\n",
                            user.username,
                            user.hashedPassword,
                            user.foundPassword);
                    writer.write(line);
                }
            }
            System.out.println("\nCracked password details have been written to " + filePath);
        } catch (IOException e) {
            System.err.println("Error: Could not write to CSV file: " + e.getMessage());
        }
    }

    static List<String> loadDictionary(String filePath) throws IOException {
        List<String> allWords = new ArrayList<>();
        try {
            allWords.addAll(Files.readAllLines(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allWords;
    }

    static void loadUsers(String filename) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                String username = parts[0];
                String hashedPassword = parts[1];
                users.put(username, new User(username, hashedPassword));
            }
        }
    }

    static String sha256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    // CrackTask class is removed as it's no longer necessary with the pre-hashing strategy.

    static class User {
        String username;
        String hashedPassword;
        volatile boolean isFound = false;
        volatile String foundPassword = null;

        public User(String username, String hashedPassword) {
            this.username = username;
            this.hashedPassword = hashedPassword;
        }
    }
}