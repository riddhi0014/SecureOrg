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
    // We use ? placeholders to prevent SQL Injection attacks
    String insertSQL = "INSERT INTO Users (username, password_hash) VALUES (?, ?)";

    // Generate a salt and hash the plain text password
    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

    // The try-with-resources block ensures the PreparedStatement closes
    // automatically
    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

      pstmt.setString(1, username);
      pstmt.setString(2, hashedPassword);
      pstmt.executeUpdate(); // Executes the INSERT

      System.out.println("User registered successfully: " + username);
      return true;

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
    String selectSQL = "SELECT password_hash FROM Users WHERE username = ?";

    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

      pstmt.setString(1, username);
      ResultSet rs = pstmt.executeQuery(); // Executes the SELECT

      // rs.next() moves the cursor to the first row. It returns false if no user was
      // found.
      if (rs.next()) {
        String storedHash = rs.getString("password_hash");

        // BCrypt checks if the plain text password mathematically matches the hash
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