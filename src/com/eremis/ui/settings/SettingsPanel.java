package com.eremis.ui.settings;

import com.eremis.config.AppConfig;
import com.eremis.config.DatabaseConfig;
import com.eremis.service.BackupService;
import com.eremis.ui.components.RoundedButton;
import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;

/**
 * Settings panel — theme, database connection test, backup / restore, app info.
 */
public class SettingsPanel extends JPanel {

    private final UIThemeManager theme   = UIThemeManager.getInstance();
    private final AppConfig      config  = AppConfig.getInstance();
    private final DatabaseConfig dbConf  = DatabaseConfig.getInstance();
    private final BackupService  backupSvc = new BackupService();

    private JLabel dbStatusLabel;

    public SettingsPanel() {
        setBackground(theme.getBgPrimary());
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JScrollPane scroll = new JScrollPane(buildInner());
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildInner() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(theme.getBgPrimary());
        inner.setBorder(new EmptyBorder(28, 32, 28, 32));

        JLabel pageTitle = new JLabel("⚙️  Settings");
        pageTitle.setFont(UIThemeManager.FONT_TITLE);
        pageTitle.setForeground(theme.getTextPrimary());
        pageTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(pageTitle);
        inner.add(Box.createVerticalStrut(24));

        inner.add(buildAppearanceSection());
        inner.add(Box.createVerticalStrut(20));
        inner.add(buildDatabaseSection());
        inner.add(Box.createVerticalStrut(20));
        inner.add(buildBackupSection());
        inner.add(Box.createVerticalStrut(20));
        inner.add(buildAboutSection());

        return inner;
    }

    // ── Appearance ────────────────────────────────────────────────────────
    private JPanel buildAppearanceSection() {
        JPanel section = card("🎨  Appearance");

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        row.setOpaque(false);

        JLabel lbl = new JLabel("Theme:");
        lbl.setFont(UIThemeManager.FONT_BODY);
        lbl.setForeground(theme.getTextPrimary());

        JToggleButton toggle = new JToggleButton(
            theme.isDark() ? "🌙  Dark Mode" : "☀️  Light Mode");
        toggle.setSelected(theme.isDark());
        toggle.setFont(UIThemeManager.FONT_BUTTON);
        toggle.setFocusPainted(false);
        toggle.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme.getBorder(), 1, true),
            new EmptyBorder(8, 18, 8, 18)));
        toggle.setBackground(theme.getBgSecondary());
        toggle.setForeground(theme.getTextPrimary());
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> {
            theme.toggleTheme();
            toggle.setText(theme.isDark() ? "🌙  Dark Mode" : "☀️  Light Mode");
            SwingUtilities.updateComponentTreeUI(
                SwingUtilities.getWindowAncestor(this));
        });

        row.add(lbl); row.add(toggle);
        section.add(row);
        return section;
    }

    // ── Database ──────────────────────────────────────────────────────────
    private JPanel buildDatabaseSection() {
        JPanel section = card("🗄️  Database Connection");

        JPanel grid = new JPanel(new GridLayout(0, 2, 10, 8));
        grid.setOpaque(false);

        addInfoRow(grid, "Host / URL:", dbConf.getUrl());
        addInfoRow(grid, "Username:",  dbConf.getUsername());
        addInfoRow(grid, "Driver:",    "com.mysql.cj.jdbc.Driver");

        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(grid);
        section.add(Box.createVerticalStrut(12));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);

        dbStatusLabel = new JLabel("Status: unknown");
        dbStatusLabel.setFont(UIThemeManager.FONT_BODY);
        dbStatusLabel.setForeground(theme.getTextSecondary());

        RoundedButton testBtn = new RoundedButton("🔌  Test Connection");
        testBtn.addActionListener(e -> testConnection());

        btnRow.add(testBtn); btnRow.add(dbStatusLabel);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(btnRow);
        return section;
    }

    // ── Backup & Restore ──────────────────────────────────────────────────
    private JPanel buildBackupSection() {
        JPanel section = card("💾  Backup & Restore");

        JLabel note = new JLabel(
            "<html>Creates a full mysqldump of the eremis_db database.<br/>" +
            "Requires <b>mysqldump</b> to be available on the system PATH.</html>");
        note.setFont(UIThemeManager.FONT_SMALL);
        note.setForeground(theme.getTextSecondary());
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(note);
        section.add(Box.createVerticalStrut(14));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);

        RoundedButton backupBtn = new RoundedButton("⬆️  Backup Now");
        backupBtn.addActionListener(e -> doBackup());

        RoundedButton restoreBtn = RoundedButton.secondary("⬇️  Restore from SQL");
        restoreBtn.addActionListener(e -> doRestore());

        btnRow.add(backupBtn); btnRow.add(restoreBtn);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(btnRow);
        return section;
    }

    // ── About ─────────────────────────────────────────────────────────────
    private JPanel buildAboutSection() {
        JPanel section = card("ℹ️  About EREMIS");

        String[][] info = {
            {"Application:",  "Enterprise Real Estate Management & Intelligence System"},
            {"Version:",      config.getVersion()},
            {"Stack:",        "Java " + System.getProperty("java.version") + " · Swing · MySQL · JDBC"},
            {"Architecture:", "Layered (Presentation → Controller → Service → DAO → DB)"},
            {"Features:",     "Auth · Property CRUD · CRM · Search · Analytics · Recommendations"}
        };

        JPanel grid = new JPanel(new GridLayout(0, 2, 10, 8));
        grid.setOpaque(false);
        for (String[] row : info) addInfoRow(grid, row[0], row[1]);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(grid);
        return section;
    }

    // ── Actions ───────────────────────────────────────────────────────────
    private void testConnection() {
        dbStatusLabel.setText("Testing…");
        SwingWorker<Boolean, Void> w = new SwingWorker<>() {
            @Override protected Boolean doInBackground() { return dbConf.testConnection(); }
            @Override protected void done() {
                try {
                    boolean ok = get();
                    dbStatusLabel.setText(ok ? "✅  Connected" : "❌  Connection failed");
                    dbStatusLabel.setForeground(ok ? theme.getSuccess() : theme.getDanger());
                } catch (Exception e) {
                    dbStatusLabel.setText("❌  Error");
                    dbStatusLabel.setForeground(theme.getDanger());
                }
            }
        };
        w.execute();
    }

    private void doBackup() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose Backup Folder");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String dir = fc.getSelectedFile().getAbsolutePath();
            SwingWorker<String, Void> w = new SwingWorker<>() {
                @Override protected String doInBackground() { return backupSvc.backup(dir); }
                @Override protected void done() {
                    try {
                        String path = get();
                        if (path != null) {
                            JOptionPane.showMessageDialog(SettingsPanel.this,
                                "Backup saved to:\n" + path, "Backup Successful",
                                JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(SettingsPanel.this,
                                "Backup failed. Ensure mysqldump is on your PATH.",
                                "Backup Failed", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(SettingsPanel.this,
                            "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            w.execute();
        }
    }

    private void doRestore() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select SQL Backup File");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File sqlFile = fc.getSelectedFile();
        int confirm = JOptionPane.showConfirmDialog(this,
            "Restoring will apply the selected SQL file to the current database. Continue?",
            "Confirm Restore", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override protected Boolean doInBackground() {
                return backupSvc.restore(sqlFile.getAbsolutePath());
            }

            @Override protected void done() {
                try {
                    boolean ok = get();
                    JOptionPane.showMessageDialog(SettingsPanel.this,
                        ok ? "Restore completed successfully." : "Restore failed. Check the backup file and MySQL logs.",
                        ok ? "Restore Successful" : "Restore Failed",
                        ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(SettingsPanel.this,
                        "Restore failed: " + e.getMessage(),
                        "Restore Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private JPanel card(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(theme.getBgSecondary());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(theme.getBorder(), 1, true), title);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 13));
        border.setTitleColor(theme.getTextPrimary());
        panel.setBorder(BorderFactory.createCompoundBorder(border, new EmptyBorder(12, 16, 16, 16)));

        return panel;
    }

    private void addInfoRow(JPanel grid, String label, String value) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIThemeManager.FONT_LABEL);
        lbl.setForeground(theme.getTextSecondary());
        JLabel val = new JLabel(value);
        val.setFont(UIThemeManager.FONT_BODY);
        val.setForeground(theme.getTextPrimary());
        grid.add(lbl); grid.add(val);
    }
}
