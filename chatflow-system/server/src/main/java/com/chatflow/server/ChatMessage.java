package com.chatflow.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ChatMessage {
    @JsonProperty("userId")
    private String userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("messageType")
    private MessageType messageType;

    // Public Constructor Needed For JSON Framework
    public ChatMessage() {}

    // Public Constructor for Java
    public ChatMessage(String userId, String username, String message, String timestamp, MessageType messageType) {
        this.userId = userId;
        this.username = username;
        this.message = message;
        this.timestamp = Instant.now();
        this.messageType = messageType;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }
}
