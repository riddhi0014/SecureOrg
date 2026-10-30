package com.orgchat;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
  private JTextField usernameField;
  private JPasswordField passwordField;
  private JButton loginBtn;
  private JButton registerBtn;
  private ChatClient client;

  public LoginFrame(ChatClient client) {
    this.client = client;

    // 1. Setup the basic window frame
    setTitle("Secure Org Chat - Login");
    setSize(340, 220);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null); // Centers the window on the screen
    setLayout(new GridLayout(4, 1, 5, 5));

    addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent e) {
        if (client != null) {
          client.close();
        }
        dispose();
        System.exit(0);
      }
    });

    // 2. Build the Username row
    JPanel userPanel = new JPanel(new FlowLayout());
    userPanel.add(new JLabel("Username:"));
    usernameField = new JTextField(15);
    userPanel.add(usernameField);

    // 3. Build the Password row
    JPanel passPanel = new JPanel(new FlowLayout());
    passPanel.add(new JLabel("Password:"));
    passwordField = new JPasswordField(15);
    passPanel.add(passwordField);

    // 4. Build the Buttons row
    JPanel buttonPanel = new JPanel(new FlowLayout());
    loginBtn = new JButton("Login");
    registerBtn = new JButton("Register");
    buttonPanel.add(loginBtn);
    buttonPanel.add(registerBtn);

    // Add everything to the main window
    add(new JLabel("Enter Credentials to Connect", SwingConstants.CENTER));
    add(userPanel);
    add(passPanel);
    add(buttonPanel);

    // 5. Attach Action Listeners to the buttons
    loginBtn.addActionListener(e -> attemptAuth("LOGIN"));
    registerBtn.addActionListener(e -> attemptAuth("REGISTER"));
  }

  private void attemptAuth(String action) {
    String username = usernameField.getText().trim();
    String password = new String(passwordField.getPassword());

    if (username.isEmpty() || password.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Fields cannot be empty.");
      return;
    }

    if (username.contains(",") || username.contains("||")) {
      JOptionPane.showMessageDialog(this, "Username cannot contain commas or '||'.");
      return;
    }

    setButtonsEnabled(false);

    SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
      @Override
      protected Boolean doInBackground() {
        return client.connectAndAuthenticate(action, username, password);
      }

      @Override
      protected void done() {
        try {
          boolean success = get();
          if (success) {
            dispose();
            new ChatFrame(client, username).setVisible(true);
            System.out.println("Login successful! Opened ChatFrame for " + username);
          } else {
            JOptionPane.showMessageDialog(LoginFrame.this, client.getLastAuthError());
            setButtonsEnabled(true);
          }
        } catch (Exception e) {
          JOptionPane.showMessageDialog(LoginFrame.this, "Authentication error: " + e.getMessage());
          setButtonsEnabled(true);
        }
      }
    };
    worker.execute();
  }

  private void setButtonsEnabled(boolean enabled) {
    loginBtn.setEnabled(enabled);
    registerBtn.setEnabled(enabled);
  }
}