# ChatFlow Load Testing Client (Part 1)

Multithreaded client for load testing with 500,000 messages.

## Requirements
- Java 17+
- Maven 3.6+
- Running ChatFlow server

## Configuration

Edit `LoadTestClient.java`:
```java
private static final String SERVER_URL = "ws://localhost:8080/chat/";
// For EC2: "ws://YOUR_EC2_IP:8080/chat/"
```

## Build
```bash
cd client-part1
mvn clean compile
```

## Run
```bash
mvn exec:java -Dexec.mainClass="com.chatflow.client.LoadTestClient"
```

## What It Does
- **Phase 1 (Warmup):** 32 threads × 1,000 messages = 32,000 messages
- **Phase 2 (Main):** ~936 threads × 500 messages = 468,000 messages
- Uses connection pooling (50 connections)
- Retry logic with exponential backoff

## Output
```
=== FINAL RESULTS ===
Total messages sent: 479952
Successful: 479952
Failed: 0
Total time: 51.39 seconds
Overall throughput: 9339.22 messages/second

=== CONNECTION STATISTICS ===
Total connections: 968
Reconnections: 39
```

## Little's Law Analysis
Predict throughput before testing:
```bash
mvn exec:java -Dexec.mainClass="com.chatflow.client.LittlesLawAnalysis"
```