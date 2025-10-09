package com.chatflow.client;

import java.util.*;
import java.util.stream.Collectors;

public class StatisticalAnalysis {

    private List<MessageMetric> metrics;

    public StatisticalAnalysis(List<MessageMetric> metrics) {
        this.metrics = metrics;
    }

    // Calculate mean latency
    public double getMean() {
        if (metrics.isEmpty()) return 0.0;
        return metrics.stream()
                .mapToLong(MessageMetric::getLatencyMs)
                .average()
                .orElse(0.0);
    }

    // Calculate median latency
    public double getMedian() {
        if (metrics.isEmpty()) return 0.0;

        List<Long> latencies = metrics.stream()
                .map(MessageMetric::getLatencyMs)
                .sorted()
                .collect(Collectors.toList());

        int size = latencies.size();
        if (size % 2 == 0) {
            return (latencies.get(size/2 - 1) + latencies.get(size/2)) / 2.0;
        } else {
            return latencies.get(size/2);
        }
    }

    // Calculate percentile (e.g., 95th, 99th)
    public double getPercentile(int percentile) {
        if (metrics.isEmpty()) return 0.0;

        List<Long> latencies = metrics.stream()
                .map(MessageMetric::getLatencyMs)
                .sorted()
                .collect(Collectors.toList());

        int index = (int) Math.ceil(percentile / 100.0 * latencies.size()) - 1;
        index = Math.max(0, Math.min(index, latencies.size() - 1));

        return latencies.get(index);
    }

    // Get minimum latency
    public long getMin() {
        return metrics.stream()
                .mapToLong(MessageMetric::getLatencyMs)
                .min()
                .orElse(0L);
    }

    // Get maximum latency
    public long getMax() {
        return metrics.stream()
                .mapToLong(MessageMetric::getLatencyMs)
                .max()
                .orElse(0L);
    }

    // Throughput per room
    public Map<Integer, Long> getThroughputPerRoom() {
        return metrics.stream()
                .collect(Collectors.groupingBy(
                        MessageMetric::getRoomId,
                        Collectors.counting()
                ));
    }

    // Message type distribution
    public Map<String, Long> getMessageTypeDistribution() {
        return metrics.stream()
                .collect(Collectors.groupingBy(
                        MessageMetric::getMessageType,
                        Collectors.counting()
                ));
    }

    // Print all statistics
    public void printStatistics() {
        System.out.println("\n=== STATISTICAL ANALYSIS ===");
        System.out.println("Total messages analyzed: " + metrics.size());
        System.out.println("\nLatency Statistics:");
        System.out.println("Mean response time: " + String.format("%.2f", getMean()) + " ms");
        System.out.println("Median response time: " + String.format("%.2f", getMedian()) + " ms");
        System.out.println("95th percentile: " + String.format("%.2f", getPercentile(95)) + " ms");
        System.out.println("99th percentile: " + String.format("%.2f", getPercentile(99)) + " ms");
        System.out.println("Min latency: " + getMin() + " ms");
        System.out.println("Max latency: " + getMax() + " ms");

        System.out.println("\nMessage Type Distribution:");
        getMessageTypeDistribution().forEach((type, count) ->
                System.out.println(type + ": " + count + " (" +
                        String.format("%.2f", count * 100.0 / metrics.size()) + "%)"));

        System.out.println("\nThroughput Per Room:");
        getThroughputPerRoom().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry ->
                        System.out.println("Room " + entry.getKey() + ": " + entry.getValue() + " messages"));
    }
}