package com.eremis.ui.login;

import com.eremis.controller.AuthController;
import com.eremis.service.AuthService.LoginResult;
import com.eremis.service.AuthService.PasswordResetResult;
import com.eremis.model.enums.UserRole;
import com.eremis.ui.components.RoundedButton;
import com.eremis.ui.components.ModernTextField;
import com.eremis.utils.UIThemeManager;
import com.eremis.utils.ValidationUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * EREMIS Premium Login Screen
 * Deep navy + gold luxury real-estate aesthetic.
 * Left: animated brand panel with property stats.
 * Right: clean, modern login form.
 */
public class LoginFrame extends JFrame {

    private final UIThemeManager theme    = UIThemeManager.getInstance();
    private final AuthController authCtrl = new AuthController();

    private ModernTextField usernameField;
    private JPasswordField  passwordField;
    private RoundedButton   loginBtn;
    private JLabel          errorLabel;
    private JLabel          attemptLabel;
    private int             failedAttempts = 0;

    // Dragging support (undecorated window)
    private Point dragStart;

    public LoginFrame() {
        setTitle("EREMIS — Login");
        setSize(980, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);
        // Rounded window corners
        setShape(new RoundRectangle2D.Double(0, 0, 980, 620, 20, 20));

        buildUI();
        addKeyListeners();
        bindWindowShortcuts();
        enableWindowDrag();
    }

    private void buildUI() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(UIThemeManager.NAVY);
        setContentPane(root);
        root.add(buildBrandPanel());
        root.add(buildFormPanel());
    }

    // ── LEFT: Premium brand panel ─────────────────────────────────────────
    private JPanel buildBrandPanel() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Deep navy gradient
                GradientPaint gp = new GradientPaint(
                    0, 0,             new Color(0x0A1628),
                    getWidth(), getHeight(), new Color(0x0D2444));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle geometric pattern — diagonal lines
                g2.setColor(new Color(255, 255, 255, 6));
                g2.setStroke(new BasicStroke(1f));
                for (int i = -getHeight(); i < getWidth() + getHeight(); i += 40) {
                    g2.drawLine(i, 0, i + getHeight(), getHeight());
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setLayout(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 44, 8, 44);

        // Gold horizontal rule at top
        JPanel goldBar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, new Color(200,169,90,0),
                    getWidth()/2, 0, UIThemeManager.GOLD);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        goldBar.setOpaque(false);
        goldBar.setPreferredSize(new Dimension(300, 3));
        gbc.gridy = 0; gbc.insets = new Insets(0, 44, 28, 44);
        panel.add(goldBar, gbc);

        // Logo / icon block
        JLabel logoIcon = new JLabel("🏢", SwingConstants.CENTER);
        logoIcon.setText("");
        logoIcon.setIcon(createBuildingLogoIcon());
        gbc.gridy = 1; gbc.insets = new Insets(0, 44, 12, 44);
        panel.add(logoIcon, gbc);

        // Brand name
        JLabel title = new JLabel("EREMIS", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(UIThemeManager.GOLD);
        gbc.gridy = 2; gbc.insets = new Insets(0, 44, 0, 44);
        panel.add(title, gbc);

        // Tagline
        JLabel tagline = new JLabel("<html><center>Enterprise Real Estate<br>Management & Intelligence</center></html>", SwingConstants.CENTER);
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tagline.setForeground(new Color(0x8FA8C8));
        gbc.gridy = 3; gbc.insets = new Insets(0, 44, 24, 44);
        panel.add(tagline, gbc);

        // Feature cards
        String[][] features = {
            {"🏠", "Full Property Lifecycle"},
            {"📊", "Analytics Dashboard"},
            {"🔐", "Role-Based Access"},
            {"💬", "CRM & Inquiry System"},
        };
        for (int i = 0; i < features.length; i++) {
            JPanel featureRow = buildFeatureRow(features[i][0], features[i][1]);
            gbc.gridy = 4 + i;
            gbc.insets = new Insets(4, 44, 4, 44);
            panel.add(featureRow, gbc);
        }

        // Bottom tagline
        JLabel ver = new JLabel("Trusted by real estate professionals", SwingConstants.CENTER);
        ver.setFont(UIThemeManager.FONT_SMALL);
        ver.setForeground(new Color(0x4A6080));
        gbc.gridy = 9; gbc.insets = new Insets(28, 44, 0, 44);
        panel.add(ver, gbc);

        return panel;
    }

    private JPanel buildFeatureRow(String icon, String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 8));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        JLabel ic = new JLabel(icon);
        ic.setText("");
        ic.setOpaque(true);
        ic.setBackground(UIThemeManager.GOLD);
        ic.setPreferredSize(new Dimension(8, 20));
        JLabel tx = new JLabel(text);
        tx.setFont(UIThemeManager.FONT_BODY);
        tx.setForeground(new Color(0xB8CFE8));
        row.add(ic);
        row.add(tx);
        return row;
    }

    // ── RIGHT: Premium form panel ─────────────────────────────────────────
    private Icon createBuildingLogoIcon() {
        return new Icon() {
            private static final int WIDTH = 220;
            private static final int HEIGHT = 176;

            @Override public int getIconWidth() {
                return WIDTH;
            }

            @Override public int getIconHeight() {
                return HEIGHT;
            }

            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                Color navy = new Color(0x071D33);
                Color navyLight = new Color(0x174469);
                Color gold = UIThemeManager.GOLD;
                Color goldDark = new Color(0xA77D22);

                g2.setColor(new Color(255, 255, 255, 245));
                g2.fillRoundRect(x + 8, y + 4, WIDTH - 16, HEIGHT - 10, 12, 12);
                g2.setColor(new Color(200, 169, 90, 45));
                g2.drawRoundRect(x + 8, y + 4, WIDTH - 16, HEIGHT - 10, 12, 12);

                int markLeft = x + 47;
                int markTop = y + 12;
                int baseY = y + 94;

                g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setPaint(new GradientPaint(markLeft, markTop + 8, gold, markLeft + 100, markTop + 72, goldDark));
                g2.drawArc(markLeft, markTop, 112, 104, 112, 218);

                g2.setPaint(new GradientPaint(x + 76, y + 38, navyLight, x + 108, baseY, navy));
                Polygon towerA = new Polygon(
                    new int[] {x + 76, x + 90, x + 90, x + 76},
                    new int[] {y + 76, y + 62, baseY, baseY},
                    4);
                Polygon towerB = new Polygon(
                    new int[] {x + 96, x + 113, x + 113, x + 96},
                    new int[] {y + 52, y + 34, baseY, baseY},
                    4);
                g2.fill(towerA);
                g2.fill(towerB);

                g2.setPaint(new GradientPaint(x + 116, y + 58, gold, x + 134, baseY, goldDark));
                Polygon towerC = new Polygon(
                    new int[] {x + 116, x + 134, x + 134, x + 116},
                    new int[] {y + 70, y + 54, baseY, baseY},
                    4);
                g2.fill(towerC);

                g2.setFont(new Font("Georgia", Font.BOLD, 86));
                FontMetrics fm = g2.getFontMetrics();
                String mark = "E";
                int eX = x + 126;
                int eY = y + 92;
                g2.setColor(new Color(0, 0, 0, 70));
                g2.drawString(mark, eX + 3, eY + 3);
                g2.setPaint(new GradientPaint(eX, y + 20, navyLight, eX + fm.stringWidth(mark), eY, navy));
                g2.drawString(mark, eX, eY);
                g2.setColor(new Color(255, 255, 255, 55));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(eX + 14, y + 29, eX + 62, y + 29);

                g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setPaint(new GradientPaint(x + 66, baseY, navy, x + 180, baseY, navyLight));
                g2.drawArc(x + 64, y + 85, 122, 28, 8, 164);

                g2.setColor(new Color(255, 255, 255, 32));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawLine(x + 99, y + 52, x + 112, y + 39);
                g2.drawLine(x + 119, y + 72, x + 133, y + 58);

                g2.setColor(navy);
                g2.setFont(new Font("Georgia", Font.PLAIN, 28));
                drawCenteredString(g2, "E R E M I S", x, y + 129, WIDTH);

                g2.setColor(goldDark);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(x + 44, y + 142, x + 72, y + 142);
                g2.drawLine(x + 148, y + 142, x + 176, y + 142);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                drawCenteredString(g2, "E S T A T E   A G E N C Y", x, y + 146, WIDTH);

                g2.setColor(navy);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 7));
                drawCenteredString(g2, "REAL ESTATE  •  REAL VALUE  •  REAL FUTURE", x, y + 163, WIDTH);
                g2.dispose();
            }
        };
    }

    private void drawCenteredString(Graphics2D g2, String text, int x, int baseline, int width) {
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (width - fm.stringWidth(text)) / 2;
        g2.drawString(text, textX, baseline);
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0xF8F9FB));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 50, 0, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(6, 0, 6, 0);

        // Window controls: hide with [-], close with X
        JButton hideBtn = new JButton("[-]");
        hideBtn.setVisible(true);
        hideBtn.setToolTipText("Hide window");
        hideBtn.getAccessibleContext().setAccessibleName("Hide window");
        hideBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        hideBtn.setForeground(new Color(0x9AA5B4));
        hideBtn.setContentAreaFilled(false);
        hideBtn.setBorderPainted(false);
        hideBtn.setFocusPainted(false);
        hideBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hideBtn.addActionListener(e -> setState(Frame.ICONIFIED));
        hideBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hideBtn.setForeground(Color.WHITE); }
            @Override public void mouseExited(MouseEvent e)  { hideBtn.setForeground(new Color(0x9AA5B4)); }
        });

        JButton closeBtn = new JButton("X");
        closeBtn.setVisible(true);
        closeBtn.setToolTipText("Close");
        closeBtn.getAccessibleContext().setAccessibleName("Close window");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.setForeground(new Color(0x9AA5B4));
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> System.exit(0));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setForeground(Color.RED); }
            @Override public void mouseExited(MouseEvent e)  { closeBtn.setForeground(new Color(0x9AA5B4)); }
        });
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        topBar.setOpaque(false);
        topBar.add(hideBtn);
        topBar.add(closeBtn);
        gbc.gridy = 0; gbc.insets = new Insets(16, 0, 0, 0);
        panel.add(topBar, gbc);

        // Gold accent line
        JPanel accentLine = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, UIThemeManager.GOLD,
                    getWidth(), 0, new Color(200,169,90,0));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        accentLine.setOpaque(false);
        accentLine.setPreferredSize(new Dimension(0, 3));
        gbc.gridy = 1; gbc.insets = new Insets(4, 0, 24, 0);
        panel.add(accentLine, gbc);

        // Welcome header
        JLabel welcome = new JLabel("Welcome Back");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcome.setForeground(UIThemeManager.NAVY);
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(welcome, gbc);

        JLabel hint = new JLabel("Sign in to your EREMIS account");
        hint.setFont(UIThemeManager.FONT_BODY);
        hint.setForeground(new Color(0x6B7B8D));
        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 28, 0);
        panel.add(hint, gbc);

        // Username
        JLabel userLbl = makeFormLabel("USERNAME");
        gbc.gridy = 4; gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(userLbl, gbc);

        usernameField = new ModernTextField("Enter your username", 340);
        gbc.gridy = 5; gbc.insets = new Insets(0, 0, 18, 0);
        panel.add(usernameField, gbc);

        // Password
        JLabel passLbl = makeFormLabel("PASSWORD");
        gbc.gridy = 6; gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(passLbl, gbc);

        passwordField = buildPasswordField();
        gbc.gridy = 7; gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(passwordField, gbc);

        // Error labels
        errorLabel = new JLabel("");
        errorLabel.setFont(UIThemeManager.FONT_SMALL);
        errorLabel.setForeground(new Color(0xDC2626));
        errorLabel.setVisible(false);
        gbc.gridy = 8; gbc.insets = new Insets(0, 0, 2, 0);
        panel.add(errorLabel, gbc);

        attemptLabel = new JLabel("");
        attemptLabel.setFont(UIThemeManager.FONT_SMALL);
        attemptLabel.setForeground(new Color(0xD97706));
        attemptLabel.setVisible(false);
        gbc.gridy = 9; gbc.insets = new Insets(0, 0, 16, 0);
        panel.add(attemptLabel, gbc);

        // Login button — full width gold
        loginBtn = new RoundedButton("Sign In →");
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginBtn.setPreferredSize(new Dimension(340, 46));
        loginBtn.addActionListener(e -> doLogin());
        gbc.gridy = 10; gbc.insets = new Insets(4, 0, 16, 0);
        panel.add(loginBtn, gbc);

        // Divider
        JPanel divider = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0xE2E8F0));
                g2.fillRect(0, getHeight()/2, getWidth(), 1);
                g2.dispose();
            }
        };
        divider.setOpaque(false);
        divider.setPreferredSize(new Dimension(0, 12));
        gbc.gridy = 11; gbc.insets = new Insets(0, 0, 12, 0);
        panel.add(divider, gbc);

        // Create account actions
        JPanel createRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        createRow.setOpaque(false);
        JLabel noAcc = new JLabel("New to EREMIS?");
        noAcc.setFont(UIThemeManager.FONT_SMALL);
        noAcc.setForeground(new Color(0x6B7B8D));
        JButton sellerLink = new JButton("Seller Signup");
        sellerLink.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sellerLink.setForeground(UIThemeManager.GOLD);
        sellerLink.setContentAreaFilled(false);
        sellerLink.setBorderPainted(false);
        sellerLink.setFocusPainted(false);
        sellerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sellerLink.addActionListener(e -> openCreateAccountDialog(UserRole.SELLER));
        sellerLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                sellerLink.setForeground(UIThemeManager.NAVY);
            }
            @Override public void mouseExited(MouseEvent e) {
                sellerLink.setForeground(UIThemeManager.GOLD);
            }
        });

        JLabel sep = new JLabel("|");
        sep.setFont(UIThemeManager.FONT_SMALL);
        sep.setForeground(new Color(0xB0BEC5));

        JButton buyerLink = new JButton("Buyer Signup");
        buyerLink.setFont(new Font("Segoe UI", Font.BOLD, 11));
        buyerLink.setForeground(UIThemeManager.GOLD);
        buyerLink.setContentAreaFilled(false);
        buyerLink.setBorderPainted(false);
        buyerLink.setFocusPainted(false);
        buyerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        buyerLink.addActionListener(e -> openCreateAccountDialog(UserRole.USER));
        buyerLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                buyerLink.setForeground(UIThemeManager.NAVY);
            }
            @Override public void mouseExited(MouseEvent e) {
                buyerLink.setForeground(UIThemeManager.GOLD);
            }
        });
        createRow.add(noAcc);
        createRow.add(sellerLink);
        createRow.add(sep);
        createRow.add(buyerLink);
        gbc.gridy = 12; gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(createRow, gbc);

        JPanel forgotRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        forgotRow.setOpaque(false);
        JButton forgotLink = new JButton("Forgot Password?");
        forgotLink.setFont(new Font("Segoe UI", Font.BOLD, 11));
        forgotLink.setForeground(new Color(0x6B7B8D));
        forgotLink.setContentAreaFilled(false);
        forgotLink.setBorderPainted(false);
        forgotLink.setFocusPainted(false);
        forgotLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotLink.addActionListener(e -> openForgotPasswordDialog());
        forgotLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                forgotLink.setForeground(UIThemeManager.NAVY);
            }
            @Override public void mouseExited(MouseEvent e) {
                forgotLink.setForeground(new Color(0x6B7B8D));
            }
        });
        forgotRow.add(forgotLink);
        gbc.gridy = 13; gbc.insets = new Insets(6, 0, 0, 0);
        panel.add(forgotRow, gbc);

        // Version footer
        JLabel version = new JLabel("v1.0.0  •  © 2025 EREMIS  •  All rights reserved", SwingConstants.CENTER);
        version.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        version.setForeground(new Color(0xC0C8D0));
        gbc.gridy = 14; gbc.insets = new Insets(16, 0, 0, 0);
        panel.add(version, gbc);

        return panel;
    }

    private JPasswordField buildPasswordField() {
        return buildPasswordField(340);
    }

    private JPasswordField buildPasswordField(int preferredWidth) {
        JPasswordField pf = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                super.paintComponent(g);
                boolean focused = hasFocus();
                g2.setColor(focused ? UIThemeManager.GOLD : new Color(0xE2E8F0));
                g2.setStroke(new BasicStroke(focused ? 2f : 1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, 10, 10));
                g2.dispose();
            }
        };
        pf.setFont(UIThemeManager.FONT_BODY);
        pf.setPreferredSize(new Dimension(preferredWidth, 44));
        pf.setBackground(Color.WHITE);
        pf.setForeground(UIThemeManager.NAVY);
        pf.setCaretColor(UIThemeManager.GOLD);
        pf.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 70));
        pf.setOpaque(false);
        pf.setLayout(new BorderLayout());
        pf.add(createPasswordRevealButton(pf), BorderLayout.EAST);
        return pf;
    }

    private JButton createPasswordRevealButton(JPasswordField field) {
        JButton revealBtn = new JButton("SHOW");
        revealBtn.setFont(new Font("Segoe UI", Font.BOLD, 9));
        revealBtn.setForeground(new Color(0x6B7B8D));
        revealBtn.setContentAreaFilled(false);
        revealBtn.setBorderPainted(false);
        revealBtn.setFocusPainted(false);
        revealBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        revealBtn.setMargin(new Insets(0, 8, 0, 10));
        revealBtn.setToolTipText("Hold to show password");
        revealBtn.getAccessibleContext().setAccessibleName("Hold to show password");

        char hiddenChar = field.getEchoChar();
        MouseAdapter revealHandler = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                field.setEchoChar((char) 0);
            }

            @Override public void mouseReleased(MouseEvent e) {
                field.setEchoChar(hiddenChar);
            }

            @Override public void mouseExited(MouseEvent e) {
                field.setEchoChar(hiddenChar);
            }
        };
        revealBtn.addMouseListener(revealHandler);
        return revealBtn;
    }

    private JLabel makeFormLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(new Color(0x6B7B8D));
        return l;
    }

    // ── Login logic ───────────────────────────────────────────────────────
    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText("Signing in…");

        SwingWorker<LoginResult, Void> worker = new SwingWorker<>() {
            @Override protected LoginResult doInBackground() {
                return authCtrl.login(username, password);
            }
            @Override protected void done() {
                try {
                    handleLoginResult(get());
                } catch (Exception ex) {
                    showError("Unexpected error. Please try again.");
                } finally {
                    loginBtn.setEnabled(true);
                    loginBtn.setText("Sign In →");
                }
            }
        };
        worker.execute();
    }

    private void handleLoginResult(LoginResult result) {
        if (result == LoginResult.SUCCESS) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                com.eremis.ui.dashboard.MainFrame main = new com.eremis.ui.dashboard.MainFrame();
                main.setVisible(true);
            });
        } else {
            if (result == LoginResult.INVALID_CREDENTIALS) failedAttempts++;
            showError(authCtrl.getLoginMessage(result));
            if (failedAttempts > 0) {
                attemptLabel.setText("⚠  Failed attempts: " + failedAttempts);
                attemptLabel.setVisible(true);
            }
            passwordField.setText("");
        }
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        shakeWindow();
    }

    private void shakeWindow() {
        Point orig = getLocation();
        Timer timer = new Timer(25, null);
        int[] count = {0};
        timer.addActionListener(e -> {
            count[0]++;
            int dx = (count[0] % 2 == 0) ? 8 : -8;
            setLocation(orig.x + dx, orig.y);
            if (count[0] > 8) { setLocation(orig); timer.stop(); }
        });
        timer.start();
    }

    private void addKeyListeners() {
        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        };
        usernameField.getField().addKeyListener(enter);
        passwordField.addKeyListener(enter);
    }

    private void bindWindowShortcuts() {
        JRootPane rootPane = getRootPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeWindow");
        rootPane.getActionMap().put("closeWindow", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void enableWindowDrag() {
        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { dragStart = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    Point loc = getLocation();
                    setLocation(loc.x + e.getX() - dragStart.x, loc.y + e.getY() - dragStart.y);
                }
            }
        };
        addMouseListener(drag);
        addMouseMotionListener(drag);
    }

    private void openCreateAccountDialog(UserRole defaultRole) {
        CreateAccountDialog dialog = new CreateAccountDialog(this, defaultRole);
        dialog.setVisible(true);
        if (dialog.isSuccess()) {
            usernameField.setText("");
            passwordField.setText("");
            errorLabel.setVisible(false);
            JOptionPane.showMessageDialog(this,
                "✅  Account created! Please sign in with your new credentials.",
                "Account Created", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void openForgotPasswordDialog() {
        final JDialog dialog = new JDialog(this, "Reset Password", true);
        dialog.setUndecorated(true);
        dialog.setSize(460, 585);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        // Ensure shape matches actual dialog size so bottom controls remain visible
        dialog.setShape(new RoundRectangle2D.Double(0, 0, 460, 585, 14, 14));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createLineBorder(new Color(0xE2E8F0), 1));
        dialog.setContentPane(root);

        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, UIThemeManager.NAVY,
                    getWidth(), getHeight(), new Color(0x0D2444));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UIThemeManager.GOLD);
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(18, 22, 12, 14));
        header.setPreferredSize(new Dimension(0, 86));

        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setOpaque(false);

        JLabel title = new JLabel("Reset Password");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Enter your username and email to reset your password");
        subtitle.setFont(UIThemeManager.FONT_SMALL);
        subtitle.setForeground(new Color(0x8FA8C8));

        titleStack.add(title);
        titleStack.add(Box.createVerticalStrut(4));
        titleStack.add(subtitle);

        JButton hideBtn = new JButton("[-]");
        hideBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        hideBtn.setForeground(new Color(0x8FA8C8));
        hideBtn.setContentAreaFilled(false);
        hideBtn.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        hideBtn.setFocusPainted(false);
        hideBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hideBtn.addActionListener(e -> dialog.setVisible(false));
        hideBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hideBtn.setForeground(Color.WHITE); }
            @Override public void mouseExited(MouseEvent e)  { hideBtn.setForeground(new Color(0x8FA8C8)); }
        });

        JButton closeBtn = new JButton("X");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.setForeground(new Color(0x8FA8C8));
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dialog.dispose());
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setForeground(Color.WHITE); }
            @Override public void mouseExited(MouseEvent e)  { closeBtn.setForeground(new Color(0x8FA8C8)); }
        });

        JPanel closeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        closeWrap.setOpaque(false);
        closeWrap.add(hideBtn);
        closeWrap.add(closeBtn);

        header.add(titleStack, BorderLayout.CENTER);
        header.add(closeWrap, BorderLayout.EAST);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(22, 28, 20, 28));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        ModernTextField usernameInput = new ModernTextField("Enter your username", 400);
        usernameInput.setText(usernameField.getText().trim());
        ModernTextField emailInput = new ModernTextField("Enter your account email", 400);
        JPasswordField newPasswordInput = buildPasswordField(400);
        JPasswordField confirmPasswordInput = buildPasswordField(400);

        JLabel msgLabel = new JLabel(" ");
        msgLabel.setFont(UIThemeManager.FONT_SMALL);
        msgLabel.setForeground(theme.getDanger());

        int row = 0;
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 6, 0);
        form.add(makeFormLabel("USERNAME"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(usernameInput, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 6, 0);
        form.add(makeFormLabel("EMAIL"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(emailInput, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 6, 0);
        form.add(makeFormLabel("NEW PASSWORD"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(newPasswordInput, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 6, 0);
        form.add(makeFormLabel("CONFIRM PASSWORD"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(confirmPasswordInput, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(msgLabel, gbc);

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRow.setOpaque(false);
        RoundedButton cancelBtn = RoundedButton.secondary("Cancel");
        cancelBtn.setPreferredSize(new Dimension(0, 44));
        cancelBtn.addActionListener(e -> dialog.dispose());
        RoundedButton resetBtn = new RoundedButton("Reset Password");
        resetBtn.setPreferredSize(new Dimension(0, 44));
        btnRow.add(cancelBtn);
        btnRow.add(resetBtn);

        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 0, 0);
        form.add(btnRow, gbc);

        resetBtn.addActionListener(e -> {
            String username = usernameInput.getText().trim();
            String email = emailInput.getText().trim();
            String newPassword = new String(newPasswordInput.getPassword());
            String confirmPassword = new String(confirmPasswordInput.getPassword());

            if (username.isEmpty() || email.isEmpty()) {
                msgLabel.setForeground(theme.getDanger());
                msgLabel.setText("Please enter username and email.");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                msgLabel.setForeground(theme.getDanger());
                msgLabel.setText("Passwords do not match.");
                return;
            }

            if (!ValidationUtil.isValidPassword(newPassword)) {
                msgLabel.setForeground(theme.getDanger());
                msgLabel.setText("Password must be at least 8 characters and contain a digit.");
                return;
            }

            PasswordResetResult result = authCtrl.resetPassword(username, email, newPassword);
            if (result == PasswordResetResult.SUCCESS) {
                failedAttempts = 0;
                errorLabel.setVisible(false);
                attemptLabel.setVisible(false);
                passwordField.setText("");
                usernameField.setText(username);
                msgLabel.setForeground(theme.getSuccess());
                msgLabel.setText(authCtrl.getPasswordResetMessage(result));
                Timer t = new Timer(900, ev -> dialog.dispose());
                t.setRepeats(false);
                t.start();
            } else {
                msgLabel.setForeground(theme.getDanger());
                msgLabel.setText(authCtrl.getPasswordResetMessage(result));
            }
        });

        root.add(header, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        dialog.setVisible(true);
    }
}
