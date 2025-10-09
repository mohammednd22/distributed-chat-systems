package com.chatflow.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class LittlesLawAnalysis {

    private static final String SERVER_URL = "ws://localhost:8080/chat/1";
    private static final int SAMPLE_MESSAGES = 100;

    public static void main(String[] args) {
        System.out.println("=== Little's Law Analysis ===");
        System.out.println("Measuring single message latency...\n");

        try {
            double avgLatency = measureAverageLatency();
            calculateTheoretical(avgLatency);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static double measureAverageLatency() throws Exception {
        List<Long> latencies = new ArrayList<>();
        Object lock = new Object();

        // Create single client
        WebSocketClient client = new WebSocketClient(new URI(SERVER_URL)) {
            private long sendTime;

            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("Connected. Sending " + SAMPLE_MESSAGES + " test messages...");

                new Thread(() -> {
                    for (int i = 0; i < SAMPLE_MESSAGES; i++) {
                        try {
                            sendTime = System.nanoTime();
                            String msg = MessageGenerator.generateMessage(1);
                            send(msg);
                            Thread.sleep(50); // Space out requests
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void onMessage(String message) {
                long receiveTime = System.nanoTime();
                long latency = (receiveTime - sendTime) / 1_000_000; // Convert to ms

                synchronized(lock) {
                    latencies.add(latency);

                    if (latencies.size() == SAMPLE_MESSAGES) {
                        lock.notify();
                    }
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("Connection closed");
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        };

        client.connect();

        // Wait for all responses
        synchronized(lock) {
            lock.wait(30000); // 30 second timeout
        }

        client.close();

        // Calculate average
        double avgLatency = latencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        System.out.println("\nLatency Results:");
        System.out.println("Samples: " + latencies.size());
        System.out.println("Average latency: " + String.format("%.2f", avgLatency) + " ms");
        System.out.println("Min latency: " + latencies.stream().min(Long::compare).get() + " ms");
        System.out.println("Max latency: " + latencies.stream().max(Long::compare).get() + " ms");

        return avgLatency;
    }

    private static void calculateTheoretical(double avgLatencyMs) {
        System.out.println("\n=== Little's Law Calculations ===");

        // Convert latency to seconds
        double avgLatencySec = avgLatencyMs / 1000.0;

        System.out.println("Average latency (L): " + String.format("%.4f", avgLatencySec) + " seconds");

        // Test different concurrency levels
        int[] concurrencyLevels = {32, 100, 250, 500};

        System.out.println("\nTheoretical Throughput (λ = N / L):");
        System.out.println("Concurrency (N) | Predicted Throughput (msg/sec)");
        System.out.println("----------------|--------------------------------");

        for (int N : concurrencyLevels) {
            double throughput = N / avgLatencySec;
            System.out.println(String.format("%15d | %,.2f", N, throughput));
        }

        System.out.println("\n=== Comparison with Actual Results ===");
        System.out.println("Your load test used 500 concurrent connections");
        double predicted500 = 500 / avgLatencySec;
        System.out.println("Predicted throughput: " + String.format("%,.2f", predicted500) + " msg/sec");
        System.out.println("Actual throughput: ~46,000 msg/sec");
        System.out.println("\nNote: Actual may differ due to:");
        System.out.println("- Network overhead");
        System.out.println("- Connection setup/teardown time");
        System.out.println("- Server processing variations");
        System.out.println("- Client-side batching effects");
    }
}