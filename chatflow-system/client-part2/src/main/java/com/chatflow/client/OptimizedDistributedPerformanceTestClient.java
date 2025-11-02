package com.chatflow.client;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class OptimizedDistributedPerformanceTestClient {

    private static final String ALB_ENDPOINT = "ws://chatflow-alb-1833520762.us-west-2.elb.amazonaws.com";
    //private static final String CONSUMER_ENDPOINT = "ws://44.248.99.167:8082";
    private static final String CONSUMER_ENDPOINT = "ws://localhost:8082";

    // Optimized for distributed system
    private static final int WARMUP_THREADS = 32;
    private static final int MESSAGES_PER_WARMUP_THREAD = 1000;
    private static final int TOTAL_MESSAGES = 500000;
    private static final int QUEUE_CAPACITY = 250000;  // Larger buffer
    private static final int SENDER_POOL_SIZE = 400;  // More senders for ALB
    private static final int RECEIVER_POOL_SIZE = 25;  // One per room + buffer
    private static final int MAIN_PHASE_THREADS = 200; // Test load distribution

    private static MetricsCollector metricsCollector = new MetricsCollector();
    private static ConnectionStats stats = new ConnectionStats();
    private static AtomicInteger successCount = new AtomicInteger(0);
    private static AtomicInteger failureCount = new AtomicInteger(0);
    private static Random random = new Random();

    public static void main(String[] args) {
        System.out.println("=== Optimized Distributed Performance Test ===");
        System.out.println("ALB Endpoint: " + ALB_ENDPOINT);
        System.out.println("Consumer Endpoint: " + CONSUMER_ENDPOINT);
        System.out.println("Architecture: Clients -> ALB -> [3 Servers] -> RabbitMQ -> Consumer");

        long startTime = System.currentTimeMillis();

        try {
            // Start receiver clients first
            startReceiverClients();
            Thread.sleep(2000); // Let receivers connect

            // Phase 1: Warmup (same as Assignment 1)
            runWarmupPhase();

            int warmupMessages = WARMUP_THREADS * MESSAGES_PER_WARMUP_THREAD;
            int remainingMessages = TOTAL_MESSAGES - warmupMessages;

            // Phase 2: Main load with optimizations
            runOptimizedMainPhase(remainingMessages);

            long endTime = System.currentTimeMillis();
            displayResults(startTime, endTime);

            // Your existing analysis tools
            performStatisticalAnalysis();
            exportToCSV();
            visualizeThroughput();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void startReceiverClients() throws Exception {
        System.out.println("Starting receiver clients for all rooms...");

        // One receiver per room to catch all broadcasts
        for (int roomId = 1; roomId <= 20; roomId++) {
            ReceiverClient receiver = new ReceiverClient(
                    CONSUMER_ENDPOINT + "/chat/" + roomId,
                    metricsCollector, successCount
            );

            new Thread(() -> {
                try {
                    receiver.connect();
                } catch (Exception e) {
                    System.out.println("Receiver error: " + e.getMessage());
                }
            }).start();

            Thread.sleep(100); // Stagger connections
        }
    }

    private static void runWarmupPhase() throws Exception {
        System.out.println("\n--- Warmup Phase (Preserving Assignment 1 Pattern) ---");

        // Create message queue
        MessageQueue queue = new MessageQueue(QUEUE_CAPACITY);
        int warmupMessages = WARMUP_THREADS * MESSAGES_PER_WARMUP_THREAD;

        // Single producer thread
        Thread producer = new Thread(new MessageProducer(queue, warmupMessages));
        producer.start();

        // Connection pool for warmup
        DistributedConnectionPool connectionPool = new DistributedConnectionPool(
                WARMUP_THREADS, RECEIVER_POOL_SIZE, ALB_ENDPOINT, CONSUMER_ENDPOINT
        );

        CountDownLatch latch = new CountDownLatch(WARMUP_THREADS);
        long phaseStart = System.currentTimeMillis();

        // Create sender clients (pulling from shared queue)
        for (int i = 0; i < WARMUP_THREADS; i++) {
            int roomId = random.nextInt(20) + 1;

            OptimizedDistributedPerformanceChatClient client = connectionPool.borrowSender(
                    roomId, queue, metricsCollector, MESSAGES_PER_WARMUP_THREAD,
                    latch, successCount, failureCount, stats
            );
        }

        latch.await(300, TimeUnit.SECONDS);
        producer.join(60000);

        long phaseEnd = System.currentTimeMillis();
        double duration = (phaseEnd - phaseStart) / 1000.0;

        System.out.println("Warmup completed: " + String.format("%.2f", warmupMessages / duration) + " msg/sec");
        connectionPool.shutdown();
    }

    private static void runOptimizedMainPhase(int remainingMessages) throws Exception {
        System.out.println("\n--- Main Phase (Load Balanced Distribution) ---");

        MessageQueue queue = new MessageQueue(QUEUE_CAPACITY);
        Thread producer = new Thread(new MessageProducer(queue, remainingMessages));
        producer.start();

        DistributedConnectionPool connectionPool = new DistributedConnectionPool(
                SENDER_POOL_SIZE, RECEIVER_POOL_SIZE, ALB_ENDPOINT, CONSUMER_ENDPOINT
        );

        // Calculate thread distribution
        int messagesPerThread = Math.max(1000, remainingMessages / MAIN_PHASE_THREADS);
        int numThreads = Math.min(MAIN_PHASE_THREADS, (remainingMessages + messagesPerThread - 1) / messagesPerThread);

        CountDownLatch latch = new CountDownLatch(numThreads);
        long phaseStart = System.currentTimeMillis();

        // Create sender clients with correct message distribution
        for (int i = 0; i < numThreads; i++) {
            int roomId = random.nextInt(20) + 1;

            // Calculate messages for this specific thread
            int messagesToSend = (i == numThreads - 1) ?
                    (remainingMessages - (i * messagesPerThread)) : messagesPerThread;

            OptimizedDistributedPerformanceChatClient client = connectionPool.borrowSender(
                    roomId, queue, metricsCollector, messagesToSend,
                    latch, successCount, failureCount, stats
            );
        }

        System.out.println("Main phase: " + numThreads + " senders -> ALB -> 3 servers");
        latch.await(600, TimeUnit.SECONDS);
        producer.join(60000);

        connectionPool.shutdown();

        long phaseEnd = System.currentTimeMillis();
        double duration = (phaseEnd - phaseStart) / 1000.0;
        System.out.println("Main phase: " + String.format("%.2f", remainingMessages / duration) + " msg/sec");
    }


    private static void displayResults(long startTime, long endTime) {
        double totalTime = (endTime - startTime) / 1000.0;
        int totalSent = successCount.get() + failureCount.get();
        double throughput = totalSent / totalTime;

        System.out.println("\n=== DISTRIBUTED SYSTEM PERFORMANCE ===");
        System.out.println("Total messages: " + totalSent);
        System.out.println("End-to-end successful: " + successCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Total time: " + totalTime + " seconds");
        System.out.println("Distributed throughput: " + String.format("%.2f", throughput) + " msg/sec");

        stats.printStats();
    }

    private static void performStatisticalAnalysis() {
        List<MessageMetric> allMetrics = metricsCollector.getAllMetrics();
        StatisticalAnalysis analysis = new StatisticalAnalysis(allMetrics);
        System.out.println("\n=== END-TO-END LATENCY ANALYSIS ===");
        analysis.printStatistics();
    }

    private static void exportToCSV() {
        List<MessageMetric> allMetrics = metricsCollector.getAllMetrics();
        CSVWriter.writeMetrics(allMetrics, "results/distributed_performance_metrics.csv");
    }

    private static void visualizeThroughput() {
        List<MessageMetric> allMetrics = metricsCollector.getAllMetrics();
        ThroughputVisualizer visualizer = new ThroughputVisualizer(allMetrics);
        visualizer.generateChart("results/distributed_throughput_chart.png");
        visualizer.exportChartData("results/distributed_throughput_over_time.csv");
    }
}