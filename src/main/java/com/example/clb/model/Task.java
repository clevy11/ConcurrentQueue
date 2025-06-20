package com.example.clb.model;

import java.time.Instant;
import java.util.UUID;

public class Task implements Comparable<Task> {
    private final UUID id;
    private final String name;
    private final int priority;  // Higher number means higher priority
    private final Instant createdTimestamp;
    private final String payload;
    private int retryCount;

    public Task(String name, int priority, String payload) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.priority = priority;
        this.createdTimestamp = Instant.now();
        this.payload = payload;
        this.retryCount = 0;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public Instant getCreatedTimestamp() {
        return createdTimestamp;
    }

    public String getPayload() {
        return payload;
    }

    public int getRetryCount() {
        return retryCount;
    }


    public void incrementRetryCount() {
        this.retryCount++;
    }

    @Override
    public int compareTo(Task other) {
        // Higher priority tasks come first
        return Integer.compare(other.priority, this.priority);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", priority=" + priority +
                ", createdTimestamp=" + createdTimestamp +
                ", retryCount=" + retryCount +
                '}';
    }
}
