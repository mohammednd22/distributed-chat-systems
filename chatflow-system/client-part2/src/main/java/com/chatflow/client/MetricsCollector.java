package com.chatflow.client;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.ArrayList;

public class MetricsCollector {

    private ConcurrentLinkedQueue<MessageMetric> metrics;

    public MetricsCollector() {
        this.metrics = new ConcurrentLinkedQueue<>();
    }

    // Thread-safe add
    public void addMetric(MessageMetric metric) {
        metrics.add(metric);
    }

    // Get all metrics as list
    public List<MessageMetric> getAllMetrics() {
        return new ArrayList<>(metrics);
    }

    // Get total count
    public int size() {
        return metrics.size();
    }
}