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
                close();
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
        System.out.println("Client closed after sending " + sentCount + " messages");
        latch.countDown();
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Error: " + ex.getMessage());
        stats.recordFailedConnection();
        failureCount.incrementAndGet();
    }
}