package com.eremis.ui.property;

import com.eremis.controller.PropertyController;
import com.eremis.controller.InquiryController;
import com.eremis.model.Inquiry;
import com.eremis.model.Property;
import com.eremis.model.enums.PropertyStatus;
import com.eremis.ui.components.*;
import com.eremis.utils.SessionManager;
import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Property management panel — table + add/edit/delete/status-change actions.
 */
public class PropertyPanel extends JPanel {

    private final UIThemeManager    theme   = UIThemeManager.getInstance();
    private final PropertyController ctrl   = new PropertyController();
    private final InquiryController inquiryCtrl = new InquiryController();
    private final SessionManager    session = SessionManager.getInstance();

    private DefaultTableModel tableModel;
    private StyledTable       table;
    private List<Property>    currentList;

    private static final String[] COLS = {
        "#", "Title", "City", "Type", "Price (PKR)", "Status", "Bedrooms", "Listed By"
    };

    public PropertyPanel() {
        setBackground(theme.getBgPrimary());
        setLayout(new BorderLayout());
        buildUI();
        loadData();
    }

    private void buildUI() {
        // ── Toolbar ──────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        toolbar.setOpaque(false);

        RoundedButton addBtn = new RoundedButton("➕  Add Property");
        addBtn.addActionListener(e -> openAddDialog());

        RoundedButton editBtn = RoundedButton.secondary("✏️  Edit");
        editBtn.addActionListener(e -> openEditDialog());

        RoundedButton deleteBtn = RoundedButton.danger("🗑️  Delete");
        deleteBtn.addActionListener(e -> doDelete());

        RoundedButton inquiryBtn = RoundedButton.secondary("💬  Inquiry");
        inquiryBtn.addActionListener(e -> openInquiryDialog());

        RoundedButton statusBtn = new RoundedButton("🔄  Change Status",
            theme.getWarning(), theme.getWarning().darker(), 10);
        statusBtn.addActionListener(e -> doChangeStatus());

        RoundedButton refreshBtn = RoundedButton.secondary("🔄  Refresh");
        refreshBtn.addActionListener(e -> loadData());

        toolbar.add(addBtn); toolbar.add(editBtn);
        toolbar.add(deleteBtn); toolbar.add(inquiryBtn); toolbar.add(statusBtn); toolbar.add(refreshBtn);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(16, 20, 0, 20));
        JLabel title = new JLabel("🏠  Property Management");
        title.setFont(UIThemeManager.FONT_TITLE);
        title.setForeground(theme.getTextPrimary());
        topBar.add(title, BorderLayout.WEST);
        topBar.add(toolbar, BorderLayout.SOUTH);
        add(topBar, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────
        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new StyledTable(tableModel);
        // Status badge renderer in column 5
        table.getColumnModel().getColumn(5)
             .setCellRenderer(new StyledTable.StatusBadgeRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(230);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorder()));
        scroll.getViewport().setBackground(theme.getBgSecondary());
        scroll.setBorder(new EmptyBorder(12, 20, 20, 20));
        add(scroll, BorderLayout.CENTER);
    }

    // ── Data loading ──────────────────────────────────────────────────────
    private void loadData() {
        SwingWorker<List<Property>, Void> w = new SwingWorker<>() {
            @Override protected List<Property> doInBackground() {
                return ctrl.getAllProperties();
            }
            @Override protected void done() {
                try {
                    currentList = get();
                    refreshTable(currentList);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PropertyPanel.this,
                        "Error loading properties: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private void refreshTable(List<Property> list) {
        tableModel.setRowCount(0);
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
        for (Property p : list) {
            tableModel.addRow(new Object[]{
                p.getId(),
                p.getTitle(),
                p.getCity(),
                p.getType().getDisplayName(),
                fmt.format(p.getPrice()),
                p.getStatus().name(),
                p.getBedrooms(),
                p.getListedByName() != null ? p.getListedByName() : "—"
            });
        }
    }

    // ── CRUD actions ──────────────────────────────────────────────────────
    private void openAddDialog() {
        PropertyFormDialog dlg = new PropertyFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), null, ctrl);
        dlg.setVisible(true);
        if (dlg.isSaved()) loadData();
    }

    private void openEditDialog() {
        Property p = getSelected();
        if (p == null) { showSelectWarning(); return; }
        if (!canEdit(p)) {
            JOptionPane.showMessageDialog(this,
                "You can only edit properties you created.",
                "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        PropertyFormDialog dlg = new PropertyFormDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), p, ctrl);
        dlg.setVisible(true);
        if (dlg.isSaved()) loadData();
    }

    private void doDelete() {
        if (!session.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Only admins can delete properties.",
                "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Property p = getSelected();
        if (p == null) { showSelectWarning(); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete property \"" + p.getTitle() + "\"?\nThis action cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                ctrl.deleteProperty(p.getId());
                loadData();
                JOptionPane.showMessageDialog(this, "Property deleted.",
                    "Deleted", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doChangeStatus() {
        Property p = getSelected();
        if (p == null) { showSelectWarning(); return; }

        PropertyStatus[] opts = PropertyStatus.values();
        PropertyStatus chosen = (PropertyStatus) JOptionPane.showInputDialog(
            this, "Select new status for:\n\"" + p.getTitle() + "\"",
            "Change Status", JOptionPane.QUESTION_MESSAGE, null, opts, p.getStatus());

        if (chosen != null && chosen != p.getStatus()) {
            try {
                ctrl.advanceStatus(p.getId(), chosen);
                loadData();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                    "Status Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Property getSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || currentList == null) return null;
        int id = (int) tableModel.getValueAt(row, 0);
        return currentList.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    private void showSelectWarning() {
        JOptionPane.showMessageDialog(this, "Please select a property first.",
            "No Selection", JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean canEdit(Property p) {
        if (session.getCurrentUser() == null) return false;
        if (session.getCurrentUser().getRole().isAdminLike()) return true;
        return p.getListedBy() == session.getCurrentUserId();
    }

    private void openInquiryDialog() {
        Property p = getSelected();
        if (p == null) { showSelectWarning(); return; }

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "Send Inquiry", true);
        dlg.setSize(520, 360);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(16, 18, 16, 18));
        root.setBackground(theme.getBgSecondary());
        dlg.setContentPane(root);

        JLabel heading = new JLabel("Inquiry for: " + p.getTitle());
        heading.setFont(UIThemeManager.FONT_TITLE);
        heading.setForeground(theme.getTextPrimary());
        root.add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 0, 6, 0);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;

        JTextField subjectField = new JTextField("Inquiry about " + p.getTitle());
        JTextArea messageArea = new JTextArea(6, 20);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        g.gridx = 0; g.gridy = 0;
        JLabel subjectLbl = new JLabel("Subject");
        subjectLbl.setFont(UIThemeManager.FONT_LABEL);
        subjectLbl.setForeground(theme.getTextPrimary());
        form.add(subjectLbl, g);
        g.gridy = 1;
        form.add(subjectField, g);
        g.gridy = 2;
        JLabel messageLbl = new JLabel("Message");
        messageLbl.setFont(UIThemeManager.FONT_LABEL);
        messageLbl.setForeground(theme.getTextPrimary());
        form.add(messageLbl, g);
        g.gridy = 3;
        form.add(new JScrollPane(messageArea), g);

        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        RoundedButton cancelBtn = RoundedButton.secondary("Cancel");
        cancelBtn.addActionListener(e -> dlg.dispose());
        RoundedButton sendBtn = new RoundedButton("Send Inquiry");
        sendBtn.addActionListener(e -> {
            try {
                Inquiry inq = new Inquiry();
                inq.setPropertyId(p.getId());
                inq.setUserId(session.getCurrentUserId());
                inq.setSubject(subjectField.getText().trim());
                inq.setMessage(messageArea.getText().trim());
                inquiryCtrl.createInquiry(inq);
                dlg.dispose();
                JOptionPane.showMessageDialog(this,
                    "Inquiry sent successfully.",
                    "Inquiry", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg,
                    "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttons.add(cancelBtn);
        buttons.add(sendBtn);
        root.add(buttons, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }
}
