package com.eremis.ui.transactions;

import com.eremis.model.Property;
import com.eremis.ui.components.ModernTextField;
import com.eremis.ui.components.RoundedButton;
import com.eremis.utils.BankRegistry;
import com.eremis.utils.UIThemeManager;
import com.eremis.utils.ValidationUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * First step of the purchase flow: collect bank, account, and amount.
 */
public class PaymentInfoDialog extends JDialog {

    public static class PaymentDraft {
        private final String bankName;
        private final String accountNumber;
        private final BigDecimal amount;

        public PaymentDraft(String bankName, String accountNumber, BigDecimal amount) {
            this.bankName = bankName;
            this.accountNumber = accountNumber;
            this.amount = amount;
        }

        public String getBankName() { return bankName; }
        public String getAccountNumber() { return accountNumber; }
        public BigDecimal getAmount() { return amount; }
    }

    private final UIThemeManager theme = UIThemeManager.getInstance();
    private final Property property;
    private final NumberFormat moneyFmt = NumberFormat.getNumberInstance(Locale.US);
    private PaymentDraft draft;

    private ModernTextField searchField;
    private JComboBox<String> bankCombo;
    private ModernTextField accountField;
    private ModernTextField amountField;
    private JLabel validationLabel;

    public PaymentInfoDialog(Frame parent, Property property) {
        super(parent, "Payment Information", true);
        this.property = property;
        setSize(640, 430);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
    }

    public PaymentDraft getDraft() {
        return draft;
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(theme.getBgSecondary());
        setContentPane(root);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(theme.getBgSidebar());
        header.setBorder(new EmptyBorder(16, 18, 16, 18));
        JLabel title = new JLabel("💳  Payment Information");
        title.setFont(UIThemeManager.FONT_TITLE);
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel(property.getTitle() + " • PKR " + moneyFmt.format(property.getPrice()));
        subtitle.setFont(UIThemeManager.FONT_SMALL);
        subtitle.setForeground(new Color(0xC4D4E6));
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(title);
        stack.add(subtitle);
        header.add(stack, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(theme.getBgSecondary());
        form.setBorder(new EmptyBorder(18, 22, 8, 22));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;

        searchField = new ModernTextField("Search bank name", 260);
        searchField.getField().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshBanks(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshBanks(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshBanks(); }
        });

        bankCombo = new JComboBox<>();
        bankCombo.setEditable(true);
        bankCombo.setFont(UIThemeManager.FONT_BODY);
        bankCombo.setBackground(theme.getBgPrimary());
        bankCombo.setForeground(theme.getTextPrimary());

        accountField = new ModernTextField("Enter account number", 260);
        amountField = new ModernTextField("Enter payment amount", 260);

        g.gridx = 0; g.gridy = 0;
        form.add(label("Search Bank"), g);
        g.gridy = 1;
        form.add(searchField, g);
        g.gridy = 2;
        form.add(label("Bank Name"), g);
        g.gridy = 3;
        form.add(bankCombo, g);
        g.gridy = 4;
        form.add(label("Account Number"), g);
        g.gridy = 5;
        form.add(accountField, g);
        g.gridy = 6;
        form.add(label("Payment Amount"), g);
        g.gridy = 7;
        form.add(amountField, g);

        validationLabel = new JLabel(" ");
        validationLabel.setFont(UIThemeManager.FONT_SMALL);
        validationLabel.setForeground(theme.getDanger());
        g.gridy = 8;
        form.add(validationLabel, g);

        refreshBanks();
        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttons.setBackground(theme.getBgSecondary());
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorder()));

        RoundedButton cancelBtn = RoundedButton.secondary("Cancel");
        cancelBtn.addActionListener(e -> {
            draft = null;
            dispose();
        });
        RoundedButton continueBtn = new RoundedButton("Continue", theme.getAccent(), theme.getAccentHover(), 10);
        continueBtn.addActionListener(e -> onContinue());

        buttons.add(cancelBtn);
        buttons.add(continueBtn);
        root.add(buttons, BorderLayout.SOUTH);
    }

    private void refreshBanks() {
        String query = searchField != null ? searchField.getText() : "";
        List<String> banks = BankRegistry.search(query);
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (String bank : banks) model.addElement(bank);
        bankCombo.setModel(model);
        if (!banks.isEmpty()) bankCombo.setSelectedIndex(0);
    }

    private void onContinue() {
        String bankName = getSelectedBank();
        String accountNumber = accountField.getText().trim();
        String amountText = amountField.getText().trim().replace(",", "");

        if (!ValidationUtil.isValidBankName(bankName) || !BankRegistry.contains(bankName)) {
            validationLabel.setText("Please select a valid bank from the list.");
            return;
        }
        if (!ValidationUtil.isValidAccountNumber(accountNumber)) {
            validationLabel.setText("Account number must contain 8 to 20 digits.");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText);
        } catch (Exception ex) {
            validationLabel.setText("Please enter a valid payment amount.");
            return;
        }
        if (amount.compareTo(property.getPrice()) < 0) {
            validationLabel.setText("Payment amount must be at least the property price.");
            return;
        }

        draft = new PaymentDraft(bankName, accountNumber, amount);
        dispose();
    }

    private String getSelectedBank() {
        Object editorValue = bankCombo.isEditable() ? bankCombo.getEditor().getItem() : bankCombo.getSelectedItem();
        return editorValue != null ? editorValue.toString().trim() : "";
    }

    private JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIThemeManager.FONT_LABEL);
        lbl.setForeground(theme.getTextPrimary());
        return lbl;
    }
}