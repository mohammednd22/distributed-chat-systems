package com.chatflow.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.time.Instant;

public class ChatFlowServer extends WebSocketServer {
    private ObjectMapper objectMapper;

    public ChatFlowServer(int port) {
        super(new InetSocketAddress(port));
        this.objectMapper = new ObjectMapper();
        System.out.println("ChatFlow Server created on port " + port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New Client Connected" + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received message: " + message);

        // TODO: We'll add message processing here
        // For now, just echo it back
        conn.send("Echo: " + message);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + conn.getRemoteSocketAddress() + " " + reason);
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

    public static void main(String[] args) {
        int port = 8080;
        ChatFlowServer server = new ChatFlowServer(port);
        server.start();

        System.out.println("Server running on port " + port);
        System.out.println("Press Ctrl+C to stop");
    }

}
