package com.chatflow.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatFlowServer extends WebSocketServer {
    private ObjectMapper objectMapper;
    private Map<WebSocket, String> connectionRooms = new ConcurrentHashMap<>();

    public ChatFlowServer(int port) {
        super(new InetSocketAddress(port));
        this.objectMapper = new ObjectMapper();
        System.out.println("ChatFlow Server created on port " + port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String uri = handshake.getResourceDescriptor();
        String roomId = extractRoomId(uri);

        if (roomId != null) {
            connectionRooms.put(conn, roomId);
            System.out.println("New Client connected to room: " + roomId + " from " + conn.getRemoteSocketAddress());
        } else {
            System.out.println("Client connected without valid room");
            conn.close(1008, "Invalid room path");
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received message: " + message);

        try {
            // Convert JSON to ChatMessage object
            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);

            // Validate the message
            String validationError = validateMessage(chatMessage);

            if (validationError != null) {
                sendErrorResponse(conn, validationError);
            } else {
                sendSuccessResponse(conn, chatMessage);
            }

        } catch (Exception e) {
            System.out.println("Parse error: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(conn, "Invalid JSON format");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String roomId = connectionRooms.remove(conn);
        System.out.println("Client disconnected from room: " + roomId + ": " + conn.getRemoteSocketAddress());

    }

    // This runs when there's an error
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("Error occurred: " + ex.getMessage());
        ex.printStackTrace();
    }

    // This runs when server starts
    @Override
    public void onStart() {
        System.out.println("ChatFlow Server started successfully!");
    }

    private String extractRoomId(String uri) {
        if (uri != null && uri.startsWith("/chat/")) {
            String roomId = uri.substring(6);
            return roomId.isEmpty() ? null : roomId;
        }
        return null;
    }

    private String validateMessage(ChatMessage msg) {
        if (msg.getUserId() == null) {
            return "username is required";
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

        // Check messageType (must be TEXT, JOIN, or LEAVE)
        if (msg.getMessageType() == null) {
            return "messageType is required";
        }

        // Check timestamp (basic check - should be ISO-8601 format)
        if (msg.getTimestamp() == null) {
            return "timestamp is required";
        }

        // If we get here, everything is valid
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
        } catch(Exception e) {
            System.out.println("Error sending error response: " + e.getMessage());
        }
    }

    private void sendSuccessResponse(WebSocket conn, ChatMessage originalMessage) {
        try {
            String successJson = String.format(
                    "{\"status\":\"SUCCESS\",\"message\":\"Message received\",\"serverTimestamp\":\"%s\",\"originalMessage\":%s}",
                    Instant.now().toString(),
                    objectMapper.writeValueAsString(originalMessage)
            );
            conn.send(successJson);
            System.out.println("Sent success response for user: " +  originalMessage.getUsername());
        } catch(Exception e) {
            System.out.println("Error sending success response: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = 8080;
        ChatFlowServer server = new ChatFlowServer(port);
        server.start();

        System.out.println("Server running on port " + port);
        System.out.println("Press Ctrl+C to stop");
    }

}
