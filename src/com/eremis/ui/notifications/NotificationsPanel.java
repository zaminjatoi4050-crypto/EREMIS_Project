package com.eremis.ui.notifications;

import com.eremis.model.Notification;
import com.eremis.service.NotificationService;
import com.eremis.utils.SessionManager;
import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Slide-in notification panel shown when the bell icon is clicked.
 *
 * FIX [FEATURE]: Bell was a dead button — no ActionListener, no panel.
 * Now fully functional: loads notifications, colour-codes by type,
 * shows unread count badge, and provides "Mark all read" action.
 */
public class NotificationsPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    private final UIThemeManager      theme   = UIThemeManager.getInstance();
    private final NotificationService service = new NotificationService();
    private final SessionManager      session = SessionManager.getInstance();

    private JPanel listPanel;
    private JLabel headerCountLabel;

    public NotificationsPanel() {
        setPreferredSize(new Dimension(360, 520));
        setLayout(new BorderLayout());
        setBackground(theme.getBgSecondary());
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme.getBorder(), 1, true),
            new EmptyBorder(0, 0, 0, 0)));
        buildUI();
        loadNotifications();
    }

    private void buildUI() {
        // ── Header ──────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0,0, UIThemeManager.NAVY, getWidth(),0, new Color(0x0D2444));
                g2.setPaint(gp); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(UIThemeManager.GOLD); g2.fillRect(0, getHeight()-2, getWidth(), 2);
                g2.dispose(); super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 56));
        header.setBorder(new EmptyBorder(0, 16, 0, 12));

        JLabel titleLbl = new JLabel("🔔  Notifications");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(Color.WHITE);

        headerCountLabel = new JLabel("");
        headerCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        headerCountLabel.setForeground(new Color(0x8FA8C8));

        JButton markAllBtn = new JButton("Mark all read");
        markAllBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        markAllBtn.setForeground(UIThemeManager.GOLD);
        markAllBtn.setContentAreaFilled(false);
        markAllBtn.setBorderPainted(false);
        markAllBtn.setFocusPainted(false);
        markAllBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        markAllBtn.addActionListener(e -> { service.markAllRead(session.getCurrentUserId()); loadNotifications(); });

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(titleLbl);
        left.add(headerCountLabel);

        header.add(left, BorderLayout.CENTER);
        header.add(markAllBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── List ────────────────────────────────────────────────────────
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(theme.getBgSecondary());

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.setBackground(theme.getBgSecondary());
        add(scroll, BorderLayout.CENTER);
    }

    public void loadNotifications() {
        SwingWorker<List<Notification>, Void> worker = new SwingWorker<>() {
            @Override protected List<Notification> doInBackground() {
                return service.getForUser(session.getCurrentUserId());
            }
            @Override protected void done() {
                try { renderNotifications(get()); }
                catch (Exception ex) { renderEmpty("Could not load notifications."); }
            }
        };
        worker.execute();
    }

    private void renderNotifications(List<Notification> notifs) {
        listPanel.removeAll();
        if (notifs == null || notifs.isEmpty()) { renderEmpty("You're all caught up!"); return; }

        long unread = notifs.stream().filter(n -> !n.isRead()).count();
        headerCountLabel.setText(unread > 0 ? unread + " unread" : "All read");

        for (Notification n : notifs) {
            listPanel.add(buildRow(n));
            JPanel div = new JPanel();
            div.setBackground(theme.getBorder());
            div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            div.setPreferredSize(new Dimension(0, 1));
            listPanel.add(div);
        }
        listPanel.revalidate(); listPanel.repaint();
    }

    private void renderEmpty(String msg) {
        listPanel.removeAll();
        headerCountLabel.setText("0 unread");
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setBackground(theme.getBgSecondary());
        empty.setPreferredSize(new Dimension(360, 300));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0,0,10,0);
        JLabel icon = new JLabel("📭", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        JLabel lbl = new JLabel("<html><center>" + msg + "</center></html>", SwingConstants.CENTER);
        lbl.setFont(UIThemeManager.FONT_BODY);
        lbl.setForeground(theme.getTextSecondary());
        empty.add(icon, gbc); gbc.gridy = 1; empty.add(lbl, gbc);
        listPanel.add(empty); listPanel.revalidate(); listPanel.repaint();
    }

    private JPanel buildRow(Notification n) {
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                if (!n.isRead()) {
                    g.setColor(new Color(200, 169, 90, 12));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(12, 16, 12, 16));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        // Coloured type dot
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(typeColor(n.getType())); g2.fillOval(0, 5, 10, 10); g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(14, 20)); dot.setOpaque(false);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS)); text.setOpaque(false);

        JLabel titleLbl = new JLabel(n.getTitle());
        titleLbl.setFont(new Font("Segoe UI", n.isRead() ? Font.PLAIN : Font.BOLD, 12));
        titleLbl.setForeground(theme.getTextPrimary());

        JLabel msgLbl = new JLabel("<html><body style='width:240px'>" + n.getMessage() + "</body></html>");
        msgLbl.setFont(UIThemeManager.FONT_SMALL); msgLbl.setForeground(theme.getTextSecondary());

        JLabel timeLbl = new JLabel(n.getCreatedAt() != null ? n.getCreatedAt().format(FMT) : "");
        timeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10)); timeLbl.setForeground(new Color(0x7A8B9A));

        text.add(titleLbl); text.add(Box.createVerticalStrut(3));
        text.add(msgLbl);  text.add(Box.createVerticalStrut(4)); text.add(timeLbl);

        row.add(dot, BorderLayout.WEST); row.add(text, BorderLayout.CENTER);
        return row;
    }

    private static Color typeColor(Notification.NotifType type) {
        if (type == null) return new Color(0x4A90D9);
        switch (type) {
            case SUCCESS: return new Color(0x27AE60);
            case WARNING: return new Color(0xF39C12);
            case ERROR:   return new Color(0xE74C3C);
            default:      return new Color(0x4A90D9);
        }
    }

    public int getUnreadCount() { return service.countUnread(session.getCurrentUserId()); }
}
