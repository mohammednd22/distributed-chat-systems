package com.chatflow.client;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ThroughputVisualizer {

    private List<MessageMetric> metrics;

    public ThroughputVisualizer(List<MessageMetric> metrics) {
        this.metrics = metrics;
    }

    // Calculate throughput in 10-second buckets
    public Map<Integer, Integer> calculateThroughputBuckets() {
        if (metrics.isEmpty()) return new TreeMap<>();

        // Find earliest timestamp
        long startTime = metrics.stream()
                .mapToLong(MessageMetric::getTimestamp)
                .min()
                .orElse(0L);

        // Group messages into 10-second buckets
        Map<Integer, Integer> buckets = new TreeMap<>();

        for (MessageMetric metric : metrics) {
            long elapsedSeconds = (metric.getTimestamp() - startTime) / 1000;
            int bucket = (int) (elapsedSeconds / 10); // 10-second buckets
            buckets.put(bucket, buckets.getOrDefault(bucket, 0) + 1);
        }

        return buckets;
    }


    // Generate PNG chart using JFreeChart
    public void generateChart(String filename) {
        Map<Integer, Integer> buckets = calculateThroughputBuckets();
        ChartGenerator.createThroughputChart(buckets, filename);
    }

    // Export CSV data
    public void exportChartData(String filename) {
        Map<Integer, Integer> buckets = calculateThroughputBuckets();

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("TimeStart,TimeEnd,MessagesPerSecond\n");

            for (Map.Entry<Integer, Integer> entry : buckets.entrySet()) {
                int bucket = entry.getKey();
                int messages = entry.getValue();
                double messagesPerSec = messages / 10.0;

                writer.write(String.format("%d,%d,%.2f%n",
                        bucket * 10, (bucket + 1) * 10, messagesPerSec));
            }

            System.out.println("Throughput chart data exported to: " + filename);

        } catch (IOException e) {
            System.out.println("Error exporting chart data: " + e.getMessage());
        }
    }
}