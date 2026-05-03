package com.eremis.ui.components;

import com.eremis.utils.UIThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

/**
 * A pre-styled JTable with modern row colours, custom header, and
 * optional status-badge rendering in a designated "Status" column.
 */
public class StyledTable extends JTable {

    private final UIThemeManager theme = UIThemeManager.getInstance();

    public StyledTable(TableModel model) {
        super(model);
        applyStyle();
    }

    public StyledTable() {
        super();
        applyStyle();
    }

    private void applyStyle() {
        setFont(UIThemeManager.FONT_BODY);
        setRowHeight(44);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setSelectionBackground(theme.getAccent());
        setSelectionForeground(Color.WHITE);
        setBackground(theme.getBgSecondary());
        setForeground(theme.getTextPrimary());
        setFillsViewportHeight(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom header
        JTableHeader header = getTableHeader();
        header.setFont(UIThemeManager.FONT_LABEL);
        header.setBackground(theme.getBgPrimary());
        header.setForeground(theme.getTextSecondary());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, theme.getBorder()));
        header.setPreferredSize(new Dimension(0, 40));
        header.setReorderingAllowed(false);

        // Default cell renderer — with alternating rows
        setDefaultRenderer(Object.class, new AlternatingRowRenderer());
    }

    // ── Alternating-row renderer ───────────────────────────────────────────
    private class AlternatingRowRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setBorder(new EmptyBorder(0, 12, 0, 12));
            setFont(UIThemeManager.FONT_BODY);

            if (isSelected) {
                setBackground(theme.getAccent());
                setForeground(Color.WHITE);
            } else {
                boolean even = (row % 2 == 0);
                Color base = theme.getBgSecondary();
                setBackground(even ? base : mixColors(base, theme.getBgPrimary(), 0.5f));
                setForeground(theme.getTextPrimary());
            }
            return this;
        }
    }

    /** Status badge renderer — coloured pill based on value string. */
    public static class StatusBadgeRenderer extends JPanel
            implements TableCellRenderer {

        private final JLabel label = new JLabel();
        private final UIThemeManager theme = UIThemeManager.getInstance();

        public StatusBadgeRenderer() {
            setLayout(new GridBagLayout());
            label.setFont(UIThemeManager.FONT_SMALL);
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(2, 10, 2, 10));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            add(label);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String text   = value == null ? "" : value.toString();
            Color  bg;
            switch (text.toUpperCase()) {
                case "AVAILABLE": bg = theme.getSuccess();  break;
                case "LOCKED":    bg = new Color(0x1E88E5); break;
                case "RESERVED":  bg = theme.getWarning();  break;
                case "SOLD":      bg = theme.getDanger();   break;
                case "PENDING":   bg = theme.getWarning();  break;
                case "APPROVED":  bg = theme.getSuccess();  break;
                case "REJECTED":  bg = theme.getDanger();   break;
                case "CONTACTED": bg = theme.getAccent();   break;
                case "CLOSED":    bg = theme.getTextSecondary(); break;
                case "ADMIN":     bg = new Color(0x9C27B0); break;
                case "SELLER":    bg = new Color(0x2E7D52); break;
                case "USER":
                case "BUYER":     bg = theme.getAccent();   break;
                default:          bg = theme.getTextSecondary();
            }
            label.setText(text);
            label.setBackground(bg);
            label.setForeground(Color.WHITE);

            boolean even = (row % 2 == 0);
            Color rowBg  = isSelected ? theme.getAccent()
                                      : (even ? theme.getBgSecondary()
                                               : mixColors(theme.getBgSecondary(),
                                                           theme.getBgPrimary(), 0.5f));
            setBackground(rowBg);
            return this;
        }
    }

    /** Blend two colours. ratio=0 → 100% c1, ratio=1 → 100% c2 */
    private static Color mixColors(Color c1, Color c2, float ratio) {
        int r = (int)(c1.getRed()   * (1-ratio) + c2.getRed()   * ratio);
        int g = (int)(c1.getGreen() * (1-ratio) + c2.getGreen() * ratio);
        int b = (int)(c1.getBlue()  * (1-ratio) + c2.getBlue()  * ratio);
        return new Color(
            Math.min(255, Math.max(0, r)),
            Math.min(255, Math.max(0, g)),
            Math.min(255, Math.max(0, b)));
    }
}
