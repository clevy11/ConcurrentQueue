package com.example.clb.producer;

import com.example.clb.model.Task;
import com.example.clb.service.TaskDispatcher;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TaskProducer implements Runnable {
    private static final String[] TASK_TYPES = {"DataProcessing", "ReportGeneration", "ImageProcessing", "DataBackup", "EmailSending"};
    private static final Random random = new Random();
    
    public enum PriorityStrategy {
        HIGH, LOW, MIXED
    }
    
    private final TaskDispatcher dispatcher;
    private final int taskCount;
    private final String producerId;
    private final PriorityStrategy strategy;
    private volatile boolean isRunning = true;

    public TaskProducer(TaskDispatcher dispatcher, int taskCount, String producerId, PriorityStrategy strategy) {
        this.dispatcher = dispatcher;
        this.taskCount = taskCount;
        this.producerId = producerId;
        this.strategy = strategy;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " started producing tasks");
        
        for (int i = 0; i < taskCount && isRunning; i++) {
            try {
                // Randomly decide task priority based on strategy
                int priority = getTaskPriority();
                String taskType = TASK_TYPES[random.nextInt(TASK_TYPES.length)];
                String payload = String.format("Task-%s-%d", taskType, i);
                
                Task task = new Task("Task from " + producerId + "-" + i, priority, payload);
                dispatcher.submitTask(task);
                
                // Random delay between task submissions
                TimeUnit.MILLISECONDS.sleep(100 + random.nextInt(400));
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(Thread.currentThread().getName() + " was interrupted");
                break;
            } catch (Exception e) {
                System.err.println("Error in producer " + producerId + ": " + e.getMessage());
            }
        }
        
        System.out.println(Thread.currentThread().getName() + " finished producing tasks");
    }

    private int getTaskPriority() {
        return switch (strategy) {
            case HIGH -> 8 + random.nextInt(3);  // Priority 8, 9, 10
            case LOW  -> 1 + random.nextInt(3);   // Priority 1, 2, 3
            case MIXED-> 1 + random.nextInt(10); // Priority 1-10
        };
    }

    public void stop() {
        isRunning = false;
    }
}
