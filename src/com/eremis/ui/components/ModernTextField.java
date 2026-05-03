package com.eremis.ui.components;

import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Premium text field with animated focus border (gold accent) and placeholder.
 */
public class ModernTextField extends JPanel {

    private final UIThemeManager theme = UIThemeManager.getInstance();
    private final JTextField     field;
    private final String         placeholder;
    private boolean              focused = false;

    public ModernTextField(String placeholder) {
        this(placeholder, 200);
    }

    public ModernTextField(String placeholder, int preferredWidth) {
        this.placeholder = placeholder;
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(preferredWidth, 44));

        field = new JTextField();
        field.setFont(UIThemeManager.FONT_BODY);
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
        field.setForeground(theme.getTextPrimary());
        field.setCaretColor(UIThemeManager.GOLD);

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
            @Override public void focusLost(FocusEvent e)   { focused = false; repaint(); }
        });

        add(field, BorderLayout.CENTER);
    }

    public String getText()          { return field.getText(); }
    public void   setText(String t)  { field.setText(t); }
    public JTextField getField()     { return field; }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2.setColor(theme.getBgSecondary());
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

        // Border — gold when focused, subtle when not
        if (focused) {
            g2.setColor(UIThemeManager.GOLD);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(1f, 1f, getWidth()-2, getHeight()-2, 10, 10));
            // Glow effect
            g2.setColor(new Color(200, 169, 90, 30));
            g2.setStroke(new BasicStroke(4f));
            g2.draw(new RoundRectangle2D.Float(2f, 2f, getWidth()-4, getHeight()-4, 10, 10));
        } else {
            g2.setColor(theme.getBorder());
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, 10, 10));
        }

        // Placeholder
        if (field.getText().isEmpty() && !focused) {
            g2.setFont(UIThemeManager.FONT_BODY);
            g2.setColor(theme.getTextSecondary());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(placeholder, 15, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
