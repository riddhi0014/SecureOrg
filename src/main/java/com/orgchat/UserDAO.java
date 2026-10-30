package com.orgchat;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

  /**
   * Registers a new user by hashing their password and storing it in SQLite.
   * Returns true if successful, false if the username already exists.
   */
  public static boolean registerUser(String username, String password) {
    if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
      return false;
    }

    String insertSQL = "INSERT INTO Users (username, password_hash) VALUES (?, ?)";
    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

    try (Connection conn = DatabaseManager.getConnection()) {
      if (conn == null) {
        System.err.println("Cannot register user: Database connection is unavailable.");
        return false;
      }

      try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
        pstmt.setString(1, username.trim());
        pstmt.setString(2, hashedPassword);
        pstmt.executeUpdate();

        System.out.println("User registered successfully: " + username);
        return true;
      }
    } catch (SQLException e) {
      System.err.println("Registration failed. Username might already exist: " + username);
      return false;
    }
  }

  /**
   * Authenticates a user by comparing the provided password with the stored hash.
   * Returns true if the password matches, false otherwise.
   */
  public static boolean authenticateUser(String username, String password) {
    if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
      return false;
    }

    String selectSQL = "SELECT password_hash FROM Users WHERE username = ?";

    try (Connection conn = DatabaseManager.getConnection()) {
      if (conn == null) {
        System.err.println("Cannot authenticate user: Database connection is unavailable.");
        return false;
      }

      try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
        pstmt.setString(1, username.trim());
        try (ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            String storedHash = rs.getString("password_hash");

            if (BCrypt.checkpw(password, storedHash)) {
              System.out.println("Authentication successful for user: " + username);
              return true;
            } else {
              System.out.println("Invalid password for user: " + username);
              return false;
            }
          } else {
            System.out.println("User not found: " + username);
            return false;
          }
        }
      }
    } catch (SQLException e) {
      System.err.println("Database error during authentication.");
      e.printStackTrace();
      return false;
    }
  }
}

// This class acts as your Data Access Object (DAO). It isolates all
// user-related database logic into one place.

// 1. Setting up the Query: It starts by defining the SQL instruction: INSERT
// INTO Users (username, password_hash) VALUES (?, ?). The ? symbols are
// placeholders. We never paste user input directly into a SQL string to prevent
// a hacking technique called SQL Injection.