package com.eremis.model.enums;

/**
 * Property status lifecycle: AVAILABLE → LOCKED/RESERVED → SOLD
 */
public enum PropertyStatus {
    AVAILABLE("Available", "#27AE60"),
     LOCKED("Locked",       "#1E88E5"),
    RESERVED("Reserved",   "#F39C12"),
    SOLD("Sold",           "#E74C3C");

    private final String displayName;
    private final String colorHex;

    PropertyStatus(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex    = colorHex;
    }

    public String getDisplayName() { return displayName; }
    public String getColorHex()    { return colorHex; }

    @Override
    public String toString() { return displayName; }
}
