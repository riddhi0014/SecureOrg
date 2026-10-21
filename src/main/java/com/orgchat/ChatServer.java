package com.orgchat;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
  // Standard port for secure (SSL/HTTPS) traffic is usually in the 8443 or 443
  // range
  private static final int PORT = 8443;

  // A Thread Pool that can handle up to 50 concurrent employees chatting at once
  private static final ExecutorService pool = Executors.newFixedThreadPool(50);

  public static void main(String[] args) {
    // 1. Tell the Java Virtual Machine where to find our SSL certificate
    System.setProperty("javax.net.ssl.keyStore", "keystore.p12");
    System.setProperty("javax.net.ssl.keyStorePassword", "password123");

    System.out.println("Secure Chat Server is booting up...");

    try {
      // 2. Create the SSL Server Socket
      SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
      SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(PORT);

      System.out.println("Server is actively listening for encrypted connections on port " + PORT);

      // 3. The Infinite Loop
      while (true) {
        // .accept() pauses the loop here until a client actually connects
        SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
        System.out.println("New secure connection established with a client!");

        // 4. Hand the client off to a background worker thread
        ClientHandler clientThread = new ClientHandler(clientSocket);
        pool.execute(clientThread);
      }

    } catch (IOException e) {
      System.err.println("Server encountered an error: " + e.getMessage());
      e.printStackTrace();
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
