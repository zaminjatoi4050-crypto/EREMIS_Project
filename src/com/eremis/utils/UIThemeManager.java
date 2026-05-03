package com.eremis.utils;

import javax.swing.*;
import java.awt.*;

/**
 * EREMIS Premium UI Theme Manager
 * Luxury Real-Estate grade colour system — inspired by zameen.com pro aesthetics.
 */
public class UIThemeManager {

    public enum Theme { LIGHT, DARK }

    private static UIThemeManager instance;
    private Theme currentTheme = Theme.LIGHT;

    // ── Premium Light palette (deep navy + gold — professional real-estate) ──
    private static final Color LIGHT_BG_PRIMARY     = new Color(0xF4F6F9);
    private static final Color LIGHT_BG_SECONDARY   = new Color(0xFFFFFF);
    private static final Color LIGHT_BG_SIDEBAR     = new Color(0x0A1628);
    private static final Color LIGHT_SIDEBAR_TEXT   = new Color(0x8FA8C8);
    private static final Color LIGHT_SIDEBAR_SEL    = new Color(0xC8A95A);
    private static final Color LIGHT_TEXT_PRIMARY   = new Color(0x0D1B2A);
    private static final Color LIGHT_TEXT_SECONDARY = new Color(0x6B7B8D);
    private static final Color LIGHT_BORDER         = new Color(0xE2E8F0);
    private static final Color LIGHT_ACCENT         = new Color(0xC8A95A);
    private static final Color LIGHT_ACCENT_HOVER   = new Color(0xB8942A);
    private static final Color LIGHT_ACCENT2        = new Color(0x0A1628);
    private static final Color LIGHT_SUCCESS        = new Color(0x2E7D52);
    private static final Color LIGHT_WARNING        = new Color(0xD97706);
    private static final Color LIGHT_DANGER         = new Color(0xDC2626);
    private static final Color LIGHT_CARD_SHADOW    = new Color(0, 0, 0, 18);
    private static final Color LIGHT_HIGHLIGHT      = new Color(0xFFF8E7);

    // ── Premium Dark palette ──
    private static final Color DARK_BG_PRIMARY      = new Color(0x0D1117);
    private static final Color DARK_BG_SECONDARY    = new Color(0x161B22);
    private static final Color DARK_BG_SIDEBAR      = new Color(0x0A0F1A);
    private static final Color DARK_SIDEBAR_TEXT    = new Color(0x8B949E);
    private static final Color DARK_SIDEBAR_SEL     = new Color(0xC8A95A);
    private static final Color DARK_TEXT_PRIMARY    = new Color(0xE6EDF3);
    private static final Color DARK_TEXT_SECONDARY  = new Color(0x8B949E);
    private static final Color DARK_BORDER          = new Color(0x21262D);
    private static final Color DARK_ACCENT          = new Color(0xC8A95A);
    private static final Color DARK_ACCENT_HOVER    = new Color(0xE5C97A);
    private static final Color DARK_ACCENT2         = new Color(0x1F6FEB);
    private static final Color DARK_SUCCESS         = new Color(0x3FB950);
    private static final Color DARK_WARNING         = new Color(0xD29922);
    private static final Color DARK_DANGER          = new Color(0xF85149);
    private static final Color DARK_CARD_SHADOW     = new Color(0, 0, 0, 60);
    private static final Color DARK_HIGHLIGHT       = new Color(0x1E1A0E);

    private UIThemeManager() {}

    public static synchronized UIThemeManager getInstance() {
        if (instance == null) instance = new UIThemeManager();
        return instance;
    }

    public Theme getCurrentTheme() { return currentTheme; }
    public boolean isDark()         { return currentTheme == Theme.DARK; }
    public void setTheme(Theme theme)  { this.currentTheme = theme; }
    public void toggleTheme()          { currentTheme = isDark() ? Theme.LIGHT : Theme.DARK; }

    public Color getBgPrimary()     { return isDark() ? DARK_BG_PRIMARY     : LIGHT_BG_PRIMARY; }
    public Color getBgSecondary()   { return isDark() ? DARK_BG_SECONDARY   : LIGHT_BG_SECONDARY; }
    public Color getBgSidebar()     { return isDark() ? DARK_BG_SIDEBAR     : LIGHT_BG_SIDEBAR; }
    public Color getSidebarText()   { return isDark() ? DARK_SIDEBAR_TEXT   : LIGHT_SIDEBAR_TEXT; }
    public Color getSidebarSel()    { return isDark() ? DARK_SIDEBAR_SEL    : LIGHT_SIDEBAR_SEL; }
    public Color getTextPrimary()   { return isDark() ? DARK_TEXT_PRIMARY   : LIGHT_TEXT_PRIMARY; }
    public Color getTextSecondary() { return isDark() ? DARK_TEXT_SECONDARY : LIGHT_TEXT_SECONDARY; }
    public Color getBorder()        { return isDark() ? DARK_BORDER         : LIGHT_BORDER; }
    public Color getAccent()        { return isDark() ? DARK_ACCENT         : LIGHT_ACCENT; }
    public Color getAccentHover()   { return isDark() ? DARK_ACCENT_HOVER   : LIGHT_ACCENT_HOVER; }
    public Color getAccent2()       { return isDark() ? DARK_ACCENT2        : LIGHT_ACCENT2; }
    public Color getSuccess()       { return isDark() ? DARK_SUCCESS        : LIGHT_SUCCESS; }
    public Color getWarning()       { return isDark() ? DARK_WARNING        : LIGHT_WARNING; }
    public Color getDanger()        { return isDark() ? DARK_DANGER         : LIGHT_DANGER; }
    public Color getCardShadow()    { return isDark() ? DARK_CARD_SHADOW    : LIGHT_CARD_SHADOW; }
    public Color getHighlight()     { return isDark() ? DARK_HIGHLIGHT      : LIGHT_HIGHLIGHT; }
    public Color getWhite()         { return isDark() ? new Color(0xE6EDF3) : Color.WHITE; }
    public Color getCardBg()        { return getBgSecondary(); }

    public void applyBackground(JComponent c) {
        c.setBackground(getBgPrimary());
        c.setForeground(getTextPrimary());
        c.setOpaque(true);
    }

    public static final Font FONT_TITLE     = new Font("Segoe UI", Font.BOLD,  24);
    public static final Font FONT_SUBTITLE  = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BODY      = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL     = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_LABEL     = new Font("Segoe UI", Font.BOLD,  12);
    public static final Font FONT_BUTTON    = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_SIDEBAR   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_HEADER    = new Font("Segoe UI", Font.BOLD,  30);
    public static final Font FONT_STAT_NUM  = new Font("Segoe UI", Font.BOLD,  32);
    public static final Font FONT_BRAND     = new Font("Segoe UI", Font.BOLD,  20);
    public static final Font FONT_CAPTION   = new Font("Segoe UI", Font.ITALIC, 11);

    public static final Color GOLD       = new Color(0xC8A95A);
    public static final Color GOLD_LIGHT = new Color(0xFFF0CC);
    public static final Color NAVY       = new Color(0x0A1628);
    public static final Color NAVY_MID   = new Color(0x1A3A5C);
}
