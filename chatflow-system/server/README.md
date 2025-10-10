# ChatFlow Server

WebSocket server with message validation and health monitoring.

## Requirements
- Java 17+
- Maven 3.6+

## Build
```bash
cd server
mvn clean package
```

## Run Locally
```bash
mvn exec:java -Dexec.mainClass="com.chatflow.server.ChatFlowServer"
```

**Endpoints:**
- WebSocket: `ws://localhost:8080/chat/{roomId}`
- Health: `http://localhost:8081/health`

## Deploy to AWS EC2

1. Upload JAR:
```bash
scp -i your-key.pem target/chatflow-server.jar ec2-user@YOUR_EC2_IP:~/
```

2. SSH and run:
```bash
ssh -i your-key.pem ec2-user@YOUR_EC2_IP
java -jar chatflow-server.jar
```

## Test
```bash
curl http://localhost:8081/health
```

## Message Format
```json
{
  "userId": "12345",
  "username": "user12345",
  "message": "Hello!",
  "timestamp": "2025-10-07T10:00:00Z",
  "messageType": "TEXT"
}
```

**Validation:**
- userId: 1-100,000
- username: 3-20 alphanumeric
- message: 1-500 characters
- messageType: TEXT, JOIN, or LEAVE