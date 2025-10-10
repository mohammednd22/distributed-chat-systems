# ChatFlow Performance Analysis Client (Part 2)

Enhanced client with per-message latency tracking, statistical analysis, and visualization.

## Requirements
- Java 17+
- Maven 3.6+
- Running ChatFlow server

## Configuration

Edit `PerformanceTestClient.java`:
```java
private static final String SERVER_URL = "ws://localhost:8080/chat/";
// For EC2: "ws://YOUR_EC2_IP:8080/chat/"
```

## Build
```bash
cd client-part2
mvn clean compile
```

## Run
```bash
mvn exec:java -Dexec.mainClass="com.chatflow.client.PerformanceTestClient"
```

## What It Does
- Same load testing as Part 1 (500K messages)
- Tracks per-message latency (send → receive)
- Calculates statistics (mean, median, percentiles)
- Exports data to CSV
- Generates throughput chart

## Output

```
=== STATISTICAL ANALYSIS ===
Total messages analyzed: 455760

Latency Statistics:
Mean response time: 277.52 ms
Median response time: 228.00 ms
95th percentile: 529.00 ms
99th percentile: 1601.00 ms
Min latency: 24 ms
Max latency: 2038 ms

Message Type Distribution:
JOIN: 22665 (4.97%)
LEAVE: 22600 (4.96%)
TEXT: 410495 (90.07%)
```

## Generated Files (in results/ directory)

1. **performance_metrics.csv** - All message metrics
2. **throughput_chart.png** - Visual chart
3. **throughput_over_time.csv** - Raw data for analysis