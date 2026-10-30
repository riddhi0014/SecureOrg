package com.orgchat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
  public static final int DEFAULT_PORT = 5050;
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

    System.out.println("Secure Org Chat Server is booting up...");

    // Initialize the relational database schema before accepting connections
    DatabaseManager.initializeDB();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutting down thread pool...");
      pool.shutdown();
    }));

    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(port);
      System.out.println("Server is actively listening for connections on port " + port);

      while (!serverSocket.isClosed()) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("New connection established with client: " + clientSocket.getRemoteSocketAddress());

        ClientHandler clientThread = new ClientHandler(clientSocket);
        pool.execute(clientThread);
      }
    } catch (IOException e) {
      if (serverSocket != null && !serverSocket.isClosed()) {
        System.err.println("Server encountered an error: " + e.getMessage());
        e.printStackTrace();
      } else if (serverSocket == null) {
        System.err.println("Could not bind to port " + port + ": " + e.getMessage());
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
