package com.eremis.dao;

import com.eremis.config.DatabaseConfig;
import com.eremis.dao.interfaces.GenericDAO;
import com.eremis.model.Property;
import com.eremis.model.enums.PropertyStatus;
import com.eremis.model.enums.PropertyType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * JDBC implementation for Property entities.
 * All search/filter methods use PreparedStatements to prevent SQL injection.
 */
public class PropertyDAO implements GenericDAO<Property, Integer> {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    private Property mapRow(ResultSet rs) throws SQLException {
        Property p = new Property();
        p.setId(rs.getInt("id"));
        p.setTitle(rs.getString("title"));
        p.setDescription(rs.getString("description"));
        p.setLocation(rs.getString("location"));
        p.setCity(rs.getString("city"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setAreaSqft(rs.getBigDecimal("area_sqft"));
        p.setBedrooms(rs.getInt("bedrooms"));
        p.setBathrooms(rs.getInt("bathrooms"));
        p.setType(PropertyType.valueOf(rs.getString("type")));
        p.setStatus(PropertyStatus.valueOf(rs.getString("status")));
        p.setOwnerName(rs.getString("owner_name"));
        p.setOwnerContact(rs.getString("owner_contact"));
        p.setListedBy(rs.getInt("listed_by"));
        try { p.setListedByName(rs.getString("listed_by_name")); } catch (SQLException ignored) {}
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) p.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) p.setUpdatedAt(updated.toLocalDateTime());
        return p;
    }

    @Override
    public Property create(Property p) throws SQLException {
        String sql = "INSERT INTO properties " +
            "(title, description, location, city, price, area_sqft, bedrooms, bathrooms, " +
            " type, status, owner_name, owner_contact, listed_by) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getTitle());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getLocation());
            ps.setString(4, p.getCity());
            ps.setBigDecimal(5, p.getPrice());
            ps.setBigDecimal(6, p.getAreaSqft());
            ps.setInt(7, p.getBedrooms());
            ps.setInt(8, p.getBathrooms());
            ps.setString(9, p.getType().name());
            ps.setString(10, p.getStatus() != null ? p.getStatus().name() : PropertyStatus.AVAILABLE.name());
            ps.setString(11, p.getOwnerName());
            ps.setString(12, p.getOwnerContact());
            ps.setInt(13, p.getListedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
            return p;
        }
    }

    @Override
    public Optional<Property> findById(Integer id) throws SQLException {
        try (Connection conn = db.getConnection()) {
            return findById(conn, id);
        }
    }

    public Optional<Property> findById(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT p.*, u.full_name AS listed_by_name " +
                     "FROM properties p LEFT JOIN users u ON p.listed_by = u.id " +
                     "WHERE p.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Property> findAll() throws SQLException {
        return findAll("created_at", "DESC");
    }

    public List<Property> findAll(String sortBy, String direction) throws SQLException {
        List<Property> list = new ArrayList<>();
        // Whitelist sort columns to prevent injection
        String safeSort = Arrays.asList("title","city","price","status","type","created_at")
                                .contains(sortBy) ? sortBy : "created_at";
        String safeDir  = "ASC".equalsIgnoreCase(direction) ? "ASC" : "DESC";
        String sql = "SELECT p.*, u.full_name AS listed_by_name " +
                     "FROM properties p LEFT JOIN users u ON p.listed_by = u.id " +
                     "ORDER BY p." + safeSort + " " + safeDir;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public boolean update(Property p) throws SQLException {
        try (Connection conn = db.getConnection()) {
            return update(conn, p);
        }
    }

    public boolean update(Connection conn, Property p) throws SQLException {
        String sql = "UPDATE properties SET title=?, description=?, location=?, city=?, " +
                     "price=?, area_sqft=?, bedrooms=?, bathrooms=?, type=?, status=?, " +
                     "owner_name=?, owner_contact=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getTitle());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getLocation());
            ps.setString(4, p.getCity());
            ps.setBigDecimal(5, p.getPrice());
            ps.setBigDecimal(6, p.getAreaSqft());
            ps.setInt(7, p.getBedrooms());
            ps.setInt(8, p.getBathrooms());
            ps.setString(9, p.getType().name());
            ps.setString(10, p.getStatus().name());
            ps.setString(11, p.getOwnerName());
            ps.setString(12, p.getOwnerContact());
            ps.setInt(13, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM properties WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(Connection conn, int propertyId, PropertyStatus newStatus) throws SQLException {
        String sql = "UPDATE properties SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, propertyId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean transferOwnership(Connection conn, int propertyId, int newOwnerId) throws SQLException {
        String sql = "UPDATE properties SET listed_by = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newOwnerId);
            ps.setInt(2, propertyId);
            return ps.executeUpdate() > 0;
        }
    }

    public Optional<Property> findByIdForUpdate(Connection conn, int id) throws SQLException {
        String sql = "SELECT p.*, u.full_name AS listed_by_name " +
                     "FROM properties p LEFT JOIN users u ON p.listed_by = u.id " +
                     "WHERE p.id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public int count() throws SQLException {
        return countByStatus(null);
    }

    public int countByStatus(PropertyStatus status) throws SQLException {
        String sql = status == null
            ? "SELECT COUNT(*) FROM properties"
            : "SELECT COUNT(*) FROM properties WHERE status = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (status != null) ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Advanced search with optional filters — all parameters nullable.
     */
    public List<Property> search(String keyword, String city, BigDecimal minPrice,
                                 BigDecimal maxPrice, PropertyType type,
                                 PropertyStatus status, String sortBy, String direction)
            throws SQLException {

        List<Property> list  = new ArrayList<>();
        List<Object>   params = new ArrayList<>();
        StringBuilder  sql   = new StringBuilder(
            "SELECT p.*, u.full_name AS listed_by_name " +
            "FROM properties p LEFT JOIN users u ON p.listed_by = u.id WHERE 1=1");

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (p.title LIKE ? OR p.location LIKE ? OR p.description LIKE ?)");
            String q = "%" + keyword.trim() + "%";
            params.add(q); params.add(q); params.add(q);
        }
        if (city != null && !city.isBlank()) {
            sql.append(" AND p.city LIKE ?");
            params.add("%" + city.trim() + "%");
        }
        if (minPrice != null) { sql.append(" AND p.price >= ?"); params.add(minPrice); }
        if (maxPrice != null) { sql.append(" AND p.price <= ?"); params.add(maxPrice); }
        if (type   != null)  { sql.append(" AND p.type = ?");   params.add(type.name()); }
        if (status != null)  { sql.append(" AND p.status = ?"); params.add(status.name()); }

        String safeSort = Arrays.asList("title","city","price","status","type","created_at")
                                .contains(sortBy) ? sortBy : "created_at";
        String safeDir  = "ASC".equalsIgnoreCase(direction) ? "ASC" : "DESC";
        sql.append(" ORDER BY p.").append(safeSort).append(" ").append(safeDir);

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Smart price suggestion — returns the average price of properties
     * of the same type in the same city that are AVAILABLE.
     * Returns null if no comparable data exists.
     */
    public BigDecimal getAveragePrice(PropertyType type, String city) throws SQLException {
        String sql = "SELECT AVG(price) FROM properties " +
                     "WHERE type = ? AND city = ? AND status = 'AVAILABLE'";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ps.setString(2, city);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getBigDecimal(1) != null) {
                    return rs.getBigDecimal(1).setScale(2, java.math.RoundingMode.HALF_UP);
                }
            }
        }
        return null;
    }

    /**
     * Update only the status field (status lifecycle: AVAILABLE → RESERVED → SOLD).
     */
    public boolean updateStatus(int propertyId, PropertyStatus newStatus) throws SQLException {
        String sql = "UPDATE properties SET status = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, propertyId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Returns most-searched cities (for the analytics dashboard).
     */
    public List<String[]> getMostSearchedCities(int limit) throws SQLException {
        List<String[]> results = new ArrayList<>();
        String sql = "SELECT city, COUNT(*) AS cnt FROM search_history " +
                     "WHERE city IS NOT NULL GROUP BY city ORDER BY cnt DESC LIMIT ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new String[]{rs.getString("city"),
                                             String.valueOf(rs.getInt("cnt"))});
                }
            }
        }
        return results;
    }

    /**
     * Properties added this month (for dashboard stats).
     */
    public int countAddedThisMonth() throws SQLException {
        String sql = "SELECT COUNT(*) FROM properties " +
                     "WHERE MONTH(created_at) = MONTH(NOW()) AND YEAR(created_at) = YEAR(NOW())";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
