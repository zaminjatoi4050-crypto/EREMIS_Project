package com.eremis.model.enums;

public enum PropertyType {
    HOUSE("House"),
    APARTMENT("Apartment"),
    COMMERCIAL("Commercial"),
    LAND("Land"),
    VILLA("Villa"),
    CONDO("Condo");

    private final String displayName;

    PropertyType(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}
