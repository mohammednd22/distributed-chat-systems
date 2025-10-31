package com.chatflow.consumer;

import org.java_websocket.WebSocket;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

public class RoomManager {
    private ConcurrentHashMap<String, Set<WebSocket>> roomSessions;
    private ConcurrentHashMap<String, UserInfo> activeUsers;
    private ConcurrentHashMap<String, String> processedMessages; // messageId -> timestamp
    private ObjectMapper objectMapper;
    private ConsumerMetrics metrics;

    public RoomManager(ConsumerMetrics metrics) {
        this.roomSessions = new ConcurrentHashMap<>();
        this.activeUsers = new ConcurrentHashMap<>();
        this.processedMessages = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
        this.metrics = metrics;
    }

    // Add user session to room
    public synchronized void addUserToRoom(String roomId, WebSocket session, String userId, String username) {
        // Add session to room
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);

        // Track user info
        String sessionId = session.toString();
        UserInfo userInfo = new UserInfo(userId, username, roomId, sessionId);
        activeUsers.put(sessionId, userInfo);

        System.out.println("User " + username + " joined room " + roomId);
    }

    // Remove user session from room
    public synchronized void removeUserFromRoom(String roomId, WebSocket session) {
        Set<WebSocket> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }

        String sessionId = session.toString();
        UserInfo userInfo = activeUsers.remove(sessionId);
        if (userInfo != null) {
            System.out.println("User " + userInfo.getUsername() + " left room " + roomId);
        }
    }

    // Broadcast message to all users in room
    public void broadcastToRoom(QueueMessage queueMessage) {
        String roomId = queueMessage.getRoomId();

        // Check for duplicate message
        if (isDuplicateMessage(queueMessage.getMessageId())) {
            metrics.incrementDuplicatesFiltered();
            return;
        }

        Set<WebSocket> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            System.out.println("No active sessions in room " + roomId);
            return;
        }

        // Create broadcast message
        String broadcastMessage = createBroadcastMessage(queueMessage);

        // Send to all sessions in room
        int delivered = 0;
        int failed = 0;

        for (WebSocket session : sessions) {
            try {
                if (session.isOpen()) {
                    session.send(broadcastMessage);
                    delivered++;
                } else {
                    // Remove dead session
                    removeUserFromRoom(roomId, session);
                    failed++;
                }
            } catch (Exception e) {
                System.out.println("Failed to send message to session: " + e.getMessage());
                failed++;
            }
        }

        // Update metrics
        if (delivered > 0) {
            metrics.incrementMessagesDelivered();
            metrics.incrementRoomMessages(roomId);
        }
        if (failed > 0) {
            metrics.incrementMessagesFailed();
        }

        System.out.println("Broadcasted to room " + roomId + ": " + delivered + " delivered, " + failed + " failed");
    }

    private boolean isDuplicateMessage(String messageId) {
        String currentTime = Instant.now().toString();
        String existingTime = processedMessages.putIfAbsent(messageId, currentTime);

        // Clean old messages periodically (simple approach)
        if (processedMessages.size() > 10000) {
            processedMessages.clear();
        }

        return existingTime != null;
    }

    private String createBroadcastMessage(QueueMessage queueMessage) {
        try {
            // Create a simplified broadcast format
            BroadcastMessage broadcast = new BroadcastMessage(
                    queueMessage.getMessageId(),
                    queueMessage.getRoomId(),
                    queueMessage.getUserId(),
                    queueMessage.getUsername(),
                    queueMessage.getMessage(),
                    queueMessage.getTimestamp(),
                    queueMessage.getMessageType().toString()
            );

            return objectMapper.writeValueAsString(broadcast);
        } catch (Exception e) {
            System.out.println("Error creating broadcast message: " + e.getMessage());
            return "{}";
        }
    }

    // Get room statistics
    public int getActiveRooms() {
        return roomSessions.size();
    }

    public int getTotalSessions() {
        return roomSessions.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    public void printStats() {
        System.out.println("\n=== Room Manager Stats ===");
        System.out.println("Active rooms: " + getActiveRooms());
        System.out.println("Total sessions: " + getTotalSessions());
        System.out.println("Active users: " + activeUsers.size());

        roomSessions.forEach((room, sessions) ->
                System.out.println("Room " + room + ": " + sessions.size() + " users"));
    }

    // Inner class for broadcast message format
    private static class BroadcastMessage {
        public String messageId;
        public String roomId;
        public String userId;
        public String username;
        public String message;
        public String timestamp;
        public String messageType;

        public BroadcastMessage(String messageId, String roomId, String userId,
                                String username, String message, String timestamp, String messageType) {
            this.messageId = messageId;
            this.roomId = roomId;
            this.userId = userId;
            this.username = username;
            this.message = message;
            this.timestamp = timestamp;
            this.messageType = messageType;
        }
    }
}