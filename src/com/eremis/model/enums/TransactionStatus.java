package com.eremis.model.enums;

/**
 * Purchase transaction lifecycle.
 */
public enum TransactionStatus {
    PENDING("Pending", "#F39C12"),
    APPROVED("Approved", "#27AE60"),
    REJECTED("Rejected", "#E74C3C");

    private final String displayName;
    private final String colorHex;

    TransactionStatus(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() { return displayName; }
    public String getColorHex() { return colorHex; }

    @Override
    public String toString() { return displayName; }
}