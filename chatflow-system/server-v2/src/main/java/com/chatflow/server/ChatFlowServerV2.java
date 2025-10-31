package com.chatflow.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatFlowServerV2 extends WebSocketServer {
    private ObjectMapper objectMapper;
    private Map<WebSocket, String> connectionRooms = new ConcurrentHashMap<>();
    private RabbitMQManager rabbitMQManager;
    private String serverId;

    public ChatFlowServerV2(int port) throws Exception {
        super(new InetSocketAddress(port));
        this.objectMapper = new ObjectMapper();
        this.serverId = "server-" + System.currentTimeMillis();
        this.rabbitMQManager = new RabbitMQManager(10); // Pool of 10 channels
        System.out.println("ChatFlow Server V2 created on port " + port);
        System.out.println("Server ID: " + serverId);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String uri = handshake.getResourceDescriptor();
        String roomId = extractRoomId(uri);

        if (roomId != null) {
            connectionRooms.put(conn, roomId);
            System.out.println("Client connected to room: " + roomId + " from " + conn.getRemoteSocketAddress());
        } else {
            System.out.println("Client connected without valid room");
            conn.close(1008, "Invalid room path");
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received message: " + message);

        try {
            // Parse the incoming message
            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
            String roomId = connectionRooms.get(conn);

            // Validate the message
            String validationError = validateMessage(chatMessage);

            if (validationError != null) {
                sendErrorResponse(conn, validationError);
            } else {
                // Create queue message
                String clientIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();
                QueueMessage queueMessage = new QueueMessage(chatMessage, roomId, serverId, clientIp);

                // Publish to RabbitMQ instead of echoing
                rabbitMQManager.publishMessage(queueMessage);

                // Send acknowledgment to client
                sendAckResponse(conn, queueMessage);
            }

        } catch (Exception e) {
            System.out.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(conn, "Failed to process message");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String roomId = connectionRooms.remove(conn);
        System.out.println("Client disconnected from room: " + roomId + ": " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("Error occurred: " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("ChatFlow Server V2 started successfully!");
    }

    private String extractRoomId(String uri) {
        if (uri != null && uri.startsWith("/chat/")) {
            String roomId = uri.substring(6);
            return roomId.isEmpty() ? null : roomId;
        }
        return null;
    }

    private String validateMessage(ChatMessage msg) {
        // Same validation logic as original server
        if (msg.getUserId() == null) {
            return "userId is required";
        }
        try {
            int userId = Integer.parseInt(msg.getUserId());
            if (userId < 1 || userId > 100000) {
                return "userId must be between 1 and 100000";
            }
        } catch (NumberFormatException e) {
            return "userId must be a valid number";
        }

        if (msg.getUsername() == null) {
            return "username is required";
        }

        if (msg.getUsername().length() < 3 || msg.getUsername().length() > 20) {
            return "username must be between 3 and 20 characters";
        }

        if (!msg.getUsername().matches("^[a-zA-Z0-9]+$")) {
            return "username must be alphanumeric only";
        }

        if (msg.getMessage() == null) {
            return "message is required";
        }

        if (msg.getMessage().length() < 1 || msg.getMessage().length() > 500) {
            return "message must be 1-500 characters";
        }

        if (msg.getMessageType() == null) {
            return "messageType is required";
        }

        if (msg.getTimestamp() == null) {
            return "timestamp is required";
        }

        return null;
    }

    private void sendErrorResponse(WebSocket conn, String errorMessage) {
        try {
            String errorJson = String.format(
                    "{\"status\":\"ERROR\",\"message\":\"%s\",\"serverTimestamp\":\"%s\"}",
                    errorMessage,
                    Instant.now().toString()
            );
            conn.send(errorJson);
            System.out.println("Sent error response: " + errorMessage);
        } catch (Exception e) {
            System.out.println("Error sending error response: " + e.getMessage());
        }
    }

    private void sendAckResponse(WebSocket conn, QueueMessage queueMessage) {
        try {
            String ackJson = String.format(
                    "{\"status\":\"SUCCESS\",\"message\":\"Message published\",\"messageId\":\"%s\",\"serverTimestamp\":\"%s\"}",
                    queueMessage.getMessageId(),
                    Instant.now().toString()
            );
            conn.send(ackJson);
            System.out.println("Message published with ID: " + queueMessage.getMessageId());
        } catch (Exception e) {
            System.out.println("Error sending ack response: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = 8080;
        int healthPort = 8081;

        try {
            ChatFlowServerV2 wsServer = new ChatFlowServerV2(port);
            wsServer.start();
            System.out.println("WebSocket server running on port " + port);

            HealthCheckServer healthServer = new HealthCheckServer(healthPort);
            healthServer.start();
            System.out.println("Health server running on port " + healthPort);

            System.out.println("Press Ctrl+C to stop");

            // Graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    wsServer.rabbitMQManager.close();
                    System.out.println("Server shutdown complete");
                } catch (Exception e) {
                    System.out.println("Error during shutdown: " + e.getMessage());
                }
            }));

        } catch (Exception e) {
            System.out.println("Failed to start servers: " + e.getMessage());
            e.printStackTrace();
        }
    }
}