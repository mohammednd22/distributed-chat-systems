package com.chatflow.consumer;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class ConsumerPool {
    private static final String RABBITMQ_HOST = System.getenv("RABBITMQ_HOST"); // Update with your IP
    private static final int RABBITMQ_PORT = 5672;
    private static final String USERNAME = System.getenv("RABBITMQ_USER");
    private static final String PASSWORD = System.getenv("RABBITMQ_PASSWORD");
    private static final int BROADCAST_PORT = 8082; // New port for broadcast server

    private Connection connection;
    private List<MessageConsumer> consumers;
    private ExecutorService executorService;
    private RoomManager roomManager;
    private ConsumerMetrics metrics;
    private BroadcastServer broadcastServer; // Add this
    private int numConsumers;

    public ConsumerPool(int numConsumers) throws Exception {
        this.numConsumers = numConsumers;
        this.consumers = new ArrayList<>();
        this.metrics = new ConsumerMetrics();
        this.roomManager = new RoomManager(metrics);

        initializeConnection();
        initializeBroadcastServer(); // Add this
        createConsumers();

        System.out.println("ConsumerPool initialized with " + numConsumers + " consumers");
    }

    private void initializeConnection() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setPort(RABBITMQ_PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);

        // Connection settings for stability
        factory.setRequestedHeartbeat(60);
        factory.setConnectionTimeout(30000);

        this.connection = factory.newConnection();
        System.out.println("Connected to RabbitMQ at " + RABBITMQ_HOST);
    }

    private void initializeBroadcastServer() {
        this.broadcastServer = new BroadcastServer(BROADCAST_PORT, roomManager);
        System.out.println("BroadcastServer initialized on port " + BROADCAST_PORT);
    }

    private void createConsumers() throws IOException {
        // Distribute rooms fairly across consumers
        List<List<String>> roomAssignments = distributeRooms(20, numConsumers);

        this.executorService = Executors.newFixedThreadPool(numConsumers);

        for (int i = 0; i < numConsumers; i++) {
            String consumerId = "consumer-" + (i + 1);
            List<String> assignedRooms = roomAssignments.get(i);

            MessageConsumer consumer = new MessageConsumer(
                    consumerId, assignedRooms, connection, roomManager, metrics
            );

            consumers.add(consumer);
        }
    }

    private List<List<String>> distributeRooms(int totalRooms, int numConsumers) {
        List<List<String>> assignments = new ArrayList<>();

        for (int i = 0; i < numConsumers; i++) {
            assignments.add(new ArrayList<>());
        }

        // Round-robin assignment
        for (int room = 1; room <= totalRooms; room++) {
            int consumerIndex = (room - 1) % numConsumers;
            assignments.get(consumerIndex).add(String.valueOf(room));
        }


        System.out.println("Room assignments:");
        for (int i = 0; i < assignments.size(); i++) {
            System.out.println("Consumer-" + (i + 1) + ": rooms " + assignments.get(i));
        }

        return assignments;
    }

    public void start() {
        System.out.println("Starting consumer pool...");

        // Start broadcast server first
        broadcastServer.start();
        System.out.println("BroadcastServer started on port " + BROADCAST_PORT);

        // Start message consumers
        for (MessageConsumer consumer : consumers) {
            executorService.submit(consumer);
        }

        System.out.println("All consumers started successfully");
        System.out.println("Clients can connect to port " + BROADCAST_PORT + " to receive messages");

        // Start metrics reporting thread
        startMetricsReporting();
    }

    private void startMetricsReporting() {
        Thread metricsThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(30000); // Report every 30 seconds

                    System.out.println("\n=== Consumer Pool Status ===");
                    metrics.printMetrics();
                    roomManager.printStats();

                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        metricsThread.setDaemon(true);
        metricsThread.start();
    }

    public void shutdown() {
        System.out.println("Shutting down consumer pool...");

        // Stop broadcast server
        try {
            if (broadcastServer != null) {
                broadcastServer.stop();
            }
        } catch (Exception e) {
            System.out.println("Error stopping broadcast server: " + e.getMessage());
        }

        // Stop all consumers
        for (MessageConsumer consumer : consumers) {
            consumer.stop();
        }

        // Shutdown executor
        executorService.shutdown();

        // Close connection
        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing RabbitMQ connection: " + e.getMessage());
        }

        System.out.println("Consumer pool shutdown complete");
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public ConsumerMetrics getMetrics() {
        return metrics;
    }
}