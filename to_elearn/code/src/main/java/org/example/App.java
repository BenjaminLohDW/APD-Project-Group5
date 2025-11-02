// --- File: App.java ---
package org.example; // <-- Adjusted to org.example

import org.example.core.CrackingEngine;
import org.example.core.DictionaryProcessor;
import org.example.io.HashManager;
import org.example.io.OutputWriter;
import org.example.model.CrackedCredential;
import org.example.model.User;
import org.example.report.StatusReporter;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;


public class App {

    // Global Atomic Counters for thread-safe shared state (Eliminate Race Conditions)
    static AtomicInteger passwordsFound = new AtomicInteger(0);
    static LongAdder hashesComputed = new LongAdder();
    static AtomicInteger usersChecked = new AtomicInteger(0);
    static ConcurrentLinkedQueue<CrackedCredential> crackedQueue = new ConcurrentLinkedQueue<>();

    private static final int THREAD_COUNT = Math.max(
        8 , 
        (int) (Runtime.getRuntime().availableProcessors() * 0.6)
    );

    public static void main(String[] args) throws Exception {

        // --- 1. CONFIGURATION AND INITIALIZATION ---
        // Use command-line arguments as per instruction
        if (args.length < 3) {
            System.out.println("Usage: java -jar <jar-file-name>.jar <input_file> <dictionary_file> <output_file>");
            System.exit(1);
        }
        String usersPath = args[0];
        String dictionaryPath = args[1];
        String passwordsPath = args[2];

        long startTime = System.currentTimeMillis();

        // --- 2. DATA LOADING (Target Hash Loading/Management Component) ---
        // long startTime2 = System.currentTimeMillis();

        HashManager hashManager = new HashManager();
        Map<String, User> users = hashManager.loadUsers(usersPath);

        DictionaryProcessor dictionaryProcessor = new DictionaryProcessor(hashesComputed);
        List<String> dictionaryWords = dictionaryProcessor.loadDictionary(dictionaryPath);

        ConcurrentHashMap<String, String> preHashedDictionary;
        // System.out.println("time loading data (milliseconds): " + (System.currentTimeMillis() - startTime2));

        // --- 3. PRE-HASHING (Sequential for optimal performance) ---
        
        // long startTime3 = System.currentTimeMillis();

        // Pre-hashing runs sequentially on main thread - faster due to no thread contention
        preHashedDictionary = dictionaryProcessor.preHashDictionary(dictionaryWords);
        // System.out.println("time pre-hashing dictionary (milliseconds): " + (System.currentTimeMillis() - startTime3));
        
        
        // Create custom pool AFTER pre-hashing for the cracking phase
        System.out.println("Starting attack with custom ForkJoinPool (" + THREAD_COUNT + " threads)...");
        
        // --- 4. LIVE STATUS REPORTING ---
        long totalUsers = users.size();
        StatusReporter reporter = new StatusReporter(totalUsers, passwordsFound, usersChecked);
        

        // --- 5. CRACKING (Core Concurrent Cracking Engine) ---
        try (ForkJoinPool customPool = new ForkJoinPool(THREAD_COUNT)) {
            CrackingEngine crackingEngine = new CrackingEngine(
                    users,
                    preHashedDictionary,
                    passwordsFound,
                    usersChecked,
                    crackedQueue,
                    reporter,
                    totalUsers   
            );
            customPool.submit(() -> crackingEngine.startAttack()).get();
        }        System.out.println("\n\nAttack complete.");
        System.out.println("Total passwords found: " + passwordsFound.get());
    System.out.println("Total dictionary hashes computed: " + hashesComputed.sum());
        System.out.println("Total time spent (milliseconds): " + (System.currentTimeMillis() - startTime));

        if (passwordsFound.get() > 0) {
            OutputWriter.writeCrackedPasswordsToCSV(passwordsPath, crackedQueue);
        }
    }
}