package com.example.clb.service;

import com.example.clb.model.Task;
import com.example.clb.model.TaskStatus;

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

    public TaskDispatcher(int numWorkers, int queueCapacity, int maxRetries) {
        this.taskQueue = new PriorityBlockingQueue<>(queueCapacity);
        this.taskStatusMap = new ConcurrentHashMap<>();
        this.workerPool = Executors.newFixedThreadPool(numWorkers);
        this.maxRetries = maxRetries;
        
        // Initialize worker threads
        for (int i = 0; i < numWorkers; i++) {
            workerPool.submit(this::processTasks);
        }
        
        // Start monitor thread
        startMonitorThread();
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

    private void startMonitorThread() {
        Thread monitorThread = new Thread(() -> {
            while (!isShutdown) {
                try {
                    System.out.println("\n--- Queue Status ---");
                    System.out.println("Queue size: " + taskQueue.size());
                    System.out.println("Tasks processed: " + tasksProcessedCount.get());
                    System.out.println("Active threads: " + Thread.activeCount());
                    System.out.println("Pending tasks: " + taskQueue.size());
                    System.out.println("Task status counts: " + getTaskStatusCounts());
                    System.out.println("-------------------\n");
                    
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Monitor-Thread");
        monitorThread.setDaemon(true);
        monitorThread.start();
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
        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(10, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("TaskDispatcher has been shut down");
    }
    
    /**
     * For testing purposes only - gets the total number of successfully processed tasks
     * @return number of tasks that have been successfully processed
     */
    public int getProcessedTaskCount() {
        return tasksProcessedCount.get();
    }
}
