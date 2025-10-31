package com.chatflow.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class QueueMessage {
    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("roomId")
    private String roomId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("messageType")
    private MessageType messageType;

    @JsonProperty("serverId")
    private String serverId;

    @JsonProperty("clientIp")
    private String clientIp;

    // Default constructor
    public QueueMessage() {}

    // Constructor from ChatMessage
    public QueueMessage(ChatMessage chatMessage, String roomId, String serverId, String clientIp) {
        this.messageId = UUID.randomUUID().toString();
        this.roomId = roomId;
        this.userId = chatMessage.getUserId();
        this.username = chatMessage.getUsername();
        this.message = chatMessage.getMessage();
        this.timestamp = chatMessage.getTimestamp();
        this.messageType = chatMessage.getMessageType();
        this.serverId = serverId;
        this.clientIp = clientIp;
    }

    // Getters and setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
}