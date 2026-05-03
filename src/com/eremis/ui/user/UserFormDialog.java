package com.eremis.ui.user;

import com.eremis.controller.UserController;
import com.eremis.model.User;
import com.eremis.model.enums.UserRole;
import com.eremis.ui.components.RoundedButton;
import com.eremis.utils.ValidationUtil;
import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Modal dialog for adding or editing a User.
 */
public class UserFormDialog extends JDialog {

    private final UIThemeManager theme;
    private final UserController ctrl;
    private final User           existing;
    private boolean              saved = false;

    private JTextField     nameField, emailField, usernameField;
    private JPasswordField passwordField;
    private JComboBox<UserRole> roleCombo;
    private JCheckBox      activeCheck;

    public UserFormDialog(Frame parent, User existing, UserController ctrl) {
        super(parent, existing == null ? "Add User" : "Edit User", true);
        this.existing = existing;
        this.ctrl     = ctrl;
        this.theme    = UIThemeManager.getInstance();

        setSize(460, 440);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
        if (existing != null) populateFields();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(theme.getBgSecondary());
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 14));
        header.setBackground(theme.getBgSidebar());
        JLabel title = new JLabel(existing == null ? "➕  New User" : "✏️  Edit User");
        title.setFont(UIThemeManager.FONT_SUBTITLE);
        title.setForeground(Color.WHITE);
        header.add(title);
        root.add(header, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.setBackground(theme.getBgSecondary());
        form.setBorder(new EmptyBorder(20, 24, 10, 24));

        form.add(makeLabel("Full Name *")); form.add(nameField = styledField());
        form.add(makeLabel("Email *"));     form.add(emailField = styledField());
        form.add(makeLabel("Username *"));  form.add(usernameField = styledField());

        String pwdLabel = existing == null ? "Password *" : "Password (leave blank = no change)";
        form.add(makeLabel(pwdLabel));
        passwordField = new JPasswordField();
        styleField(passwordField);
        form.add(passwordField);

        form.add(makeLabel("Role"));
        roleCombo = new JComboBox<>(UserRole.values());
        roleCombo.setFont(UIThemeManager.FONT_BODY);
        roleCombo.setBackground(theme.getBgSecondary());
        form.add(roleCombo);

        form.add(makeLabel("Active"));
        activeCheck = new JCheckBox("Account is active");
        activeCheck.setSelected(true);
        activeCheck.setFont(UIThemeManager.FONT_BODY);
        activeCheck.setForeground(theme.getTextPrimary());
        activeCheck.setBackground(theme.getBgSecondary());
        activeCheck.setOpaque(false);
        form.add(activeCheck);

        root.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnRow.setBackground(theme.getBgSecondary());
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorder()));
        RoundedButton cancelBtn = RoundedButton.secondary("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        RoundedButton saveBtn = new RoundedButton("💾  Save User");
        saveBtn.addActionListener(e -> doSave());
        btnRow.add(cancelBtn); btnRow.add(saveBtn);
        root.add(btnRow, BorderLayout.SOUTH);
    }

    private void populateFields() {
        nameField.setText(existing.getFullName());
        emailField.setText(existing.getEmail());
        usernameField.setText(existing.getUsername());
        roleCombo.setSelectedItem(existing.getRole());
        activeCheck.setSelected(existing.isActive());
    }

    private void doSave() {
        try {
            User u = existing != null ? existing : new User();
            u.setFullName(nameField.getText().trim());
            u.setEmail(emailField.getText().trim());
            u.setUsername(usernameField.getText().trim());
            u.setRole((UserRole) roleCombo.getSelectedItem());
            u.setActive(activeCheck.isSelected());

            String pwd = new String(passwordField.getPassword()).trim();
            if (!pwd.isEmpty() && !ValidationUtil.isValidPassword(pwd)) {
                JOptionPane.showMessageDialog(this,
                    "Password must be at least 8 characters and contain a digit.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (existing == null) {
                u.setPasswordHash(pwd);
            } else if (!pwd.isEmpty()) {
                u.setPasswordHash(existing.getPasswordHash());
            }

            if (existing == null && pwd.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Password is required for new users.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (existing == null) ctrl.createUser(u);
            else {
                ctrl.updateUser(u);
                if (!pwd.isEmpty()) {
                    ctrl.changePassword(u.getId(), pwd);
                }
            }

            saved = true;
            dispose();
            JOptionPane.showMessageDialog(getParent(),
                "User saved successfully!", "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() { return saved; }

    private JTextField styledField() {
        JTextField f = new JTextField();
        styleField(f);
        return f;
    }

    private void styleField(JTextField f) {
        f.setFont(UIThemeManager.FONT_BODY);
        f.setBackground(theme.getBgPrimary());
        f.setForeground(theme.getTextPrimary());
        f.setCaretColor(theme.getAccent());
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme.getBorder(), 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIThemeManager.FONT_LABEL);
        l.setForeground(theme.getTextPrimary());
        return l;
    }
}
