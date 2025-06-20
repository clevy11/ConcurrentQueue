package com.example.clb;

import com.example.clb.producer.TaskProducer;
import com.example.clb.service.TaskDispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final int NUM_PRODUCERS = 3;
    private static final int TASKS_PER_PRODUCER = 15;
    private static final int NUM_WORKERS = 5;
    private static final int QUEUE_CAPACITY = 50;
    private static final int MAX_RETRIES = 3;

    public static void main(String[] args) {
        System.out.println("🚀 Starting ConcurQueue - A Multithreaded Job Processing Platform");
        
        // Create task dispatcher with worker pool
        TaskDispatcher dispatcher = new TaskDispatcher(NUM_WORKERS, QUEUE_CAPACITY, MAX_RETRIES);
        
        // Create producers
        List<TaskProducer> producers = new ArrayList<>();
        ExecutorService producerPool = Executors.newFixedThreadPool(NUM_PRODUCERS);
        
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            TaskProducer producer = new TaskProducer(dispatcher, TASKS_PER_PRODUCER, "Producer-" + (i + 1));
            producers.add(producer);
            producerPool.submit(producer);
        }
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutdown requested. Stopping producers and workers...");
            
            // Stop producers
            producers.forEach(TaskProducer::stop);
            producerPool.shutdown();
            
            try {
                if (!producerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    producerPool.shutdownNow();
                }
                
                // Shutdown dispatcher (which will shutdown workers)
                dispatcher.shutdown();
                
                System.out.println("✅ Application shutdown complete.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("❌ Error during shutdown: " + e.getMessage());
            }
        }));
        
        // Wait for producers to finish
        producerPool.shutdown();
        try {
            if (!producerPool.awaitTermination(2, TimeUnit.MINUTES)) {
                System.err.println("⚠️ Warning: Some producers did not finish in time");
            }
            
            // Allow some time for remaining tasks to be processed
            System.out.println("\nAll producers finished. Waiting for remaining tasks to complete...");
            Thread.sleep(5000);
            
            // Initiate shutdown
            System.out.println("\nInitiating shutdown...");
            System.exit(0);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main thread was interrupted: " + e.getMessage());
            System.exit(1);
        }
    }
}