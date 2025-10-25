// File: src/main/java/org/example/report/StatusReporter.java
package org.example.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// Implements a Live Status Reporter [cite: 62] that runs in a separate thread [cite: 66]
public class StatusReporter {

    private final long totalTasks;
    private final AtomicInteger passwordsFound;
    private final AtomicInteger usersChecked;

    public StatusReporter(long totalTasks, AtomicInteger passwordsFound, AtomicInteger usersChecked) {
        this.totalTasks = totalTasks;
        this.passwordsFound = passwordsFound;
        this.usersChecked = usersChecked;
    }

    public void start(ScheduledExecutorService scheduler) {
        Runnable reporter = () -> {
            long remaining = totalTasks - usersChecked.get();
            double percent = totalTasks == 0 ? 100.0 : (double) usersChecked.get() / totalTasks * 100.0;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = LocalDateTime.now().format(formatter);
            // Use the required format and non-blocking print [cite: 63, 64]
            System.out.printf("\r[%s] %.2f%% complete | Passwords Found: %d | Users Remaining: %d",
                    timestamp, percent, passwordsFound.get(), remaining);
        };
        // Schedule to run periodically
        scheduler.scheduleAtFixedRate(reporter, 1, 1, TimeUnit.SECONDS);
    }
}