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
        "Enter the Server IP Address (leave as localhost for same machine):",
        "CivicConnect - Network Setup",
        JOptionPane.QUESTION_MESSAGE);

    // 2. If the user clicks "Cancel" or closes the window, exit the program
    // gracefully
    if (serverIP == null) {
      System.exit(0);
    }

    // 3. If they leave it blank, default to localhost
    if (serverIP.trim().isEmpty()) {
      serverIP = "localhost";
    }

    // 4. Launch the client with the provided IP
    new ChatClient(serverIP, 5000);
  }

  public ChatClient(String serverAddress, int port) {
    try {
      // Use the dynamic IP address to establish the TCP connection
      socket = new Socket(serverAddress, port);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out = new PrintWriter(socket.getOutputStream(), true);

      // Launch the Login UI
      // NOTE: Change 'AuthFrame' to your actual login window class name if different
      SwingUtilities.invokeLater(() -> new LoginFrame(this).setVisible(true));

    } catch (IOException e) {
      JOptionPane.showMessageDialog(null,
          "Could not connect to server at " + serverAddress + "\nMake sure the server is running.",
          "Connection Error",
          JOptionPane.ERROR_MESSAGE);
      System.exit(0);
    }
  }

  public BufferedReader getIn() {
    return in;
  }

  public PrintWriter getOut() {
    return out;
  }
}