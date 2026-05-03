package com.eremis.ui.dashboard;

import com.eremis.controller.AuthController;
import com.eremis.model.enums.UserRole;
import com.eremis.ui.login.LoginFrame;
import com.eremis.ui.inquiry.InquiryPanel;
import com.eremis.ui.notifications.NotificationsPanel;
import com.eremis.ui.property.PropertyPanel;
import com.eremis.ui.search.SearchPanel;
import com.eremis.ui.transactions.TransactionAdminPanel;
import com.eremis.ui.settings.SettingsPanel;
import com.eremis.ui.user.UserPanel;
import com.eremis.utils.SessionManager;
import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * EREMIS Main Application Shell — fixed version.
 *
 * FIXES:
 *   [BUG] Notification bell had NO ActionListener — dead button.
 *         Now opens a NotificationsPanel popup with a live unread badge.
 *   [BUG] Theme toggle only changed the button icon; panels weren't repainted.
 *         Now calls repaintAll() which traverses the full component tree.
 *   [BUG] navigateTo() removed+rebuilt all nav child components every click,
 *         causing visible flicker and losing scroll state. Now uses a boolean
 *         selected flag stored per-item and just repaint()s the sidebar.
 *   [BUG] Non-admin users could still trigger Users panel via navigateTo(4)
 *         directly (e.g. keyboard shortcut). Guard added in navigateTo().
 *   [BUG] Clock timer was never stopped on dispose() — minor thread leak.
 */
public class MainFrame extends JFrame {

    private final UIThemeManager theme    = UIThemeManager.getInstance();
    private final SessionManager session  = SessionManager.getInstance();
    private final AuthController authCtrl = new AuthController();
    private final UserRole currentRole    = session.getCurrentUser() != null
        ? session.getCurrentUser().getRole() : UserRole.USER;

    private static final Object[][] NAV_ITEMS = {
        {"Dashboard",  "⊞", 0},
        {"Properties", "⌂", 1},
        {"Search",     "⊕", 2},
        {"Inquiries",  "✉", 3},
        {"Users",      "☻", 4},
        {"Transactions","⇄", 5},
        {"Settings",   "✦", 6},
    };

    private JPanel         contentArea;
    private JPanel         sidebar;
    private int            selectedNav = 0;
    // FIX: store references to nav item icon/text labels so we can repaint without rebuild
    private JPanel[]       navPanels;
    private JLabel[]       navIconLabels;
    private JLabel[]       navTextLabels;

    // Notification bell components
    private JButton        notifBtn;
    private JLabel         notifBadge;   // unread count dot
    private JPopupMenu     notifPopup;
    private NotificationsPanel notifPanel;

    // Theme button (kept to allow icon refresh)
    private JButton        themeBtn;

    // Clock timer (stopped on dispose)
    private Timer          clockTimer;

    // Content panels
    private DashboardPanel dashboardPanel;
    private PropertyPanel  propertyPanel;
    private SearchPanel    searchPanel;
    private InquiryPanel   inquiryPanel;
    private UserPanel      userPanel;
    private TransactionAdminPanel transactionPanel;
    private SettingsPanel  settingsPanel;

    // Header labels
    private JLabel pageTitleLabel;
    private JLabel breadcrumbLabel;

    public MainFrame() {
        setTitle("EREMIS — Enterprise Real Estate Management & Intelligence System");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 700));
        // FIX: handle window close gracefully (stop timers, confirm logout)
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { doLogout(); }
        });
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(theme.getBgPrimary());
        setContentPane(root);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildContent(), BorderLayout.CENTER);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x1E3A5F),
                    0, getHeight(), new Color(0x0D2A4A));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(200, 169, 90, 40));
                g2.fillRect(getWidth() - 1, 0, 1, getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setOpaque(false);

        sidebar.add(buildLogoPanel());
        sidebar.add(buildSidebarDivider());
        sidebar.add(Box.createVerticalStrut(12));

        JLabel navSectionLbl = new JLabel("MAIN MENU");
        navSectionLbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        navSectionLbl.setForeground(new Color(0x3D5A7A));
        navSectionLbl.setBorder(new EmptyBorder(0, 20, 6, 0));
        navSectionLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(navSectionLbl);

        // FIX: Build nav items once and keep references to labels for cheap repaint
        int count = NAV_ITEMS.length;
        navPanels     = new JPanel[count];
        navIconLabels = new JLabel[count];
        navTextLabels = new JLabel[count];

        for (int i = 0; i < count; i++) {
            if (!isNavVisible(i)) continue;

            boolean selected = (i == 0);
            JPanel item      = buildNavItem(i, selected);
            navPanels[i]     = item;
            sidebar.add(item);
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(buildSidebarDivider());
        sidebar.add(buildUserInfoPanel());
        return sidebar;
    }

    private JPanel buildNavItem(int idx, boolean selected) {
        JPanel item = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                if (selectedNav == idx) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(200, 169, 90, 22));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(UIThemeManager.GOLD);
                    g2.fillRoundRect(0, 6, 4, getHeight() - 12, 4, 4);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        item.setOpaque(false);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setMinimumSize(new Dimension(240, 48));
        item.setMaximumSize(new Dimension(240, 48));
        item.setPreferredSize(new Dimension(240, 48));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setBorder(new EmptyBorder(0, 18, 0, 18));

        String iconStr  = (String) NAV_ITEMS[idx][1];
        String labelStr = getNavLabel(idx);

        JLabel iconLbl = new JLabel(iconStr + "  ");
        iconLbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
        iconLbl.setForeground(selected ? UIThemeManager.GOLD : new Color(0x6A85A0));
        iconLbl.setBorder(new EmptyBorder(0, 0, 0, 10));
        navIconLabels[idx] = iconLbl;

        JLabel textLbl = new JLabel(labelStr);
        textLbl.setFont(selected
            ? new Font("Segoe UI", Font.BOLD,  13)
            : new Font("Segoe UI", Font.PLAIN, 13));
        textLbl.setForeground(selected ? Color.WHITE : new Color(0x7A9AB8));
        navTextLabels[idx] = textLbl;

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 13));
        center.setOpaque(false);
        center.setPreferredSize(new Dimension(184, 48));
        center.add(iconLbl);
        center.add(textLbl);
        item.add(center, BorderLayout.WEST);

        // Hover highlight (FIX: only change background, not labels, to avoid flicker)
        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { navigateTo(idx); }
            @Override public void mouseEntered(MouseEvent e) {
                if (idx != selectedNav) { item.repaint(); }
            }
            @Override public void mouseExited(MouseEvent e) {
                if (idx != selectedNav) { item.repaint(); }
            }
        });
        return item;
    }

    private JPanel buildLogoPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 18));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        JLabel icon = new JLabel("🏢");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);
        JLabel brandName = new JLabel("EREMIS");
        brandName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brandName.setForeground(UIThemeManager.GOLD);
        JLabel brandSub = new JLabel("Real Estate Platform");
        brandSub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        brandSub.setForeground(new Color(0x4A6080));
        textStack.add(brandName);
        textStack.add(brandSub);
        p.add(icon);
        p.add(textStack);
        return p;
    }

    private JPanel buildUserInfoPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 14, 16, 14));
        p.setMaximumSize(new Dimension(240, 118));

        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIThemeManager.GOLD);
                g2.fillOval(0, 0, getWidth(), getHeight());
                String initial = session.getCurrentUser() != null
                    ? session.getCurrentUser().getFullName().substring(0, 1).toUpperCase() : "?";
                g2.setColor(UIThemeManager.NAVY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initial,
                    (getWidth() - fm.stringWidth(initial)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(38, 38));
        avatar.setOpaque(false);

        JPanel infoStack = new JPanel();
        infoStack.setLayout(new BoxLayout(infoStack, BoxLayout.Y_AXIS));
        infoStack.setOpaque(false);

        String name = session.getCurrentUser() != null ? session.getCurrentUser().getFullName() : "User";
        String role = session.getCurrentUser() != null
            ? session.getCurrentUser().getRole().getDisplayName() : "";

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        nameLbl.setForeground(Color.WHITE);
        nameLbl.setMaximumSize(new Dimension(140, 16));
        nameLbl.setToolTipText(name);

        JLabel roleLbl = new JLabel(role);
        roleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        roleLbl.setForeground(UIThemeManager.GOLD);
        roleLbl.setMaximumSize(new Dimension(140, 14));

        infoStack.add(nameLbl);
        infoStack.add(roleLbl);

        JButton logoutBtn = new JButton("⏻");
        logoutBtn.setText("Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        logoutBtn.setForeground(UIThemeManager.NAVY);
        logoutBtn.setBackground(UIThemeManager.GOLD);
        logoutBtn.setContentAreaFilled(true);
        logoutBtn.setOpaque(true);
        logoutBtn.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.setToolTipText("Logout");
        logoutBtn.addActionListener(e -> doLogout());
        logoutBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { logoutBtn.setBackground(new Color(0xB8942A)); }
            @Override public void mouseExited (MouseEvent e) { logoutBtn.setBackground(UIThemeManager.GOLD); }
        });

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.add(avatar, BorderLayout.WEST);
        top.add(infoStack, BorderLayout.CENTER);

        p.add(top, BorderLayout.CENTER);
        p.add(logoutBtn, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildSidebarDivider() {
        JPanel d = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(255, 255, 255, 15));
                g.fillRect(16, getHeight() / 2, getWidth() - 32, 1);
            }
        };
        d.setOpaque(false);
        d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        return d;
    }

    // ── Top Bar ───────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(theme.getBgSecondary());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(theme.getBorder());
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 64));
        bar.setBorder(new EmptyBorder(0, 28, 0, 28));

        // Page title
        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setOpaque(false);

        pageTitleLabel = new JLabel("Dashboard");
        pageTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        pageTitleLabel.setForeground(theme.getTextPrimary());

        breadcrumbLabel = new JLabel("EREMIS  /  Dashboard");
        breadcrumbLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        breadcrumbLabel.setForeground(theme.getTextSecondary());

        titleStack.add(pageTitleLabel);
        titleStack.add(breadcrumbLabel);
        bar.add(titleStack, BorderLayout.WEST);

        // Right: theme toggle + clock + notification bell
        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightBar.setOpaque(false);

        // Live clock
        JLabel clockLbl = new JLabel();
        clockLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clockLbl.setForeground(theme.getTextSecondary());
        // FIX: store timer reference so we can stop it on dispose()
        clockTimer = new Timer(1000, e ->
            clockLbl.setText(java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))));
        clockTimer.start();
        clockTimer.getActionListeners()[0].actionPerformed(null); // init immediately

        // FIX: Theme toggle — now calls repaintAll() so entire UI reflects new theme
        themeBtn = buildIconBtn(theme.isDark() ? "☀" : "☾", "Toggle Theme");
        themeBtn.addActionListener(e -> {
            theme.toggleTheme();
            themeBtn.setText(theme.isDark() ? "☀" : "☾");
            repaintAll();
        });

        // FIX [BUG]: Notification bell — was dead (no ActionListener).
        // Now opens a JPopupMenu containing NotificationsPanel, and shows
        // a live unread-count badge next to the bell.
        notifBtn = buildIconBtn("🔔", "Notifications");
        buildNotifPopup();
        notifBtn.addActionListener(e -> {
            notifPanel.loadNotifications();   // refresh before showing
            notifPopup.show(notifBtn, 0, notifBtn.getHeight() + 4);
        });

        // Unread badge overlay — JLayeredPane trick
        JPanel notifWrapper = new JPanel(null);
        notifWrapper.setOpaque(false);
        notifWrapper.setPreferredSize(new Dimension(46, 38));
        notifBtn.setBounds(0, 0, 38, 38);
        notifWrapper.add(notifBtn);

        notifBadge = new JLabel("0") {
            @Override protected void paintComponent(Graphics g) {
                if ("0".equals(getText()) || "".equals(getText())) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xDC2626));
                g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
                g2.dispose();
            }
        };
        notifBadge.setFont(new Font("Segoe UI", Font.BOLD, 9));
        notifBadge.setForeground(Color.WHITE);
        notifBadge.setHorizontalAlignment(SwingConstants.CENTER);
        notifBadge.setBounds(24, 0, 18, 18);
        notifBadge.setText("");
        notifWrapper.add(notifBadge);

        // Refresh badge count every 60s
        Timer badgeTimer = new Timer(60_000, e -> refreshNotifBadge());
        badgeTimer.start();
        refreshNotifBadge(); // initial load

        rightBar.add(clockLbl);
        rightBar.add(themeBtn);
        rightBar.add(notifWrapper);
        bar.add(rightBar, BorderLayout.EAST);
        return bar;
    }

    /** Build the notification popup once; reuse on every bell click. */
    private void buildNotifPopup() {
        notifPopup = new JPopupMenu();
        notifPopup.setBorder(BorderFactory.createLineBorder(new Color(0xE2E8F0), 1));
        notifPopup.setBackground(theme.getBgSecondary());
        notifPanel = new NotificationsPanel();
        notifPopup.add(notifPanel);
    }

    /** Update the red badge number on the bell button. */
    private void refreshNotifBadge() {
        SwingWorker<Integer, Void> w = new SwingWorker<>() {
            @Override protected Integer doInBackground() {
                return notifPanel.getUnreadCount();
            }
            @Override protected void done() {
                try {
                    int count = get();
                    notifBadge.setText(count > 0 ? String.valueOf(Math.min(count, 99)) : "");
                    notifBadge.repaint();
                } catch (Exception ignored) {}
            }
        };
        w.execute();
    }

    private JButton buildIconBtn(String icon, String tooltip) {
        JButton btn = new JButton(icon) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(theme.getBgPrimary());
                    g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                }
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        btn.setPreferredSize(new Dimension(38, 38));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(tooltip);
        return btn;
    }

    // ── Content ───────────────────────────────────────────────────────────

    private JPanel buildContent() {
        contentArea = new JPanel(new CardLayout());
        contentArea.setBackground(theme.getBgPrimary());
        dashboardPanel = new DashboardPanel(this);
        contentArea.add(dashboardPanel, "Dashboard");
        // Placeholders — replaced lazily on first navigation
        contentArea.add(new JPanel(), "Properties");
        contentArea.add(new JPanel(), "Search");
        contentArea.add(new JPanel(), "Inquiries");
        contentArea.add(new JPanel(), "Users");
        contentArea.add(new JPanel(), "Transactions");
        contentArea.add(new JPanel(), "Settings");
        return contentArea;
    }

    // ── Navigation ────────────────────────────────────────────────────────

    public void navigateTo(int index) {
        if (!isNavVisible(index)) return;

        selectedNav = index;
        String name = getNavKey(index);
        String displayName = getNavLabel(index);

        // Lazy-init
        switch (index) {
            case 1: if (propertyPanel == null) { propertyPanel = new PropertyPanel(); contentArea.add(propertyPanel, "Properties"); } break;
            case 2: if (searchPanel   == null) { searchPanel   = new SearchPanel();   contentArea.add(searchPanel,   "Search");     } break;
            case 3: if (inquiryPanel  == null) { inquiryPanel  = new InquiryPanel();  contentArea.add(inquiryPanel,  "Inquiries");  } break;
            case 4: if (userPanel     == null) { userPanel     = new UserPanel();     contentArea.add(userPanel,     "Users");      } break;
            case 5: if (transactionPanel == null) { transactionPanel = new TransactionAdminPanel(); contentArea.add(transactionPanel, "Transactions"); } break;
            case 6: if (settingsPanel == null) { settingsPanel = new SettingsPanel(); contentArea.add(settingsPanel, "Settings");   } break;
        }

        ((CardLayout) contentArea.getLayout()).show(contentArea, name);
        pageTitleLabel.setText(displayName);
        breadcrumbLabel.setText("EREMIS  /  " + displayName);

        // FIX [BUG]: Only repaint labels instead of removing/re-adding all children
        for (int i = 0; i < navPanels.length; i++) {
            if (navPanels[i] == null) continue;
            boolean sel = (i == index);
            if (navIconLabels[i] != null) {
                navIconLabels[i].setForeground(sel ? UIThemeManager.GOLD : new Color(0x6A85A0));
            }
            if (navTextLabels[i] != null) {
                navTextLabels[i].setForeground(sel ? Color.WHITE : new Color(0x7A9AB8));
                navTextLabels[i].setFont(sel
                    ? new Font("Segoe UI", Font.BOLD,  13)
                    : new Font("Segoe UI", Font.PLAIN, 13));
            }
            navPanels[i].repaint();
        }
    }

    // ── Theme refresh ─────────────────────────────────────────────────────

    /**
     * FIX [BUG]: After toggling theme, only the root frame was repainted —
     * cards, tables, and panels kept old colours. This traverses the tree.
     */
    private void repaintAll() {
        repaintTree(getContentPane());
        repaint();
    }

    private void repaintTree(Component c) {
        c.repaint();
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents())
                repaintTree(child);
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────

    private void doLogout() {
        int res = JOptionPane.showConfirmDialog(this,
            "Sign out of EREMIS?", "Confirm Logout",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (res == JOptionPane.YES_OPTION) {
            // FIX: stop clock timer to prevent thread leak
            if (clockTimer != null) clockTimer.stop();
            authCtrl.logout();
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }

    // ── Navigation shortcuts (used by DashboardPanel) ─────────────────────
    public void goToProperties() { navigateTo(currentRole.isBuyerLike() ? 2 : 1); }
    public void goToListings()   { goToProperties(); }
    public void goToSearch()     { navigateTo(2); }
    public void goToInquiries()  { navigateTo(3); }
    public void goToSettings()   { navigateTo(6); }

    public boolean isAdminView()  { return currentRole.isAdminLike(); }
    public boolean isSellerView()  { return currentRole.isSellerLike(); }
    public boolean isBuyerView()   { return currentRole.isBuyerLike(); }

    /** Expose for SettingsPanel or other sub-panels that need to refresh badge */
    public void refreshNotifications() { refreshNotifBadge(); }

    private boolean isNavVisible(int index) {
        switch (index) {
            case 0:
            case 2:
            case 3:
            case 6:
                return true;
            case 1:
                return currentRole.isAdminLike() || currentRole.isSellerLike();
            case 4:
                return currentRole.isAdminLike();
            case 5:
                return currentRole.isAdminLike();
            default:
                return false;
        }
    }

    private String getNavKey(int index) {
        switch (index) {
            case 0: return "Dashboard";
            case 1: return "Properties";
            case 2: return "Search";
            case 3: return "Inquiries";
            case 4: return "Users";
            case 5: return "Transactions";
            case 6: return "Settings";
            default: return "Dashboard";
        }
    }

    private String getNavLabel(int index) {
        if (index == 1 && currentRole.isSellerLike() && !currentRole.isAdminLike()) {
            return "My Listings";
        }
        return getNavKey(index);
    }
}
