package com.eremis.ui.inquiry;

import com.eremis.controller.InquiryController;
import com.eremis.model.Inquiry;
import com.eremis.model.enums.InquiryStatus;
import com.eremis.ui.components.*;
import com.eremis.utils.SessionManager;
import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CRM Inquiry management panel.
 * Admins see all inquiries; regular users see only their own.
 * Features tabbed filter view + inline notes/status editor.
 */
public class InquiryPanel extends JPanel {

    private final UIThemeManager   theme   = UIThemeManager.getInstance();
    private final InquiryController ctrl   = new InquiryController();
    private final SessionManager   session = SessionManager.getInstance();

    private DefaultTableModel tableModel;
    private StyledTable       table;
    private List<Inquiry>     currentList;

    // Detail pane
    private JLabel      detailTitle, detailProperty, detailUser, detailStatus, detailDate;
    private JTextArea   detailMessage, notesArea;
    private JComboBox<InquiryStatus> statusCombo;
    private RoundedButton saveNotesBtn;

    private static final String[] COLS =
        {"#", "Property", "From", "Subject", "Status", "Created"};

    public InquiryPanel() {
        setBackground(theme.getBgPrimary());
        setLayout(new BorderLayout());
        buildUI();
        loadData(null);
    }

    private void buildUI() {
        // ── Top bar ───────────────────────────────────────────────────────
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(16, 20, 8, 20));

        JLabel title = new JLabel("💬  Inquiry / CRM Management");
        title.setFont(UIThemeManager.FONT_TITLE);
        title.setForeground(theme.getTextPrimary());
        top.add(title, BorderLayout.WEST);

        // Filter tabs (Pending / Contacted / Closed / All)
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tabs.setOpaque(false);
        for (String label : new String[]{"All", "PENDING", "CONTACTED", "CLOSED"}) {
            RoundedButton btn = label.equals("All")
                ? RoundedButton.secondary("All")
                : new RoundedButton(label,
                    statusColor(InquiryStatus.valueOf(label)),
                    statusColor(InquiryStatus.valueOf(label)).darker(), 8);
            btn.setFont(UIThemeManager.FONT_SMALL);
            btn.addActionListener(e -> {
                InquiryStatus s = "All".equals(label) ? null : InquiryStatus.valueOf(label);
                loadData(s);
            });
            tabs.add(btn);
        }
        top.add(tabs, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // ── Split: table (left) + detail pane (right) ─────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                          buildTablePanel(), buildDetailPanel());
        split.setDividerLocation(580);
        split.setDividerSize(4);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(theme.getBgPrimary());
        panel.setBorder(new EmptyBorder(0, 20, 20, 6));

        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new StyledTable(tableModel);
        table.getColumnModel().getColumn(4).setCellRenderer(new StyledTable.StatusBadgeRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail(getSelected());
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(theme.getBorder(), 1));
        scroll.getViewport().setBackground(theme.getBgSecondary());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(theme.getBgSecondary());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, theme.getBorder()),
            new EmptyBorder(20, 16, 20, 16)));

        JLabel heading = new JLabel("📋  Inquiry Detail");
        heading.setFont(UIThemeManager.FONT_LABEL);
        heading.setForeground(theme.getTextSecondary());
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(12));

        detailTitle    = detailLine("Subject:", "—");
        detailProperty = detailLine("Property:", "—");
        detailUser     = detailLine("From:", "—");
        detailStatus   = detailLine("Status:", "—");
        detailDate     = detailLine("Date:", "—");

        for (JLabel l : new JLabel[]{detailTitle, detailProperty, detailUser, detailStatus, detailDate}) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(l);
            panel.add(Box.createVerticalStrut(6));
        }

        panel.add(Box.createVerticalStrut(10));
        JLabel msgHdr = makeLabel("Message:");
        msgHdr.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(msgHdr);
        panel.add(Box.createVerticalStrut(4));

        detailMessage = new JTextArea(4, 20);
        detailMessage.setEditable(false);
        detailMessage.setLineWrap(true);
        detailMessage.setWrapStyleWord(true);
        detailMessage.setFont(UIThemeManager.FONT_BODY);
        detailMessage.setBackground(theme.getBgPrimary());
        detailMessage.setForeground(theme.getTextPrimary());
        JScrollPane msgScroll = new JScrollPane(detailMessage);
        msgScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        msgScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        panel.add(msgScroll);

        // Notes + status — admin only
        if (session.isAdmin()) {
            panel.add(Box.createVerticalStrut(14));
            JLabel notesHdr = makeLabel("Admin Notes:");
            notesHdr.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(notesHdr);

            notesArea = new JTextArea(3, 20);
            notesArea.setFont(UIThemeManager.FONT_BODY);
            notesArea.setBackground(theme.getBgPrimary());
            notesArea.setForeground(theme.getTextPrimary());
            notesArea.setLineWrap(true);
            notesArea.setWrapStyleWord(true);
            JScrollPane notesScroll = new JScrollPane(notesArea);
            notesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
            notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            panel.add(notesScroll);
            panel.add(Box.createVerticalStrut(10));

            JLabel statusHdr = makeLabel("Change Status:");
            statusHdr.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(statusHdr);

            statusCombo = new JComboBox<>(InquiryStatus.values());
            statusCombo.setFont(UIThemeManager.FONT_BODY);
            statusCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
            statusCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            panel.add(statusCombo);
            panel.add(Box.createVerticalStrut(10));

            saveNotesBtn = RoundedButton.success("💾  Save Changes");
            saveNotesBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            saveNotesBtn.setEnabled(false);
            saveNotesBtn.addActionListener(e -> saveInquiryUpdate());
            panel.add(saveNotesBtn);
        }

        return panel;
    }

    private void loadData(InquiryStatus filter) {
        SwingWorker<List<Inquiry>, Void> w = new SwingWorker<>() {
            @Override protected List<Inquiry> doInBackground() {
                if (!session.isAdmin()) return ctrl.getMyInquiries(session.getCurrentUserId());
                return filter == null ? ctrl.getAllInquiries() : ctrl.getByStatus(filter);
            }
            @Override protected void done() {
                try {
                    currentList = get();
                    refreshTable();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(InquiryPanel.this,
                        "Error loading inquiries: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        for (Inquiry i : currentList) {
            tableModel.addRow(new Object[]{
                i.getId(),
                i.getPropertyTitle() != null ? i.getPropertyTitle() : "#" + i.getPropertyId(),
                i.getUserName()      != null ? i.getUserName()      : "#" + i.getUserId(),
                i.getSubject(),
                i.getStatus().name(),
                i.getCreatedAt() != null ? i.getCreatedAt().format(fmt) : "—"
            });
        }
    }

    private void showDetail(Inquiry i) {
        if (i == null) return;
        detailTitle.setText("<html><b>Subject:</b>  " + i.getSubject() + "</html>");
        detailProperty.setText("<html><b>Property:</b>  " +
            (i.getPropertyTitle() != null ? i.getPropertyTitle() : "#" + i.getPropertyId()) + "</html>");
        detailUser.setText("<html><b>From:</b>  " +
            (i.getUserName() != null ? i.getUserName() : "#" + i.getUserId()) + "</html>");
        detailStatus.setText("<html><b>Status:</b>  " + i.getStatus().getDisplayName() + "</html>");
        detailDate.setText("<html><b>Date:</b>  " +
            (i.getCreatedAt() != null
             ? i.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
             : "—") + "</html>");
        detailMessage.setText(i.getMessage());

        if (session.isAdmin() && notesArea != null) {
            notesArea.setText(i.getNotes() != null ? i.getNotes() : "");
            statusCombo.setSelectedItem(i.getStatus());
            saveNotesBtn.setEnabled(true);
        }
    }

    private void saveInquiryUpdate() {
        Inquiry i = getSelected();
        if (i == null) return;
        i.setNotes(notesArea.getText().trim());
        i.setStatus((InquiryStatus) statusCombo.getSelectedItem());
        try {
            ctrl.updateInquiry(i);
            JOptionPane.showMessageDialog(this, "Inquiry updated.",
                "Saved", JOptionPane.INFORMATION_MESSAGE);
            loadData(null);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Inquiry getSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || currentList == null) return null;
        int id = (int) tableModel.getValueAt(row, 0);
        return currentList.stream().filter(i -> i.getId() == id).findFirst().orElse(null);
    }

    private JLabel detailLine(String label, String value) {
        JLabel l = new JLabel("<html><b>" + label + "</b>  " + value + "</html>");
        l.setFont(UIThemeManager.FONT_BODY);
        l.setForeground(theme.getTextPrimary());
        return l;
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIThemeManager.FONT_LABEL);
        l.setForeground(theme.getTextPrimary());
        return l;
    }

    private Color statusColor(InquiryStatus s) {
        switch (s) {
            case PENDING:   return theme.getWarning();
            case CONTACTED: return theme.getAccent();
            case CLOSED:    return theme.getTextSecondary();
            default:        return theme.getAccent();
        }
    }
}
