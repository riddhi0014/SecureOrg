package com.orgchat;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
  private Socket socket;
  private BufferedReader in;
  private PrintWriter out;

  public static void main(String[] args) {
    // 1. Prompt the user for the IP address before doing anything else
    String serverIP = JOptionPane.showInputDialog(
        null,
        "Enter Server IP Address (e.g., localhost or 192.168.1.5, optionally with port e.g. localhost:5050):",
        "CivicConnect - Network Setup",
        JOptionPane.QUESTION_MESSAGE);

    // 2. If the user clicks "Cancel" or closes the window, exit the program gracefully
    if (serverIP == null) {
      System.exit(0);
    }

    // 3. If they leave it blank, default to localhost
    if (serverIP.trim().isEmpty()) {
      serverIP = "localhost";
    }

    String serverAddress = "localhost";
    int port = 5050;
    if (serverIP.contains(":")) {
      String[] parts = serverIP.split(":", 2);
      serverAddress = parts[0].trim();
      try {
        port = Integer.parseInt(parts[1].trim());
      } catch (NumberFormatException ignored) {}
    } else {
      serverAddress = serverIP.trim();
    }

    // 4. Launch the client with the resolved host and port
    new ChatClient(serverAddress, port);
  }

  public ChatClient(String serverAddress, int port) {
    try {
      socket = new Socket(serverAddress, port);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out = new PrintWriter(socket.getOutputStream(), true);

      // Launch the Login UI
      SwingUtilities.invokeLater(() -> new LoginFrame(this).setVisible(true));

    } catch (IOException e) {
      JOptionPane.showMessageDialog(null,
          "Could not connect to server at " + serverAddress + ":" + port + "\nMake sure the server is running.",
          "Connection Error",
          JOptionPane.ERROR_MESSAGE);
      System.exit(0);
    }
  }

  private String lastAuthError = "Authentication failed or Server is offline.";

  public boolean connectAndAuthenticate(String action, String username, String password) {
    try {
      socket.setSoTimeout(5000); // 5 second timeout for auth handshake
      out.println(action + "||" + username + "||" + password);
      String response = in.readLine();
      socket.setSoTimeout(0); // reset to infinite timeout for chat listening
      if (response != null && (response.startsWith("AUTH_SUCCESS") || response.startsWith("SUCCESS"))) {
        return true;
      }
      if (response != null && response.startsWith("ERROR||")) {
        lastAuthError = response.substring(7);
      } else {
        lastAuthError = "Authentication failed.";
      }
      return false;
    } catch (java.net.SocketTimeoutException e) {
      lastAuthError = "Server response timed out. Please verify that port " + socket.getPort() + " is running the chat server.";
      e.printStackTrace();
      return false;
    } catch (IOException e) {
      lastAuthError = "Network error: Connection to server lost.";
      e.printStackTrace();
      return false;
    }
  }

  public String getLastAuthError() {
    return lastAuthError;
  }

  public void close() {
    new Thread(() -> {
      try {
        if (socket != null && !socket.isClosed()) {
          socket.close(); // Closing socket first unblocks in.readLine() immediately
        }
      } catch (IOException ignored) {}
      try {
        if (in != null) in.close();
      } catch (IOException ignored) {}
      if (out != null) out.close();
    }).start();
  }

  public BufferedReader getIn() {
    return in;
  }

  public PrintWriter getOut() {
    return out;
  }
}