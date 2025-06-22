package com.example.clb.service;

import com.example.clb.model.Task;
import com.example.clb.model.TaskStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskDispatcher {
    private final BlockingQueue<Task> taskQueue;
    private final Map<UUID, TaskStatus> taskStatusMap;
    private final ExecutorService workerPool;
    private final AtomicInteger tasksProcessedCount = new AtomicInteger(0);
    private volatile boolean isShutdown = false;
    private final int maxRetries;

    // New fields for enhanced monitoring
    private final ScheduledExecutorService monitorScheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile Instant lastCompletionTime;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String STATUS_FILE_PATH = "task_status.json";

    public TaskDispatcher(int numWorkers, int queueCapacity, int maxRetries) {
        this.taskQueue = new PriorityBlockingQueue<>(queueCapacity);
        this.taskStatusMap = new ConcurrentHashMap<>();
        this.workerPool = Executors.newFixedThreadPool(numWorkers);
        this.maxRetries = maxRetries;
        this.lastCompletionTime = Instant.now();

        // Initialize worker threads
        for (int i = 0; i < numWorkers; i++) {
            workerPool.submit(this::processTasks);
        }

        // Start monitor thread
        startMonitoring();
    }

    public void submitTask(Task task) {
        if (isShutdown) {
            throw new IllegalStateException("TaskDispatcher is shutting down");
        }
        taskStatusMap.put(task.getId(), TaskStatus.SUBMITTED);
        try {
            taskQueue.put(task);
            System.out.println("Submitted task: " + task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while submitting task: " + e.getMessage());
        }
    }

    private void processTasks() {
        while (!isShutdown || !taskQueue.isEmpty()) {
            try {
                Task task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                if (task != null) {
                    processTask(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error processing task: " + e.getMessage());
            }
        }
    }

    private void processTask(Task task) {
        taskStatusMap.put(task.getId(), TaskStatus.PROCESSING);
        System.out.println(Thread.currentThread().getName() + " processing task: " + task);

        try {
            // Simulate work
            Thread.sleep(100 + (long) (Math.random() * 500));

            // Randomly fail some tasks for demonstration
            if (Math.random() < 0.1) { // 10% chance of failure
                throw new RuntimeException("Random failure for demonstration");
            }

            taskStatusMap.put(task.getId(), TaskStatus.COMPLETED);
            tasksProcessedCount.incrementAndGet();
            lastCompletionTime = Instant.now(); // Update last completion time
            System.out.println(Thread.currentThread().getName() + " completed task: " + task);

        } catch (Exception e) {
            handleTaskFailure(task, e);
        }
    }

    private void handleTaskFailure(Task task, Exception e) {
        System.err.println(Thread.currentThread().getName() + " failed to process task " + task.getId() + ": " + e.getMessage());

        task.incrementRetryCount();
        if (task.getRetryCount() <= maxRetries) {
            System.out.println("Retrying task " + task.getId() + " (attempt " + task.getRetryCount() + " of " + maxRetries + ")");
            taskStatusMap.put(task.getId(), TaskStatus.SUBMITTED);
            taskQueue.add(task);
        } else {
            System.err.println("Max retries (" + maxRetries + ") exceeded for task " + task.getId());
            taskStatusMap.put(task.getId(), TaskStatus.FAILED);
        }
    }

    private void startMonitoring() {
        // Task for logging status every 5 seconds
        monitorScheduler.scheduleAtFixedRate(() -> {
            System.out.println("\n--- Queue Status ---");
            System.out.println("Queue size: " + taskQueue.size());
            System.out.println("Tasks processed: " + tasksProcessedCount.get());
            System.out.println("Active threads in worker pool: " + ((ThreadPoolExecutor) workerPool).getActiveCount());
            System.out.println("Task status counts: " + getTaskStatusCounts());

            // Detect stalled workers
            long secondsSinceLastCompletion = Duration.between(lastCompletionTime, Instant.now()).getSeconds();
            if (secondsSinceLastCompletion > 30 && !taskQueue.isEmpty()) {
                System.err.println("!!! WARNING: No tasks completed in the last " + secondsSinceLastCompletion + " seconds. Workers might be stalled!");
            }
            System.out.println("-------------------\n");
        }, 5, 5, TimeUnit.SECONDS);

        // Task for exporting to JSON every minute
        monitorScheduler.scheduleAtFixedRate(this::exportStatusToJson, 1, 1, TimeUnit.MINUTES);
    }

    private void exportStatusToJson() {
        try (FileWriter writer = new FileWriter(STATUS_FILE_PATH)) {
            gson.toJson(this.taskStatusMap, writer);
            System.out.println("Successfully exported task status to " + STATUS_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Error exporting task status to JSON: " + e.getMessage());
        }
    }

    private Map<TaskStatus, Long> getTaskStatusCounts() {
        Map<TaskStatus, Long> counts = new java.util.EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            counts.put(status, taskStatusMap.values().stream().filter(s -> s == status).count());
        }
        return counts;
    }

    public void shutdown() {
        isShutdown = true;
        monitorScheduler.shutdown();
        workerPool.shutdown();
        try {
            if (!monitorScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                monitorScheduler.shutdownNow();
            }
            if (!workerPool.awaitTermination(10, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            monitorScheduler.shutdownNow();
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("TaskDispatcher has been shut down");
        exportStatusToJson(); // Final export on shutdown
    }

    /**
     * For testing purposes only - gets the total number of successfully processed tasks
     * @return number of tasks that have been successfully processed
     */
    public int getProcessedTaskCount() {
        return tasksProcessedCount.get();
    }
}
