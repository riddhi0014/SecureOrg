package com.orgchat;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestDB {
  public static void main(String[] args) {
    // 1. Print the EXACT physical path where Java is saving/looking for this file
    File dbFile = new File("chat_history.db");
    System.out.println("Java is reading the database from: " + dbFile.getAbsolutePath());

    // 2. Query the database
    try (Connection conn = DatabaseManager.getConnection()) {
      if (conn == null) {
        System.err.println("Could not connect to database.");
        return;
      }
      System.out.println("\n--- USERS IN DATABASE ---");
      try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Users");
          ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          System.out.println("ID: " + rs.getInt("id") + " | Username: " + rs.getString("username"));
        }
      }

      System.out.println("\n--- MESSAGES IN DATABASE ---");
      try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Messages");
          ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          System.out.println(rs.getString("sender_name") + ": " + rs.getString("message_content"));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
