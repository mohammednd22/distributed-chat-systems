package com.chatflow.consumer;

import com.rabbitmq.client.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class MessageConsumer implements Runnable {
    private String consumerId;
    private List<String> assignedRooms;
    private Connection connection;
    private Channel channel;
    private RoomManager roomManager;
    private ConsumerMetrics metrics;
    private ObjectMapper objectMapper;
    private volatile boolean running = true;

    public MessageConsumer(String consumerId, List<String> assignedRooms,
                           Connection connection, RoomManager roomManager,
                           ConsumerMetrics metrics) throws IOException {
        this.consumerId = consumerId;
        this.assignedRooms = assignedRooms;
        this.connection = connection;
        this.channel = connection.createChannel();
        this.roomManager = roomManager;
        this.metrics = metrics;
        this.objectMapper = new ObjectMapper();

        // Configure channel for optimal performance
        this.channel.basicQos(10); // Prefetch 10 messages

        System.out.println("Consumer " + consumerId + " created for rooms: " + assignedRooms);
    }

    @Override
    public void run() {
        System.out.println("Consumer " + consumerId + " started");

        try {
            // Set up consumers for each assigned room
            for (String room : assignedRooms) {
                String queueName = "room." + room;

                DefaultConsumer consumer = new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope,
                                               AMQP.BasicProperties properties, byte[] body) throws IOException {
                        processMessage(envelope, body);
                    }
                };

                // Start consuming from queue
                channel.basicConsume(queueName, false, consumer); // false = manual ack
                System.out.println("Consumer " + consumerId + " consuming from " + queueName);
            }

            // Keep consumer alive
            while (running) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            System.out.println("Consumer " + consumerId + " error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void processMessage(Envelope envelope, byte[] body) {
        long deliveryTag = envelope.getDeliveryTag();

        try {
            // Parse message
            String messageJson = new String(body);
            QueueMessage queueMessage = objectMapper.readValue(messageJson, QueueMessage.class);

            System.out.println("Consumer " + consumerId + " processing message: " + queueMessage.getMessageId());

            // Update metrics
            metrics.incrementMessagesProcessed();
            metrics.incrementConsumerMessages(consumerId);

            // Broadcast to room
            roomManager.broadcastToRoom(queueMessage);

            // Acknowledge message after successful broadcast
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            System.out.println("Error processing message: " + e.getMessage());
            e.printStackTrace();

            try {
                // Negative acknowledgment - requeue the message
                channel.basicNack(deliveryTag, false, true);
                metrics.incrementMessagesFailed();
            } catch (IOException ackError) {
                System.out.println("Error sending NACK: " + ackError.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        System.out.println("Consumer " + consumerId + " stopping...");
    }

    private void cleanup() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (IOException | TimeoutException e) {
            System.out.println("Error closing channel for consumer " + consumerId + ": " + e.getMessage());
        }
    }

    public String getConsumerId() {
        return consumerId;
    }

    public List<String> getAssignedRooms() {
        return assignedRooms;
    }
}