package com.chatflow.client;

import java.util.Random;

public class MessageProducer implements Runnable {

    private MessageQueue messageQueue;
    private int totalMessages;
    private Random random = new Random();

    public MessageProducer(MessageQueue messageQueue, int totalMessages) {
        this.messageQueue = messageQueue;
        this.totalMessages = totalMessages;
    }

    @Override
    public void run() {
        System.out.println("Producer thread started - generating " + totalMessages + " messages");

        try {
            for (int i = 0; i < totalMessages; i++) {
                // Generate random room (1-20)
                int roomId = random.nextInt(20) + 1;

                // Generate message
                String message = MessageGenerator.generateMessage(roomId);

                // Put in queue (blocks if queue is full)
                messageQueue.put(message);

                // print progress
                if ((i + 1) % 50000 == 0) {
                    System.out.println("Generated " + (i + 1) + " messages...");
                }
            }

            messageQueue.markProducerFinished();
            System.out.println("Producer finished generating all messages");

        } catch (InterruptedException e) {
            System.out.println("Producer interrupted: " + e.getMessage());
        }
    }
}