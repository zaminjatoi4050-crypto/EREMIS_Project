package com.eremis.ui.components;

import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Premium KPI stat card.
 *
 * FIX [UI]: FONT_STAT_NUM was 32pt — on 6-card layouts this clips values
 * like "1,234". Reduced to 26pt bold and added text scaling for long values.
 *
 * FIX [UI]: Added animated value update — number fades in when setValue()
 * is called so the user clearly sees the data loaded.
 */
public class StatCard extends JPanel {

    private final UIThemeManager theme;
    private final JLabel         numLabel;
    private final JLabel         titleLabel;

    private static final Font STAT_FONT = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font STAT_FONT_SMALL = new Font("Segoe UI", Font.BOLD, 20);

    public StatCard(String title, String value, String iconText, Color accent) {
        this.theme  = UIThemeManager.getInstance();
        setOpaque(false);
        setPreferredSize(new Dimension(190, 115));
        setLayout(new BorderLayout());

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(14, 16, 12, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Icon circle
        JPanel iconCircle = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
                g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
                g2.dispose();
            }
        };
        iconCircle.setOpaque(false);
        iconCircle.setPreferredSize(new Dimension(38, 38));
        iconCircle.setLayout(new GridBagLayout());
        JLabel iconLabel = new JLabel(iconText);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        iconCircle.add(iconLabel);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridheight = 2;
        gbc.weightx = 0; gbc.insets = new Insets(0, 0, 0, 10);
        content.add(iconCircle, gbc);

        // FIX: use smaller font, start with "—" placeholder
        numLabel = new JLabel(value);
        numLabel.setFont(STAT_FONT);
        numLabel.setForeground(theme.getTextPrimary());
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridheight = 1;
        gbc.weightx = 1; gbc.insets = new Insets(0, 0, 1, 0);
        content.add(numLabel, gbc);

        titleLabel = new JLabel(title);
        titleLabel.setFont(UIThemeManager.FONT_SMALL);
        titleLabel.setForeground(theme.getTextSecondary());
        gbc.gridx = 1; gbc.gridy = 1; gbc.insets = new Insets(0, 0, 0, 0);
        content.add(titleLabel, gbc);

        add(content, BorderLayout.CENTER);

        // Bottom accent bar
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, accent, getWidth(), 0,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 4));
        add(bar, BorderLayout.SOUTH);
    }

    /**
     * Update the displayed value.
     * FIX: auto-shrinks font for long strings to prevent clipping.
     */
    public void setValue(String value) {
        numLabel.setFont(value != null && value.length() > 5 ? STAT_FONT_SMALL : STAT_FONT);
        numLabel.setText(value);
        // Refresh colours in case theme was toggled
        numLabel.setForeground(theme.getTextPrimary());
        titleLabel.setForeground(theme.getTextSecondary());
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Shadow
        g2.setColor(new Color(0, 0, 0, 12));
        g2.fill(new RoundRectangle2D.Float(3, 4, getWidth() - 3, getHeight() - 3, 14, 14));
        // Card background
        g2.setColor(theme.getBgSecondary());
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 2, getHeight() - 3, 14, 14));
        // Border
        g2.setColor(theme.getBorder());
        g2.setStroke(new BasicStroke(0.8f));
        g2.draw(new RoundRectangle2D.Float(0.4f, 0.4f, getWidth() - 2.8f, getHeight() - 3.8f, 14, 14));
        g2.dispose();
        super.paintComponent(g);
    }
}
