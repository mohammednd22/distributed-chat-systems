package com.chatflow.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTestClient {

    private static final String SERVER_URL = "ws://localhost:8080/chat/";
    private static final int WARMUP_THREADS = 32;
    private static final int MESSAGES_PER_WARMUP_THREAD = 1000;
    private static final int TOTAL_MESSAGES = 500000;

    private static AtomicInteger successCount = new AtomicInteger(0);
    private static AtomicInteger failureCount = new AtomicInteger(0);
    private static Random random = new Random();

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

            // Phase 2: Main load
            System.out.println("\n--- Phase 2: Main Load ---");
            System.out.println("Remaining messages: " + remainingMessages);
            runMainPhase(remainingMessages);

            // Calculate and display results
            long endTime = System.currentTimeMillis();
            displayResults(startTime, endTime);

        } catch (Exception e) {
            System.out.println("Error during load test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Phase 1: 32 threads, each sends 1000 messages
    private static void runWarmupPhase() throws Exception {
        CountDownLatch latch = new CountDownLatch(WARMUP_THREADS);
        List<ChatClient> clients = new ArrayList<>();

        long phaseStart = System.currentTimeMillis();

        // Create 32 clients
        for (int i = 0; i < WARMUP_THREADS; i++) {
            int roomId = random.nextInt(20) + 1; // Random room 1-20
            URI serverUri = new URI(SERVER_URL + roomId);

            ChatClient client = new ChatClient(
                    serverUri,
                    MESSAGES_PER_WARMUP_THREAD,
                    roomId,
                    latch,
                    successCount,
                    failureCount
            );

            clients.add(client);
            client.connect();

            // Small delay between connections
            Thread.sleep(10);
        }

        // Wait for all warmup clients to finish
        System.out.println("Waiting for warmup phase to complete...");
        latch.await();

        long phaseEnd = System.currentTimeMillis();
        double phaseDuration = (phaseEnd - phaseStart) / 1000.0;
        int warmupMessages = WARMUP_THREADS * MESSAGES_PER_WARMUP_THREAD;

        System.out.println("Warmup complete!");
        System.out.println("Time: " + phaseDuration + " seconds");
        System.out.println("Warmup throughput: " + (warmupMessages / phaseDuration) + " msg/sec");
    }

    // Phase 2: Send remaining messages (you can optimize thread count here)
    private static void runMainPhase(int remainingMessages) throws Exception {
        // For now, use same strategy as warmup
        // You can optimize this later (more/fewer threads, connection reuse, etc.)

        int messagesPerThread = 1000;
        int numThreads = (int) Math.ceil((double) remainingMessages / messagesPerThread);

        CountDownLatch latch = new CountDownLatch(numThreads);

        long phaseStart = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            int roomId = random.nextInt(20) + 1;
            URI serverUri = new URI(SERVER_URL + roomId);

            // Last thread might send fewer messages
            int messagesToSend = (i == numThreads - 1) ?
                    (remainingMessages - (i * messagesPerThread)) : messagesPerThread;

            ChatClient client = new ChatClient(
                    serverUri,
                    messagesToSend,
                    roomId,
                    latch,
                    successCount,
                    failureCount
            );

            client.connect();
            Thread.sleep(10);
        }

        System.out.println("Waiting for main phase to complete...");
        latch.await();

        long phaseEnd = System.currentTimeMillis();
        double phaseDuration = (phaseEnd - phaseStart) / 1000.0;

        System.out.println("Main phase complete!");
        System.out.println("Time: " + phaseDuration + " seconds");
    }

    // Display final results
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
    }
}