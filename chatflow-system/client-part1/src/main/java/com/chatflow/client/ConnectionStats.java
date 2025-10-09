package com.chatflow.client;

import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionStats {

    private AtomicInteger totalConnections = new AtomicInteger(0);
    private AtomicInteger successfulConnections = new AtomicInteger(0);
    private AtomicInteger failedConnections = new AtomicInteger(0);
    private AtomicInteger reconnections = new AtomicInteger(0);
    private AtomicInteger totalRetries = new AtomicInteger(0);

    public void recordConnection() {
        totalConnections.incrementAndGet();
    }

    public void recordSuccessfulConnection() {
        successfulConnections.incrementAndGet();
    }

    public void recordFailedConnection() {
        failedConnections.incrementAndGet();
    }

    public void recordReconnection() {
        reconnections.incrementAndGet();
    }

    public void recordRetry() {
        totalRetries.incrementAndGet();
    }

    // Getters
    public int getTotalConnections() {
        return totalConnections.get();
    }

    public int getSuccessfulConnections() {
        return successfulConnections.get();
    }

    public int getFailedConnections() {
        return failedConnections.get();
    }

    public int getReconnections() {
        return reconnections.get();
    }

    public int getTotalRetries() {
        return totalRetries.get();
    }

    public void printStats() {
        System.out.println("\n=== CONNECTION STATISTICS ===");
        System.out.println("Total connections: " + getTotalConnections());
        System.out.println("Successful connections: " + getSuccessfulConnections());
        System.out.println("Failed connections: " + getFailedConnections());
        System.out.println("Reconnections: " + getReconnections());
        System.out.println("Total retry attempts: " + getTotalRetries());
    }
}