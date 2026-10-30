package com.orgchat;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RoomDAO {

  // 1. Create a new room with a securely hashed secret code
  public static boolean createRoom(String roomName, String secretCode) {
    if (roomName == null || secretCode == null || roomName.trim().isEmpty() || secretCode.trim().isEmpty()) {
      return false;
    }

    String insertSQL = "INSERT INTO Rooms (room_name, secret_code_hash) VALUES (?, ?)";
    String hashedCode = BCrypt.hashpw(secretCode, BCrypt.gensalt());

    try (Connection conn = DatabaseManager.getConnection()) {
      if (conn == null) {
        System.err.println("Cannot create room: Database connection unavailable.");
        return false;
      }
      try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
        pstmt.setString(1, roomName.trim());
        pstmt.setString(2, hashedCode);
        pstmt.executeUpdate();
        System.out.println("Room created successfully: " + roomName);
        return true;
      }
    } catch (SQLException e) {
      System.err.println("Failed to create room. The room name might already exist: " + roomName);
      return false;
    }
  }

  // 2. Authenticate a user into a room and return the room's internal database ID
  public static int authenticateRoom(String roomName, String secretCode) {
    if (roomName == null || secretCode == null || roomName.trim().isEmpty() || secretCode.trim().isEmpty()) {
      return -1;
    }

    String selectSQL = "SELECT id, secret_code_hash FROM Rooms WHERE room_name = ?";

    try (Connection conn = DatabaseManager.getConnection()) {
      if (conn == null) {
        System.err.println("Cannot authenticate room: Database connection unavailable.");
        return -1;
      }
      try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
        pstmt.setString(1, roomName.trim());
        try (ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            String storedHash = rs.getString("secret_code_hash");
            if (BCrypt.checkpw(secretCode, storedHash)) {
              return rs.getInt("id"); // Return primary key of the room
            }
          }
        }
      }
    } catch (SQLException e) {
      System.err.println("Database error during room authentication.");
      e.printStackTrace();
    }
    return -1; // Return -1 if authentication fails
  }

  // 3. Log a user joining a room into the audit trail (Room_Members table)
  public static void logUserJoin(int roomId, String username) {
    if (username == null || username.trim().isEmpty()) {
      return;
    }

    String insertSQL = "INSERT OR IGNORE INTO Room_Members (room_id, user_id) " +
        "SELECT ?, id FROM Users WHERE username = ?";

    try (Connection conn = DatabaseManager.getConnection()) {
      if (conn == null) {
        return;
      }
      try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
        pstmt.setInt(1, roomId);
        pstmt.setString(2, username.trim());
        pstmt.executeUpdate();
      }
    } catch (SQLException e) {
      System.err.println("Failed to log room join for user: " + username);
    }
  }

  // 4. Update the room's activity timestamp whenever a message is sent
  public static void updateLastActive(int roomId) {
    String updateSQL = "UPDATE Rooms SET last_active = CURRENT_TIMESTAMP WHERE id = ?";
    try (Connection conn = DatabaseManager.getConnection()) {
      if (conn == null) {
        return;
      }
      try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
        pstmt.setInt(1, roomId);
        pstmt.executeUpdate();
      }
    } catch (SQLException e) {
      System.err.println("Failed to update last_active timestamp for room ID: " + roomId);
    }
  }
}