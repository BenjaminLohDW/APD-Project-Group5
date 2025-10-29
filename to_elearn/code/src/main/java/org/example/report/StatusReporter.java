// File: src/main/java/org/example/report/StatusReporter.java
package org.example.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

// Implements a Live Status Reporter that reports every N tasks
public class StatusReporter {

    private final long totalTasks;
    private final AtomicInteger passwordsFound;
    private final AtomicInteger usersChecked;
    private final int reportInterval;
    private final DateTimeFormatter formatter;
    private long lastReportedMilestone = 0; // Regular long, protected by synchronized

    public StatusReporter(long totalTasks, AtomicInteger passwordsFound, AtomicInteger usersChecked) {
        this(totalTasks, passwordsFound, usersChecked, 1000);
    }

    public StatusReporter(long totalTasks, AtomicInteger passwordsFound, AtomicInteger usersChecked, int reportInterval) {
        this.totalTasks = totalTasks;
        this.passwordsFound = passwordsFound;
        this.usersChecked = usersChecked;
        this.reportInterval = reportInterval;
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Check if we should report based on milestones.
     * Thread-safe with synchronized block.
     */
    public void checkAndReport() {
        long currentCount = usersChecked.get();
        long currentMilestone = (currentCount / reportInterval) * reportInterval;
        
        synchronized (this) {
            // Report if we've crossed a new milestone
            if (currentMilestone > lastReportedMilestone && currentMilestone > 0) {
                lastReportedMilestone = currentMilestone;
                doReport(currentMilestone);
            }
            
            // Special case: report on completion
            if (currentCount == totalTasks && lastReportedMilestone < totalTasks) {
                lastReportedMilestone = totalTasks;
                doReport(totalTasks);
            }
        }
    }

    /**
     * Public method to manually trigger a status update (used by CrackingEngine).
     * This reports the exact current count if it hasn't been reported yet.
     */
    public void reportNow() {
        long currentCount = usersChecked.get();
        
        synchronized (this) {
            // Only report if this count hasn't been reported
            if (currentCount > lastReportedMilestone) {
                lastReportedMilestone = currentCount;
                doReport(currentCount);
            }
        }
    }

    // Extracted the printing logic into a reusable method
    private void doReport(long milestone) {
        long remaining = totalTasks - milestone;
        double percent = totalTasks == 0 ? 100.0 : (double) milestone / totalTasks * 100.0;
        String timestamp = LocalDateTime.now().format(formatter);
        
        System.out.printf("[%s] %.2f%% complete | Passwords Found: %d | Users Remaining: %d%n",
                timestamp, percent, passwordsFound.get(), remaining);
    }
}