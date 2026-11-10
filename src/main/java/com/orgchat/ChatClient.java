package com.orgchat;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.SecureRandom;

public class ChatClient {
  public static final int DEFAULT_PORT = 8443;
  private SSLSocket socket;
  private BufferedReader in;
  private PrintWriter out;

  public static void main(String[] args) {
    // 1. Prompt the user for the IP address before doing anything else
    String serverIP = JOptionPane.showInputDialog(
        null,
        "Enter Server IP Address (e.g., localhost or 192.168.1.5, optionally with port e.g. localhost:8443):",
        "CivicConnect - Network Setup (TLS Encrypted)",
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
    int port = DEFAULT_PORT;
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
      socket = createSSLSocket(serverAddress, port);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out = new PrintWriter(socket.getOutputStream(), true);

      // Launch the Login UI
      SwingUtilities.invokeLater(() -> new LoginFrame(this).setVisible(true));

    } catch (Exception e) {
      JOptionPane.showMessageDialog(null,
          "Could not establish secure TLS connection to server at " + serverAddress + ":" + port + "\nError: " + e.getMessage() + "\nMake sure the server is running with SSL.",
          "Secure Connection Error",
          JOptionPane.ERROR_MESSAGE);
      System.exit(0);
    }
  }

  private SSLSocket createSSLSocket(String host, int port) throws Exception {
    KeyStore trustStore = KeyStore.getInstance("PKCS12");
    InputStream stream = ChatClient.class.getResourceAsStream("/keystore.p12");
    if (stream == null && new File("keystore.p12").exists()) {
      stream = new FileInputStream("keystore.p12");
    }
    if (stream == null) {
      throw new IOException("keystore.p12 (TrustStore) not found on classpath or in working directory!");
    }
    try (InputStream is = stream) {
      trustStore.load(is, "password123".toCharArray());
    }

    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(trustStore);

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

    SSLSocketFactory factory = sslContext.getSocketFactory();
    SSLSocket sslSocket = (SSLSocket) factory.createSocket(host, port);
    sslSocket.startHandshake();
    return sslSocket;
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