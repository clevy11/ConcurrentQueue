package com.example.clb.demo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SynchronizationDemo {

    // --- Race Condition Demonstration ---
    private static class UnsafeCounter {
        private int count = 0;

        public void increment() {
            count++; // This is not an atomic operation
        }

        public int getCount() {
            return count;
        }
    }

    private static class SafeCounter {
        private final AtomicInteger count = new AtomicInteger(0);

        public void increment() {
            count.incrementAndGet();
        }

        public int getCount() {
            return count.get();
        }
    }

    public static void demonstrateRaceCondition() throws InterruptedException {
        System.out.println("\n--- Demonstrating Race Condition ---");
        final int numThreads = 10;
        final int incrementsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // 1. Unsafe Counter
        UnsafeCounter unsafeCounter = new UnsafeCounter();
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    unsafeCounter.increment();
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("Unsafe Counter Final Value: " + unsafeCounter.getCount());
        System.out.println("Expected Value: " + (numThreads * incrementsPerThread));
        System.out.println("The unsafe counter often shows a lower value due to lost updates (race conditions).");


        // 2. Safe Counter
        SafeCounter safeCounter = new SafeCounter();
        executor = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    safeCounter.increment();
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("\nSafe Counter (AtomicInteger) Final Value: " + safeCounter.getCount());
        System.out.println("The safe counter correctly reaches the expected value.");
        System.out.println("--- Race Condition Demo Finished ---\n");
    }


    // --- Deadlock Demonstration ---
    public static void demonstrateDeadlock() {
        System.out.println("\n--- Demonstrating Deadlock ---");
        Lock lock1 = new ReentrantLock();
        Lock lock2 = new ReentrantLock();

        Thread thread1 = new Thread(() -> {
            System.out.println("Thread 1: Trying to acquire lock 1...");
            lock1.lock();
            System.out.println("Thread 1: Acquired lock 1.");
            try {
                Thread.sleep(100); // Simulate some work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Thread 1: Trying to acquire lock 2...");
            lock2.lock();
            System.out.println("Thread 1: Acquired lock 2.");
            lock2.unlock();
            lock1.unlock();
        }, "Deadlock-Thread-1");

        Thread thread2 = new Thread(() -> {
            System.out.println("Thread 2: Trying to acquire lock 2...");
            lock2.lock();
            System.out.println("Thread 2: Acquired lock 2.");
            try {
                Thread.sleep(100); // Simulate some work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Thread 2: Trying to acquire lock 1...");
            lock1.lock();
            System.out.println("Thread 2: Acquired lock 1.");
            lock1.unlock();
            lock2.unlock();
        }, "Deadlock-Thread-2");

        thread1.start();
        thread2.start();

        System.out.println("Started two threads that will likely deadlock.");
        System.out.println("The application will hang here. To fix this, ensure all threads acquire locks in the same order.");
        System.out.println("--- Deadlock Demo Finished (theoretically) ---");
        // In a real app, you'd use a Watchdog or ThreadMXBean to detect this and recover.
    }

    public static void main(String[] args) throws InterruptedException {
        demonstrateRaceCondition();
        // Note: Running demonstrateDeadlock() will cause the program to hang.
        // demonstrateDeadlock();
    }
}
