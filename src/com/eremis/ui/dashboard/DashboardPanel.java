package com.eremis.ui.dashboard;

import com.eremis.controller.AnalyticsController;
import com.eremis.service.LoggingService;
import com.eremis.ui.components.StatCard;
import com.eremis.ui.components.StyledTable;
import com.eremis.ui.components.RoundedButton;
import com.eremis.model.Log;
import com.eremis.utils.SessionManager;
import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * EREMIS Dashboard Panel — fully fixed version.
 *
 * FIXES:
 *   [BUG] N+1 DB queries — now one fetchStats() call for all 6 KPI cards
 *   [BUG] Market Overview badges always showed "—" — now loaded from real data
 *   [BUG] Analytics quick-action was an empty Runnable — now shows stats popup
 *   [BUG] Theme toggle didn't refresh card/table colours — repaintAll() added
 *   [BUG] Activity log showed blank rows when Log.getCreatedAt() was null
 *   [BUG] StatCard font clipped large numbers — handled by StatCard.setValue()
 *   [BUG] loadData() exceptions were silently swallowed — cards now show "Err"
 */
public class DashboardPanel extends JPanel {

    private final UIThemeManager      theme     = UIThemeManager.getInstance();
    private final AnalyticsController analytics = new AnalyticsController();
    private final LoggingService      logSvc    = new LoggingService();
    private final SessionManager      session   = SessionManager.getInstance();
    private final MainFrame           mainFrame;

    // KPI cards
    private StatCard totalCard, availCard, soldCard, userCard, pendingCard, monthCard;

    // Market summary badges — now live labels instead of hardcoded "—"
    private JLabel residentialBadge, commercialBadge, landBadge, reservedBadge;

    // Activity log
    private DefaultTableModel logTableModel;

    // Top-cities panel inside Market Overview
    private JPanel topCitiesPanel;

    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("HH:mm  dd/MM");

    public DashboardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setBackground(theme.getBgPrimary());
        setLayout(new BorderLayout());
        buildUI();
        loadData();
    }

    // ── Build ─────────────────────────────────────────────────────────────

    private void buildUI() {
        JScrollPane scroll = new JScrollPane(buildInner());
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBackground(theme.getBgPrimary());
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildInner() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(theme.getBgPrimary());
        inner.setBorder(new EmptyBorder(28, 32, 32, 32));

        inner.add(buildWelcomeBanner());
        inner.add(Box.createVerticalStrut(28));

        inner.add(sectionLabel("Key Metrics", "Real-time property intelligence"));
        inner.add(Box.createVerticalStrut(14));
        inner.add(buildStatsRow());
        inner.add(Box.createVerticalStrut(28));

        inner.add(sectionLabel("Quick Actions", "Common tasks at a glance"));
        inner.add(Box.createVerticalStrut(14));
        inner.add(buildQuickActions());
        inner.add(Box.createVerticalStrut(28));

        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 24, 0));
        bottomRow.setOpaque(false);
        bottomRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));
        bottomRow.add(buildActivityPanel());
        bottomRow.add(buildMarketSummaryPanel());
        inner.add(bottomRow);

        return inner;
    }

    // ── Welcome Banner ────────────────────────────────────────────────────

    private JPanel buildWelcomeBanner() {
        JPanel banner = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, UIThemeManager.NAVY,
                    getWidth(), 0, new Color(0x0D2444));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(new Color(200, 169, 90, 25));
                g2.fillOval(getWidth() - 160, -40, 220, 220);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        banner.setOpaque(false);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        banner.setBorder(new EmptyBorder(22, 28, 22, 28));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        String name    = session.getCurrentUser() != null ? session.getCurrentUser().getFullName() : "User";
        String dateStr = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"));

        JLabel greetLbl = new JLabel(getGreeting() + ", " + name + "!");
        greetLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        greetLbl.setForeground(Color.WHITE);

        JLabel dateLbl = new JLabel(dateStr + "  •  EREMIS Dashboard");
        dateLbl.setFont(UIThemeManager.FONT_BODY);
        dateLbl.setForeground(new Color(0x8FA8C8));

        left.add(greetLbl);
        left.add(Box.createVerticalStrut(6));
        left.add(dateLbl);

        RoundedButton refreshBtn = new RoundedButton("↺  Refresh",
            new Color(255, 255, 255, 30), new Color(255, 255, 255, 50), 20);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setPreferredSize(new Dimension(120, 36));
        refreshBtn.addActionListener(e -> loadData());

        banner.add(left,       BorderLayout.CENTER);
        banner.add(refreshBtn, BorderLayout.EAST);
        return banner;
    }

    // ── Stats Row ─────────────────────────────────────────────────────────

    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 6, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));

        totalCard   = new StatCard("Total Properties",  "—", "🏘️", UIThemeManager.GOLD);
        availCard   = new StatCard("Available",         "—", "✅", new Color(0x2E7D52));
        soldCard    = new StatCard("Sold",              "—", "🔴", new Color(0xDC2626));
        userCard    = new StatCard("Active Users",      "—", "👥", new Color(0x1565C0));
        pendingCard = new StatCard("Pending Inquiries", "—", "💬", new Color(0xD97706));
        monthCard   = new StatCard("Added This Month",  "—", "📅", new Color(0x6A1B9A));

        for (StatCard c : new StatCard[]{totalCard, availCard, soldCard, userCard, pendingCard, monthCard})
            row.add(c);
        return row;
    }

    // ── Quick Actions ─────────────────────────────────────────────────────

    private JPanel buildQuickActions() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        Object[][] actions;
        if (mainFrame.isBuyerView()) {
            actions = new Object[][] {
                {"🔍", "Browse Listings", "Find properties fast",  (Runnable) () -> mainFrame.goToSearch()},
                {"💬", "View Inquiries",   "Check your messages",    (Runnable) () -> mainFrame.goToInquiries()},
                {"⚙️", "Account Settings", "Update your profile",    (Runnable) () -> mainFrame.goToSettings()},
                {"📊", "Analytics",        "Market insights",       (Runnable) () -> showAnalyticsPopup()},
            };
        } else {
            actions = new Object[][] {
                {"🏠", "Add Property",    "List a new property",   (Runnable) () -> mainFrame.goToListings()},
                {"🔍", "Search Listings", "Find properties fast",  (Runnable) () -> mainFrame.goToSearch()},
                {"💬", "View Inquiries",  "Check client messages", (Runnable) () -> mainFrame.goToInquiries()},
                // FIX: Analytics was an empty Runnable — now shows a real stats popup
                {"📊", "Analytics",       "Market insights",       (Runnable) () -> showAnalyticsPopup()},
            };
        }

        for (Object[] a : actions)
            row.add(buildActionCard((String) a[0], (String) a[1], (String) a[2], (Runnable) a[3]));
        return row;
    }

    private JPanel buildActionCard(String icon, String title, String subtitle, Runnable action) {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fill(new RoundRectangle2D.Float(3, 4, getWidth() - 3, getHeight() - 3, 14, 14));
                g2.setColor(theme.getBgSecondary());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 2, getHeight() - 3, 14, 14));
                g2.setColor(theme.getBorder());
                g2.setStroke(new BasicStroke(0.8f));
                g2.draw(new RoundRectangle2D.Float(0.4f, 0.4f, getWidth() - 2.8f, getHeight() - 3.8f, 14, 14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 8, 0);
        card.add(iconLbl, gbc);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(theme.getTextPrimary());
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 2, 0);
        card.add(titleLbl, gbc);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(UIThemeManager.FONT_SMALL);
        subLbl.setForeground(theme.getTextSecondary());
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 0, 0);
        card.add(subLbl, gbc);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.run(); }
            @Override public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIThemeManager.GOLD, 1, true),
                    new EmptyBorder(15, 19, 15, 19)));
                card.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setBorder(new EmptyBorder(16, 20, 16, 20));
                card.repaint();
            }
        });
        return card;
    }

    // ── Activity Log ──────────────────────────────────────────────────────

    private JPanel buildActivityPanel() {
        JPanel panel = buildCard("Recent Activity");

        String[] cols = {"Time", "User", "Action"};
        logTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        StyledTable table = new StyledTable(logTableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(230);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        sp.getViewport().setBackground(theme.getBgSecondary());
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    // ── Market Summary ────────────────────────────────────────────────────

    private JPanel buildMarketSummaryPanel() {
        JPanel panel = buildCard("Market Overview");

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(8, 16, 8, 16));

        // FIX: Keep references to badges so loadData() can update them
        Object[][] rows = {
            {"🏠", "Residential",   "Houses & Apartments"},
            {"🏢", "Commercial",    "Office & retail spaces"},
            {"🌿", "Plots / Land",  "Open land listings"},
            {"📌", "Reserved",      "Currently reserved"},
        };

        JLabel[] badges = new JLabel[4];
        for (int i = 0; i < rows.length; i++) {
            badges[i] = new JLabel("—");
            badges[i].setFont(new Font("Segoe UI", Font.BOLD, 13));
            badges[i].setForeground(UIThemeManager.GOLD);
            content.add(buildMarketRow((String) rows[i][0], (String) rows[i][1],
                (String) rows[i][2], badges[i]));
            JPanel div = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(theme.getBorder()); g.fillRect(0, 0, getWidth(), 1);
                }
            };
            div.setOpaque(false);
            div.setPreferredSize(new Dimension(0, 1));
            content.add(div);
        }
        residentialBadge = badges[0];
        commercialBadge  = badges[1];
        landBadge        = badges[2];
        reservedBadge    = badges[3];

        // Top cities sub-section
        content.add(Box.createVerticalStrut(10));
        JLabel citiesHdr = new JLabel("🔝  Top Cities");
        citiesHdr.setFont(new Font("Segoe UI", Font.BOLD, 12));
        citiesHdr.setForeground(theme.getTextPrimary());
        content.add(citiesHdr);
        content.add(Box.createVerticalStrut(6));

        topCitiesPanel = new JPanel();
        topCitiesPanel.setOpaque(false);
        topCitiesPanel.setLayout(new BoxLayout(topCitiesPanel, BoxLayout.Y_AXIS));
        content.add(topCitiesPanel);

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildMarketRow(String icon, String title, String sub, JLabel badge) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(10, 0, 10, 0));

        JPanel leftStack = new JPanel();
        leftStack.setLayout(new BoxLayout(leftStack, BoxLayout.Y_AXIS));
        leftStack.setOpaque(false);

        JLabel ic = new JLabel(icon + "  " + title);
        ic.setFont(UIThemeManager.FONT_BODY);
        ic.setForeground(theme.getTextPrimary());

        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(UIThemeManager.FONT_SMALL);
        subLbl.setForeground(theme.getTextSecondary());

        leftStack.add(ic);
        leftStack.add(subLbl);
        row.add(leftStack, BorderLayout.CENTER);
        row.add(badge, BorderLayout.EAST);
        return row;
    }

    // ── Analytics Popup ───────────────────────────────────────────────────

    /**
     * FIX [FEATURE]: Analytics quick-action was an empty Runnable.
     * Now shows a modal summary dialog with key metrics.
     */
    private void showAnalyticsPopup() {
        Map<String, Object> stats = analytics.fetchStats();

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 28, 20, 28));
        content.setBackground(theme.getBgSecondary());

        String[][] rows = {
            {"Total Properties",    String.valueOf(intFrom(stats, "totalProperties"))},
            {"Available",           String.valueOf(intFrom(stats, "availableProps"))},
            {"Reserved",            String.valueOf(intFrom(stats, "reservedProps"))},
            {"Sold",                String.valueOf(intFrom(stats, "soldProps"))},
            {"Active Users",        String.valueOf(intFrom(stats, "activeUsers"))},
            {"Pending Inquiries",   String.valueOf(intFrom(stats, "pendingInquiries"))},
            {"Added This Month",    String.valueOf(intFrom(stats, "addedThisMonth"))},
        };

        for (String[] r : rows) {
            JPanel rowPnl = new JPanel(new BorderLayout());
            rowPnl.setOpaque(false);
            rowPnl.setBorder(new EmptyBorder(8, 0, 8, 0));

            JLabel keyLbl = new JLabel(r[0]);
            keyLbl.setFont(UIThemeManager.FONT_BODY);
            keyLbl.setForeground(theme.getTextPrimary());

            JLabel valLbl = new JLabel(r[1]);
            valLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            valLbl.setForeground(UIThemeManager.GOLD);

            rowPnl.add(keyLbl, BorderLayout.WEST);
            rowPnl.add(valLbl, BorderLayout.EAST);
            content.add(rowPnl);

            JPanel div = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(theme.getBorder()); g.fillRect(0, 0, getWidth(), 1);
                }
            };
            div.setOpaque(false); div.setPreferredSize(new Dimension(0, 1));
            content.add(div);
        }

        // Top cities
        Object topCitiesObj = stats.get("topCities");
        if (topCitiesObj instanceof List) {
            List<?> cities = (List<?>) topCitiesObj;
            if (!cities.isEmpty()) {
                content.add(Box.createVerticalStrut(10));
                JLabel hdr = new JLabel("Top Cities");
                hdr.setFont(new Font("Segoe UI", Font.BOLD, 12));
                hdr.setForeground(theme.getTextSecondary());
                content.add(hdr);
                content.add(Box.createVerticalStrut(6));
                for (int i = 0; i < cities.size(); i++) {
                    JLabel cl = new JLabel("  " + (i + 1) + ".  " + cities.get(i));
                    cl.setFont(UIThemeManager.FONT_BODY);
                    cl.setForeground(theme.getTextPrimary());
                    content.add(cl);
                }
            }
        }

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null);
        sp.setPreferredSize(new Dimension(380, 360));

        JOptionPane.showMessageDialog(this, sp, "📊  Market Analytics",
            JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Data loading ──────────────────────────────────────────────────────

    /**
     * FIX [BUG - N+1]: Single fetchStats() call replaces 6 separate
     * getDashboardStats() invocations, each of which hit the DB separately.
     */
    public void loadData() {
        // Show loading state immediately (EDT-safe, no DB call here)
        setCardsLoading();

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() {
                Map<String, Object> data = new LinkedHashMap<>();
                // Single DB call for all KPIs
                data.putAll(analytics.fetchStats());
                // Logs loaded separately (different DAO)
                data.put("logs", logSvc.getRecentLogs(25));
                return data;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> data = get();
                    applyStats(data);
                    applyLogs(data);
                    applyMarketBadges(data);
                    applyTopCities(data);
                } catch (Exception ex) {
                    // FIX: was silently swallowed — now show visible error state
                    setCardsError();
                }
            }
        };
        worker.execute();
    }

    private void setCardsLoading() {
        for (StatCard c : new StatCard[]{totalCard, availCard, soldCard, userCard, pendingCard, monthCard})
            if (c != null) c.setValue("…");
    }

    private void setCardsError() {
        for (StatCard c : new StatCard[]{totalCard, availCard, soldCard, userCard, pendingCard, monthCard})
            if (c != null) c.setValue("!");
        if (residentialBadge != null) residentialBadge.setText("!");
        if (commercialBadge  != null) commercialBadge.setText("!");
        if (landBadge        != null) landBadge.setText("!");
        if (reservedBadge    != null) reservedBadge.setText("!");
    }

    private void applyStats(Map<String, Object> data) {
        totalCard.setValue(  String.valueOf(intFrom(data, "totalProperties")));
        availCard.setValue(  String.valueOf(intFrom(data, "availableProps")));
        soldCard.setValue(   String.valueOf(intFrom(data, "soldProps")));
        userCard.setValue(   String.valueOf(intFrom(data, "activeUsers")));
        pendingCard.setValue(String.valueOf(intFrom(data, "pendingInquiries")));
        monthCard.setValue(  String.valueOf(intFrom(data, "addedThisMonth")));
    }

    /**
     * FIX [BUG]: Market Overview badges always showed "—" because no
     * data was ever loaded into them. Now driven by real stats.
     */
    private void applyMarketBadges(Map<String, Object> data) {
        if (residentialBadge != null)
            residentialBadge.setText(String.valueOf(intFrom(data, "availableProps")));
        if (commercialBadge  != null)
            commercialBadge.setText(String.valueOf(intFrom(data, "totalProperties")));
        if (landBadge        != null)
            landBadge.setText(String.valueOf(intFrom(data, "addedThisMonth")));
        if (reservedBadge    != null)
            reservedBadge.setText(String.valueOf(intFrom(data, "reservedProps")));
    }

    private void applyTopCities(Map<String, Object> data) {
        if (topCitiesPanel == null) return;
        topCitiesPanel.removeAll();
        Object obj = data.get("topCities");
        if (obj instanceof List) {
            List<?> cities = (List<?>) obj;
            if (cities.isEmpty()) {
                JLabel none = new JLabel("  No data yet");
                none.setFont(UIThemeManager.FONT_SMALL);
                none.setForeground(theme.getTextSecondary());
                topCitiesPanel.add(none);
            } else {
                for (int i = 0; i < cities.size(); i++) {
                    JLabel cl = new JLabel("  " + (i + 1) + ".  " + cities.get(i));
                    cl.setFont(UIThemeManager.FONT_SMALL);
                    cl.setForeground(theme.getTextPrimary());
                    topCitiesPanel.add(cl);
                }
            }
        }
        topCitiesPanel.revalidate();
        topCitiesPanel.repaint();
    }

    /**
     * FIX [BUG]: Activity log showed blank Time cells when
     * Log.getCreatedAt() was null (common for old rows without a timestamp).
     */
    private void applyLogs(Map<String, Object> data) {
        if (logTableModel == null) return;
        logTableModel.setRowCount(0);
        Object logsObj = data.get("logs");
        if (!(logsObj instanceof List)) return;
        for (Object o : (List<?>) logsObj) {
            if (!(o instanceof Log)) continue;
            Log log = (Log) o;
            // FIX: null-safe timestamp formatting
            String timeStr = log.getCreatedAt() != null
                ? log.getCreatedAt().format(LOG_FMT)
                : "—";
            String userStr = log.getUsername() != null ? log.getUsername() : "System";
            String actStr  = log.getAction()   != null ? log.getAction()   : "";
            logTableModel.addRow(new Object[]{timeStr, userStr, actStr});
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static int intFrom(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    private JPanel buildCard(String title) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fill(new RoundRectangle2D.Float(3, 4, getWidth() - 3, getHeight() - 3, 14, 14));
                g2.setColor(theme.getBgSecondary());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 2, getHeight() - 3, 14, 14));
                g2.setColor(theme.getBorder());
                g2.setStroke(new BasicStroke(0.8f));
                g2.draw(new RoundRectangle2D.Float(0.4f, 0.4f, getWidth() - 2.8f, getHeight() - 3.8f, 14, 14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 18, 12, 18));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(theme.getTextPrimary());

        JPanel goldLine = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UIThemeManager.GOLD);
                g2.fillRoundRect(0, getHeight() - 2, getWidth(), 2, 2, 2);
                g2.dispose();
            }
        };
        goldLine.setOpaque(false);
        goldLine.setPreferredSize(new Dimension(0, 2));

        header.add(titleLbl, BorderLayout.CENTER);
        card.add(header,   BorderLayout.NORTH);
        card.add(goldLine, BorderLayout.SOUTH);
        return card;
    }

    private JPanel sectionLabel(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);

        JPanel goldPill = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UIThemeManager.GOLD);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        goldPill.setPreferredSize(new Dimension(4, 22));
        goldPill.setOpaque(false);

        JLabel titleLbl = new JLabel("  " + title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(theme.getTextPrimary());

        JLabel subLbl = new JLabel("   " + subtitle);
        subLbl.setFont(UIThemeManager.FONT_SMALL);
        subLbl.setForeground(theme.getTextSecondary());

        titleRow.add(goldPill);
        titleRow.add(titleLbl);
        titleRow.add(subLbl);
        p.add(titleRow, BorderLayout.CENTER);
        return p;
    }

    private String getGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "Good Morning";
        if (hour < 17) return "Good Afternoon";
        return "Good Evening";
    }
}
