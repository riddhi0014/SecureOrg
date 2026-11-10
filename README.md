# SecureOrg Chat

A robust, multithreaded client-server chat application built in Java. This project demonstrates enterprise-grade network programming, relational database management, and cryptographic security by offering isolated, password-protected chat rooms and a real-time administrative audit dashboard.

## 🚀 Features

- **Multithreaded TCP Server**: Handles multiple concurrent client connections without bottlenecking, utilizing nested `ConcurrentHashMap` routing to physically isolate network traffic between different chat rooms.
- **Cryptographic Security**: Employs `jBCrypt` to salt and hash all user passwords and room secret codes. Plaintext credentials are never stored in the database.
- **Relational Database Backend**: Powered by SQLite with strict foreign key constraints (`PRAGMA foreign_keys = ON;`) and cascading deletes to prevent orphaned data.
- **Isolated Chat Rooms**: Users can create and join secure rooms. Network broadcasts are strictly limited to the current members of that specific room.
- **Administrative Audit Dashboard**: A separate Java Swing portal for administrators that uses complex SQL `JOIN` queries to display real-time active rooms, a membership audit trail (who joined what, and when), and global chat logs.
- **LAN-Ready Executable**: The client application can be exported as a standalone `.jar` file with a dynamic IP prompt, allowing cross-device communication over a Local Area Network (LAN) without sharing source code.

## 🛠️ Tech Stack

- **Language**: Java (JDK 11+)
- **GUI Framework**: Java Swing
- **Networking**: `javax.net.ssl.SSLSocket` / TCP/IP with TLS Encryption
- **Database**: SQLite via JDBC
- **Security**: TLS 1.3 / PKCS12 Keystore & jBCrypt
- **Version Control**: Git / GitHub

## 📂 Project Structure

- `ChatServer.java`: The multithreaded TLS server daemon that listens on port 8443 and dispatches `ClientHandler` threads.
- `ClientHandler.java`: Manages the state machine for individual user sockets and routes messages based on `room_id`.
- `ChatClient.java`: The entry point for the standalone desktop app, handling network connections and dynamic IP resolution.
- `ChatFrame.java` / `LoginFrame.java`: Modern, dark-themed UI components for room selection, authentication, and messaging.
- `DatabaseManager.java`: Initializes the relational schema and manages JDBC connections.
- `UserDAO.java` / `RoomDAO.java`: Data Access Objects abstracting SQL queries and BCrypt verification.
- `AdminDashboard.java`: The administrative portal for viewing SQL `JOIN` generated audit logs.

## 💻 How to Run

### 1. Start the Server

The server must be running before any clients can connect. Run this from your IDE or terminal:

```bash
java -cp target/classes:path/to/sqlite.jar:path/to/jbcrypt.jar com.orgchat.ChatServer
```

_Note: The first time the server runs, it will automatically generate `chat_history.db` and initialize the relational tables._

### 2. Connect a Client (Local or LAN)

Run the client application:

```bash
java -cp target/classes com.orgchat.ChatClient
```

- **Local Testing**: Leave the IP address prompt blank to default to `localhost`.
- **LAN Testing**: Enter the IPv4 address of the machine hosting the `ChatServer`.

### 3. Open the Admin Dashboard

To view database audits and live logs:

```bash
java -cp target/classes:path/to/sqlite.jar com.orgchat.AdminDashboard
```

## 📦 Building the Standalone Client JAR

To distribute the client to other users on your network without exposing backend logic:

1. Package `ChatClient`, `ChatFrame`, and `LoginFrame` into an executable JAR.
2. Exclude all DAO, Database, and Server classes.
3. Exclude SQLite and jBCrypt dependencies (the client only needs native Java libraries).
4. Distribute the resulting `SecureOrg-client.jar`.

## 🛡️ Database Schema Overview

- `Users`: `id`, `username`, `password_hash`
- `Rooms`: `id`, `room_name`, `secret_code_hash`, `created_at`, `last_active`
- `Room_Members`: `room_id`, `user_id`, `joined_at` (Composite Primary Key)
- `Messages`: `id`, `room_id`, `sender_name`, `message_content`, `sent_at`
