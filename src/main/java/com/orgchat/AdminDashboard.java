package com.orgchat;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminDashboard extends JFrame {
  private JTable logTable;
  private DefaultTableModel tableModel;

  public AdminDashboard() {
    setTitle("CivicConnect Secure Network - Admin Audit Portal");
    setSize(700, 500);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setLocationRelativeTo(null);
    setLayout(new BorderLayout());

    // 1. Set up the Table Columns
    String[] columns = { "Message ID", "Sender", "Message Content" };
    tableModel = new DefaultTableModel(columns, 0);
    logTable = new JTable(tableModel);

    // 2. Style the Table for a professional look
    logTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
    logTable.setRowHeight(25);
    logTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
    logTable.getTableHeader().setBackground(new Color(43, 45, 49));
    logTable.getTableHeader().setForeground(Color.WHITE);

    JScrollPane scrollPane = new JScrollPane(logTable);
    add(scrollPane, BorderLayout.CENTER);

    // 3. Add a Refresh Button at the bottom
    JPanel bottomPanel = new JPanel();
    JButton refreshBtn = new JButton("Refresh Logs");
    refreshBtn.addActionListener(e -> fetchLogsFromDatabase());
    bottomPanel.add(refreshBtn);
    add(bottomPanel, BorderLayout.SOUTH);

    // 4. Initial data load
    fetchLogsFromDatabase();
  }

  private void fetchLogsFromDatabase() {
    // Clear existing rows before fetching fresh data
    tableModel.setRowCount(0);

    String sql = "SELECT id, sender_name, message_content FROM Messages ORDER BY id ASC";

    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery()) {

      while (rs.next()) {
        int id = rs.getInt("id");
        String sender = rs.getString("sender_name");
        String content = rs.getString("message_content");

        // Add the row directly to the Swing UI table
        tableModel.addRow(new Object[] { id, sender, content });
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Failed to load database logs.");
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    // Launch the Admin Dashboard
    SwingUtilities.invokeLater(() -> {
      new AdminDashboard().setVisible(true);
    });
  }
}