package com.orgchat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class ChatFrame extends JFrame {
  private JTextArea chatArea;
  private JTextField inputField;
  private PrintWriter out;
  private BufferedReader in;
  private String username;

  public ChatFrame(ChatClient client, String username) {
    this.username = username;
    this.out = client.getOut();
    this.in = client.getIn();

    // 1. Set up the main window using BorderLayout
    setTitle("Secure Org Chat - Logged in as: " + username);
    setSize(500, 600);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
    setLayout(new BorderLayout());

    // 2. The Center: Chat History
    chatArea = new JTextArea();
    chatArea.setEditable(false);
    chatArea.setLineWrap(true);
    chatArea.setWrapStyleWord(true);
    JScrollPane scrollPane = new JScrollPane(chatArea);
    add(scrollPane, BorderLayout.CENTER);

    // 3. The Bottom: Input Area
    JPanel bottomPanel = new JPanel(new BorderLayout());
    inputField = new JTextField();
    JButton sendButton = new JButton("Send");

    bottomPanel.add(inputField, BorderLayout.CENTER);
    bottomPanel.add(sendButton, BorderLayout.EAST);
    add(bottomPanel, BorderLayout.SOUTH);

    // 4. Attach Event Listeners (Clicking the button or pressing Enter)
    sendButton.addActionListener(e -> sendMessage());
    inputField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          sendMessage();
        }
      }
    });

    // 5. Start the background listening thread
    startListening();
  }

  private void sendMessage() {
    String message = inputField.getText().trim();
    if (!message.isEmpty()) {
      out.println(message); // Send over the raw TCP socket
      inputField.setText(""); // Clear the input box
    }
  }

  private void startListening() {
    // Spin up a dedicated thread just for waiting on incoming network data
    Thread listenerThread = new Thread(() -> {
      try {
        String incomingMessage;
        // This blocks until the server broadcasts a message
        while ((incomingMessage = in.readLine()) != null) {
          final String text = incomingMessage;

          // Safely update the UI from the background thread
          SwingUtilities.invokeLater(() -> chatArea.append(text + "\n"));
        }
      } catch (IOException e) {
        SwingUtilities.invokeLater(() -> chatArea.append("Connection to server lost.\n"));
      }
    });
    listenerThread.start();
  }
}