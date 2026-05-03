package com.eremis.ui.property;

import com.eremis.controller.PropertyController;
import com.eremis.model.Property;
import com.eremis.model.enums.PropertyStatus;
import com.eremis.model.enums.PropertyType;
import com.eremis.ui.components.RoundedButton;
import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Modal dialog for adding or editing a Property.
 * Includes smart price suggestion (average-based).
 */
public class PropertyFormDialog extends JDialog {

    private final UIThemeManager    theme;
    private final PropertyController ctrl;
    private final Property          existing;   // null = add mode
    private boolean                 saved = false;

    // Form fields
    private JTextField  titleField, locationField, cityField, priceField,
                        areaField, bedroomsField, bathroomsField,
                        ownerNameField, ownerContactField;
    private JTextArea   descArea;
    private JComboBox<PropertyType>   typeCombo;
    private JComboBox<PropertyStatus> statusCombo;
    private JLabel      priceSuggestLabel;

    public PropertyFormDialog(Frame parent, Property existing, PropertyController ctrl) {
        super(parent, existing == null ? "Add New Property" : "Edit Property", true);
        this.existing = existing;
        this.ctrl     = ctrl;
        this.theme    = UIThemeManager.getInstance();

        setSize(680, 700);
        setLocationRelativeTo(parent);
        setResizable(false);

        buildUI();
        if (existing != null) populateFields();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(theme.getBgSecondary());
        setContentPane(root);

        // ── Header ──────────────────────────────────────────────────────
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 14));
        header.setBackground(theme.getBgSidebar());
        JLabel title = new JLabel(existing == null ? "➕  New Property" : "✏️  Edit Property");
        title.setFont(UIThemeManager.FONT_SUBTITLE);
        title.setForeground(Color.WHITE);
        header.add(title);
        root.add(header, BorderLayout.NORTH);

        // ── Form ─────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(theme.getBgSecondary());
        form.setBorder(new EmptyBorder(20, 24, 10, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill   = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;

        int row = 0;

        // Title
        addRow(form, g, row++, "Property Title *", titleField = new JTextField());

        // Location & City (same row)
        g.gridx = 0; g.gridy = row; g.gridwidth = 1;
        form.add(makeLabel("Location *"), g);
        g.gridx = 1;
        form.add(locationField = new JTextField(), g);
        g.gridx = 2;
        form.add(makeLabel("City *"), g);
        g.gridx = 3;
        form.add(cityField = new JTextField(), g);
        row++;

        // Type & Status
        g.gridx = 0; g.gridy = row; g.gridwidth = 1;
        form.add(makeLabel("Type *"), g);
        g.gridx = 1;
        typeCombo = new JComboBox<>(PropertyType.values());
        styleCombo(typeCombo);
        form.add(typeCombo, g);
        g.gridx = 2;
        form.add(makeLabel("Status"), g);
        g.gridx = 3;
        statusCombo = new JComboBox<>(PropertyStatus.values());
        styleCombo(statusCombo);
        form.add(statusCombo, g);
        row++;

        // Price with suggestion
        g.gridx = 0; g.gridy = row; g.gridwidth = 1;
        form.add(makeLabel("Price (PKR) *"), g);
        g.gridx = 1;
        priceField = new JTextField();
        form.add(priceField, g);
        g.gridx = 2;
        RoundedButton suggestBtn = new RoundedButton("💡 Suggest Price",
            new Color(0xFF9800), new Color(0xF57C00), 8);
        suggestBtn.setFont(UIThemeManager.FONT_SMALL);
        suggestBtn.addActionListener(e -> suggestPrice());
        form.add(suggestBtn, g);
        g.gridx = 3;
        priceSuggestLabel = new JLabel("");
        priceSuggestLabel.setFont(UIThemeManager.FONT_SMALL);
        priceSuggestLabel.setForeground(theme.getTextSecondary());
        form.add(priceSuggestLabel, g);
        row++;

        // Area, Bedrooms, Bathrooms
        g.gridx = 0; g.gridy = row; g.gridwidth = 1;
        form.add(makeLabel("Area (sq.ft)"), g);
        g.gridx = 1; form.add(areaField = new JTextField(), g);
        g.gridx = 2; form.add(makeLabel("Bedrooms"), g);
        g.gridx = 3; form.add(bedroomsField = new JTextField("0"), g);
        row++;

        g.gridx = 0; g.gridy = row; g.gridwidth = 1;
        form.add(makeLabel("Bathrooms"), g);
        g.gridx = 1; form.add(bathroomsField = new JTextField("0"), g);
        g.gridx = 2; form.add(makeLabel("Owner Name"), g);
        g.gridx = 3; form.add(ownerNameField = new JTextField(), g);
        row++;

        addRow(form, g, row++, "Owner Contact", ownerContactField = new JTextField());

        // Description
        g.gridx = 0; g.gridy = row; g.gridwidth = 1;
        form.add(makeLabel("Description"), g);
        g.gridx = 1; g.gridwidth = 3;
        descArea = new JTextArea(3, 20);
        descArea.setFont(UIThemeManager.FONT_BODY);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBackground(theme.getBgPrimary());
        descArea.setForeground(theme.getTextPrimary());
        descArea.setBorder(BorderFactory.createLineBorder(theme.getBorder()));
        form.add(new JScrollPane(descArea), g);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        root.add(formScroll, BorderLayout.CENTER);

        // ── Buttons ──────────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnRow.setBackground(theme.getBgSecondary());
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorder()));

        RoundedButton cancelBtn = RoundedButton.secondary("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        RoundedButton saveBtn = new RoundedButton("💾  Save Property");
        saveBtn.addActionListener(e -> doSave());

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        root.add(btnRow, BorderLayout.SOUTH);
    }

    private void populateFields() {
        titleField.setText(existing.getTitle());
        locationField.setText(existing.getLocation());
        cityField.setText(existing.getCity());
        priceField.setText(existing.getPrice().toPlainString());
        if (existing.getAreaSqft() != null) areaField.setText(existing.getAreaSqft().toPlainString());
        bedroomsField.setText(String.valueOf(existing.getBedrooms()));
        bathroomsField.setText(String.valueOf(existing.getBathrooms()));
        ownerNameField.setText(existing.getOwnerName() != null ? existing.getOwnerName() : "");
        ownerContactField.setText(existing.getOwnerContact() != null ? existing.getOwnerContact() : "");
        descArea.setText(existing.getDescription() != null ? existing.getDescription() : "");
        typeCombo.setSelectedItem(existing.getType());
        statusCombo.setSelectedItem(existing.getStatus());
    }

    private void suggestPrice() {
        PropertyType type = (PropertyType) typeCombo.getSelectedItem();
        String city = cityField.getText().trim();
        if (city.isEmpty()) {
            priceSuggestLabel.setText("Enter city first");
            return;
        }
        priceSuggestLabel.setText("Fetching…");
        SwingWorker<BigDecimal, Void> w = new SwingWorker<>() {
            @Override protected BigDecimal doInBackground() {
                return ctrl.suggestPrice(type, city);
            }
            @Override protected void done() {
                try {
                    BigDecimal avg = get();
                    if (avg != null) {
                        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
                        priceSuggestLabel.setText("Avg: PKR " + fmt.format(avg));
                        priceField.setText(avg.toPlainString());
                    } else {
                        priceSuggestLabel.setText("No data for this city/type");
                    }
                } catch (Exception e) {
                    priceSuggestLabel.setText("Error fetching suggestion");
                }
            }
        };
        w.execute();
    }

    private void doSave() {
        try {
            Property p = existing != null ? existing : new Property();
            p.setTitle(titleField.getText().trim());
            p.setLocation(locationField.getText().trim());
            p.setCity(cityField.getText().trim());
            p.setPrice(new BigDecimal(priceField.getText().trim().replace(",", "")));
            String area = areaField.getText().trim();
            if (!area.isEmpty()) p.setAreaSqft(new BigDecimal(area));
            else p.setAreaSqft(null);
            p.setBedrooms(Integer.parseInt(bedroomsField.getText().trim()));
            p.setBathrooms(Integer.parseInt(bathroomsField.getText().trim()));
            p.setOwnerName(ownerNameField.getText().trim());
            p.setOwnerContact(ownerContactField.getText().trim());
            p.setDescription(descArea.getText().trim());
            p.setType((PropertyType) typeCombo.getSelectedItem());
            p.setStatus((PropertyStatus) statusCombo.getSelectedItem());

            if (existing == null) ctrl.createProperty(p);
            else ctrl.updateProperty(p);

            saved = true;
            dispose();
            JOptionPane.showMessageDialog(getParent(),
                "Property saved successfully!", "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Please enter valid numbers for price, area, bedrooms, and bathrooms.",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() { return saved; }

    // ── Helpers ──────────────────────────────────────────────────────────
    private void addRow(JPanel panel, GridBagConstraints g, int row,
                        String label, JTextField field) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1;
        panel.add(makeLabel(label), g);
        g.gridx = 1; g.gridwidth = 3;
        styleField(field);
        panel.add(field, g);
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIThemeManager.FONT_LABEL);
        l.setForeground(theme.getTextPrimary());
        return l;
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

    private void styleCombo(JComboBox<?> cb) {
        cb.setFont(UIThemeManager.FONT_BODY);
        cb.setBackground(theme.getBgPrimary());
        cb.setForeground(theme.getTextPrimary());
    }
}
