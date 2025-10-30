package org.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.example.util.Hasher;
 
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
 
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
    static java.util.concurrent.atomic.LongAdder hashesComputed = new java.util.concurrent.atomic.LongAdder(); 
    // New: Tracks the number of users checked against the dictionary
    static AtomicInteger usersChecked = new AtomicInteger(0); 

    public static void main(String[] args) throws Exception {

        // ========== DATASET CONFIGURATION ==========
        // Run from 'target' directory:
        // String datasetPath = "../../datasets/small/";  // Use small dataset
        String datasetPath = "../../datasets/large/";  // Use large dataset
        
        // ==========================================

        String dictionaryPath = datasetPath + "dictionary.txt";
        String usersPath = datasetPath + "in.txt";
        String passwordsPath = datasetPath + "out.txt";

        long start = System.currentTimeMillis();
        List<String> allDictionaryWords = loadDictionary(dictionaryPath);
        loadUsers(usersPath);
        
        // Optional: override parallelism via JVM flags or ForkJoin settings if needed

        // --- STEP 1: PARALLEL PRE-HASHING (blocking) ---
        long totalHashingTasks = allDictionaryWords.size();
        System.out.println("Starting parallel pre-hashing of " + totalHashingTasks + " dictionary words using parallel streams...");

        // Use the fast ThreadLocal Hasher and parallelStream to fully complete pre-hashing before lookups
        allDictionaryWords.parallelStream().forEach(word -> {
            String hash = Hasher.sha256(word);
            preHashedDictionary.putIfAbsent(hash, word);
            hashesComputed.increment();
        });

        // --- STEP 2: PARALLEL LOOKUP (one lookup per user) ---
        long totalUsers = users.size();
        long totalTasks = totalUsers;
        System.out.println("\nStarting attack (lookup) on " + totalUsers + " users using parallel streams...");

        // Status reporter (prints every 1000 completed users)
        org.example.report.StatusReporter reporter = new org.example.report.StatusReporter(totalTasks, passwordsFound, usersChecked, 1000);

        // Perform lookups in parallel using the common ForkJoinPool
        users.values().parallelStream().forEach(user -> {
            try {
                String crackedPassword = preHashedDictionary.get(user.hashedPassword);
                if (crackedPassword != null) {
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
                usersChecked.incrementAndGet();
                // Let the StatusReporter decide whether to print a milestone (every 1000 tasks)
                reporter.checkAndReport();
            }
        });

    // Final report
    reporter.reportNow();
        System.out.println("");
        System.out.println("");
        System.out.println("Total passwords found: " + passwordsFound.get());
    System.out.println("Total dictionary hashes computed: " + hashesComputed.sum());
        System.out.println("Total time spent (milliseconds): " + (System.currentTimeMillis() - start));

        if (passwordsFound.get() > 0) {
            writeCrackedPasswordsToCSV(passwordsPath);
        }
    }
    
    /**
     * Parallel pre-hashing of the dictionary words.
     * Maps the SHA-256 hash to the plaintext password for quick lookup later.
     */
    


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