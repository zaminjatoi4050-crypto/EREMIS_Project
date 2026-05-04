package com.eremis.ui.transactions;

import com.eremis.controller.TransactionController;
import com.eremis.model.Transaction;
import com.eremis.ui.components.RoundedButton;
import com.eremis.ui.components.StyledTable;
import com.eremis.utils.EncryptionUtil;
import com.eremis.utils.SessionManager;
import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Admin panel for pending purchase approvals/rejections.
 */
public class TransactionAdminPanel extends JPanel {

    private final UIThemeManager theme = UIThemeManager.getInstance();
    private final TransactionController controller = new TransactionController();
    private final SessionManager session = SessionManager.getInstance();

    private DefaultTableModel tableModel;
    private StyledTable table;
    private List<Transaction> transactions;
    private Transaction selectedTransaction;

    private JLabel detailProperty;
    private JLabel detailBuyer;
    private JLabel detailSeller;
    private JLabel detailAmount;
    private JLabel detailBank;
    private JLabel detailAccount;
    private JLabel detailStatus;
    private JLabel detailDate;
    private JTextArea detailReason;
    private RoundedButton approveBtn;
    private RoundedButton rejectBtn;

    private static final String[] COLS = {"#", "Property", "Buyer", "Seller", "Amount", "Status", "Created"};

    public TransactionAdminPanel() {
        setBackground(theme.getBgPrimary());
        setLayout(new BorderLayout());
        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(16, 20, 8, 20));

        JLabel title = new JLabel("💼  Purchase Transactions");
        title.setFont(UIThemeManager.FONT_TITLE);
        title.setForeground(theme.getTextPrimary());
        top.add(title, BorderLayout.WEST);

        RoundedButton refreshBtn = RoundedButton.secondary("🔄  Refresh");
        refreshBtn.addActionListener(e -> loadData());
        top.add(refreshBtn, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildTablePanel(), buildDetailPanel());
        split.setDividerLocation(620);
        split.setDividerSize(4);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(theme.getBgPrimary());
        panel.setBorder(new EmptyBorder(0, 20, 20, 6));

        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new StyledTable(tableModel);
        table.getColumnModel().getColumn(5).setCellRenderer(new StyledTable.StatusBadgeRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail(getSelected());
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(theme.getBorder(), 1));
        scroll.getViewport().setBackground(theme.getBgSecondary());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(theme.getBgSecondary());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, theme.getBorder()),
            new EmptyBorder(20, 16, 20, 16)));

        JLabel heading = new JLabel("📑  Transaction Detail");
        heading.setFont(UIThemeManager.FONT_LABEL);
        heading.setForeground(theme.getTextSecondary());
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(12));

        detailProperty = line("Property:", "—");
        detailBuyer = line("Buyer:", "—");
        detailSeller = line("Seller:", "—");
        detailAmount = line("Amount:", "—");
        detailBank = line("Bank:", "—");
        detailAccount = line("Account:", "—");
        detailStatus = line("Status:", "—");
        detailDate = line("Created:", "—");

        for (JLabel label : new JLabel[]{detailProperty, detailBuyer, detailSeller, detailAmount, detailBank, detailAccount, detailStatus, detailDate}) {
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(label);
            panel.add(Box.createVerticalStrut(6));
        }

        panel.add(Box.createVerticalStrut(10));
        JLabel reasonLbl = line("Rejection Reason:", "—");
        reasonLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(reasonLbl);

        detailReason = new JTextArea(3, 18);
        detailReason.setEditable(false);
        detailReason.setLineWrap(true);
        detailReason.setWrapStyleWord(true);
        detailReason.setFont(UIThemeManager.FONT_BODY);
        detailReason.setBackground(theme.getBgPrimary());
        detailReason.setForeground(theme.getTextPrimary());
        JScrollPane reasonScroll = new JScrollPane(detailReason);
        reasonScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        reasonScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panel.add(reasonScroll);

        panel.add(Box.createVerticalStrut(14));
        approveBtn = RoundedButton.success("✅  Approve");
        approveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        approveBtn.addActionListener(e -> approveSelected());
        rejectBtn = RoundedButton.danger("❌  Reject");
        rejectBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        rejectBtn.addActionListener(e -> rejectSelected());
        panel.add(approveBtn);
        panel.add(Box.createVerticalStrut(8));
        panel.add(rejectBtn);

        return panel;
    }

    private void loadData() {
        SwingWorker<List<Transaction>, Void> worker = new SwingWorker<>() {
            @Override protected List<Transaction> doInBackground() {
                return controller.getAllTransactions();
            }

            @Override protected void done() {
                try {
                    transactions = get();
                    refreshTable();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(TransactionAdminPanel.this,
                        "Error loading transactions: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        for (Transaction tx : transactions) {
            tableModel.addRow(new Object[]{
                tx.getId(),
                tx.getPropertyTitle(),
                tx.getBuyerName(),
                tx.getSellerName(),
                "PKR " + fmt.format(tx.getAmount()),
                tx.getStatus().name(),
                tx.getCreatedAt() != null ? tx.getCreatedAt().format(timeFmt) : "—"
            });
        }
    }

    private void showDetail(Transaction tx) {
        selectedTransaction = tx;
        boolean hasSelection = tx != null;
        approveBtn.setEnabled(hasSelection);
        rejectBtn.setEnabled(hasSelection);
        if (!hasSelection) {
            clearDetail();
            return;
        }

        detailProperty.setText(textLine("Property:", tx.getPropertyTitle()));
        detailBuyer.setText(textLine("Buyer:", tx.getBuyerName()));
        detailSeller.setText(textLine("Seller:", tx.getSellerName()));
        detailAmount.setText(textLine("Amount:", "PKR " + money(tx.getAmount())));
        detailBank.setText(textLine("Bank:", tx.getBankName()));
        detailAccount.setText(textLine("Account:", EncryptionUtil.maskAccountNumber(EncryptionUtil.decrypt(tx.getAccountNumberEncrypted()))));
        detailStatus.setText(textLine("Status:", tx.getStatus().getDisplayName()));
        detailDate.setText(textLine("Created:", tx.getCreatedAt() != null ? tx.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "—"));
        detailReason.setText(tx.getRejectionReason() != null ? tx.getRejectionReason() : "");
    }

    private void clearDetail() {
        detailProperty.setText(textLine("Property:", "—"));
        detailBuyer.setText(textLine("Buyer:", "—"));
        detailSeller.setText(textLine("Seller:", "—"));
        detailAmount.setText(textLine("Amount:", "—"));
        detailBank.setText(textLine("Bank:", "—"));
        detailAccount.setText(textLine("Account:", "—"));
        detailStatus.setText(textLine("Status:", "—"));
        detailDate.setText(textLine("Created:", "—"));
        detailReason.setText("");
    }

    private void approveSelected() {
        if (selectedTransaction == null) return;
        try {
            controller.approve(selectedTransaction.getId(), session.getCurrentUserId());
            loadData();
            clearDetail();
            JOptionPane.showMessageDialog(this, "Transaction approved.",
                "Approved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rejectSelected() {
        if (selectedTransaction == null) return;
        String reason = JOptionPane.showInputDialog(this, "Enter rejection reason:", "Reject Transaction",
            JOptionPane.QUESTION_MESSAGE);
        if (reason == null) return;
        try {
            controller.reject(selectedTransaction.getId(), session.getCurrentUserId(), reason.trim());
            loadData();
            clearDetail();
            JOptionPane.showMessageDialog(this, "Transaction rejected and refunded.",
                "Rejected", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Transaction getSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || transactions == null) return null;
        int id = (int) tableModel.getValueAt(row, 0);
        return transactions.stream().filter(tx -> tx.getId() == id).findFirst().orElse(null);
    }

    private JLabel line(String label, String value) {
        JLabel l = new JLabel(textLine(label, value));
        l.setFont(UIThemeManager.FONT_BODY);
        l.setForeground(theme.getTextPrimary());
        return l;
    }

    private String textLine(String label, String value) {
        return "<html><b>" + label + "</b>  " + value + "</html>";
    }

    private String money(BigDecimal amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }
}