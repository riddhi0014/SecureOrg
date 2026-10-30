package com.orgchat;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RoomDAO {

  // 1. Create a new room with a securely hashed secret code
  public static boolean createRoom(String roomName, String secretCode) {
    String insertSQL = "INSERT INTO Rooms (room_name, secret_code_hash) VALUES (?, ?)";
    String hashedCode = BCrypt.hashpw(secretCode, BCrypt.gensalt());
    Connection conn = DatabaseManager.getConnection();

    try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
      pstmt.setString(1, roomName);
      pstmt.setString(2, hashedCode);
      pstmt.executeUpdate();
      System.out.println("Room created successfully: " + roomName);
      return true;
    } catch (SQLException e) {
      System.err.println("Failed to create room. The room name might already exist: " + roomName);
      return false;
    }
  }

  // 2. Authenticate a user into a room and return the room's internal database ID
  public static int authenticateRoom(String roomName, String secretCode) {
    String selectSQL = "SELECT id, secret_code_hash FROM Rooms WHERE room_name = ?";
    Connection conn = DatabaseManager.getConnection();

    try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
      pstmt.setString(1, roomName);
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        String storedHash = rs.getString("secret_code_hash");
        // Mathematically verify the provided code against the stored hash
        if (BCrypt.checkpw(secretCode, storedHash)) {
          return rs.getInt("id"); // Return the primary key of the room
        }
      }
    } catch (SQLException e) {
      System.err.println("Database error during room authentication.");
    }
    return -1; // Return -1 if authentication fails
  }

  // 3. Log a user joining a room into the audit trail (Room_Members table)
  public static void logUserJoin(int roomId, String username) {
    // We use a subquery to automatically fetch the user's ID based on their
    // username.
    // INSERT OR IGNORE prevents a crash if the user joins the same room twice.
    String insertSQL = "INSERT OR IGNORE INTO Room_Members (room_id, user_id) " +
        "SELECT ?, id FROM Users WHERE username = ?";
    Connection conn = DatabaseManager.getConnection();

    try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
      pstmt.setInt(1, roomId);
      pstmt.setString(2, username);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      System.err.println("Failed to log room join for user: " + username);
    }
  }

  // 4. Update the room's activity timestamp whenever a message is sent
  public static void updateLastActive(int roomId) {
    String updateSQL = "UPDATE Rooms SET last_active = CURRENT_TIMESTAMP WHERE id = ?";
    Connection conn = DatabaseManager.getConnection();
    try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
      pstmt.setInt(1, roomId);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      System.err.println("Failed to update last_active timestamp for room ID: " + roomId);
    }
  }
}