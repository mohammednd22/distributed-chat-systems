package com.chatflow.consumer;

import java.time.Instant;

public class UserInfo {
    private String userId;
    private String username;
    private String roomId;
    private Instant lastSeen;
    private String sessionId;

    public UserInfo(String userId, String username, String roomId, String sessionId) {
        this.userId = userId;
        this.username = username;
        this.roomId = roomId;
        this.sessionId = sessionId;
        this.lastSeen = Instant.now();
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public void updateLastSeen() {
        this.lastSeen = Instant.now();
    }
}