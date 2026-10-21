package com.orgchat;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
  private JTextField usernameField;
  private JPasswordField passwordField;
  private ChatClient client;

  public LoginFrame(ChatClient client) {
    this.client = client;

    // 1. Setup the basic window frame
    setTitle("Secure Org Chat - Login");
    setSize(320, 200);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null); // Centers the window on the screen
    setLayout(new GridLayout(4, 1, 5, 5));

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
    JButton loginBtn = new JButton("Login");
    JButton registerBtn = new JButton("Register");
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
    String username = usernameField.getText();
    String password = new String(passwordField.getPassword());

    if (username.isEmpty() || password.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Fields cannot be empty.");
      return;
    }

    // Call our ChatClient method to send the network request
    if (client.connectAndAuthenticate(action, username, password)) {
      dispose(); // Destroys the login window

      // We will uncomment this line when ChatFrame is built!
      new ChatFrame(client, username).setVisible(true);
      System.out.println("Login successful! Ready to open ChatFrame.");
    } else {
      JOptionPane.showMessageDialog(this, "Authentication failed or Server is offline.");
    }
  }
}