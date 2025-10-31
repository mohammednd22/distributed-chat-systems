package com.chatflow.consumer;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

public class ConsumerMetrics {
    private AtomicLong messagesProcessed = new AtomicLong(0);
    private AtomicLong messagesDelivered = new AtomicLong(0);
    private AtomicLong messagesFailed = new AtomicLong(0);
    private AtomicLong duplicatesFiltered = new AtomicLong(0);

    private ConcurrentHashMap<String, AtomicLong> messagesPerRoom = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, AtomicLong> messagesPerConsumer = new ConcurrentHashMap<>();

    public void incrementMessagesProcessed() {
        messagesProcessed.incrementAndGet();
    }

    public void incrementMessagesDelivered() {
        messagesDelivered.incrementAndGet();
    }

    public void incrementMessagesFailed() {
        messagesFailed.incrementAndGet();
    }

    public void incrementDuplicatesFiltered() {
        duplicatesFiltered.incrementAndGet();
    }

    public void incrementRoomMessages(String roomId) {
        messagesPerRoom.computeIfAbsent(roomId, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void incrementConsumerMessages(String consumerId) {
        messagesPerConsumer.computeIfAbsent(consumerId, k -> new AtomicLong(0)).incrementAndGet();
    }

    // Getters
    public long getMessagesProcessed() { return messagesProcessed.get(); }
    public long getMessagesDelivered() { return messagesDelivered.get(); }
    public long getMessagesFailed() { return messagesFailed.get(); }
    public long getDuplicatesFiltered() { return duplicatesFiltered.get(); }

    public void printMetrics() {
        System.out.println("\n=== Consumer Metrics ===");
        System.out.println("Messages Processed: " + getMessagesProcessed());
        System.out.println("Messages Delivered: " + getMessagesDelivered());
        System.out.println("Messages Failed: " + getMessagesFailed());
        System.out.println("Duplicates Filtered: " + getDuplicatesFiltered());

        System.out.println("\nMessages per Room:");
        messagesPerRoom.forEach((room, count) ->
                System.out.println("  " + room + ": " + count.get()));
    }
}