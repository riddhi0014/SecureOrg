package com.orgchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
  // ENTERPRISE ROUTING: Maps RoomID -> (Username -> Socket Output Stream)
  private static final ConcurrentHashMap<Integer, ConcurrentHashMap<String, PrintWriter>> roomSessions = new ConcurrentHashMap<>();
  // Tracks all actively logged-in users across the entire server to prevent duplicate logins
  private static final ConcurrentHashMap<String, ClientHandler> activeUsers = new ConcurrentHashMap<>();

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private String loggedInUser = null;
  private int currentRoomId = -1; // -1 indicates the user is authenticated but hasn't joined a room yet

  public ClientHandler(Socket socket) {
    this.socket = socket;
  }

  @Override
  public void run() {
    try {
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out = new PrintWriter(socket.getOutputStream(), true);

      // Step 1: User Identity Authentication
      if (!authenticateClient()) {
        socket.close();
        return;
      }

      // Step 2: The Command & Chat Routing Loop
      String incomingMessage;
      while ((incomingMessage = in.readLine()) != null) {
        if (incomingMessage.startsWith("CREATE_ROOM||")) {
          handleCreateRoom(incomingMessage);
        } else if (incomingMessage.startsWith("JOIN_ROOM||")) {
          handleJoinRoom(incomingMessage);
        } else if (incomingMessage.equals("LEAVE_ROOM")) {
          leaveCurrentRoom();
          out.println("SERVER: You have left the room.");
        } else if (currentRoomId != -1) {
          // Standard chat message routing
          logMessageToDB(loggedInUser, incomingMessage, currentRoomId);
          RoomDAO.updateLastActive(currentRoomId);
          broadcastToRoom("CHAT||" + loggedInUser + "||" + incomingMessage, currentRoomId);
        } else {
          out.println("SERVER: You must create or join a room first.");
        }
      }
    } catch (IOException e) {
      System.out.println("Connection closed for client: " + (loggedInUser != null ? loggedInUser : "unauthenticated"));
    } finally {
      cleanUp();
    }
  }

  private boolean authenticateClient() throws IOException {
    String authData = in.readLine();
    if (authData == null) {
      return false;
    }

    String[] parts = authData.split("\\|\\|");
    if (parts.length != 3) {
      out.println("ERROR||Invalid authentication format.");
      return false;
    }

    String action = parts[0].trim();
    String username = parts[1].trim();
    String password = parts[2];

    if (username.isEmpty() || password.isEmpty() || username.contains(",") || username.contains("||")) {
      out.println("ERROR||Username cannot be empty, or contain commas or '||'.");
      return false;
    }

    boolean success = false;
    if (action.equals("LOGIN")) {
      success = UserDAO.authenticateUser(username, password);
    } else if (action.equals("REGISTER")) {
      success = UserDAO.registerUser(username, password);
      if (success) {
        success = UserDAO.authenticateUser(username, password);
      }
    }

    if (success) {
      ClientHandler existing = activeUsers.putIfAbsent(username, this);
      if (existing != null) {
        out.println("ERROR||User '" + username + "' is already logged in from another session.");
        return false;
      }

      loggedInUser = username;
      out.println("AUTH_SUCCESS||Welcome " + username + "! Please join or create a room.");
      return true;
    } else {
      out.println("ERROR||Authentication failed. Invalid credentials or username taken.");
      return false;
    }
  }

  private void handleCreateRoom(String command) {
    String[] parts = command.split("\\|\\|");
    if (parts.length == 3) {
      String roomName = parts[1].trim();
      String secretCode = parts[2].trim();

      if (roomName.isEmpty() || secretCode.isEmpty()) {
        out.println("SERVER: Room name and secret code cannot be empty.");
        return;
      }

      if (RoomDAO.createRoom(roomName, secretCode)) {
        out.println("SERVER: Room '" + roomName + "' created successfully. You can now join it.");
      } else {
        out.println("SERVER: Failed to create room. Name might already be taken.");
      }
    } else {
      out.println("SERVER: Invalid CREATE_ROOM format.");
    }
  }

  private void handleJoinRoom(String command) {
    String[] parts = command.split("\\|\\|");
    if (parts.length == 3) {
      String roomName = parts[1].trim();
      String secretCode = parts[2].trim();

      if (roomName.isEmpty() || secretCode.isEmpty()) {
        out.println("SERVER: Room name and secret code cannot be empty.");
        return;
      }

      int roomId = RoomDAO.authenticateRoom(roomName, secretCode);
      if (roomId != -1) {
        leaveCurrentRoom();

        currentRoomId = roomId;
        RoomDAO.logUserJoin(roomId, loggedInUser);

        roomSessions.putIfAbsent(roomId, new ConcurrentHashMap<>());
        roomSessions.get(roomId).put(loggedInUser, out);

        out.println("ROOM_JOINED||" + roomName);
        broadcastToRoom("SERVER: " + loggedInUser + " has joined the room.", currentRoomId);
        broadcastUserList(currentRoomId);
      } else {
        out.println("SERVER: Invalid room name or secret code.");
      }
    } else {
      out.println("SERVER: Invalid JOIN_ROOM format.");
    }
  }

  private void broadcastToRoom(String message, int roomId) {
    ConcurrentHashMap<String, PrintWriter> roomUsers = roomSessions.get(roomId);
    if (roomUsers != null) {
      for (PrintWriter writer : roomUsers.values()) {
        writer.println(message);
      }
    }
  }

  private void broadcastUserList(int roomId) {
    ConcurrentHashMap<String, PrintWriter> roomUsers = roomSessions.get(roomId);
    if (roomUsers != null) {
      String usersString = String.join(",", roomUsers.keySet());
      broadcastToRoom("UPDATE_USERS||" + usersString, roomId);
    }
  }

  private void logMessageToDB(String sender, String content, int roomId) {
    String sql = "INSERT INTO Messages (room_id, sender_name, message_content) VALUES (?, ?, ?)";
    try (Connection conn = DatabaseManager.getConnection()) {
      if (conn == null) {
        System.err.println("Cannot log message: Database connection unavailable.");
        return;
      }
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, roomId);
        pstmt.setString(2, sender);
        pstmt.setString(3, content);
        pstmt.executeUpdate();
      }
    } catch (SQLException e) {
      System.err.println("Failed to log message to database: " + e.getMessage());
    }
  }

  private void leaveCurrentRoom() {
    if (currentRoomId != -1) {
      ConcurrentHashMap<String, PrintWriter> roomUsers = roomSessions.get(currentRoomId);
      if (roomUsers != null) {
        roomUsers.remove(loggedInUser);
        broadcastToRoom("SERVER: " + loggedInUser + " has left the room.", currentRoomId);
        broadcastUserList(currentRoomId);

        if (roomUsers.isEmpty()) {
          roomSessions.remove(currentRoomId);
        }
      }
      currentRoomId = -1;
    }
  }

  private void cleanUp() {
    leaveCurrentRoom();
    if (loggedInUser != null) {
      activeUsers.remove(loggedInUser, this);
    }
    try {
      if (in != null) in.close();
      if (out != null) out.close();
      if (socket != null && !socket.isClosed()) socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
// 1. private static final ConcurrentHashMap<String, PrintWriter> activeUsers =
// new ConcurrentHashMap<>();
// What it does: Because it has the keyword static, this map does not belong to
// just one ClientHandler; it is a single, shared directory used by all active
// worker threads. It maps a user's name (e.g., "Riddhi") to their PrintWriter
// (their direct network output pipe).
/*
 * Why it matters: When John wants to send a message to the group, John's thread
 * will look at this exact map to find the network pipes for every other online
 * user. ConcurrentHashMap ensures that if two people join or leave at the exact
 * same millisecond, the application won't crash from modifying the map
 * simultaneously.
 */

// 2. The Entry Point (run method)
/*
 * Who initiates it: The ExecutorService thread pool inside your ChatServer.
 * When pool.execute() is called, Java automatically triggers this run() method
 * on a brand-new background thread.
 * 
 * What it does: It opens a two-way radio. The BufferedReader reads incoming
 * text from the client, and the PrintWriter pushes text out to the client. The
 * true parameter in PrintWriter means "auto-flush"—it forces the text over the
 * network instantly without waiting for a buffer to fill up.
 */

// 3. The Infinite Listening Loop (The Core Engine)

/*
 * Who initiates it: The user sitting at their computer.
 * 
 * The Flow:
 * 
 * in.readLine() is a blocking method. Just like .accept() in the server, this
 * thread will freeze on this line and go to sleep.
 * 
 * The exact millisecond the user clicks "Send" on their Java Swing GUI, the
 * text travels over the Wi-Fi.
 * 
 * readLine() wakes up, reads the text, and stores it in incomingMessage.
 * 
 * It instantly jumps to logMessageToDB() (which asks DatabaseManager for a
 * connection and saves the text to SQLite).
 * 
 * Then it calls broadcastMessage(), which loops through the activeUsers map and
 * shoves the text down the PrintWriter of every other person online.
 * 
 * It hits the bottom of the loop and immediately goes back to sleep on
 * readLine().
 */