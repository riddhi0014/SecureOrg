package com.orgchat;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
  public static final int DEFAULT_PORT = 8443;
  private static final ExecutorService pool = Executors.newCachedThreadPool();

  public static void main(String[] args) {
    int port = DEFAULT_PORT;
    if (args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        System.err.println("Invalid port provided, using default " + DEFAULT_PORT);
      }
    }

    System.out.println("Secure Org Chat Server (TLS Encrypted) is booting up...");

    // Initialize the relational database schema before accepting connections
    DatabaseManager.initializeDB();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutting down thread pool...");
      pool.shutdown();
    }));

    SSLServerSocket serverSocket = null;
    try {
      serverSocket = createSSLServerSocket(port);
      System.out.println("Server is actively listening for encrypted connections on port " + port);

      while (!serverSocket.isClosed()) {
        SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
        System.out.println("New secure TLS connection established with client: " + clientSocket.getRemoteSocketAddress());

        ClientHandler clientThread = new ClientHandler(clientSocket);
        pool.execute(clientThread);
      }
    } catch (Exception e) {
      if (serverSocket != null && !serverSocket.isClosed()) {
        System.err.println("Server encountered an error: " + e.getMessage());
        e.printStackTrace();
      } else if (serverSocket == null) {
        System.err.println("Could not bind SSL server to port " + port + ": " + e.getMessage());
        e.printStackTrace();
      }
    } finally {
      pool.shutdown();
      try {
        if (serverSocket != null && !serverSocket.isClosed()) {
          serverSocket.close();
        }
      } catch (IOException ignored) {}
    }
  }

  private static SSLServerSocket createSSLServerSocket(int port) throws Exception {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    InputStream stream = ChatServer.class.getResourceAsStream("/keystore.p12");
    if (stream == null && new File("keystore.p12").exists()) {
      stream = new FileInputStream("keystore.p12");
    }
    if (stream == null) {
      throw new IOException("keystore.p12 not found on classpath or in working directory!");
    }
    try (InputStream is = stream) {
      keyStore.load(is, "password123".toCharArray());
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, "password123".toCharArray());

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

    SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
    return (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);
  }
}

// Instead of constantly creating and destroying threads (which eats up computer
// memory), the thread pool recycles them. If 50 employees connect, all 50 slots
// are filled. If a 51st tries to connect, they must wait in a queue until
// someone logs off.

// serverSocket.accept() is a blocking method. When Java reads this line, the
// entire main thread freezes and goes to sleep. It will sit there for days or
// weeks doing absolutely nothing. The exact millisecond a user opens their Java
// Swing app and connects to your IP address, the method "wakes up", completes
// the SSL handshake, and spits out a new SSLSocket specific to that user.

// The Flow:

/*
 * We take the new user's clientSocket and pass it into a new instance of
 * ClientHandler.
 * 
 * We hand that ClientHandler to the pool.execute().
 * 
 * The Thread Pool assigns an empty background thread to handle Client A's login
 * and messaging.
 * 
 * The main server thread instantly hits the bottom of the loop, jumps back up
 * to while(true), and hits .accept() again, going back to sleep until the next
 * user connects.
 */
