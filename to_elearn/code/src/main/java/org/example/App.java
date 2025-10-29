// --- File: App.java ---
package org.example; // <-- Adjusted to org.example

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.example.core.CrackingEngine;
import org.example.core.DictionaryProcessor;
import org.example.io.HashManager;
import org.example.io.OutputWriter;
import org.example.model.CrackedCredential;
import org.example.model.User;
import org.example.report.StatusReporter;
import org.example.util.Hasher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;


public class App {

    // Global Atomic Counters for thread-safe shared state (Eliminate Race Conditions)
    static AtomicInteger passwordsFound = new AtomicInteger(0);
    static AtomicInteger hashesComputed = new AtomicInteger(0);
    static AtomicInteger usersChecked = new AtomicInteger(0);
    static ConcurrentLinkedQueue<CrackedCredential> crackedQueue = new ConcurrentLinkedQueue<>();

    private static final int THREAD_COUNT = Math.max(
        8 , 
        (int) (Runtime.getRuntime().availableProcessors() * 1.25)
    );

    public static void main(String[] args) throws Exception {

        // -- warmup ---
        Hasher.warmup();

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
        HashManager hashManager = new HashManager();
        Map<String, User> users = hashManager.loadUsers(usersPath);

        DictionaryProcessor dictionaryProcessor = new DictionaryProcessor(hashesComputed);
        List<String> dictionaryWords = dictionaryProcessor.loadDictionary(dictionaryPath);

        System.out.println("Using custom ForkJoinPool with " + THREAD_COUNT + " threads.");
        ForkJoinPool customPool = new ForkJoinPool(THREAD_COUNT);

        ConcurrentHashMap<String, String> preHashedDictionary;
        // --- 3. PRE-HASHING  ---
        Callable<ConcurrentHashMap<String, String>> preHashTask = 
            () -> dictionaryProcessor.preHashDictionary(dictionaryWords);
                    
        // .get() blocks until the hashing is complete
        preHashedDictionary = customPool.submit(preHashTask).get();

                
        // --- 4. LIVE STATUS REPORTING ---
        long totalUsers = users.size();
        StatusReporter reporter = new StatusReporter(totalUsers, passwordsFound, usersChecked);
        // reporter.start(scheduler); // Live status reporter runs in a separate thread

        // --- 5. CRACKING (Core Concurrent Cracking Engine) ---
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
        customPool.shutdownNow();

        System.out.println("\n\nAttack complete.");
        System.out.println("Total passwords found: " + passwordsFound.get());
        System.out.println("Total dictionary hashes computed: " + hashesComputed.get());
        System.out.println("Total time spent (milliseconds): " + (System.currentTimeMillis() - startTime));

        if (passwordsFound.get() > 0) {
            OutputWriter.writeCrackedPasswordsToCSV(passwordsPath, crackedQueue);
        }
    }
}