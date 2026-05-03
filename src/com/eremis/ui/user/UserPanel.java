package com.eremis.ui.user;

import com.eremis.controller.UserController;
import com.eremis.model.User;
import com.eremis.ui.components.*;
import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin-only User management panel.
 * Provides CRUD + role badge rendering + active/inactive toggle.
 */
public class UserPanel extends JPanel {

    private final UIThemeManager theme = UIThemeManager.getInstance();
    private final UserController ctrl  = new UserController();

    private DefaultTableModel tableModel;
    private StyledTable       table;
    private List<User>        currentList;

    private static final String[] COLS =
        {"#", "Full Name", "Email", "Username", "Role", "Active", "Created"};

    public UserPanel() {
        setBackground(theme.getBgPrimary());
        setLayout(new BorderLayout());
        buildUI();
        loadData();
    }

    private void buildUI() {
        // ── Top bar ───────────────────────────────────────────────────────
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(16, 20, 0, 20));

        JLabel title = new JLabel("👥  User Management");
        title.setFont(UIThemeManager.FONT_TITLE);
        title.setForeground(theme.getTextPrimary());
        top.add(title, BorderLayout.WEST);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbar.setOpaque(false);

        RoundedButton addBtn = new RoundedButton("➕  Add User");
        addBtn.addActionListener(e -> openAddDialog());

        RoundedButton editBtn = RoundedButton.secondary("✏️  Edit");
        editBtn.addActionListener(e -> openEditDialog());

        RoundedButton deleteBtn = RoundedButton.danger("🗑️  Delete");
        deleteBtn.addActionListener(e -> doDelete());

        RoundedButton toggleBtn = new RoundedButton("🔁  Toggle Active",
            new Color(0xFF9800), new Color(0xF57C00), 10);
        toggleBtn.addActionListener(e -> doToggleActive());

        RoundedButton refreshBtn = RoundedButton.secondary("🔄  Refresh");
        refreshBtn.addActionListener(e -> loadData());

        toolbar.add(addBtn); toolbar.add(editBtn);
        toolbar.add(deleteBtn); toolbar.add(toggleBtn); toolbar.add(refreshBtn);
        top.add(toolbar, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────
        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new StyledTable(tableModel);
        // Role badge renderer (column 4) + Active badge (column 5)
        table.getColumnModel().getColumn(4).setCellRenderer(new StyledTable.StatusBadgeRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new StyledTable.StatusBadgeRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(12, 20, 20, 20));
        scroll.getViewport().setBackground(theme.getBgSecondary());
        add(scroll, BorderLayout.CENTER);
    }

    private void loadData() {
        SwingWorker<List<User>, Void> w = new SwingWorker<>() {
            @Override protected List<User> doInBackground() { return ctrl.getAllUsers(); }
            @Override protected void done() {
                try {
                    currentList = get();
                    refreshTable();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UserPanel.this,
                        "Error loading users: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        for (User u : currentList) {
            tableModel.addRow(new Object[]{
                u.getId(), u.getFullName(), u.getEmail(), u.getUsername(),
                u.getRole().name(),
                u.isActive() ? "ACTIVE" : "INACTIVE",
                u.getCreatedAt() != null ? u.getCreatedAt().format(fmt) : "—"
            });
        }
    }

    private void openAddDialog() {
        UserFormDialog dlg = new UserFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), null, ctrl);
        dlg.setVisible(true);
        if (dlg.isSaved()) loadData();
    }

    private void openEditDialog() {
        User u = getSelected();
        if (u == null) { showSelectWarn(); return; }
        UserFormDialog dlg = new UserFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), u, ctrl);
        dlg.setVisible(true);
        if (dlg.isSaved()) loadData();
    }

    private void doDelete() {
        User u = getSelected();
        if (u == null) { showSelectWarn(); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete user \"" + u.getUsername() + "\"? This cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try { ctrl.deleteUser(u.getId()); loadData(); }
            catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doToggleActive() {
        User u = getSelected();
        if (u == null) { showSelectWarn(); return; }
        u.setActive(!u.isActive());
        try { ctrl.updateUser(u); loadData(); }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private User getSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || currentList == null) return null;
        int id = (int) tableModel.getValueAt(row, 0);
        return currentList.stream().filter(u -> u.getId() == id).findFirst().orElse(null);
    }

    private void showSelectWarn() {
        JOptionPane.showMessageDialog(this, "Please select a user first.",
            "No Selection", JOptionPane.INFORMATION_MESSAGE);
    }
}
