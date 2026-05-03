package com.eremis.dao;

import com.eremis.config.DatabaseConfig;
import com.eremis.model.PropertyImage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO for property_images table.
 *
 * FIX [FEATURE]: This class was entirely missing despite the table existing
 * in the schema and PropertyImage model being defined.  Without it, image
 * upload functionality cannot be persisted.
 */
public class PropertyImageDAO {

    private static final Logger  LOGGER = Logger.getLogger(PropertyImageDAO.class.getName());
    private final DatabaseConfig db     = DatabaseConfig.getInstance();

    // ── Mappers ─────────────────────────────────────────────────────────────

    private PropertyImage mapRow(ResultSet rs) throws SQLException {
        PropertyImage img = new PropertyImage();
        img.setId(rs.getInt("id"));
        img.setPropertyId(rs.getInt("property_id"));
        img.setFilePath(rs.getString("file_path"));
        img.setPrimary(rs.getInt("is_primary") == 1);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) img.setCreatedAt(ts.toLocalDateTime());
        return img;
    }

    // ── Write operations ────────────────────────────────────────────────────

    public PropertyImage create(PropertyImage img) throws SQLException {
        String sql = "INSERT INTO property_images (property_id, file_path, is_primary) VALUES (?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, img.getPropertyId());
            ps.setString(2, img.getFilePath());
            ps.setInt(3, img.isPrimary() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) img.setId(keys.getInt(1));
            }
            LOGGER.fine("Saved image: " + img.getFilePath());
            return img;
        }
    }

    /**
     * Persist a batch of images for one property in a single transaction.
     * Rolls back all inserts if any fail.
     */
    public void createBatch(List<PropertyImage> images) throws SQLException {
        if (images == null || images.isEmpty()) return;
        String sql = "INSERT INTO property_images (property_id, file_path, is_primary) VALUES (?,?,?)";
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (PropertyImage img : images) {
                    ps.setInt(1, img.getPropertyId());
                    ps.setString(2, img.getFilePath());
                    ps.setInt(3, img.isPrimary() ? 1 : 0);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                LOGGER.info("Batch saved " + images.size() + " images.");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Replace all images for a property (used when editing an existing listing).
     */
    public void replaceAll(int propertyId, List<PropertyImage> images) throws SQLException {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete existing
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM property_images WHERE property_id = ?")) {
                    del.setInt(1, propertyId);
                    del.executeUpdate();
                }
                // Insert new batch
                if (images != null && !images.isEmpty()) {
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO property_images (property_id, file_path, is_primary) VALUES (?,?,?)")) {
                        for (PropertyImage img : images) {
                            ins.setInt(1, propertyId);
                            ins.setString(2, img.getFilePath());
                            ins.setInt(3, img.isPrimary() ? 1 : 0);
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM property_images WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteByProperty(int propertyId) throws SQLException {
        String sql = "DELETE FROM property_images WHERE property_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, propertyId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Read operations ─────────────────────────────────────────────────────

    public List<PropertyImage> findByProperty(int propertyId) throws SQLException {
        List<PropertyImage> list = new ArrayList<>();
        String sql = "SELECT * FROM property_images WHERE property_id = ? ORDER BY is_primary DESC, id ASC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, propertyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public PropertyImage findPrimaryByProperty(int propertyId) throws SQLException {
        String sql = "SELECT * FROM property_images WHERE property_id = ? AND is_primary = 1 LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, propertyId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public boolean setPrimary(int imageId, int propertyId) throws SQLException {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Clear existing primary
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE property_images SET is_primary=0 WHERE property_id=?")) {
                    ps.setInt(1, propertyId); ps.executeUpdate();
                }
                // Set new primary
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE property_images SET is_primary=1 WHERE id=?")) {
                    ps.setInt(1, imageId); ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback(); throw e;
            } finally { conn.setAutoCommit(true); }
        }
    }
}
