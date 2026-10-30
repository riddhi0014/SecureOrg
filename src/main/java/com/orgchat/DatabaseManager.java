package com.orgchat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
  private static final String URL = "jdbc:sqlite:chat_history.db";
  private static Connection connection = null;

  public static Connection getConnection() {
    try {
      if (connection == null || connection.isClosed()) {
        connection = DriverManager.getConnection(URL);
        System.out.println("SQLite database connected successfully.");

        // CRITICAL SECURITY ENFORCEMENT:
        // SQLite ignores foreign key constraints by default. We must force it ON.
        try (Statement stmt = connection.createStatement()) {
          stmt.execute("PRAGMA foreign_keys = ON;");
        }

        initializeDB();
      }
    } catch (SQLException e) {
      System.err.println("Database connection failed.");
      e.printStackTrace();
    }
    return connection;
  }

  private static void initializeDB() {
    try (Statement stmt = connection.createStatement()) {

      // Create Users Table
      String createUsers = "CREATE TABLE IF NOT EXISTS Users ("
          + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
          + "username TEXT UNIQUE NOT NULL, "
          + "password_hash TEXT NOT NULL)";
      stmt.execute(createUsers);

      // Create Rooms Table
      String createRooms = "CREATE TABLE IF NOT EXISTS Rooms ("
          + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
          + "room_name TEXT UNIQUE NOT NULL, "
          + "secret_code_hash TEXT NOT NULL, "
          + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, "
          + "last_active DATETIME DEFAULT CURRENT_TIMESTAMP)";
      stmt.execute(createRooms);

      // Create Room_Members Table (The Audit Trail)
      String createRoomMembers = "CREATE TABLE IF NOT EXISTS Room_Members ("
          + "room_id INTEGER NOT NULL, "
          + "user_id INTEGER NOT NULL, "
          + "joined_at DATETIME DEFAULT CURRENT_TIMESTAMP, "
          + "PRIMARY KEY (room_id, user_id), "
          + "FOREIGN KEY (room_id) REFERENCES Rooms(id) ON DELETE CASCADE, "
          + "FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE)";
      stmt.execute(createRoomMembers);

      // Create Messages Table (Now isolated by room)
      String createMessages = "CREATE TABLE IF NOT EXISTS Messages ("
          + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
          + "room_id INTEGER NOT NULL, "
          + "sender_name TEXT NOT NULL, "
          + "message_content TEXT NOT NULL, "
          + "sent_at DATETIME DEFAULT CURRENT_TIMESTAMP, "
          + "FOREIGN KEY (room_id) REFERENCES Rooms(id) ON DELETE CASCADE)";
      stmt.execute(createMessages);

      System.out.println("Relational database schema initialized successfully.");
    } catch (SQLException e) {
      System.err.println("Failed to initialize database tables.");
      e.printStackTrace();
    }
  }
}