package com.example.clb.producer;

import com.example.clb.model.Task;
import com.example.clb.service.TaskDispatcher;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TaskProducer implements Runnable {
    private static final String[] TASK_TYPES = {"DataProcessing", "ReportGeneration", "ImageProcessing", "DataBackup", "EmailSending"};
    private static final Random random = new Random();
    
    private final TaskDispatcher dispatcher;
    private final int taskCount;
    private final String producerId;
    private volatile boolean isRunning = true;

    public TaskProducer(TaskDispatcher dispatcher, int taskCount, String producerId) {
        this.dispatcher = dispatcher;
        this.taskCount = taskCount;
        this.producerId = producerId;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " started producing tasks");
        
        for (int i = 0; i < taskCount && isRunning; i++) {
            try {
                // Randomly decide task priority (1-10, higher is more important)
                int priority = random.nextInt(10) + 1;
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

    public void stop() {
        isRunning = false;
    }
}
