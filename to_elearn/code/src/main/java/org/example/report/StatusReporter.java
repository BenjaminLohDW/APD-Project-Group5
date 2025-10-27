// File: src/main/java/org/example/report/StatusReporter.java
package org.example.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class StatusReporter {

    private final long totalTasks;
    private final AtomicInteger passwordsFound;
    private final AtomicInteger usersChecked;
    private final long reportIntervalTasks = 1000; // e.g., 1000

    // lastReported holds the highest usersChecked value we've reported for
    private volatile long lastReported = 0L;

    public StatusReporter(long totalTasks,
                          AtomicInteger passwordsFound,
                          AtomicInteger usersChecked) {
        this.totalTasks = totalTasks;
        this.passwordsFound = passwordsFound;
        this.usersChecked = usersChecked;
    }

    /**
     * Start the periodic reporter. The scheduler runs the reporter frequently,
     * but the reporter only prints when the processed-count crosses each interval boundary.
     *
     * @param scheduler ScheduledExecutorService to schedule the reporter on
     */
    public void start(ScheduledExecutorService scheduler) {
        Runnable reporter = () -> {
            long processed = usersChecked.get();
            // Only report once every `reportIntervalTasks` tasks
            if (processed - lastReported >= reportIntervalTasks) {
                // Advance lastReported to the latest interval boundary (avoids frequent prints)
                long intervals = (processed / reportIntervalTasks);
                lastReported = intervals * reportIntervalTasks;

                double percent = totalTasks == 0 ? 100.0
                        : (double) processed / totalTasks * 100.0;

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String timestamp = LocalDateTime.now().format(formatter);

                // Clear the console line to avoid leftover characters
                System.out.print("\r\033[K");
                System.out.printf("[%s] %.2f%% complete | Passwords Found: %d | Users Processed: %d | Users Remaining: %d",
                        timestamp, percent, passwordsFound.get(), processed, Math.max(0L, totalTasks - processed));
                System.out.flush();
            }
        };

        // Poll frequently but only print rarely; 250ms is a reasonable poll interval.
        scheduler.scheduleAtFixedRate(reporter, 100, 250, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop the reporter by shutting down the scheduler.
     * Caller should call shutdown on the same scheduler passed to start().
     */
    public void stop() {
        // Intentionally left blank: caller should call scheduler.shutdown() externally to avoid double control.
        // This method is a placeholder if you want the reporter to own the scheduler in the future.
    }
}
