package com.orgchat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class ChatFrame extends JFrame {
  private JTextArea chatArea;
  private JTextField inputField;
  private DefaultListModel<String> onlineUsersModel;
  private JList<String> onlineUsersList;
  private PrintWriter out;
  private BufferedReader in;
  private String username;

  // Modern UI Color Palette
  private final Color BG_DARK = new Color(43, 45, 49);
  private final Color BG_LIGHTER = new Color(49, 51, 56);
  private final Color TEXT_COLOR = new Color(219, 222, 225);
  private final Color ACCENT_COLOR = new Color(88, 101, 242);
  private final Font MAIN_FONT = new Font("SansSerif", Font.PLAIN, 14);

  public ChatFrame(ChatClient client, String username) {
    this.username = username;
    this.out = client.getOut();
    this.in = client.getIn();

    setTitle("CivicConnect Secure Network - " + username);
    setSize(800, 500);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
    setLayout(new BorderLayout());
    getContentPane().setBackground(BG_DARK);

    // --- Left Panel: Chat History ---
    chatArea = new JTextArea();
    chatArea.setEditable(false);
    chatArea.setLineWrap(true);
    chatArea.setWrapStyleWord(true);
    chatArea.setBackground(BG_LIGHTER);
    chatArea.setForeground(TEXT_COLOR);
    chatArea.setFont(MAIN_FONT);
    chatArea.setBorder(new EmptyBorder(10, 10, 10, 10)); // Internal padding

    JScrollPane chatScroll = new JScrollPane(chatArea);
    chatScroll.setBorder(BorderFactory.createLineBorder(BG_DARK, 2));

    // --- Right Panel: Online Users List ---
    onlineUsersModel = new DefaultListModel<>();
    onlineUsersList = new JList<>(onlineUsersModel);
    onlineUsersList.setBackground(BG_DARK);
    onlineUsersList.setForeground(new Color(46, 204, 113)); // Green text for online status
    onlineUsersList.setFont(new Font("SansSerif", Font.BOLD, 13));
    onlineUsersList.setBorder(new EmptyBorder(10, 10, 10, 10));

    JScrollPane userScroll = new JScrollPane(onlineUsersList);
    userScroll.setPreferredSize(new Dimension(180, 0));
    userScroll.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(BG_DARK),
        "Online Users",
        0, 0, MAIN_FONT, TEXT_COLOR));
    userScroll.setBackground(BG_DARK);

    // --- Split Pane Configuration ---
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScroll, userScroll);
    splitPane.setResizeWeight(1.0); // Gives all extra resize space to the chat area
    splitPane.setDividerSize(0); // Hides the clunky default divider line
    add(splitPane, BorderLayout.CENTER);

    // --- Bottom Panel: Input Area ---
    JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
    bottomPanel.setBackground(BG_DARK);
    bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

    inputField = new JTextField();
    inputField.setBackground(BG_LIGHTER);
    inputField.setForeground(TEXT_COLOR);
    inputField.setCaretColor(Color.WHITE);
    inputField.setFont(MAIN_FONT);
    inputField.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(BG_LIGHTER, 5),
        new EmptyBorder(5, 5, 5, 5)));

    JButton sendButton = new JButton("Send");
    sendButton.setBackground(ACCENT_COLOR);
    sendButton.setForeground(Color.WHITE);
    sendButton.setFont(new Font("SansSerif", Font.BOLD, 14));
    sendButton.setFocusPainted(false); // Removes the ugly dotted focus box
    sendButton.setBorder(new EmptyBorder(8, 20, 8, 20));

    bottomPanel.add(inputField, BorderLayout.CENTER);
    bottomPanel.add(sendButton, BorderLayout.EAST);
    add(bottomPanel, BorderLayout.SOUTH);

    // --- Event Listeners ---
    sendButton.addActionListener(e -> sendMessage());
    inputField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          sendMessage();
        }
      }
    });

    startListening();
  }

  private void sendMessage() {
    String message = inputField.getText().trim();
    if (!message.isEmpty()) {
      out.println(message);
      inputField.setText("");
    }
  }

  private void startListening() {
    Thread listenerThread = new Thread(() -> {
      try {
        String incomingMessage;
        while ((incomingMessage = in.readLine()) != null) {
          final String text = incomingMessage;

          SwingUtilities.invokeLater(() -> {
            // Protocol Routing: Intercept system commands vs normal messages
            if (text.startsWith("UPDATE_USERS||")) {
              updateUserList(text.substring(14)); // Strip the protocol prefix
            } else {
              chatArea.append(text + "\n");
            }
          });
        }
      } catch (IOException e) {
        SwingUtilities.invokeLater(() -> chatArea.append("Connection to server lost.\n"));
      }
    });
    listenerThread.start();
  }

  private void updateUserList(String csvUsers) {
    onlineUsersModel.clear();
    if (csvUsers.isEmpty())
      return;

    String[] users = csvUsers.split(",");
    for (String u : users) {
      onlineUsersModel.addElement("● " + u);
    }
  }
}