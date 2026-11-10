package com.orgchat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class ChatFrame extends JFrame {
  private JTextArea chatArea;
  private JTextField inputField;
  private DefaultListModel<String> onlineUsersModel;
  private JList<String> onlineUsersList;
  private JLabel roomHeaderLabel;
  private PrintWriter out;
  private BufferedReader in;
  private String username;
  private boolean inChatRoom = false;

  // UI Panels for state switching
  private JPanel mainContainer;
  private JPanel roomSelectionPanel;
  private JPanel chatInterfacePanel;

  // Modern UI Color Palette
  private final Color BG_DARK = new Color(43, 45, 49);
  private final Color BG_LIGHTER = new Color(49, 51, 56);
  private final Color TEXT_COLOR = new Color(219, 222, 225);
  private final Color ACCENT_COLOR = new Color(88, 101, 242);
  private final Color DANGER_COLOR = new Color(237, 66, 69);
  private final Font MAIN_FONT = new Font("SansSerif", Font.PLAIN, 14);

  public ChatFrame(ChatClient client, String username) {
    this.username = username;
    this.out = client.getOut();
    this.in = client.getIn();

    setTitle("CivicConnect Secure Network - " + username);
    setSize(850, 520);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        if (client != null) {
          client.close();
        }
        dispose();
        System.exit(0);
      }
    });

    mainContainer = new JPanel(new CardLayout());

    buildRoomSelectionPanel();
    buildChatPanel();

    mainContainer.add(roomSelectionPanel, "ROOM_SELECTION");
    mainContainer.add(chatInterfacePanel, "CHAT_INTERFACE");

    add(mainContainer);

    // Start listening to the server on a background thread
    startListening();
  }

  private void buildRoomSelectionPanel() {
    roomSelectionPanel = new JPanel(new GridBagLayout());
    roomSelectionPanel.setBackground(BG_DARK);

    JPanel innerPanel = new JPanel(new GridLayout(6, 1, 10, 10));
    innerPanel.setBackground(BG_DARK);
    innerPanel.setBorder(new EmptyBorder(20, 40, 20, 40));

    JLabel titleLabel = new JLabel("Join or Create a Secure Room", SwingConstants.CENTER);
    titleLabel.setForeground(Color.WHITE);
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

    JTextField roomNameField = new JTextField();
    JPasswordField roomCodeField = new JPasswordField();

    styleTextField(roomNameField, "Room Name");
    styleTextField(roomCodeField, "Secret Code");

    JButton joinBtn = createStyledButton("Join Room", ACCENT_COLOR);
    JButton createBtn = createStyledButton("Create New Room", ACCENT_COLOR);

    joinBtn.addActionListener(e -> {
      String name = roomNameField.getText().trim();
      String code = new String(roomCodeField.getPassword()).trim();
      if (!name.isEmpty() && !code.isEmpty()) {
        out.println("JOIN_ROOM||" + name + "||" + code);
      } else {
        JOptionPane.showMessageDialog(this, "Room Name and Secret Code cannot be empty.");
      }
    });

    createBtn.addActionListener(e -> {
      String name = roomNameField.getText().trim();
      String code = new String(roomCodeField.getPassword()).trim();
      if (!name.isEmpty() && !code.isEmpty()) {
        out.println("CREATE_ROOM||" + name + "||" + code);
      } else {
        JOptionPane.showMessageDialog(this, "Room Name and Secret Code cannot be empty.");
      }
    });

    JLabel roomNameLbl = new JLabel("Room Name:", SwingConstants.LEFT);
    roomNameLbl.setForeground(TEXT_COLOR);
    JLabel secretCodeLbl = new JLabel("Secret Code:", SwingConstants.LEFT);
    secretCodeLbl.setForeground(TEXT_COLOR);

    innerPanel.add(titleLabel);
    innerPanel.add(roomNameLbl);
    innerPanel.add(roomNameField);
    innerPanel.add(secretCodeLbl);
    innerPanel.add(roomCodeField);

    JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
    btnPanel.setBackground(BG_DARK);
    btnPanel.add(joinBtn);
    btnPanel.add(createBtn);
    innerPanel.add(btnPanel);

    roomSelectionPanel.add(innerPanel);
  }

  private void buildChatPanel() {
    chatInterfacePanel = new JPanel(new BorderLayout());
    chatInterfacePanel.setBackground(BG_DARK);

    // --- Top Header Bar ---
    JPanel topBar = new JPanel(new BorderLayout());
    topBar.setBackground(BG_DARK);
    topBar.setBorder(new EmptyBorder(8, 12, 8, 12));

    roomHeaderLabel = new JLabel("Secure Room", SwingConstants.LEFT);
    roomHeaderLabel.setForeground(Color.WHITE);
    roomHeaderLabel.setFont(new Font("SansSerif", Font.BOLD, 15));

    JButton leaveBtn = createStyledButton("Leave Room", DANGER_COLOR);
    leaveBtn.setBorder(new EmptyBorder(5, 14, 5, 14));
    leaveBtn.addActionListener(e -> leaveRoom());

    topBar.add(roomHeaderLabel, BorderLayout.WEST);
    topBar.add(leaveBtn, BorderLayout.EAST);

    // --- Center Split: Chat History (Left) and Room Roster (Right) ---
    chatArea = new JTextArea();
    chatArea.setEditable(false);
    chatArea.setLineWrap(true);
    chatArea.setWrapStyleWord(true);
    chatArea.setBackground(BG_LIGHTER);
    chatArea.setForeground(TEXT_COLOR);
    chatArea.setFont(MAIN_FONT);
    chatArea.setBorder(new EmptyBorder(10, 10, 10, 10));

    JScrollPane chatScroll = new JScrollPane(chatArea);
    chatScroll.setBorder(BorderFactory.createLineBorder(BG_DARK, 2));

    onlineUsersModel = new DefaultListModel<>();
    onlineUsersList = new JList<>(onlineUsersModel);
    onlineUsersList.setBackground(BG_DARK);
    onlineUsersList.setForeground(new Color(46, 204, 113));
    onlineUsersList.setFont(new Font("SansSerif", Font.BOLD, 13));
    onlineUsersList.setBorder(new EmptyBorder(10, 10, 10, 10));

    JScrollPane userScroll = new JScrollPane(onlineUsersList);
    userScroll.setPreferredSize(new Dimension(190, 0));
    userScroll.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(BG_DARK),
        "Room Roster",
        0, 0, MAIN_FONT, TEXT_COLOR));
    userScroll.setBackground(BG_DARK);

    JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScroll, userScroll);
    centerSplit.setResizeWeight(0.8);
    centerSplit.setDividerSize(4);

    // --- Bottom Panel: Input Area ---
    JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
    bottomPanel.setBackground(BG_DARK);
    bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

    inputField = new JTextField();
    styleTextField(inputField, "");

    JButton sendButton = createStyledButton("Send", ACCENT_COLOR);
    sendButton.setBorder(new EmptyBorder(8, 20, 8, 20));

    sendButton.addActionListener(e -> sendMessage());
    inputField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          sendMessage();
        }
      }
    });

    bottomPanel.add(inputField, BorderLayout.CENTER);
    bottomPanel.add(sendButton, BorderLayout.EAST);

    chatInterfacePanel.add(topBar, BorderLayout.NORTH);
    chatInterfacePanel.add(centerSplit, BorderLayout.CENTER);
    chatInterfacePanel.add(bottomPanel, BorderLayout.SOUTH);
  }

  private void leaveRoom() {
    out.println("LEAVE_ROOM");
    inChatRoom = false;
    CardLayout cl = (CardLayout) (mainContainer.getLayout());
    cl.show(mainContainer, "ROOM_SELECTION");
    setTitle("CivicConnect Secure Network - " + username);
    onlineUsersModel.clear();
    chatArea.setText("");
  }

  private void styleTextField(JTextField field, String placeholder) {
    field.setBackground(BG_LIGHTER);
    field.setForeground(TEXT_COLOR);
    field.setCaretColor(Color.WHITE);
    field.setFont(MAIN_FONT);
    field.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(BG_LIGHTER, 5),
        new EmptyBorder(5, 5, 5, 5)));
  }

  private JButton createStyledButton(String text, Color bgColor) {
    JButton btn = new JButton(text);
    btn.setBackground(bgColor);
    btn.setForeground(Color.WHITE);
    btn.setFont(new Font("SansSerif", Font.BOLD, 13));
    btn.setFocusPainted(false);
    btn.setOpaque(true);
    btn.setBorderPainted(false);
    return btn;
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
            if (text.startsWith("UPDATE_USERS||")) {
              updateUserList(text.substring(14));
            } else if (text.startsWith("ROOM_JOINED||")) {
              String roomName = text.substring(13);
              inChatRoom = true;
              roomHeaderLabel.setText("Room: " + roomName);
              CardLayout cl = (CardLayout) (mainContainer.getLayout());
              cl.show(mainContainer, "CHAT_INTERFACE");
              setTitle("Secure Room: " + roomName + " | User: " + username);
              chatArea.setText("");
            } else if (text.startsWith("CHAT||")) {
              String[] parts = text.split("\\|\\|", 3);
              if (parts.length == 3) {
                chatArea.append(parts[1] + ": " + parts[2] + "\n");
              } else {
                chatArea.append(text + "\n");
              }
            } else if (text.startsWith("SERVER: ")) {
              if (!inChatRoom) {
                JOptionPane.showMessageDialog(this, text.substring(8));
              } else {
                chatArea.append(text + "\n");
              }
            } else {
              chatArea.append(text + "\n");
            }
          });
        }
      } catch (IOException e) {
        SwingUtilities.invokeLater(() -> chatArea.append("Connection to server lost.\n"));
      }
    });
    listenerThread.setDaemon(true);
    listenerThread.start();
  }

  private void updateUserList(String csvUsers) {
    onlineUsersModel.clear();
    if (csvUsers.isEmpty())
      return;

    String[] users = csvUsers.split(",");
    for (String u : users) {
      if (!u.trim().isEmpty()) {
        onlineUsersModel.addElement("● " + u.trim());
      }
    }
  }
}