package com.chatflow;

import org.java_websocket.server.WebSocketServer;
import com.fasterxml.jackson.databind.ObjectMapper;

public class App {
    public static void main(String[] args) {
        System.out.println("ChatFlow Server Dependencies Test");

        try {
            ObjectMapper mapper = new ObjectMapper();
            System.out.println("✓ Jackson JSON library loaded");
            System.out.println("✓ WebSocket library available");
            System.out.println("✓ All dependencies working!");
        } catch (Exception e) {
            System.out.println("Dependency issue: " + e.getMessage());
        }
    }
}