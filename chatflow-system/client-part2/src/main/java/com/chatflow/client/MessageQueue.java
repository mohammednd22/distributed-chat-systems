package com.chatflow.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageQueue {

    private BlockingQueue<String> queue;
    private volatile boolean producerFinished = false;

    public MessageQueue(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    // Producer calls this to add messages
    public void put(String message) throws InterruptedException {
        queue.put(message);
    }

    // Consumer calls this to get messages
    public String take() throws InterruptedException {
        return queue.take();
    }

    // Check if there are messages available
    public boolean hasMessages() {
        return !queue.isEmpty() || !producerFinished;
    }

    // Producer calls this when done generating
    public void markProducerFinished() {
        this.producerFinished = true;
    }

    // Check if producer is done
    public boolean isProducerFinished() {
        return producerFinished;
    }

    // Get current queue size
    public int size() {
        return queue.size();
    }
}