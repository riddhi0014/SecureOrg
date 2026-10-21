package com.orgchat;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class ChatClient {
  // In a real environment, this would be your server's public IP address.
  private static final String SERVER_ADDRESS = "localhost";
  private static final int SERVER_PORT = 8443;

  private SSLSocket socket;
  private BufferedReader in;
  private PrintWriter out;

  public static void main(String[] args) {
    // Tell the client to trust our self-signed certificate
    System.setProperty("javax.net.ssl.trustStore", "keystore.p12");
    System.setProperty("javax.net.ssl.trustStorePassword", "password123");

    // Swing GUIs must be started on the Event Dispatch Thread to prevent freezing
    SwingUtilities.invokeLater(() -> {
      new LoginFrame(new ChatClient()).setVisible(true);
    });
  }

  public boolean connectAndAuthenticate(String action, String username, String password) {
    try {
      // Establish the secure connection to the server
      SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
      socket = (SSLSocket) factory.createSocket(SERVER_ADDRESS, SERVER_PORT);

      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out = new PrintWriter(socket.getOutputStream(), true);

      // Send the raw protocol string we defined in ClientHandler
      out.println(action + "||" + username + "||" + password);

      // Wait for the server's response
      String response = in.readLine();
      if (response != null && response.startsWith("SUCCESS")) {
        return true;
      }
    } catch (IOException e) {
      System.err.println("Could not connect to the server.");
    }
    return false;
  }

  // Getters so the main ChatFrame can read and write messages later
  public BufferedReader getIn() {
    return in;
  }

  public PrintWriter getOut() {
    return out;
  }
}