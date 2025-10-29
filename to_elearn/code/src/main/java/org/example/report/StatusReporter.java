// File: src/main/java/org/example/report/StatusReporter.java
package org.example.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

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

    public void checkAndReport() {
        long currentCount = usersChecked.get();
        long currentMilestone = (currentCount / reportInterval) * reportInterval;
        
        // Synchronized to ensure only one thread reports per milestone
        synchronized (this) {
            if (currentMilestone > lastReportedMilestone && currentMilestone > 0) {
                lastReportedMilestone = currentMilestone;
                doReport(currentMilestone);
            }
            
            // Special case: final report
            if (currentCount == totalTasks && lastReportedMilestone < totalTasks) {
                lastReportedMilestone = totalTasks;
                doReport(totalTasks);
            }
        }
    }

    private void doReport(long milestone) {
        long remaining = totalTasks - milestone;
        double percent = totalTasks == 0 ? 100.0 : (double) milestone / totalTasks * 100.0;
        String timestamp = LocalDateTime.now().format(formatter);
        
        System.out.printf("[%s] %.2f%% complete | Passwords Found: %d | Users Remaining: %d%n",
                timestamp, percent, passwordsFound.get(), remaining);
    }

    public void reportFinal() {
        synchronized (this) {
            long currentCount = usersChecked.get();
            if (currentCount > lastReportedMilestone) {
                lastReportedMilestone = currentCount;
                doReport(currentCount);
            }
        }
    }
}