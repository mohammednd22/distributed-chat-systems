package com.chatflow.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatClient extends WebSocketClient {

    private ConnectionStats stats;
    private MessageQueue messageQueue;
    private CountDownLatch latch;
    private AtomicInteger successCount;
    private AtomicInteger failureCount;
    private int messagesToSend;
    private int sentCount = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private int reconnectAttempts = 0;
    private ConnectionPool connectionPool;
    private boolean shouldReturnToPool = false;

    // New constructor with pool
    public ChatClient(URI serverUri, MessageQueue messageQueue, int messagesToSend,
                      CountDownLatch latch, AtomicInteger successCount,
                      AtomicInteger failureCount, ConnectionStats stats,
                      ConnectionPool connectionPool) {
        super(serverUri);
        this.messageQueue = messageQueue;
        this.messagesToSend = messagesToSend;
        this.latch = latch;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.stats = stats;
        this.connectionPool = connectionPool;
        this.shouldReturnToPool = (connectionPool != null);
    }

    public ChatClient(URI serverUri, MessageQueue messageQueue, int messagesToSend,
                      CountDownLatch latch, AtomicInteger successCount,
                      AtomicInteger failureCount, ConnectionStats stats) {
        super(serverUri);
        this.messageQueue = messageQueue;
        this.messagesToSend = messagesToSend;
        this.latch = latch;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.stats = stats;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        stats.recordConnection();
        stats.recordSuccessfulConnection();
        System.out.println("Client connected, will send " + messagesToSend + " messages");

        // Start consumer thread
        new Thread(() -> {
            try {
                while (sentCount < messagesToSend && messageQueue.hasMessages()) {
                    // Take message from queue
                    String message = messageQueue.take();

                    // Try to send with retries
                    boolean success = sendWithRetry(message, 5);

                    if (success) {
                        sentCount++;
                    } else {
                        failureCount.incrementAndGet();
                    }

                    // Thread.sleep(1);
                }

                // Wait for remaining responses
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                System.out.println("Consumer interrupted: " + e.getMessage());
            } finally {
                if (!shouldReturnToPool) {
                    close();
                } else {
                    if (connectionPool != null) {
                        connectionPool.release(this);
                    }
                    latch.countDown();
                }
            }
        }).start();
    }

    // New method: Send with exponential backoff retry
    private boolean sendWithRetry(String message, int maxRetries) {
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                send(message);
                return true; // Success

            } catch (Exception e) {
                attempt++;
                if (attempt > 1) {
                    stats.recordRetry();
                }

                if (attempt >= maxRetries) {
                    System.out.println("Failed after " + maxRetries + " attempts");
                    return false;
                }

                // Exponential backoff: 100ms, 200ms, 400ms, 800ms, 1600ms
                long backoffTime = (long) (100 * Math.pow(2, attempt - 1));

                System.out.println("Retry attempt " + attempt + " after " + backoffTime + "ms");

                try {
                    Thread.sleep(backoffTime);
                } catch (InterruptedException ie) {
                    return false;
                }
            }
        }

        return false;
    }

    private void attemptReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            System.out.println("Max reconnection attempts reached");
            stats.recordFailedConnection();
            latch.countDown();
            return;
        }

        reconnectAttempts++;
        stats.recordReconnection();

        long backoffMs = (long) (1000 * Math.pow(2, reconnectAttempts - 1));
        System.out.println("Reconnection attempt " + reconnectAttempts + " after " + backoffMs + "ms");

        // Run reconnect in a NEW thread (not the WebSocket thread)
        new Thread(() -> {
            try {
                Thread.sleep(backoffMs);
                reconnect();
            } catch (InterruptedException e) {
                System.out.println("Reconnection interrupted");
                latch.countDown();
            }
        }).start();
    }


    @Override
    public void onMessage(String message) {
        if (message.contains("\"status\":\"SUCCESS\"")) {
            successCount.incrementAndGet();
        } else if (message.contains("\"status\":\"ERROR\"")) {
            failureCount.incrementAndGet();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (remote && sentCount < messagesToSend && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            System.out.println("Unexpected disconnect, reconnecting...");
            attemptReconnect();
        } else {
            System.out.println("Client closed after sending " + sentCount + " messages");
            latch.countDown();
        }
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Error: " + ex.getMessage());

        if (!isOpen() && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            attemptReconnect();
        } else {
            stats.recordFailedConnection();
            failureCount.incrementAndGet();
        }
    }
}