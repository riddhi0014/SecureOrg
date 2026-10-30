package com.orgchat;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminDashboard extends JFrame {
  private JTable roomsTable, auditTable, messagesTable;
  private DefaultTableModel roomsModel, auditModel, messagesModel;

  public AdminDashboard() {
    setTitle("CivicConnect Secure Network - Admin Audit Portal");
    setSize(900, 600);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setLocationRelativeTo(null);
    setLayout(new BorderLayout());

    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 14));

    // 1. Rooms Tab
    roomsModel = new DefaultTableModel(new String[] { "Room ID", "Room Name", "Created At", "Last Active" }, 0);
    roomsTable = styleTable(roomsModel);
    tabbedPane.addTab("Active Rooms", new JScrollPane(roomsTable));

    // 2. Audit Trail Tab (Room_Members JOIN Users JOIN Rooms)
    auditModel = new DefaultTableModel(new String[] { "Room Name", "Username", "Joined At" }, 0);
    auditTable = styleTable(auditModel);
    tabbedPane.addTab("Membership Audit Trail", new JScrollPane(auditTable));

    // 3. Global Messages Tab (Messages JOIN Rooms)
    messagesModel = new DefaultTableModel(new String[] { "Room Name", "Sender", "Message Content", "Sent At" }, 0);
    messagesTable = styleTable(messagesModel);
    tabbedPane.addTab("Global Chat Logs", new JScrollPane(messagesTable));

    add(tabbedPane, BorderLayout.CENTER);

    // Refresh Button
    JPanel bottomPanel = new JPanel();
    bottomPanel.setBackground(new Color(43, 45, 49));
    JButton refreshBtn = new JButton("Refresh All Logs");
    refreshBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
    refreshBtn.addActionListener(e -> fetchAllData());
    bottomPanel.add(refreshBtn);
    add(bottomPanel, BorderLayout.SOUTH);

    fetchAllData();
  }

  private JTable styleTable(DefaultTableModel model) {
    JTable table = new JTable(model);
    table.setFont(new Font("SansSerif", Font.PLAIN, 14));
    table.setRowHeight(25);
    table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
    table.getTableHeader().setBackground(new Color(43, 45, 49));
    table.getTableHeader().setForeground(Color.WHITE);
    return table;
  }

  private void fetchAllData() {
    fetchRooms();
    fetchAuditTrail();
    fetchMessages();
  }

  private void fetchRooms() {
    roomsModel.setRowCount(0);
    String sql = "SELECT id, room_name, created_at, last_active FROM Rooms ORDER BY created_at DESC";
    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery()) {
      while (rs.next()) {
        roomsModel.addRow(new Object[] {
            rs.getInt("id"), rs.getString("room_name"),
            rs.getString("created_at"), rs.getString("last_active")
        });
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void fetchAuditTrail() {
    auditModel.setRowCount(0);
    String sql = "SELECT r.room_name, u.username, rm.joined_at " +
        "FROM Room_Members rm " +
        "JOIN Rooms r ON rm.room_id = r.id " +
        "JOIN Users u ON rm.user_id = u.id " +
        "ORDER BY rm.joined_at DESC";
    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery()) {
      while (rs.next()) {
        auditModel.addRow(new Object[] {
            rs.getString("room_name"), rs.getString("username"), rs.getString("joined_at")
        });
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void fetchMessages() {
    messagesModel.setRowCount(0);
    String sql = "SELECT r.room_name, m.sender_name, m.message_content, m.sent_at " +
        "FROM Messages m " +
        "JOIN Rooms r ON m.room_id = r.id " +
        "ORDER BY m.sent_at DESC";
    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery()) {
      while (rs.next()) {
        messagesModel.addRow(new Object[] {
            rs.getString("room_name"), rs.getString("sender_name"),
            rs.getString("message_content"), rs.getString("sent_at")
        });
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new AdminDashboard().setVisible(true));
  }
}