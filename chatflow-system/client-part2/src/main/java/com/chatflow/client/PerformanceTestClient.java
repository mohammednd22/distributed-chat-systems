package com.chatflow.client;

import java.net.URI;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceTestClient {

    private static final String SERVER_URL = "ws://54.244.12.165:8080/chat/";
    private static final int WARMUP_THREADS = 32;
    private static final int MESSAGES_PER_WARMUP_THREAD = 1000;
    private static final int TOTAL_MESSAGES = 500000;
    private static final int QUEUE_CAPACITY = 40000;

    private static MetricsCollector metricsCollector = new MetricsCollector();
    private static ConnectionStats stats = new ConnectionStats();
    private static AtomicInteger successCount = new AtomicInteger(0);
    private static AtomicInteger failureCount = new AtomicInteger(0);
    private static Random random = new Random();
    private static final int CONNECTION_POOL_SIZE = 50; // Reuse 50 connections

    public static void main(String[] args) {
        System.out.println("=== ChatFlow Performance Test Client ===");
        System.out.println("Target: " + TOTAL_MESSAGES + " messages with detailed metrics");

        long startTime = System.currentTimeMillis();

        try {
            // Phase 1: Warmup
            System.out.println("\n--- Phase 1: Warmup ---");
            runWarmupPhase();

            int warmupMessages = WARMUP_THREADS * MESSAGES_PER_WARMUP_THREAD;
            int remainingMessages = TOTAL_MESSAGES - warmupMessages;

            // Phase 2: Main load
            System.out.println("\n--- Phase 2: Main Load ---");
            runMainPhase(remainingMessages);

            // Calculate results
            long endTime = System.currentTimeMillis();
            displayResults(startTime, endTime);

            // Statistical analysis
            performStatisticalAnalysis();

            // Write to CSV
            exportToCSV();

            // Visualize throughput
            visualizeThroughput();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runWarmupPhase() throws Exception {
        MessageQueue queue = new MessageQueue(QUEUE_CAPACITY);
        int warmupMessages = WARMUP_THREADS * MESSAGES_PER_WARMUP_THREAD;

        Thread producer = new Thread(new MessageProducer(queue, warmupMessages));
        producer.start();

        CountDownLatch latch = new CountDownLatch(WARMUP_THREADS);
        long phaseStart = System.currentTimeMillis();

        for (int i = 0; i < WARMUP_THREADS; i++) {
            int roomId = random.nextInt(20) + 1;
            URI serverUri = new URI(SERVER_URL + roomId);

            PerformanceChatClient client = new PerformanceChatClient(
                    serverUri, queue, metricsCollector, MESSAGES_PER_WARMUP_THREAD,
                    roomId, latch, successCount, failureCount, stats
            );

            client.connect();
        }

        latch.await(180, TimeUnit.SECONDS);
        producer.join(60000);

        long phaseEnd = System.currentTimeMillis();
        double duration = (phaseEnd - phaseStart) / 1000.0;

        System.out.println("Warmup complete in " + duration + " seconds");
    }

    private static void runMainPhase(int remainingMessages) throws Exception {
        MessageQueue queue = new MessageQueue(QUEUE_CAPACITY);

        Thread producer = new Thread(new MessageProducer(queue, remainingMessages));
        producer.start();

        // Create connection pool
        ConnectionPool connectionPool = new ConnectionPool(
                CONNECTION_POOL_SIZE,
                SERVER_URL,
                metricsCollector,
                successCount,
                failureCount,
                stats
        );

        int messagesPerThread = 500;
        int numThreads = (int) Math.ceil((double) remainingMessages / messagesPerThread);

        CountDownLatch latch = new CountDownLatch(numThreads);
        long phaseStart = System.currentTimeMillis();

        // Create worker threads that use pooled connections
        for (int i = 0; i < numThreads; i++) {
            int roomId = random.nextInt(20) + 1;

            int messagesToSend = (i == numThreads - 1) ?
                    (remainingMessages - (i * messagesPerThread)) : messagesPerThread;


            // For simplicity, we'll create new clients but they'll be pooled
            URI serverUri = new URI(SERVER_URL + roomId);
            PerformanceChatClient client = new PerformanceChatClient(
                    serverUri, queue, metricsCollector, messagesToSend,
                    roomId, latch, successCount, failureCount, stats, connectionPool
            );

            if (!client.isOpen()) {
                client.connect();
            }
        }

        System.out.println("Connection pool size: " + connectionPool.getPoolSize());
        latch.await(180, TimeUnit.SECONDS);
        producer.join(60000);

        // Shutdown pool
        connectionPool.shutdown();

        long phaseEnd = System.currentTimeMillis();
        double duration = (phaseEnd - phaseStart) / 1000.0;

        System.out.println("Main phase complete in " + duration + " seconds");
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
        System.out.println("Overall throughput: " + String.format("%.2f", throughput) + " msg/sec");

        stats.printStats();
    }

    private static void performStatisticalAnalysis() {
        List<MessageMetric> allMetrics = metricsCollector.getAllMetrics();
        StatisticalAnalysis analysis = new StatisticalAnalysis(allMetrics);
        analysis.printStatistics();
    }

    private static void exportToCSV() {
        List<MessageMetric> allMetrics = metricsCollector.getAllMetrics();
        String filename = "results/performance_metrics.csv";
        CSVWriter.writeMetrics(allMetrics, filename);
    }

    private static void visualizeThroughput() {
        List<MessageMetric> allMetrics = metricsCollector.getAllMetrics();
        ThroughputVisualizer visualizer = new ThroughputVisualizer(allMetrics);


        visualizer.generateChart("results/throughput_chart.png");
        visualizer.exportChartData("results/throughput_over_time.csv");
    }
}