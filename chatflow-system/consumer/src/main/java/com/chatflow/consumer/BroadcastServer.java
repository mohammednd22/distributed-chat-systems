package com.chatflow.consumer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BroadcastServer extends WebSocketServer {
    private Map<WebSocket, String> connectionRooms = new ConcurrentHashMap<>();
    private RoomManager roomManager;

    public BroadcastServer(int port, RoomManager roomManager) {
        super(new InetSocketAddress(port));
        this.roomManager = roomManager;
        System.out.println("BroadcastServer created on port " + port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String uri = handshake.getResourceDescriptor();
        String roomId = extractRoomId(uri);

        if (roomId != null) {
            connectionRooms.put(conn, roomId);

            // Add user to room manager
            String userId = "broadcast-user-" + System.currentTimeMillis();
            String username = "user" + userId.substring(userId.length() - 4);

            roomManager.addUserToRoom(roomId, conn, userId, username);

            System.out.println("Broadcast client connected to room: " + roomId +
                    " from " + conn.getRemoteSocketAddress());
        } else {
            System.out.println("Client connected without valid room");
            conn.close(1008, "Invalid room path");
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Clients connected to broadcast server are read-only
        // They should only receive messages, not send them
        System.out.println("Received unexpected message from broadcast client: " + message);

        String errorResponse = "{\"status\":\"ERROR\",\"message\":\"This server is for receiving messages only. Use the main server to send messages.\"}";
        conn.send(errorResponse);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String roomId = connectionRooms.remove(conn);
        if (roomId != null) {
            roomManager.removeUserFromRoom(roomId, conn);
            System.out.println("Broadcast client disconnected from room: " + roomId +
                    ": " + conn.getRemoteSocketAddress());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("BroadcastServer error: " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("BroadcastServer started successfully!");
    }

    private String extractRoomId(String uri) {
        if (uri != null && uri.startsWith("/chat/")) {
            String roomId = uri.substring(6);
            return roomId.isEmpty() ? null : roomId;
        }
        return null;
    }
}