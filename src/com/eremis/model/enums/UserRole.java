package com.eremis.model.enums;

/**
 * User roles for role-based access control.
 */
public enum UserRole {
    ADMIN("Admin"),
    SELLER("Seller"),
    USER("Buyer"),
    AGENT("Agent"),
    ANALYST("Analyst");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public boolean isAdminLike() {
        return this == ADMIN || this == ANALYST;
    }

    public boolean isSellerLike() {
        return this == SELLER || this == AGENT;
    }

    public boolean isBuyerLike() {
        return this == USER;
    }

    @Override
    public String toString() { return displayName; }
}
