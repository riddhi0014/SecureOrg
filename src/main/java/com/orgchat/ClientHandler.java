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
  // A thread-safe map shared across all instances to track online users and their
  // output streams
  private static final ConcurrentHashMap<String, PrintWriter> activeUsers = new ConcurrentHashMap<>();

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private String loggedInUser = null;

  public ClientHandler(Socket socket) {
    this.socket = socket;
  }

  @Override
  public void run() {
    try {
      // Setup input (reading from client) and output (sending to client) streams
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out = new PrintWriter(socket.getOutputStream(), true);

      // Step 1: Force the client to authenticate before allowing them into the chat
      if (!authenticateClient()) {
        socket.close();
        return;
      }

      // Step 2: The infinite loop listening for incoming chat messages
      String incomingMessage;
      while ((incomingMessage = in.readLine()) != null) {
        System.out.println("Received from " + loggedInUser + ": " + incomingMessage);

        // Save to SQLite for auditing
        logMessageToDB(loggedInUser, incomingMessage);

        // Broadcast to everyone else
        broadcastMessage(loggedInUser + ": " + incomingMessage);
      }

    } catch (IOException e) {
      System.out.println("Connection lost with client.");
    } finally {
      cleanUp();
    }
  }

  private boolean authenticateClient() throws IOException {
    // We expect a strictly formatted string from the client:
    // ACTION||username||password
    String authData = in.readLine();
    if (authData == null)
      return false;

    String[] parts = authData.split("\\|\\|");
    if (parts.length != 3) {
      out.println("ERROR||Invalid authentication format.");
      return false;
    }

    String action = parts[0];
    String username = parts[1];
    String password = parts[2];

    boolean success = false;
    if (action.equals("LOGIN")) {
      success = UserDAO.authenticateUser(username, password);
    } else if (action.equals("REGISTER")) {
      success = UserDAO.registerUser(username, password);
      if (success) {
        success = UserDAO.authenticateUser(username, password); // Auto-login after register
      }
    }

    if (success) {
      loggedInUser = username;
      activeUsers.put(loggedInUser, out); // Add to the global online directory
      out.println("SUCCESS||Welcome " + username + "!");
      broadcastMessage("SERVER: " + username + " has joined the chat.");

      broadcastUserList(); // Update everyone with the new user list
      return true;
    } else {
      out.println("ERROR||Authentication failed.");
      return false;
    }
  }

  private void broadcastMessage(String message) {
    // Loop through every connected user's output stream and send the message
    for (PrintWriter writer : activeUsers.values()) {
      writer.println(message);
    }
  }

  private void broadcastUserList() {
    String usersString = String.join(",", activeUsers.keySet());
    broadcastMessage("UPDATE_USERS||" + usersString);
  }

  private void logMessageToDB(String sender, String content) {
    String sql = "INSERT INTO Messages (sender_name, message_content) VALUES (?, ?)";
    Connection conn = DatabaseManager.getConnection();
    try (
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, sender);
      pstmt.setString(2, content);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      System.err.println("Failed to log message to database.");
    }
  }

  private void cleanUp() {
    if (loggedInUser != null) {
      activeUsers.remove(loggedInUser); // Remove from directory when they disconnect
      broadcastMessage("SERVER: " + loggedInUser + " has left the chat.");
      broadcastUserList(); // Update everyone with the new user list
    }
    try {
      if (in != null)
        in.close();
      if (out != null)
        out.close();
      if (socket != null)
        socket.close();
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