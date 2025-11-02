package com.chatflow.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class OptimizedDistributedPerformanceChatClient extends WebSocketClient {

    private MessageQueue messageQueue;
    private MetricsCollector metricsCollector;
    private CountDownLatch latch;
    private AtomicInteger successCount;
    private AtomicInteger failureCount;
    private ConnectionStats stats;
    private int messagesToSend;
    private int sentCount = 0;
    private int roomId;
    private DistributedConnectionPool connectionPool;
    private boolean shouldReturnToPool = false;

    // Batch correlation for performance
    private static ConcurrentHashMap<String, MessageTracker> globalMessageTracker =
            new ConcurrentHashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    public OptimizedDistributedPerformanceChatClient(URI serverUri, MessageQueue messageQueue,
                                                     MetricsCollector metricsCollector,
                                                     int messagesToSend, int roomId,
                                                     CountDownLatch latch,
                                                     AtomicInteger successCount,
                                                     AtomicInteger failureCount,
                                                     ConnectionStats stats,
                                                     DistributedConnectionPool connectionPool) {
        super(serverUri);
        this.messageQueue = messageQueue;
        this.metricsCollector = metricsCollector;
        this.messagesToSend = messagesToSend;
        this.roomId = roomId;
        this.latch = latch;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.stats = stats;
        this.connectionPool = connectionPool;
        this.shouldReturnToPool = (connectionPool != null);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        stats.recordConnection();
        stats.recordSuccessfulConnection();
        System.out.println("Sender connected to ALB for room " + roomId);

        // Start consumer thread (same pattern as Assignment 1)
        new Thread(() -> {
            try {
                while (sentCount < messagesToSend && messageQueue.hasMessages()) {
                    String message = messageQueue.take();

                    // Enhanced message with tracking for correlation
                    String enhancedMessage = enhanceMessageForTracking(message);

                    boolean success = sendWithRetry(enhancedMessage, 5);

                    if (success) {
                        sentCount++;
                    } else {
                        failureCount.incrementAndGet();
                    }
                }

                // Wait for remaining responses
                Thread.sleep(8000);

            } catch (InterruptedException e) {
                System.out.println("Consumer interrupted: " + e.getMessage());
            } finally {
                if (!shouldReturnToPool) {
                    close();
                } else {
                    if (connectionPool != null) {
                        connectionPool.releaseSender(this);
                    }
                }
                latch.countDown();
            }
        }).start();
    }

    private String enhanceMessageForTracking(String message) {
        try {
            JsonNode messageNode = objectMapper.readTree(message);
            String trackingId = messageNode.get("trackingId").asText(); // Changed from userId
            String messageType = messageNode.get("messageType").asText();

            long sendTime = System.currentTimeMillis();

            MessageTracker tracker = new MessageTracker(sendTime, messageType, roomId);
            globalMessageTracker.put(trackingId, tracker); // Use trackingId as key

            return message;

        } catch (Exception e) {
            return message;
        }
    }

    // Keep your existing retry logic
    private boolean sendWithRetry(String message, int maxRetries) {
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                send(message);
                return true;

            } catch (Exception e) {
                attempt++;
                if (attempt > 1) {
                    stats.recordRetry();
                }

                if (attempt >= maxRetries) {
                    return false;
                }

                long backoffTime = (long) (100 * Math.pow(2, attempt - 1));
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
        // Handle ALB acknowledgments (not the end-to-end metrics)
        if (message.contains("\"status\":\"SUCCESS\"")) {
            // Just acknowledgment, real metrics come from receiver clients
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Sender client closed after sending " + sentCount + " messages");
        latch.countDown();
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Sender error: " + ex.getMessage());
        stats.recordFailedConnection();
        failureCount.incrementAndGet();
    }

    public static void recordBroadcastMetric(String broadcastMessage,
                                             MetricsCollector metricsCollector,
                                             AtomicInteger successCount) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode messageNode = mapper.readTree(broadcastMessage);

            // Use trackingId for correlation instead of userId
            String trackingId = messageNode.get("trackingId").asText();
            MessageTracker tracker = globalMessageTracker.remove(trackingId);

            if (tracker != null) {
                long receiveTime = System.currentTimeMillis();
                long latency = receiveTime - tracker.sendTime;

                MessageMetric metric = new MessageMetric(
                        tracker.sendTime, tracker.messageType, latency,
                        "SUCCESS", tracker.roomId
                );
                metricsCollector.addMetric(metric);
                successCount.incrementAndGet();
            } else {
                // Log when correlation fails for debugging
                System.out.println("CORRELATION MISS: No tracker found for trackingId: " + trackingId);
                successCount.incrementAndGet();
            }

        } catch (Exception e) {
            System.out.println("Error in recordBroadcastMetric: " + e.getMessage());
            successCount.incrementAndGet();
        }
    }

    // Helper class for message tracking
    private static class MessageTracker {
        final long sendTime;
        final String messageType;
        final int roomId;

        MessageTracker(long sendTime, String messageType, int roomId) {
            this.sendTime = sendTime;
            this.messageType = messageType;
            this.roomId = roomId;
        }
    }
}