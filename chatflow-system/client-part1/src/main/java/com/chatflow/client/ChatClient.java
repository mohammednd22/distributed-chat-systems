package com.chatflow.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


public class ChatClient extends WebSocketClient {
    private int messagesToSend;
    private int roomId;
    private CountDownLatch latch;
    private AtomicInteger successCount;
    private AtomicInteger failureCount;

    public ChatClient(URI serverUri, int messageToSend, int roomId, CountDownLatch latch, AtomicInteger successCount, AtomicInteger failureCount) {
        super(serverUri);
        this.messagesToSend = messageToSend;
        this.roomId = roomId;
        this.latch = latch;
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to server, sending " + messagesToSend + " messages to room " + roomId);

        // Start sending messages
        new Thread(() -> {
            for (int i = 0; i < messagesToSend; i++) {
                try {
                    String message = MessageGenerator.generateMessage(roomId);
                    send(message);

                    // Small delay to avoid overwhelming server (optional)
                    Thread.sleep(1);

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }

            // Wait a bit for responses, then close
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            close();
        }).start();
    }

    @Override
    public void onMessage(String message) {
        // Check if response is success or error
        if (message.contains("\"status\":\"SUCCESS\"")) {
            successCount.incrementAndGet();
        } else if (message.contains("\"status\":\"ERROR\"")) {
            failureCount.incrementAndGet();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Connection closed for room " + roomId);
        latch.countDown(); // Signal that this client is done
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Error occurred: " + ex.getMessage());
        failureCount.incrementAndGet();
    }


}
