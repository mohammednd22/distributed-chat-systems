package com.chatflow.client;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionPool {

    private BlockingQueue<ChatClient> availableConnections;
    private ConcurrentHashMap<ChatClient, Boolean> allConnections;
    private int maxPoolSize;
    private String serverUrl;
    private AtomicInteger successCount;
    private AtomicInteger failureCount;
    private ConnectionStats stats;

    public ConnectionPool(int maxPoolSize, String serverUrl,
                          AtomicInteger successCount,
                          AtomicInteger failureCount,
                          ConnectionStats stats) {
        this.maxPoolSize = maxPoolSize;
        this.serverUrl = serverUrl;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.stats = stats;
        this.availableConnections = new LinkedBlockingQueue<>(maxPoolSize);
        this.allConnections = new ConcurrentHashMap<>();
    }

    public ChatClient acquire(int roomId, int messagesToSend,
                              MessageQueue messageQueue,
                              CountDownLatch latch) throws Exception {
        ChatClient client = availableConnections.poll();

        if (client != null && client.isOpen()) {
            System.out.println("Reusing pooled connection for room " + roomId);
            return client;
        }

        if (allConnections.size() < maxPoolSize) {
            System.out.println("Creating new pooled connection for room " + roomId);
            URI serverUri = new URI(serverUrl + roomId);

            client = new ChatClient(
                    serverUri, messageQueue, messagesToSend,
                    latch, successCount, failureCount, stats, this
            );

            allConnections.put(client, true);
            client.connect();
            Thread.sleep(100);

            return client;
        }

        System.out.println("Pool full, waiting for available connection...");
        return availableConnections.take();
    }

    public void release(ChatClient client) {
        if (client != null && client.isOpen()) {
            availableConnections.offer(client);
            System.out.println("Connection returned to pool");
        }
    }

    public void shutdown() {
        System.out.println("Shutting down connection pool...");
        for (ChatClient client : allConnections.keySet()) {
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