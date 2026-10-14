# Simple Chat Application Using Redis

A production-ready, REST-compliant backend chat application built with **Java 21**, **Spring Boot 4**, and **Redis** (or Memurai on Windows). It supports room creation, participant tracking, message persistence in chronological order, real-time message broadcasting via Redis Pub/Sub, and robust error handling.

---

## 📌 Architecture & Redis Data Structures

The application directly leverages native Redis data structures without unnecessary layers:

| Component | Redis Data Structure | Key Pattern / Channel | Description |
| :--- | :--- | :--- | :--- |
| **Chat Room Metadata** | **Hash** | `chatroom:{roomId}:meta` | Stores `roomId`, `roomName`, and `createdAt` timestamp. |
| **Participants** | **Set** | `chatroom:{roomId}:participants` | Uniquely tracks room members using `SADD` ($O(1)$ membership). |
| **Chat Messages** | **List** | `chatroom:{roomId}:messages` | Appends messages via `RPUSH` to maintain strict chronological order. |
| **Real-time Messaging** | **Pub/Sub** | `chatroom:{roomId}` | Broadcasts message JSON payloads to real-time subscribers via `convertAndSend`. |

---

## 🚀 Prerequisites

- **Java**: JDK 21 or higher
- **Redis Engine**:
  - **Windows**: [Memurai](https://www.memurai.com/) (Redis-compatible for Windows) or Docker Redis
  - **Linux / macOS**: Redis Server 6.x or newer
  - **Docker** (Optional): `docker run -d --name redis -p 6379:6379 redis:latest`
- **Maven**: Pre-configured via included `./mvnw` (Linux/macOS) or `.\mvnw.cmd` (Windows)

---

## ⚙️ Quick Start & Setup

### 1. Verify Redis / Memurai is Running
```powershell
# On Windows with Memurai:
memurai-cli.exe PING
# Expected response: PONG

# On Linux / macOS / Docker:
redis-cli ping
# Expected response: PONG
```

### 2. Build and Run the Spring Boot Application
```powershell
# Windows
.\mvnw.cmd clean spring-boot:run

# Linux / macOS
./mvnw clean spring-boot:run
```
The application starts on **`http://localhost:8080`**.

---

## 📖 Interactive Swagger UI & OpenAPI Documentation

Once started, explore and test all APIs interactively in your browser:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 📡 API Specifications & Examples

### 1. Create a Chat Room
- **Method & Endpoint**: `POST /api/chatapp/chatrooms`
- **Request Body**:
  ```json
  {
    "roomName": "general"
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "message": "Chat room 'general' created successfully.",
    "roomId": "general",
    "status": "success"
  }
  ```
- **Duplicate Error Response (409 Conflict)**:
  ```json
  {
    "status": "error",
    "message": "Chat room 'general' already exists."
  }
  ```

---

### 2. Join a Chat Room
- **Method & Endpoint**: `POST /api/chatapp/chatrooms/{roomId}/join`
- **Request URL**: `POST /api/chatapp/chatrooms/general/join`
- **Request Body**:
  ```json
  {
    "participant": "guest_user"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "message": "User 'guest_user' joined chat room 'general'.",
    "status": "success"
  }
  ```
- **Non-existent Room Error Response (404 Not Found)**:
  ```json
  {
    "status": "error",
    "message": "Chat room 'general' does not exist."
  }
  ```

---

### 3. Send a Message
- **Method & Endpoint**: `POST /api/chatapp/chatrooms/{roomId}/messages`
- **Request URL**: `POST /api/chatapp/chatrooms/general/messages`
- **Request Body**:
  ```json
  {
    "participant": "guest_user",
    "message": "Hello, everyone!"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "message": "Message sent successfully.",
    "status": "success"
  }
  ```
*Action: Saves message JSON into Redis List `chatroom:general:messages` and broadcasts it to Pub/Sub channel `chatroom:general`.*

---

### 4. Retrieve Chat History
- **Method & Endpoint**: `GET /api/chatapp/chatrooms/{roomId}/messages?limit=10`
- **Request URL**: `GET /api/chatapp/chatrooms/general/messages?limit=10`
- **Response (200 OK)**:
  ```json
  {
    "messages": [
      {
        "participant": "guest_user",
        "message": "Hello, everyone!",
        "timestamp": "2026-10-14T13:38:11Z"
      },
      {
        "participant": "another_user",
        "message": "Hi, guest_user!",
        "timestamp": "2026-10-14T13:39:00Z"
      }
    ]
  }
  ```
*Action: Efficiently fetches the last $N$ messages in chronological order using Redis `LRANGE -limit -1`.*

---

### 5. Delete a Chat Room (Optional Requirement)
- **Method & Endpoint**: `DELETE /api/chatapp/chatrooms/{roomId}`
- **Request URL**: `DELETE /api/chatapp/chatrooms/general`
- **Response (200 OK)**:
  ```json
  {
    "message": "Chat room 'general' deleted successfully.",
    "status": "success"
  }
  ```
*Action: Cleans up the metadata Hash, participants Set, and message history List from Redis.*

---

## 🧪 Testing Guide

### 1. Automated Test Suite (29/29 Passing)
Run all unit and end-to-end integration tests:
```powershell
.\mvnw.cmd clean test
```
**Test Coverage Includes:**
- **`ChatRoomServiceImplTest`**: Unit tests verifying room creation, duplicate checks, joining, chronological message order, Pub/Sub emission, and room deletion.
- **`ChatRoomControllerTest`**: Controller layer tests validating HTTP status codes (201, 200, 400, 404, 409).
- **`GlobalExceptionHandlerTest`**: Centralized exception handling verification.
- **`ChatAppIntegrationTest`**: Live end-to-end tests against Redis/Memurai verifying all 5 document test cases.

---

### 2. Live Real-Time Pub/Sub Test (Test Case 3)

You can demonstrate real-time Pub/Sub live in seconds:

1. **Terminal 1** — Subscribe to the room's Pub/Sub channel:
   ```powershell
   memurai-cli.exe SUBSCRIBE chatroom:general
   # On Linux/Docker: redis-cli SUBSCRIBE chatroom:general
   ```
2. **Terminal 2** — Send a message via curl, Postman, or Swagger UI:
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:8080/api/chatapp/chatrooms/general/messages" -Method Post -ContentType "application/json" -Body '{"participant":"guest_user","message":"Live PubSub Message!"}'
   ```
3. **Observation:**
   - Terminal 1 instantly receives and displays the JSON payload.
   - Application console logs:
     `Real-time message received on channel 'chatroom:general': {"participant":"guest_user", ...}`

---

### 3. Testing with Postman
1. Open Postman.
2. Click **Import** $\rightarrow$ select `postman_collection.json`.
3. Open the imported **`Redis Chat App API`** collection and execute the requests sequentially.

---

## 💾 Redis Persistence (Requirement 5)

By default, Redis stores data in RAM. Requirement 5 specifies using persistence (RDB snapshots or Append-Only File) to guarantee **durability** so data survives server restarts.

### Configuration
Configure persistence in `redis.conf` (or `memurai.conf` on Windows):
- **RDB Snapshots** (point-in-time binary dumps):
  ```text
  save 60 1
  dbfilename dump.rdb
  dir ./
  ```
- **AOF (Append Only File)** (logs every write operation):
  ```text
  appendonly yes
  appendfsync everysec
  ```

### How to Test Persistence (Step-by-Step Verification)

You can prove data durability across database restarts in 4 simple steps:

#### Step 1: Create a room and send a message
```powershell
# Create test room
Invoke-RestMethod -Uri "http://localhost:8080/api/chatapp/chatrooms" -Method Post -ContentType "application/json" -Body '{"roomName":"persistence_test"}'

# Send a message to the room
Invoke-RestMethod -Uri "http://localhost:8080/api/chatapp/chatrooms/persistence_test/messages" -Method Post -ContentType "application/json" -Body '{"participant":"surya","message":"This message should survive a restart!"}'
```

#### Step 2: Trigger a manual snapshot to disk
```powershell
# On Windows with Memurai:
memurai-cli.exe BGSAVE

# On Linux/macOS/Docker:
redis-cli bgsave
```
*Output: `Background saving started` (persists all current memory keys to `dump.rdb` on disk).*

#### Step 3: Restart the Redis / Memurai Service
Simulate an unexpected server crash or reboot:
```powershell
# On Windows:
Restart-Service memurai

# On Linux:
sudo systemctl restart redis

# On Docker:
docker restart redis
```
Verify the service is back up:
```powershell
memurai-cli.exe PING
# Expected: PONG
```

#### Step 4: Verify the data survived after restart
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/chatapp/chatrooms/persistence_test/messages?limit=10"
```
**Expected Response:**
```json
{
  "messages": [
    {
      "participant": "surya",
      "message": "This message should survive a restart!",
      "timestamp": "..."
    }
  ]
}
```
*Result: The chat room metadata and messages were completely preserved and reloaded from disk into memory, verifying Requirement 5.*

