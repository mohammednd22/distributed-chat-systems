package com.chatflow.client;

public class MessageMetric {
    private long timestamp;
    private String messageType;
    private long latencyMs;
    private String statusCode;
    private int roomId;

    public MessageMetric(long timestamp, String messageType, long latencyMs,
                         String statusCode, int roomId) {
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.latencyMs = latencyMs;
        this.statusCode = statusCode;
        this.roomId = roomId;
    }

    // Getters
    public long getTimestamp() { return timestamp; }
    public String getMessageType() { return messageType; }
    public long getLatencyMs() { return latencyMs; }
    public String getStatusCode() { return statusCode; }
    public int getRoomId() { return roomId; }

    // Convert to CSV format
    public String toCSV() {
        return timestamp + "," + messageType + "," + latencyMs + "," +
                statusCode + "," + roomId;
    }
}