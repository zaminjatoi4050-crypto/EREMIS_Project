package com.eremis.model.enums;

public enum InquiryStatus {
    PENDING("Pending",     "#F39C12"),
    CONTACTED("Contacted", "#2980B9"),
    CLOSED("Closed",       "#7F8C8D");

    private final String displayName;
    private final String colorHex;

    InquiryStatus(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex    = colorHex;
    }

    public String getDisplayName() { return displayName; }
    public String getColorHex()    { return colorHex; }

    @Override
    public String toString() { return displayName; }
}
