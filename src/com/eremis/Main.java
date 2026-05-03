package com.eremis;

import com.eremis.config.DatabaseConfig;
import com.eremis.ui.login.LoginFrame;
import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ╔═══════════════════════════════════════════════════════════════╗
 * ║          EREMIS — Enterprise Real Estate Management &        ║
 * ║                   Intelligence System                        ║
 * ║                                                              ║
 * ║   Entry point. Run this class to start the application.      ║
 * ║   Ensure MySQL is running and db.properties is configured.   ║
 * ╚═══════════════════════════════════════════════════════════════╝
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        // ── 1. Configure Java logging ─────────────────────────────────────
        System.setProperty("java.util.logging.SimpleFormatter.format",
            "[%1$tT] [%4$s] %2$s - %5$s%n");

        // ── 2. Apply system Look & Feel for native OS chrome ──────────────
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not apply system LAF, using default.", e);
        }

        // ── 3. Enable font anti-aliasing for crisp text rendering ─────────
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // ── 4. Override key UIManager defaults with our theme colours ──────
        applyThemeDefaults();

        // ── 5. Verify database connectivity before showing the UI ─────────
        LOGGER.info("Testing database connection…");
        DatabaseConfig db = DatabaseConfig.getInstance();
        boolean dbOk = db.testConnection();
        if (!dbOk) {
            boolean configured = promptForDatabaseCredentials(db);
            if (configured) {
                dbOk = db.testConnection();
            }

            if (!dbOk) {
                // Show a user-friendly error rather than a stack trace
                JOptionPane.showMessageDialog(null,
                    "<html><b>Cannot connect to the database.</b><br/><br/>" +
                    "Please ensure:<br/>" +
                    "1. MySQL server is running.<br/>" +
                    "2. <b>resources/db.properties</b> has correct credentials.<br/>" +
                    "3. The <b>eremis_db</b> schema has been created.<br/><br/>" +
                    "Run <code>sql/schema.sql</code> then <code>sql/sample_data.sql</code> first.</html>",
                    "Database Connection Error",
                    JOptionPane.ERROR_MESSAGE);
                LOGGER.severe("Database connection test FAILED — check db.properties");
            } else {
                LOGGER.info("Database connection OK after credential refresh.");
            }
        } else {
            LOGGER.info("Database connection OK.");
        }

        // ── 6. Register JVM shutdown hook to close the connection pool ────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("JVM shutdown — closing database connection pool.");
            DatabaseConfig.getInstance().shutdown();
        }, "db-pool-shutdown"));

        // ── 7. Launch login frame on the Event Dispatch Thread ────────────
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
            LOGGER.info("EREMIS started — login screen displayed.");
        });
    }

    /**
     * Push our brand colours into UIManager defaults so that any components
     * that haven't been individually styled still look correct.
     */
    private static void applyThemeDefaults() {
        UIThemeManager tm = UIThemeManager.getInstance();

        UIManager.put("Panel.background",          tm.getBgPrimary());
        UIManager.put("OptionPane.background",     tm.getBgSecondary());
        UIManager.put("OptionPane.messageForeground", tm.getTextPrimary());
        UIManager.put("Button.font",               UIThemeManager.FONT_BUTTON);
        UIManager.put("Label.font",                UIThemeManager.FONT_BODY);
        UIManager.put("TextField.font",            UIThemeManager.FONT_BODY);
        UIManager.put("TextArea.font",             UIThemeManager.FONT_BODY);
        UIManager.put("ComboBox.font",             UIThemeManager.FONT_BODY);
        UIManager.put("Table.font",                UIThemeManager.FONT_BODY);
        UIManager.put("TableHeader.font",          UIThemeManager.FONT_LABEL);
        UIManager.put("ScrollBar.width",           8);
    }

    private static boolean promptForDatabaseCredentials(DatabaseConfig db) {
        JTextField usernameField = new JTextField(db.getUsername() != null ? db.getUsername() : "root", 24);
        JPasswordField passwordField = new JPasswordField(24);
        passwordField.setText("");

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 0, 6, 10);
        panel.add(new JLabel("MySQL Username:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("MySQL Password:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(passwordField, gbc);

        int choice = JOptionPane.showConfirmDialog(
            null,
            panel,
            "Database Credentials Required",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return false;
        }

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty()) {
            username = "root";
        }

        try {
            persistDatabaseProperties(db.getUrl(), username);
            db.updateCredentials(db.getUrl(), username, password);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not save DB credentials", e);
            JOptionPane.showMessageDialog(null,
                "Could not save database username to resources/db.properties.\n" +
                "The password will not be stored on disk. DB access may still fail if the entered password is incorrect.",
                "Database Configuration Warning",
                JOptionPane.WARNING_MESSAGE);
            db.updateCredentials(db.getUrl(), username, password);
            return true;
        }
    }

    private static void persistDatabaseProperties(String url, String username) throws IOException {
        Properties props = new Properties();
        File file = new File("resources/db.properties");

        if (file.exists()) {
            try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                props.load(in);
            }
        }

        props.setProperty("db.url", url != null ? url : props.getProperty("db.url", ""));
        props.setProperty("db.username", username != null ? username : "root");
    props.setProperty("db.password", "");

        try (OutputStream out = new FileOutputStream(file)) {
            props.store(out, "EREMIS Database Configuration");
        }
    }
}