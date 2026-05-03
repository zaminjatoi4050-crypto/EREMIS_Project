package com.eremis.ui.transactions;

import com.eremis.controller.TransactionController;
import com.eremis.model.Property;
import com.eremis.model.Transaction;
import com.eremis.ui.components.RoundedButton;
import com.eremis.utils.EncryptionUtil;
import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Final review step before the purchase transaction is submitted.
 */
public class PaymentConfirmationDialog extends JDialog {

    private final UIThemeManager theme = UIThemeManager.getInstance();
    private final TransactionController controller = new TransactionController();
    private final Property property;
    private final PaymentInfoDialog.PaymentDraft draft;
    private boolean confirmed = false;
    private Transaction createdTransaction;

    public PaymentConfirmationDialog(Frame parent, Property property, PaymentInfoDialog.PaymentDraft draft) {
        super(parent, "Confirm Payment", true);
        this.property = property;
        this.draft = draft;
        setSize(700, 470);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
    }

    public boolean isConfirmed() { return confirmed; }
    public Transaction getCreatedTransaction() { return createdTransaction; }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(theme.getBgSecondary());
        setContentPane(root);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(theme.getBgSidebar());
        header.setBorder(new EmptyBorder(16, 18, 16, 18));
        JLabel title = new JLabel("✅  Confirm Payment");
        title.setFont(UIThemeManager.FONT_TITLE);
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Review the details before submitting the transaction.");
        subtitle.setFont(UIThemeManager.FONT_SMALL);
        subtitle.setForeground(new Color(0xC4D4E6));
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(title);
        stack.add(subtitle);
        header.add(stack, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 16, 0));
        body.setBackground(theme.getBgSecondary());
        body.setBorder(new EmptyBorder(18, 18, 12, 18));
        body.add(buildSummaryCard());
        body.add(buildPaymentCard());
        root.add(body, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        actions.setBackground(theme.getBgSecondary());
        actions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorder()));
        RoundedButton cancelBtn = RoundedButton.secondary("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        RoundedButton confirmBtn = new RoundedButton("Confirm Payment", theme.getAccent(), theme.getAccentHover(), 10);
        confirmBtn.addActionListener(e -> confirmPurchase());
        actions.add(cancelBtn);
        actions.add(confirmBtn);
        root.add(actions, BorderLayout.SOUTH);
    }

    private JPanel buildSummaryCard() {
        JPanel card = cardPanel("Property Details");
        addLine(card, "Title", property.getTitle());
        addLine(card, "City", property.getCity());
        addLine(card, "Price", "PKR " + money(property.getPrice().doubleValue()));
        addLine(card, "Status", property.getStatus().getDisplayName());
        return card;
    }

    private JPanel buildPaymentCard() {
        JPanel card = cardPanel("Payment Details");
        addLine(card, "Bank", draft.getBankName());
        addLine(card, "Account", EncryptionUtil.maskAccountNumber(draft.getAccountNumber()));
        addLine(card, "Amount", "PKR " + money(draft.getAmount().doubleValue()));
        addLine(card, "Flow", "Pending approval after confirmation");
        return card;
    }

    private JPanel cardPanel(String titleText) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(theme.getBgPrimary());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme.getBorder(), 1, true),
            new EmptyBorder(14, 14, 14, 14)));
        JLabel title = new JLabel(titleText);
        title.setFont(UIThemeManager.FONT_LABEL);
        title.setForeground(theme.getTextPrimary());
        card.add(title);
        card.add(Box.createVerticalStrut(10));
        return card;
    }

    private void addLine(JPanel card, String label, String value) {
        JLabel l = new JLabel("<html><b>" + label + ":</b> " + value + "</html>");
        l.setFont(UIThemeManager.FONT_BODY);
        l.setForeground(theme.getTextPrimary());
        l.setBorder(new EmptyBorder(0, 0, 8, 0));
        card.add(l);
    }

    private void confirmPurchase() {
        try {
            createdTransaction = controller.submitPurchase(property.getId(), draft.getBankName(),
                draft.getAccountNumber(), draft.getAmount());
            confirmed = true;
            dispose();
            String message = createdTransaction.getStatus().name().equals("APPROVED")
                ? "Payment approved automatically. The property is now sold."
                : "Payment submitted successfully. Waiting for admin approval.";
            JOptionPane.showMessageDialog(getParent(), message,
                "Payment Submitted", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                "Payment Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String money(double amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }
}