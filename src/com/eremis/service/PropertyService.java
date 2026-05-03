package com.eremis.service;

import com.eremis.dao.PropertyDAO;
import com.eremis.model.Property;
import com.eremis.model.enums.PropertyStatus;
import com.eremis.model.enums.PropertyType;
import com.eremis.utils.SessionManager;
import com.eremis.utils.ValidationUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Business logic for Property operations.
 * Validates, delegates to DAO, and triggers side-effects (logging, notifications).
 */
public class PropertyService {

    private static final Logger LOGGER   = Logger.getLogger(PropertyService.class.getName());
    private final PropertyDAO   propDAO  = new PropertyDAO();
    private final LoggingService logSvc  = new LoggingService();

    /**
     * Create a new property after validation.
     * @throws IllegalArgumentException on validation failure
     */
    public Property createProperty(Property p) {
        validate(p);
        p.setListedBy(SessionManager.getInstance().getCurrentUserId());
        if (p.getStatus() == null) p.setStatus(PropertyStatus.AVAILABLE);
        try {
            p = propDAO.create(p);
            logSvc.log(p.getListedBy(), "PROPERTY_CREATE", "PROPERTY", p.getId(),
                       "Created: " + p.getTitle());
            return p;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create property", e);
            throw new RuntimeException("Database error while creating property.", e);
        }
    }

    public Property updateProperty(Property p) {
        validate(p);
        try {
            Property existing = propDAO.findById(p.getId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found."));
            int currentUserId = SessionManager.getInstance().getCurrentUserId();
            boolean adminLike = SessionManager.getInstance().getCurrentUser() != null
                && SessionManager.getInstance().getCurrentUser().getRole().isAdminLike();
            if (!adminLike && existing.getListedBy() != currentUserId) {
                throw new SecurityException("You can only edit properties you created.");
            }
            p.setListedBy(existing.getListedBy());
            boolean updated = propDAO.update(p);
            if (!updated) {
                throw new IllegalStateException("Property update did not persist.");
            }
            logSvc.log(SessionManager.getInstance().getCurrentUserId(),
                       "PROPERTY_UPDATE", "PROPERTY", p.getId(),
                       "Updated: " + p.getTitle());
            return p;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update property", e);
            throw new RuntimeException("Database error while updating property.", e);
        }
    }

    public void deleteProperty(int id) {
        try {
            if (SessionManager.getInstance().getCurrentUser() == null
                || !SessionManager.getInstance().getCurrentUser().getRole().isAdminLike()) {
                throw new SecurityException("Only admin accounts can delete properties.");
            }
            boolean deleted = propDAO.delete(id);
            if (!deleted) {
                throw new IllegalStateException("Property delete did not persist.");
            }
            logSvc.log(SessionManager.getInstance().getCurrentUserId(),
                       "PROPERTY_DELETE", "PROPERTY", id, "Deleted property #" + id);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete property " + id, e);
            throw new RuntimeException("Database error while deleting property.", e);
        }
    }

    public List<Property> getAllProperties() {
        try { return propDAO.findAll(); }
        catch (SQLException e) { throw new RuntimeException("Cannot load properties.", e); }
    }

    public List<Property> search(String keyword, String city, BigDecimal minPrice,
                                 BigDecimal maxPrice, PropertyType type,
                                 PropertyStatus status, String sortBy, String dir) {
        try {
            return propDAO.search(keyword, city, minPrice, maxPrice, type, status, sortBy, dir);
        } catch (SQLException e) {
            throw new RuntimeException("Search failed.", e);
        }
    }

    /**
     * Advance the status lifecycle: AVAILABLE → RESERVED → SOLD.
     * Enforces the one-directional transition rule.
     */
    public void advanceStatus(int propertyId, PropertyStatus newStatus) {
        try {
            Property current = propDAO.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found."));
            validateStatusTransition(current.getStatus(), newStatus);
            boolean updated = propDAO.updateStatus(propertyId, newStatus);
            if (!updated) {
                throw new IllegalStateException("Property status update did not persist.");
            }
            logSvc.log(SessionManager.getInstance().getCurrentUserId(),
                       "STATUS_CHANGE", "PROPERTY", propertyId,
                       "Status → " + newStatus);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot update status.", e);
        }
    }

    /**
     * Smart price suggestion — returns average price of comparable properties,
     * or null if no data is available yet.
     */
    public BigDecimal suggestPrice(PropertyType type, String city) {
        try { return propDAO.getAveragePrice(type, city); }
        catch (SQLException e) { return null; }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private void validate(Property p) {
        if (ValidationUtil.isNullOrBlank(p.getTitle()))
            throw new IllegalArgumentException("Property title is required.");
        if (ValidationUtil.isNullOrBlank(p.getCity()))
            throw new IllegalArgumentException("City is required.");
        if (ValidationUtil.isNullOrBlank(p.getLocation()))
            throw new IllegalArgumentException("Location is required.");
        if (!ValidationUtil.isPositiveDecimal(p.getPrice()))
            throw new IllegalArgumentException("Price must be greater than 0.");
        if (p.getAreaSqft() != null && p.getAreaSqft().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Area cannot be negative.");
        if (p.getBedrooms() < 0 || p.getBathrooms() < 0)
            throw new IllegalArgumentException("Bedrooms and bathrooms cannot be negative.");
        if (!ValidationUtil.isValidPhone(p.getOwnerContact()))
            throw new IllegalArgumentException("Owner contact number is invalid.");
        if (p.getType() == null)
            throw new IllegalArgumentException("Property type is required.");
    }

    private void validateStatusTransition(PropertyStatus current, PropertyStatus next) {
        if (current == next) {
            return;
        }

        if (current == PropertyStatus.SOLD)
            throw new IllegalStateException("A sold property cannot change status.");

        switch (current) {
            case AVAILABLE:
                if (next != PropertyStatus.RESERVED && next != PropertyStatus.LOCKED) {
                    throw new IllegalStateException("Available properties can only move to reserved or locked.");
                }
                break;
            case RESERVED:
                if (next != PropertyStatus.SOLD) {
                    throw new IllegalStateException("Reserved properties can only move to sold.");
                }
                break;
            case LOCKED:
                if (next != PropertyStatus.AVAILABLE && next != PropertyStatus.SOLD) {
                    throw new IllegalStateException("Locked properties can only return to available or move to sold.");
                }
                break;
            case SOLD:
                throw new IllegalStateException("A sold property cannot change status.");
        }
    }
}
