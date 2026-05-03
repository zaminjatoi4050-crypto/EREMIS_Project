package com.eremis.ui.login;

import com.eremis.config.AppConfig;
import com.eremis.controller.AuthController;
import com.eremis.model.User;
import com.eremis.model.enums.UserRole;
import com.eremis.service.AuthService.EmailVerificationResult;
import com.eremis.service.AuthService.RegistrationOtpResult;
import com.eremis.service.EmailService;
import com.eremis.service.UserService;
import com.eremis.ui.components.RoundedButton;
import com.eremis.ui.components.ModernTextField;
import com.eremis.utils.UIThemeManager;
import com.eremis.utils.ValidationUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EREMIS Premium Create Account Dialog
 * Collects: Full Name, Email, Phone, City, Username, Password, Role.
 * Professionally styled with gold accent, navy header, real-estate context.
 */
public class CreateAccountDialog extends JDialog {

    private static final Logger LOG = Logger.getLogger(CreateAccountDialog.class.getName());

    private final AuthController authCtrl = new AuthController();
    private final AppConfig      config = AppConfig.getInstance();
    private final EmailService   emailService = new EmailService();
    private final UserService    userService = new UserService();
    private final UserRole      defaultRole;
    private final UIThemeManager theme = UIThemeManager.getInstance();

    // Form fields
    private ModernTextField fullNameField;
    private ModernTextField emailField;
    private ModernTextField otpField;
    private ModernTextField phoneField;
    private ModernTextField cityField;
    private ModernTextField usernameField;
    private JPasswordField  passwordField;
    private JPasswordField  confirmPasswordField;
    private JComboBox<UserRole> roleCombo;
    private JLabel          errorLabel;
    private RoundedButton   createBtn;
    private RoundedButton   sendOtpBtn;
    private RoundedButton   verifyOtpBtn;

    // Password strength
    private JPanel strengthBar;
    private JLabel strengthLabel;

    private boolean success = false;
    private boolean emailVerified = false;
    private String verifiedEmail;

    public CreateAccountDialog(JFrame parent) {
        this(parent, UserRole.USER);
    }

    public CreateAccountDialog(JFrame parent, UserRole defaultRole) {
        super(parent, "Create Account", true);
        this.defaultRole = defaultRole != null ? defaultRole : UserRole.USER;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(560, 760);
        // Center on screen so the wider dialog stays visually balanced.
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);
        setShape(new RoundRectangle2D.Double(0, 0, 560, 760, 16, 16));

        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(theme.getBgPrimary());
        root.setBorder(BorderFactory.createLineBorder(theme.getBorder(), 1));
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildForm(),   BorderLayout.CENTER);
    }

    // ── Top navy header ───────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0,0, UIThemeManager.NAVY,
                    getWidth(), getHeight(), UIThemeManager.NAVY_MID);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Gold line at bottom
                g2.setColor(UIThemeManager.GOLD);
                g2.fillRect(0, getHeight()-3, getWidth(), 3);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 82));
        header.setBorder(new EmptyBorder(16, 24, 12, 14));

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);

        JLabel icon = new JLabel("Create Account");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 17));
        icon.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Create a Seller or Buyer account");
        sub.setFont(UIThemeManager.FONT_SMALL);
        sub.setForeground(theme.getTextSecondary());

        textCol.add(icon);
        textCol.add(Box.createVerticalStrut(4));
        textCol.add(sub);

        header.add(textCol, BorderLayout.CENTER);

        // Window controls: hide with [-], close with X
        // Simpler, cleaner window controls (text-only, professional)
        JLabel hideLbl = new JLabel("[-]");
        hideLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        hideLbl.setForeground(new Color(0x8FA8C8));
        hideLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hideLbl.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        hideLbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { setVisible(false); }
            @Override public void mouseEntered(MouseEvent e) { hideLbl.setForeground(Color.WHITE); }
            @Override public void mouseExited(MouseEvent e)  { hideLbl.setForeground(new Color(0x8FA8C8)); }
        });

        JLabel closeLbl = new JLabel("X");
        closeLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeLbl.setForeground(new Color(0x8FA8C8));
        closeLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeLbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        closeLbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { dispose(); }
            @Override public void mouseEntered(MouseEvent e) { closeLbl.setForeground(Color.WHITE); }
            @Override public void mouseExited(MouseEvent e)  { closeLbl.setForeground(new Color(0x8FA8C8)); }
        });

        JPanel closePnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        closePnl.setOpaque(false);
        closePnl.add(hideLbl);
        closePnl.add(closeLbl);
        header.add(closePnl, BorderLayout.EAST);

        return header;
    }

    // ── Scrollable form body ──────────────────────────────────────────────
    private JScrollPane buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(theme.getBgPrimary());
        form.setBorder(new EmptyBorder(18, 24, 18, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1; gbc.insets = new Insets(4, 0, 4, 0);

        int row = 0;

        // Section: Personal Information
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 8, 0);
        form.add(sectionHeader("Personal Information"), gbc);

        // Two-column row: Full Name + Phone
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 3, 0);
        form.add(makeLabel("FULL NAME"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 8, 0);
        fullNameField = new ModernTextField("e.g. Ahmad Ali", 500);
        form.add(fullNameField, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 3, 0);
        form.add(makeLabel("EMAIL ADDRESS"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 8, 0);
        emailField = new ModernTextField("example@company.com", 500);
        form.add(emailField, gbc);
        emailField.getField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { resetEmailVerificationState(); }
            public void removeUpdate(DocumentEvent e) { resetEmailVerificationState(); }
            public void changedUpdate(DocumentEvent e) { resetEmailVerificationState(); }
        });

        if (isRegistrationOtpEnabled()) {
            gbc.gridy = row++; gbc.insets = new Insets(0, 0, 3, 0);
            form.add(makeLabel("EMAIL OTP"), gbc);
            gbc.gridy = row++; gbc.insets = new Insets(0, 0, 8, 0);
            otpField = new ModernTextField("Enter 6-digit OTP", 500);
            form.add(otpField, gbc);

            JPanel otpButtonRow = new JPanel(new GridLayout(1, 2, 10, 0));
            otpButtonRow.setOpaque(false);
            sendOtpBtn = RoundedButton.secondary("Send OTP");
            sendOtpBtn.setPreferredSize(new Dimension(0, 38));
            sendOtpBtn.addActionListener(e -> sendRegistrationOtp());
            verifyOtpBtn = RoundedButton.secondary("Verify OTP");
            verifyOtpBtn.setPreferredSize(new Dimension(0, 38));
            verifyOtpBtn.addActionListener(e -> verifyRegistrationOtp());
            otpButtonRow.add(sendOtpBtn);
            otpButtonRow.add(verifyOtpBtn);
            gbc.gridy = row++; gbc.insets = new Insets(0, 0, 10, 0);
            form.add(otpButtonRow, gbc);
        }

        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 3, 0);
        form.add(makeLabel("PHONE NUMBER"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 8, 0);
        phoneField = new ModernTextField("e.g. 0300-1234567", 500);
        form.add(phoneField, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 3, 0);
        form.add(makeLabel("CITY / LOCATION"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 10, 0);
        cityField = new ModernTextField("e.g. Karachi, Lahore, Islamabad", 500);
        form.add(cityField, gbc);

        // Section: Account Credentials
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 8, 0);
        form.add(sectionHeader("Account Credentials"), gbc);

        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 3, 0);
        form.add(makeLabel("USERNAME"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 8, 0);
        usernameField = new ModernTextField("Choose a unique username", 500);
        form.add(usernameField, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 3, 0);
        form.add(makeLabel("ACCOUNT ROLE"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 8, 0);
        roleCombo = buildRoleCombo();
        form.add(roleCombo, gbc);

        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 3, 0);
        form.add(makeLabel("PASSWORD"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 3, 0);
        passwordField = buildPasswordField("Enter a strong password");
        form.add(passwordField, gbc);

        // Password strength bar
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 8, 0);
        form.add(buildStrengthPanel(), gbc);
        passwordField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });

        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 3, 0);
        form.add(makeLabel("CONFIRM PASSWORD"), gbc);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 10, 0);
        confirmPasswordField = buildPasswordField("Re-enter your password");
        form.add(confirmPasswordField, gbc);

        // Error label
        errorLabel = new JLabel("");
        errorLabel.setFont(UIThemeManager.FONT_SMALL);
        errorLabel.setForeground(theme.getDanger());
        errorLabel.setVisible(false);
        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 6, 0);
        form.add(errorLabel, gbc);

        // Buttons
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        btnPanel.setOpaque(false);

        RoundedButton cancelBtn = RoundedButton.secondary("Cancel");
        cancelBtn.setPreferredSize(new Dimension(0, 46));
        cancelBtn.addActionListener(e -> dispose());
        btnPanel.add(cancelBtn);

        createBtn = new RoundedButton("Create Account");
        createBtn.setPreferredSize(new Dimension(0, 42));
        createBtn.addActionListener(e -> doCreateAccount());
        btnPanel.add(createBtn);

        gbc.gridy = row++; gbc.insets = new Insets(0, 0, 0, 0);
        form.add(btnPanel, gbc);

        // Terms note
        JLabel terms = new JLabel("<html><center>By creating an account you agree to the<br>EREMIS Terms of Service and Privacy Policy.</center></html>");
        terms.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        terms.setForeground(theme.getTextSecondary());
        terms.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = row++;
        gbc.insets = new Insets(8, 0, 0, 0);
        form.add(terms, gbc);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(10);
        scroll.getViewport().setBackground(theme.getBgPrimary());
        scroll.setBackground(theme.getBgPrimary());
        return scroll;
    }

    private JPanel sectionHeader(String text) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(theme.getTextPrimary());
        JPanel line = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(theme.getBorder());
                g.fillRect(0, getHeight()/2, getWidth(), 1);
            }
        };
        line.setOpaque(false);
        panel.add(lbl, BorderLayout.WEST);
        panel.add(line, BorderLayout.CENTER);
        return panel;
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(theme.getTextSecondary());
        return l;
    }

    private JComboBox<UserRole> buildRoleCombo() {
        JComboBox<UserRole> combo = new JComboBox<>(new UserRole[] {
            UserRole.SELLER,
            UserRole.USER
        });
        combo.setSelectedItem(defaultRole);
        combo.setFont(UIThemeManager.FONT_BODY);
        combo.setPreferredSize(new Dimension(500, 40));
        combo.setBackground(theme.getBgSecondary());
        combo.setForeground(theme.getTextPrimary());
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme.getBorder(), 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        return combo;
    }

    private JPasswordField buildPasswordField(String placeholder) {
        JPasswordField pf = new JPasswordField();
        pf.setFont(UIThemeManager.FONT_BODY);
        pf.setPreferredSize(new Dimension(500, 40));
        pf.setBackground(theme.getBgSecondary());
        pf.setForeground(theme.getTextPrimary());
        pf.setCaretColor(theme.getAccent());
        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme.getBorder(), 1, true),
            BorderFactory.createEmptyBorder(4, 14, 4, 14)));
        return pf;
    }

    private JPanel buildStrengthPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);

        strengthBar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0xF0F0F0));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                int len = getPassword().length;
                Color col = len < 6 ? new Color(0xDC2626) :
                            len < 10 ? new Color(0xD97706) : new Color(0x2E7D52);
                float pct = Math.min(1f, len / 12f);
                g2.setColor(col);
                g2.fillRoundRect(0, 0, (int)(getWidth()*pct), getHeight(), 4, 4);
                g2.dispose();
            }
        };
        strengthBar.setPreferredSize(new Dimension(0, 5));
        strengthBar.setOpaque(false);

        strengthLabel = new JLabel("Password strength");
        strengthLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        strengthLabel.setForeground(theme.getTextSecondary());

        panel.add(strengthBar,   BorderLayout.CENTER);
        panel.add(strengthLabel, BorderLayout.EAST);
        return panel;
    }

    private char[] getPassword() {
        return passwordField != null ? passwordField.getPassword() : new char[0];
    }

    private void updateStrength() {
        int len = getPassword().length;
        String password = new String(getPassword());
        String msg;
        Color col;
        if (len == 0) {
            msg = "Enter password to check strength";
            col = new Color(0xB0BEC5);
        } else if (len < 8) {
            msg = "Too short — minimum 8 characters";
            col = new Color(0xDC2626);
        } else if (!password.matches(".*[0-9].*")) {
            msg = "Add at least one digit";
            col = new Color(0xDC2626);
        } else if (len < 10) {
            msg = "Moderate — consider using more characters";
            col = new Color(0xD97706);
        } else {
            msg = "Strong password ✓";
            col = new Color(0x2E7D52);
        }
        strengthLabel.setText(msg);
        strengthLabel.setForeground(col);
        strengthBar.repaint();
    }

    // ── Account creation logic ────────────────────────────────────────────
    private void sendRegistrationOtp() {
        String email = emailField.getText().trim();
        if (!ValidationUtil.isValidEmail(email)) {
            showError("Please enter a valid email address before requesting OTP.");
            return;
        }

        emailVerified = false;
        verifiedEmail = null;
        verifyOtpBtn.setText("Verify OTP");
        sendOtpBtn.setEnabled(false);
        verifyOtpBtn.setEnabled(false);
        sendOtpBtn.setText("Sending...");
        showInfo("Sending OTP...");

        SwingWorker<RegistrationOtpResult, Void> worker = new SwingWorker<>() {
            @Override protected RegistrationOtpResult doInBackground() {
                return authCtrl.sendRegistrationEmailOtp(email);
            }

            @Override protected void done() {
                try {
                    RegistrationOtpResult result = get();
                    if (result == RegistrationOtpResult.SUCCESS) {
                        showInfo(authCtrl.getRegistrationOtpMessage(result));
                    } else {
                        showError(authCtrl.getRegistrationOtpMessage(result));
                    }
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Could not send registration OTP", ex);
                    showError("Could not send OTP. Please try again.");
                } finally {
                    sendOtpBtn.setEnabled(true);
                    verifyOtpBtn.setEnabled(true);
                    sendOtpBtn.setText("Send OTP");
                }
            }
        };
        worker.execute();
    }

    private void verifyRegistrationOtp() {
        String email = emailField.getText().trim();
        String otp = otpField.getText().trim();
        EmailVerificationResult result = authCtrl.verifyRegistrationEmailOtp(email, otp);
        if (result == EmailVerificationResult.SUCCESS) {
            emailVerified = true;
            verifiedEmail = normalizedEmail(email);
            verifyOtpBtn.setText("Verified");
            showInfo(authCtrl.getEmailVerificationMessage(result));
        } else {
            showError(authCtrl.getEmailVerificationMessage(result));
        }
    }

    private void resetEmailVerificationState() {
        String currentEmail = normalizedEmail(emailField != null ? emailField.getText() : null);
        if (emailVerified && currentEmail.equals(verifiedEmail)) {
            return;
        }
        emailVerified = false;
        verifiedEmail = null;
        if (verifyOtpBtn != null) {
            verifyOtpBtn.setText("Verify OTP");
        }
    }

    private boolean isCurrentEmailVerified(String email) {
        return emailVerified && normalizedEmail(email).equals(verifiedEmail);
    }

    private String normalizedEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private void doCreateAccount() {
        String fullName   = fullNameField.getText().trim();
        String email      = emailField.getText().trim();
        String phone      = phoneField.getText().trim();
        String city       = cityField.getText().trim();
        String username   = usernameField.getText().trim();
        String password   = new String(passwordField.getPassword());
        String confirm    = new String(confirmPasswordField.getPassword());
        UserRole role     = (UserRole) roleCombo.getSelectedItem();

        errorLabel.setVisible(false);

        if (role == null) {
            showError("Please choose an account type.");
            return;
        }

        if (fullName.isEmpty() || email.isEmpty() || username.isEmpty()
                || password.isEmpty() || confirm.isEmpty()) {
            showError("Full Name, Email, Username and Password are required.");
            return;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            showError("Please enter a valid email address.");
            return;
        }
        if (isRegistrationOtpEnabled() && !isCurrentEmailVerified(email)) {
            showError("Please verify your email OTP before creating an account.");
            return;
        }
        if (!ValidationUtil.isValidPhone(phone)) {
            showError("Please enter a valid phone number.");
            return;
        }
        if (!ValidationUtil.isValidPassword(password)) {
            showError("Password must be at least 8 characters and contain a digit.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            confirmPasswordField.setText("");
            return;
        }

        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setPasswordHash(password);
        newUser.setRole(role);
        newUser.setActive(true);
        newUser.setPhone(phone);
        newUser.setCity(city);

        createBtn.setEnabled(false);
        createBtn.setText("Creating…");

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override protected User doInBackground() {
                return userService.createUser(newUser);
            }
            @Override protected void done() {
                try {
                    User result = get();
                    if (result != null) {
                        success = true;
                        dispose();
                    } else {
                        showError("Username or email may already exist. Please try another.");
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    LOG.log(Level.SEVERE, "Error creating user account", ex);
                    String message = cause.getMessage();
                    if (message != null && !message.isBlank()) {
                        showError(message);
                    } else {
                        showError("Account creation failed. Please check your input and try again.");
                    }
                } finally {
                    createBtn.setEnabled(true);
                    createBtn.setText("Create Account");
                }
            }
        };
        worker.execute();
    }

    private void showError(String message) {
        errorLabel.setForeground(theme.getDanger());
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void showInfo(String message) {
        errorLabel.setForeground(theme.getSuccess());
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private boolean isRegistrationOtpEnabled() {
        return config.isRegistrationEmailOtpRequired() && emailService.isConfigured();
    }

    public boolean isSuccess() { return success; }
}
