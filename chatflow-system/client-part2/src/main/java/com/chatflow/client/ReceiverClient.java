package com.chatflow.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

public class ReceiverClient extends WebSocketClient {
    private MetricsCollector metricsCollector;
    private AtomicInteger successCount;

    public ReceiverClient(String uri, MetricsCollector metricsCollector,
                          AtomicInteger successCount) throws Exception {
        super(new URI(uri));
        this.metricsCollector = metricsCollector;
        this.successCount = successCount;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Receiver connected: " + getURI().getPath());
    }

    @Override
    public void onMessage(String message) {
        // Use static method to record end-to-end metrics
        OptimizedDistributedPerformanceChatClient.recordBroadcastMetric(
                message, metricsCollector, successCount
        );
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Receiver closed: " + getURI().getPath());
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Receiver error: " + ex.getMessage());
    }
}