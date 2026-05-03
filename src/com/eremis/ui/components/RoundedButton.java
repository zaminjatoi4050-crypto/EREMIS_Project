package com.eremis.ui.components;

import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Premium pill-style button with smooth hover + press animation.
 * Supports gold accent, navy, danger, success variants.
 */
public class RoundedButton extends JButton {

    private Color bgColor;
    private Color hoverColor;
    private Color pressColor;
    private int   arc;
    private boolean pressed = false;

    public RoundedButton(String text) {
        this(text, null, null, 12);
    }

    public RoundedButton(String text, Color bg, Color hover, int arc) {
        super(text);
        this.arc        = arc;
        this.bgColor    = bg    != null ? bg    : UIThemeManager.GOLD;
        this.hoverColor = hover != null ? hover : UIThemeManager.GOLD.darker();
        this.pressColor = this.hoverColor.darker();

        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(Color.WHITE);
        setFont(UIThemeManager.FONT_BUTTON);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(getPreferredSize().width + 24, 40));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                setBackground(hoverColor); repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                setBackground(bgColor); pressed = false; repaint();
            }
            @Override public void mousePressed(MouseEvent e) {
                pressed = true; repaint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                pressed = false; repaint();
            }
        });
        setBackground(bgColor);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        Color bg = pressed ? pressColor : getBackground();

        // Drop shadow
        if (!pressed) {
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fill(new RoundRectangle2D.Float(2, 3, getWidth() - 2, getHeight() - 2, arc, arc));
        }

        // Button background
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - (pressed?0:1), getHeight() - (pressed?0:1), arc, arc));

        // Subtle top shine
        if (!pressed) {
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fill(new RoundRectangle2D.Float(2, 1, getWidth() - 4, getHeight() / 2, arc, arc));
        }

        super.paintComponent(g);
        g2.dispose();
    }

    public void setColors(Color bg, Color hover) {
        this.bgColor    = bg;
        this.hoverColor = hover;
        this.pressColor = hover.darker();
        setBackground(bg);
    }

    /** Navy/dark variant. */
    public static RoundedButton navy(String text) {
        RoundedButton b = new RoundedButton(text, UIThemeManager.NAVY, UIThemeManager.NAVY_MID, 12);
        b.setForeground(Color.WHITE);
        return b;
    }

    /** Danger variant (red). */
    public static RoundedButton danger(String text) {
        UIThemeManager t = UIThemeManager.getInstance();
        return new RoundedButton(text, t.getDanger(), t.getDanger().darker(), 12);
    }

    /** Success variant (green). */
    public static RoundedButton success(String text) {
        UIThemeManager t = UIThemeManager.getInstance();
        return new RoundedButton(text, t.getSuccess(), t.getSuccess().darker(), 12);
    }

    /** Outlined secondary variant. */
    public static RoundedButton secondary(String text) {
        UIThemeManager t = UIThemeManager.getInstance();
        RoundedButton b = new RoundedButton(text, t.getBgSecondary(), t.getBorder(), 12) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(UIThemeManager.getInstance().getBorder());
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.75f, 0.75f, getWidth()-1.5f, getHeight()-1.5f, 12, 12));
                super.paintComponent(g);
                g2.dispose();
            }
        };
        b.setForeground(t.getTextPrimary());
        return b;
    }
}
