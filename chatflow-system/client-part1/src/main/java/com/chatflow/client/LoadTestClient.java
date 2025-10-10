package com.chatflow.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTestClient {

    private static final String SERVER_URL = "ws://54.244.12.165:8080/chat/";
    private static final int WARMUP_THREADS = 32;
    private static final int MESSAGES_PER_WARMUP_THREAD = 1000;
    private static final int TOTAL_MESSAGES = 500000;
    private static final int QUEUE_CAPACITY = 40000;

    private static ConnectionStats stats = new ConnectionStats();
    private static AtomicInteger successCount = new AtomicInteger(0);
    private static AtomicInteger failureCount = new AtomicInteger(0);
    private static Random random = new Random();
    private static final int CONNECTION_POOL_SIZE = 50;

    public static void main(String[] args) {
        System.out.println("=== ChatFlow Load Test Client ===");
        System.out.println("Target: " + TOTAL_MESSAGES + " messages");

        long startTime = System.currentTimeMillis();

        try {
            // Phase 1: Warmup
            System.out.println("\n--- Phase 1: Warmup ---");
            runWarmupPhase();

            int warmupMessages = WARMUP_THREADS * MESSAGES_PER_WARMUP_THREAD;
            int remainingMessages = TOTAL_MESSAGES - warmupMessages;

            // Phase 2: Main load with producer-consumer
            System.out.println("\n--- Phase 2: Main Load (Producer-Consumer) ---");
            System.out.println("Remaining messages: " + remainingMessages);
            runMainPhaseWithQueue(remainingMessages);

            // Calculate and display results
            long endTime = System.currentTimeMillis();
            displayResults(startTime, endTime);

        } catch (Exception e) {
            System.out.println("Error during load test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runWarmupPhase() throws Exception {
        // Create message queue for warmup
        MessageQueue queue = new MessageQueue(QUEUE_CAPACITY);
        int warmupMessages = WARMUP_THREADS * MESSAGES_PER_WARMUP_THREAD;

        // Start producer thread
        Thread producer = new Thread(new MessageProducer(queue, warmupMessages));
        producer.start();

        // Create latch for warmup clients
        CountDownLatch latch = new CountDownLatch(WARMUP_THREADS);

        long phaseStart = System.currentTimeMillis();

        // Create 32 consumer clients
        for (int i = 0; i < WARMUP_THREADS; i++) {
            int roomId = random.nextInt(20) + 1;
            URI serverUri = new URI(SERVER_URL + roomId);

            ChatClient client = new ChatClient(
                    serverUri,
                    queue,
                    MESSAGES_PER_WARMUP_THREAD,
                    latch,
                    successCount,
                    failureCount,
                    stats
            );

            client.connect();
            //Thread.sleep(10);
        }

        System.out.println("Waiting for warmup phase to complete...");
        latch.await(250, TimeUnit.SECONDS);
        System.out.println("Messages still in queue: " + queue.size());
        producer.join(60000);

        long phaseEnd = System.currentTimeMillis();
        double phaseDuration = (phaseEnd - phaseStart) / 1000.0;

        System.out.println("Warmup complete!");
        System.out.println("Time: " + phaseDuration + " seconds");
        System.out.println("Warmup throughput: " + (warmupMessages / phaseDuration) + " msg/sec");
    }

    private static void runMainPhaseWithQueue(int remainingMessages) throws Exception {
        MessageQueue queue = new MessageQueue(QUEUE_CAPACITY);

        Thread producer = new Thread(new MessageProducer(queue, remainingMessages));
        producer.start();

        // Create connection pool
        ConnectionPool connectionPool = new ConnectionPool(
                CONNECTION_POOL_SIZE,
                SERVER_URL,
                successCount,
                failureCount,
                stats
        );

        int messagesPerThread = 500;
        int numThreads = (int) Math.ceil((double) remainingMessages / messagesPerThread);

        CountDownLatch latch = new CountDownLatch(numThreads);
        long phaseStart = System.currentTimeMillis();

        // Create clients using connection pool
        for (int i = 0; i < numThreads; i++) {
            int roomId = random.nextInt(20) + 1;

            int messagesToSend = (i == numThreads - 1) ?
                    (remainingMessages - (i * messagesPerThread)) : messagesPerThread;

            // Create client with pool
            URI serverUri = new URI(SERVER_URL + roomId);
            ChatClient client = new ChatClient(
                    serverUri, queue, messagesToSend,
                    latch, successCount, failureCount, stats, connectionPool
            );

            if (!client.isOpen()) {
                client.connect();
            }
        }

        System.out.println("Connection pool size: " + connectionPool.getPoolSize());
        latch.await(180, TimeUnit.SECONDS);
        System.out.println("Messages still in queue: " + queue.size());
        producer.join(60000);

        // Shutdown pool
        connectionPool.shutdown();

        long phaseEnd = System.currentTimeMillis();
        double phaseDuration = (phaseEnd - phaseStart) / 1000.0;

        System.out.println("Main phase complete!");
        System.out.println("Time: " + phaseDuration + " seconds");
    }

    private static void displayResults(long startTime, long endTime) {
        double totalTime = (endTime - startTime) / 1000.0;
        int totalSent = successCount.get() + failureCount.get();
        double throughput = totalSent / totalTime;

        System.out.println("\n=== FINAL RESULTS ===");
        System.out.println("Total messages sent: " + totalSent);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Total time: " + totalTime + " seconds");
        System.out.println("Overall throughput: " + String.format("%.2f", throughput) + " messages/second");

        stats.printStats();
    }
}