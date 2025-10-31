package com.chatflow.server;

import com.rabbitmq.client.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

public class RabbitMQManager {
    private static final String RABBITMQ_HOST = System.getenv("RABBITMQ_HOST");
    private static final int RABBITMQ_PORT = 5672;
    private static final String USERNAME = System.getenv("RABBITMQ_USER");
    private static final String PASSWORD = System.getenv("RABBITMQ_PASSWORD");
    private static final String EXCHANGE_NAME = "chat.exchange";

    private Connection connection;
    private BlockingQueue<Channel> channelPool;
    private ObjectMapper objectMapper;

    public RabbitMQManager(int poolSize) throws IOException, TimeoutException {
        this.objectMapper = new ObjectMapper();
        this.channelPool = new ArrayBlockingQueue<>(poolSize);
        initializeConnection();
        initializeExchangeAndQueues();
        createChannelPool(poolSize);
    }

    private void initializeConnection() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setPort(RABBITMQ_PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);

        this.connection = factory.newConnection();
        System.out.println("Connected to RabbitMQ at " + RABBITMQ_HOST);
    }

    private void initializeExchangeAndQueues() throws IOException, TimeoutException {
        try (Channel channel = connection.createChannel()) {
            // Declare topic exchange
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC, true);

            // Create queues for rooms 1-20
            for (int i = 1; i <= 20; i++) {
                String queueName = "room." + i;
                String routingKey = "room." + i;

                channel.queueDeclare(queueName, true, false, false, null);
                channel.queueBind(queueName, EXCHANGE_NAME, routingKey);

                System.out.println("Created and bound queue: " + queueName);
            }
        }
    }

    private void createChannelPool(int poolSize) throws IOException {
        for (int i = 0; i < poolSize; i++) {
            Channel channel = connection.createChannel();
            channelPool.offer(channel);
        }
        System.out.println("Created channel pool with " + poolSize + " channels");
    }

    public Channel borrowChannel() throws InterruptedException {
        return channelPool.take();
    }

    public void returnChannel(Channel channel) {
        if (channel.isOpen()) {
            channelPool.offer(channel);
        }
    }

    public void publishMessage(QueueMessage queueMessage) throws Exception {
        Channel channel = borrowChannel();
        try {
            String routingKey = "room." + queueMessage.getRoomId();
            String messageJson = objectMapper.writeValueAsString(queueMessage);

            channel.basicPublish(
                    EXCHANGE_NAME,
                    routingKey,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    messageJson.getBytes()
            );

            System.out.println("Published message to " + routingKey);
        } finally {
            returnChannel(channel);
        }
    }

    public void close() throws IOException, TimeoutException {
        // Close all channels in pool
        while (!channelPool.isEmpty()) {
            Channel channel = channelPool.poll();
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        }

        if (connection != null && connection.isOpen()) {
            connection.close();
        }

        System.out.println("RabbitMQ connection closed");
    }
}