package com.eremis.ui.search;

import com.eremis.controller.PropertyController;
import com.eremis.controller.InquiryController;
import com.eremis.model.Inquiry;
import com.eremis.model.Property;
import com.eremis.model.enums.PropertyStatus;
import com.eremis.model.enums.PropertyType;
import com.eremis.service.RecommendationService;
import com.eremis.ui.transactions.PaymentConfirmationDialog;
import com.eremis.ui.transactions.PaymentInfoDialog;
import com.eremis.ui.components.*;
import com.eremis.utils.SessionManager;
import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Advanced property search panel with keyword, city, price range,
 * type, status filters, sort options, and AI-style recommendations.
 */
public class SearchPanel extends JPanel {

    private final UIThemeManager    theme   = UIThemeManager.getInstance();
    private final PropertyController ctrl   = new PropertyController();
    private final InquiryController inquiryCtrl = new InquiryController();
    private final RecommendationService rec = new RecommendationService();
    private final SessionManager    session = SessionManager.getInstance();

    // Filter controls
    private ModernTextField  keywordField, cityField, minPriceField, maxPriceField;
    private JComboBox<String>        typeCombo, statusCombo, sortCombo, dirCombo;
    private DefaultTableModel        tableModel;
    private StyledTable              table;
    private List<Property>           results;
    private JLabel                   resultCountLabel;
    private JLabel                   selectedInfoLabel;
    private RoundedButton            buyBtn;
    private RoundedButton            inquiryBtn;
    private Property                 selectedProperty;

    private static final String[] COLS =
        {"#", "Title", "City", "Type", "Price (PKR)", "Status", "Area (sqft)", "Bedrooms"};

    public SearchPanel() {
        setBackground(theme.getBgPrimary());
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    private void buildUI() {
        add(buildFilterPanel(), BorderLayout.NORTH);
        add(buildResultPanel(), BorderLayout.CENTER);
        add(buildRecommendPanel(), BorderLayout.EAST);
    }

    // ── Filter panel ──────────────────────────────────────────────────────
    private JPanel buildFilterPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(theme.getBgSecondary());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, theme.getBorder()),
            new EmptyBorder(16, 20, 16, 20)));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("🔍  Advanced Property Search");
        title.setFont(UIThemeManager.FONT_TITLE);
        title.setForeground(theme.getTextPrimary());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(14));

        // Row 1: keyword + city
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row1.setOpaque(false);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row1.add(labeledField("Keyword", keywordField = new ModernTextField("Search title, location…", 220)));
        row1.add(labeledField("City", cityField = new ModernTextField("e.g. Lahore, Karachi", 160)));
        row1.add(labeledField("Min Price (PKR)", minPriceField = new ModernTextField("e.g. 5000000", 140)));
        row1.add(labeledField("Max Price (PKR)", maxPriceField = new ModernTextField("e.g. 50000000", 140)));
        panel.add(row1);
        panel.add(Box.createVerticalStrut(10));

        // Row 2: type, status, sort
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row2.setOpaque(false);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] types   = {"All Types", "HOUSE", "APARTMENT", "COMMERCIAL", "LAND", "VILLA", "CONDO"};
        String[] statuses = {"All Statuses", "AVAILABLE", "LOCKED", "RESERVED", "SOLD"};
        String[] sorts   = {"created_at", "price", "title", "city"};
        String[] dirs    = {"DESC", "ASC"};

        typeCombo   = styledCombo(types);
        statusCombo = styledCombo(statuses);
        sortCombo   = styledCombo(sorts);
        dirCombo    = styledCombo(dirs);

        row2.add(labeledCombo("Type",      typeCombo));
        row2.add(labeledCombo("Status",    statusCombo));
        row2.add(labeledCombo("Sort By",   sortCombo));
        row2.add(labeledCombo("Direction", dirCombo));

        RoundedButton searchBtn = new RoundedButton("🔍  Search");
        searchBtn.setPreferredSize(new Dimension(130, 36));
        searchBtn.addActionListener(e -> doSearch());
        RoundedButton clearBtn = RoundedButton.secondary("✕ Clear");
        clearBtn.addActionListener(e -> clearFilters());

        row2.add(Box.createHorizontalStrut(8));
        row2.add(searchBtn);
        row2.add(clearBtn);
        panel.add(row2);

        return panel;
    }

    // ── Results table ─────────────────────────────────────────────────────
    private JPanel buildResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(theme.getBgPrimary());
        panel.setBorder(new EmptyBorder(12, 20, 20, 4));

        resultCountLabel = new JLabel("Enter search criteria above and click Search.");
        resultCountLabel.setFont(UIThemeManager.FONT_SMALL);
        resultCountLabel.setForeground(theme.getTextSecondary());
        resultCountLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
        panel.add(resultCountLabel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new StyledTable(tableModel);
        table.getColumnModel().getColumn(5).setCellRenderer(new StyledTable.StatusBadgeRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateSelectedProperty();
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(theme.getBorder(), 1));
        scroll.getViewport().setBackground(theme.getBgSecondary());
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buildActionPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildActionPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(12, 0, 0, 0));

        selectedInfoLabel = new JLabel("Select a property to buy or inquire.");
        selectedInfoLabel.setFont(UIThemeManager.FONT_SMALL);
        selectedInfoLabel.setForeground(theme.getTextSecondary());
        panel.add(selectedInfoLabel, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        inquiryBtn = RoundedButton.secondary("💬  Send Inquiry");
        inquiryBtn.addActionListener(e -> openInquiryDialog());

        buyBtn = new RoundedButton("🛒  Buy Property",
            new Color(0x1E88E5), new Color(0x1565C0), 10);
        buyBtn.addActionListener(e -> openPurchaseFlow());

        boolean buyerLike = session.getCurrentUser() != null && session.getCurrentUser().getRole().isBuyerLike();
        buyBtn.setEnabled(buyerLike);
        buyBtn.setToolTipText(buyerLike ? "Buy the selected property" : "Buyer accounts only");

        actions.add(inquiryBtn);
        actions.add(buyBtn);
        panel.add(actions, BorderLayout.EAST);
        return panel;
    }

    // ── Recommendation sidebar ─────────────────────────────────────────────
    private JPanel buildRecommendPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(theme.getBgSecondary());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, theme.getBorder()),
            new EmptyBorder(16, 14, 16, 14)));
        panel.setPreferredSize(new Dimension(220, 0));

        JLabel title = new JLabel("✨  Recommended");
        title.setFont(UIThemeManager.FONT_LABEL);
        title.setForeground(theme.getTextPrimary());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(8));

        JLabel hint = new JLabel("<html><i>Based on your search history</i></html>");
        hint.setFont(UIThemeManager.FONT_SMALL);
        hint.setForeground(theme.getTextSecondary());
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(hint);
        panel.add(Box.createVerticalStrut(12));

        // Load recommendations async
        SwingWorker<List<Property>, Void> w = new SwingWorker<>() {
            @Override protected List<Property> doInBackground() {
                return rec.getRecommendations(session.getCurrentUserId());
            }
            @Override protected void done() {
                try {
                    List<Property> recs = get();
                    if (recs.isEmpty()) {
                        JLabel none = new JLabel("<html>No recommendations yet.<br/>Try searching first!</html>");
                        none.setFont(UIThemeManager.FONT_SMALL);
                        none.setForeground(theme.getTextSecondary());
                        panel.add(none);
                    } else {
                        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
                        for (Property p : recs) {
                            panel.add(buildRecCard(p, fmt));
                            panel.add(Box.createVerticalStrut(10));
                        }
                    }
                    panel.revalidate(); panel.repaint();
                } catch (Exception ignored) {}
            }
        };
        w.execute();
        return panel;
    }

    private JPanel buildRecCard(Property p, NumberFormat fmt) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(theme.getBgPrimary());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme.getBorder(), 1, true),
            new EmptyBorder(8, 10, 8, 10)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JLabel nameL = new JLabel("<html><b>" + truncate(p.getTitle(), 28) + "</b></html>");
        nameL.setFont(UIThemeManager.FONT_SMALL);
        nameL.setForeground(theme.getTextPrimary());

        JLabel cityL = new JLabel("📍 " + p.getCity());
        cityL.setFont(UIThemeManager.FONT_SMALL);
        cityL.setForeground(theme.getTextSecondary());

        JLabel priceL = new JLabel("PKR " + fmt.format(p.getPrice()));
        priceL.setFont(UIThemeManager.FONT_SMALL);
        priceL.setForeground(theme.getAccent());

        card.add(nameL); card.add(cityL); card.add(priceL);
        return card;
    }

    // ── Search logic ──────────────────────────────────────────────────────
    private void doSearch() {
        String  keyword   = keywordField.getText().trim();
        String  city      = cityField.getText().trim();
        BigDecimal minP   = parseBD(minPriceField.getText().trim());
        BigDecimal maxP   = parseBD(maxPriceField.getText().trim());
        String  typeStr   = (String) typeCombo.getSelectedItem();
        String  statusStr = (String) statusCombo.getSelectedItem();
        String  sortBy    = (String) sortCombo.getSelectedItem();
        String  dir       = (String) dirCombo.getSelectedItem();

        PropertyType   type   = "All Types".equals(typeStr)   ? null : PropertyType.valueOf(typeStr);
        PropertyStatus status = "All Statuses".equals(statusStr) ? null : PropertyStatus.valueOf(statusStr);

        resultCountLabel.setText("Searching…");

        SwingWorker<List<Property>, Void> w = new SwingWorker<>() {
            @Override protected List<Property> doInBackground() {
                // Save to history
                rec.saveSearch(session.getCurrentUserId(), keyword, city, minP, maxP, typeStr);
                return ctrl.search(keyword.isEmpty() ? null : keyword,
                                   city.isEmpty() ? null : city,
                                   minP, maxP, type, status, sortBy, dir);
            }
            @Override protected void done() {
                try {
                    results = get();
                    tableModel.setRowCount(0);
                    NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
                    for (Property p : results) {
                        tableModel.addRow(new Object[]{
                            p.getId(), p.getTitle(), p.getCity(),
                            p.getType().getDisplayName(),
                            fmt.format(p.getPrice()),
                            p.getStatus().name(),
                            p.getAreaSqft() != null ? p.getAreaSqft().toPlainString() : "—",
                            p.getBedrooms()
                        });
                    }
                    resultCountLabel.setText("Found " + results.size() + " result(s).");
                    selectedProperty = null;
                    updateSelectedActionState();
                } catch (Exception e) {
                    resultCountLabel.setText("Search error: " + e.getMessage());
                }
            }
        };
        w.execute();
    }

    private void clearFilters() {
        keywordField.setText(""); cityField.setText("");
        minPriceField.setText(""); maxPriceField.setText("");
        typeCombo.setSelectedIndex(0); statusCombo.setSelectedIndex(0);
        sortCombo.setSelectedIndex(0); dirCombo.setSelectedIndex(0);
        tableModel.setRowCount(0);
        resultCountLabel.setText("Filters cleared.");
        selectedProperty = null;
        updateSelectedActionState();
    }

    private void updateSelectedProperty() {
        int row = table.getSelectedRow();
        if (row < 0 || results == null) {
            selectedProperty = null;
        } else {
            int id = (int) tableModel.getValueAt(row, 0);
            selectedProperty = results.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
        }
        updateSelectedActionState();
    }

    private void updateSelectedActionState() {
        boolean hasSelection = selectedProperty != null;
        if (selectedInfoLabel != null) {
            if (hasSelection) {
                selectedInfoLabel.setText(selectedProperty.getTitle() + " | " + selectedProperty.getCity() +
                    " | PKR " + NumberFormat.getNumberInstance(Locale.US).format(selectedProperty.getPrice()) +
                    " | " + selectedProperty.getStatus().getDisplayName());
            } else {
                selectedInfoLabel.setText("Select a property to buy or inquire.");
            }
        }
        if (inquiryBtn != null) inquiryBtn.setEnabled(hasSelection);
        if (buyBtn != null) {
            boolean canBuy = session.getCurrentUser() != null && session.getCurrentUser().getRole().isBuyerLike()
                && hasSelection && selectedProperty.getStatus() == PropertyStatus.AVAILABLE;
            buyBtn.setEnabled(canBuy);
            if (selectedProperty != null && selectedProperty.getStatus() != PropertyStatus.AVAILABLE) {
                buyBtn.setToolTipText("Only available properties can be bought");
            }
        }
    }

    private void openPurchaseFlow() {
        if (selectedProperty == null) {
            JOptionPane.showMessageDialog(this, "Please select a property first.",
                "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        PaymentInfoDialog infoDialog = new PaymentInfoDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), selectedProperty);
        infoDialog.setVisible(true);

        PaymentInfoDialog.PaymentDraft draft = infoDialog.getDraft();
        if (draft == null) {
            return;
        }

        PaymentConfirmationDialog confirmationDialog = new PaymentConfirmationDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), selectedProperty, draft);
        confirmationDialog.setVisible(true);
        if (confirmationDialog.isConfirmed()) {
            doSearch();
        }
    }

    private void openInquiryDialog() {
        if (selectedProperty == null) {
            JOptionPane.showMessageDialog(this, "Please select a property first.",
                "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Send Inquiry", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(520, 360);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(theme.getBgSecondary());
        root.setBorder(new EmptyBorder(16, 18, 16, 18));
        dlg.setContentPane(root);

        JLabel heading = new JLabel("Inquiry for: " + selectedProperty.getTitle());
        heading.setFont(UIThemeManager.FONT_TITLE);
        heading.setForeground(theme.getTextPrimary());
        root.add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 0, 6, 0);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;

        JTextField subjectField = new JTextField("Inquiry about " + selectedProperty.getTitle());
        JTextArea messageArea = new JTextArea(6, 20);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        g.gridx = 0; g.gridy = 0;
        form.add(new JLabel("Subject"), g);
        g.gridy = 1;
        form.add(subjectField, g);
        g.gridy = 2;
        form.add(new JLabel("Message"), g);
        g.gridy = 3;
        form.add(new JScrollPane(messageArea), g);

        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        RoundedButton cancelBtn = RoundedButton.secondary("Cancel");
        cancelBtn.addActionListener(e -> dlg.dispose());
        RoundedButton sendBtn = new RoundedButton("Send Inquiry");
        sendBtn.addActionListener(e -> {
            try {
                Inquiry inq = new Inquiry();
                inq.setPropertyId(selectedProperty.getId());
                inq.setUserId(session.getCurrentUserId());
                inq.setSubject(subjectField.getText().trim());
                inq.setMessage(messageArea.getText().trim());
                inquiryCtrl.createInquiry(inq);
                dlg.dispose();
                JOptionPane.showMessageDialog(this, "Inquiry sent successfully.",
                    "Inquiry", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttons.add(cancelBtn);
        buttons.add(sendBtn);
        root.add(buttons, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private BigDecimal parseBD(String s) {
        try { return s.isEmpty() ? null : new BigDecimal(s.replace(",", "")); }
        catch (NumberFormatException e) { return null; }
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private JPanel labeledField(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(UIThemeManager.FONT_LABEL);
        l.setForeground(theme.getTextSecondary());
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel labeledCombo(String label, JComboBox<String> combo) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(UIThemeManager.FONT_LABEL);
        l.setForeground(theme.getTextSecondary());
        p.add(l, BorderLayout.NORTH);
        p.add(combo, BorderLayout.CENTER);
        return p;
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(UIThemeManager.FONT_BODY);
        cb.setBackground(theme.getBgSecondary());
        cb.setForeground(theme.getTextPrimary());
        cb.setPreferredSize(new Dimension(140, 36));
        return cb;
    }
}
