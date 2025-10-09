package com.chatflow.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceChatClient extends WebSocketClient {

    private MessageQueue messageQueue;
    private MetricsCollector metricsCollector;
    private CountDownLatch latch;
    private AtomicInteger successCount;
    private AtomicInteger failureCount;
    private ConnectionStats stats;
    private int messagesToSend;
    private int sentCount = 0;
    private int roomId;

    // Track send times for each message
    private ConcurrentHashMap<Integer, Long> sendTimes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, String> messageTypes = new ConcurrentHashMap<>();
    private AtomicInteger messageIdCounter = new AtomicInteger(0);

    private ObjectMapper objectMapper = new ObjectMapper();

    public PerformanceChatClient(URI serverUri, MessageQueue messageQueue,
                                 MetricsCollector metricsCollector, int messagesToSend,
                                 int roomId, CountDownLatch latch,
                                 AtomicInteger successCount, AtomicInteger failureCount,
                                 ConnectionStats stats) {
        super(serverUri);
        this.messageQueue = messageQueue;
        this.metricsCollector = metricsCollector;
        this.messagesToSend = messagesToSend;
        this.roomId = roomId;
        this.latch = latch;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.stats = stats;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        stats.recordConnection();
        stats.recordSuccessfulConnection();

        new Thread(() -> {
            try {
                while (sentCount < messagesToSend && messageQueue.hasMessages()) {
                    String message = messageQueue.take();

                    // Extract message type from JSON
                    String msgType = extractMessageType(message);

                    // Generate unique message ID
                    int msgId = messageIdCounter.incrementAndGet();

                    // Record send time
                    long sendTime = System.currentTimeMillis();
                    sendTimes.put(msgId, sendTime);
                    messageTypes.put(msgId, msgType);

                    // Send message
                    send(message);
                    sentCount++;
                }

                Thread.sleep(2000); // Wait for remaining responses

            } catch (InterruptedException e) {
                System.out.println("Client interrupted: " + e.getMessage());
            } finally {
                close();
            }
        }).start();
    }

    @Override
    public void onMessage(String response) {
        try {
            long receiveTime = System.currentTimeMillis();

            // Parse response to get status
            JsonNode jsonNode = objectMapper.readTree(response);
            String status = jsonNode.get("status").asText();

            // Find matching send time (use most recent)
            Integer msgId = findOldestUnmatchedMessage();

            if (msgId != null) {
                Long sendTime = sendTimes.remove(msgId);
                String msgType = messageTypes.remove(msgId);

                if (sendTime != null) {
                    long latency = receiveTime - sendTime;

                    // Record metric
                    MessageMetric metric = new MessageMetric(
                            sendTime, msgType, latency, status, roomId
                    );
                    metricsCollector.addMetric(metric);

                    if (status.equals("SUCCESS")) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error processing response: " + e.getMessage());
        }
    }

    private Integer findOldestUnmatchedMessage() {
        return sendTimes.keySet().stream()
                .min(Integer::compare)
                .orElse(null);
    }

    private String extractMessageType(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            return node.get("messageType").asText();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        latch.countDown();
    }

    @Override
    public void onError(Exception ex) {
        stats.recordFailedConnection();
        failureCount.incrementAndGet();
    }
}