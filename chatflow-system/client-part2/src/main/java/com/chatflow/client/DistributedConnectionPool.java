package com.chatflow.client;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DistributedConnectionPool {
    private BlockingQueue<OptimizedDistributedPerformanceChatClient> senderPool;
    private BlockingQueue<ReceiverClient> receiverPool;
    private String albEndpoint;
    private String consumerEndpoint;
    private int maxSenders;
    private int maxReceivers;
    private AtomicInteger activeSenders = new AtomicInteger(0);
    private AtomicInteger activeReceivers = new AtomicInteger(0);

    public DistributedConnectionPool(int maxSenders, int maxReceivers,
                                     String albEndpoint, String consumerEndpoint) {
        this.maxSenders = maxSenders;
        this.maxReceivers = maxReceivers;
        this.albEndpoint = albEndpoint;
        this.consumerEndpoint = consumerEndpoint;
        this.senderPool = new LinkedBlockingQueue<>(maxSenders);
        this.receiverPool = new LinkedBlockingQueue<>(maxReceivers);
    }

    public OptimizedDistributedPerformanceChatClient borrowSender(int roomId,
                                                                  MessageQueue messageQueue,
                                                                  MetricsCollector metricsCollector,
                                                                  int messagesToSend,
                                                                  java.util.concurrent.CountDownLatch latch,
                                                                  AtomicInteger successCount,
                                                                  AtomicInteger failureCount,
                                                                  ConnectionStats stats) throws Exception {

        OptimizedDistributedPerformanceChatClient client = senderPool.poll();

        if (client != null && client.isOpen()) {
            return client;
        }

        if (activeSenders.get() < maxSenders) {
            URI senderUri = new URI(albEndpoint + "/chat/" + roomId);
            client = new OptimizedDistributedPerformanceChatClient(
                    senderUri, messageQueue, metricsCollector, messagesToSend,
                    roomId, latch, successCount, failureCount, stats, this
            );

            activeSenders.incrementAndGet();
            client.connect();
            Thread.sleep(50); // Connection stagger
            return client;
        }

        return senderPool.take();
    }

    public void releaseSender(OptimizedDistributedPerformanceChatClient client) {
        if (client != null && client.isOpen()) {
            senderPool.offer(client);
        }
    }

    public void shutdown() {
        System.out.println("Shutting down connection pool...");

        while (!senderPool.isEmpty()) {
            OptimizedDistributedPerformanceChatClient client = senderPool.poll();
            if (client != null) {
                client.close();
            }
        }
    }

    public int getActiveSenders() { return activeSenders.get(); }
    public int getActiveReceivers() { return activeReceivers.get(); }
}