package com.orgchat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
  private static final String URL = "jdbc:sqlite:chat_history.db";
  private static Connection connection = null;

  private DatabaseManager() {
  }

  public static synchronized Connection getConnection() {
    if (connection == null) {
      try {
        connection = DriverManager.getConnection(URL);
        System.out.println("SQLite database connected successfully.");
        initializeDatabase();
      } catch (SQLException e) {
        System.err.println("Failed to connect to SQLite database.");
        e.printStackTrace();
      }
    }
    return connection;
  }

  private static void initializeDatabase() {
    String createUsersTable = "CREATE TABLE IF NOT EXISTS Users ("
        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "username TEXT UNIQUE NOT NULL, "
        + "password_hash TEXT NOT NULL"
        + ");";

    String createMessagesTable = "CREATE TABLE IF NOT EXISTS Messages ("
        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "sender_name TEXT NOT NULL, "
        + "message_content TEXT NOT NULL, "
        + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP"
        + ");";

    try (Statement stmt = connection.createStatement()) {
      stmt.execute(createUsersTable);
      stmt.execute(createMessagesTable);
      System.out.println("Database tables initialized.");
    } catch (SQLException e) {
      System.err.println("Failed to initialize tables.");
      e.printStackTrace();
    }
  }
}

// 1. synchronized keyword: Because your chat server is multi-threaded (multiple
// users chatting simultaneously), two threads might ask for a database
// connection at the exact same millisecond. synchronized forces threads to line
// up and execute this method one at a time, preventing a race condition where
// two connections get created.

// 2. private DatabaseManager() {}: This is a private constructor. It physically
// prevents any other developer or class from accidentally typing new
// DatabaseManager() and creating a duplicate instance.

// 3. Since the constructor is private, no user can create an instance of the
// DatabaseManager class. So, how are we accessing all these methods inside? Is
// it because these are static methods and we can just access them using their
// class name instead of creating objects?

/*
 * Yes, you are exactly right. Because the methods and variables are declared as
 * static, they belong to the class itself rather than to any individual object
 * instance.
 * 
 * By making getConnection() a static method, you can call it from anywhere in
 * your application simply by typing DatabaseManager.getConnection(), completely
 * bypassing the need to use the new keyword.
 * 
 * This combination—a private constructor plus a static method—is the standard
 * way to enforce the Singleton Pattern (or a static utility class) in Java. It
 * guarantees that the Java Virtual Machine only allocates memory for one single
 * shared database connection, rather than letting different parts of your app
 * accidentally spin up hundreds of separate connections by creating new
 * DatabaseManager objects.
 */
