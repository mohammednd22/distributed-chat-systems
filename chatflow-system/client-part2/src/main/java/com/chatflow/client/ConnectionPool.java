package com.chatflow.client;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionPool {

    private BlockingQueue<PerformanceChatClient> availableConnections;
    private ConcurrentHashMap<PerformanceChatClient, Boolean> allConnections;
    private int maxPoolSize;
    private String serverUrl;
    private MetricsCollector metricsCollector;
    private AtomicInteger successCount;
    private AtomicInteger failureCount;
    private ConnectionStats stats;

    public ConnectionPool(int maxPoolSize, String serverUrl,
                          MetricsCollector metricsCollector,
                          AtomicInteger successCount,
                          AtomicInteger failureCount,
                          ConnectionStats stats) {
        this.maxPoolSize = maxPoolSize;
        this.serverUrl = serverUrl;
        this.metricsCollector = metricsCollector;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.stats = stats;
        this.availableConnections = new LinkedBlockingQueue<>(maxPoolSize);
        this.allConnections = new ConcurrentHashMap<>();
    }

    // Get a connection from pool (or create new if available)
    public PerformanceChatClient acquire(int roomId, int messagesToSend,
                                         CountDownLatch latch) throws Exception {
        PerformanceChatClient client = availableConnections.poll();

        if (client != null && client.isOpen()) {
            System.out.println("Reusing pooled connection for room " + roomId);
            return client;
        }

        // Create new connection if pool not full
        if (allConnections.size() < maxPoolSize) {
            System.out.println("Creating new pooled connection for room " + roomId);
            URI serverUri = new URI(serverUrl + roomId);

            client = new PerformanceChatClient(
                    serverUri, null, metricsCollector, messagesToSend,
                    roomId, latch, successCount, failureCount, stats
            );

            allConnections.put(client, true);
            client.connect();

            // Wait for connection to establish
            Thread.sleep(100);

            return client;
        }

        // Wait for available connection
        System.out.println("Pool full, waiting for available connection...");
        return availableConnections.take();
    }

    // Return connection to pool for reuse
    public void release(PerformanceChatClient client) {
        if (client != null && client.isOpen()) {
            availableConnections.offer(client);
            System.out.println("Connection returned to pool");
        }
    }

    // Close all connections
    public void shutdown() {
        System.out.println("Shutting down connection pool...");
        for (PerformanceChatClient client : allConnections.keySet()) {
            try {
                client.close();
            } catch (Exception e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public int getPoolSize() {
        return allConnections.size();
    }

    public int getAvailableCount() {
        return availableConnections.size();
    }
}