# ChatFlow - Distributed Chat System

A scalable WebSocket-based chat infrastructure built for high-volume real-time messaging. Part of a distributed systems class exploring enterprise-grade messaging patterns.

## Project Structure

```
chatflow-distributed-system/
├── server/              # WebSocket server with validation
├── client-part1/        # Load testing client
├── client-part2/        # Performance analysis (coming soon)
└── results/            # Test results and metrics
```

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+

### Running the Server

```bash
cd server
mvn compile exec:java -Dexec.mainClass="com.chatflow.server.ChatFlowServer"
```

Server endpoints:
- WebSocket: `ws://localhost:8080/chat/{roomId}`
- Health: `http://localhost:8081/health`

### Running the Load Test Client

```bash
cd client-part1
mvn compile exec:java -Dexec.mainClass="com.chatflow.client.LoadTestClient"
```

Sends 500,000 messages across 20 rooms with performance metrics.

## Message Format

```json
{
  "userId": "12345",
  "username": "user12345",
  "message": "Hello!",
  "timestamp": "2025-09-30T10:00:00Z",
  "messageType": "TEXT"
}
```

**Validation:**
- userId: 1-100,000
- username: 3-20 alphanumeric characters
- message: 1-500 characters
- messageType: TEXT | JOIN | LEAVE

## Testing

**Quick test with Postman/Insomnia:**
1. Create WebSocket request
2. Connect to: `ws://localhost:8080/chat/room1`
3. Send JSON message
4. Receive SUCCESS/ERROR response

**Command line with wscat:**
```bash
npm install -g wscat
wscat -c ws://localhost:8080/chat/room1
```

## Tech Stack

- Java 17
- Java-WebSocket 1.5.4
- Jackson (JSON processing)
- Maven

## Build

```bash
# Build all modules
mvn clean install

# Build specific module
cd server && mvn clean package
```