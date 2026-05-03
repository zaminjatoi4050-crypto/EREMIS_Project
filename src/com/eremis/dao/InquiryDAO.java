package com.eremis.dao;

import com.eremis.config.DatabaseConfig;
import com.eremis.dao.interfaces.GenericDAO;
import com.eremis.model.Inquiry;
import com.eremis.model.enums.InquiryStatus;

import java.sql.*;
import java.util.*;

/**
 * JDBC DAO for Inquiry (CRM) entities.
 */
public class InquiryDAO implements GenericDAO<Inquiry, Integer> {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    private Inquiry mapRow(ResultSet rs) throws SQLException {
        Inquiry i = new Inquiry();
        i.setId(rs.getInt("id"));
        i.setPropertyId(rs.getInt("property_id"));
        i.setUserId(rs.getInt("user_id"));
        i.setSubject(rs.getString("subject"));
        i.setMessage(rs.getString("message"));
        i.setStatus(InquiryStatus.valueOf(rs.getString("status")));
        i.setNotes(rs.getString("notes"));
        int at = rs.getInt("assigned_to");
        if (!rs.wasNull()) i.setAssignedTo(at);
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) i.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) i.setUpdatedAt(updated.toLocalDateTime());
        try { i.setPropertyTitle(rs.getString("property_title")); } catch (SQLException ignored) {}
        try { i.setUserName(rs.getString("user_name")); }          catch (SQLException ignored) {}
        try { i.setAssignedToName(rs.getString("assigned_name")); } catch (SQLException ignored) {}
        return i;
    }

    @Override
    public Inquiry create(Inquiry inq) throws SQLException {
        String sql = "INSERT INTO inquiries (property_id, user_id, subject, message, status, notes, assigned_to) " +
                     "VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, inq.getPropertyId());
            ps.setInt(2, inq.getUserId());
            ps.setString(3, inq.getSubject());
            ps.setString(4, inq.getMessage());
            ps.setString(5, InquiryStatus.PENDING.name());
            ps.setString(6, inq.getNotes());
            if (inq.getAssignedTo() != null) ps.setInt(7, inq.getAssignedTo());
            else ps.setNull(7, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) inq.setId(keys.getInt(1));
            }
            return inq;
        }
    }

    @Override
    public Optional<Inquiry> findById(Integer id) throws SQLException {
        String sql = joinedSelect() + " WHERE i.id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Inquiry> findAll() throws SQLException {
        List<Inquiry> list = new ArrayList<>();
        String sql = joinedSelect() + " ORDER BY i.created_at DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Inquiry> findByStatus(InquiryStatus status) throws SQLException {
        List<Inquiry> list = new ArrayList<>();
        String sql = joinedSelect() + " WHERE i.status = ? ORDER BY i.created_at DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Inquiry> findByUserId(int userId) throws SQLException {
        List<Inquiry> list = new ArrayList<>();
        String sql = joinedSelect() + " WHERE i.user_id = ? ORDER BY i.created_at DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public boolean update(Inquiry inq) throws SQLException {
        String sql = "UPDATE inquiries SET status=?, notes=?, assigned_to=? WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inq.getStatus().name());
            ps.setString(2, inq.getNotes());
            if (inq.getAssignedTo() != null) ps.setInt(3, inq.getAssignedTo());
            else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, inq.getId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM inquiries WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM inquiries";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countByStatus(InquiryStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inquiries WHERE status = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private String joinedSelect() {
        return "SELECT i.*, p.title AS property_title, " +
               "u.full_name AS user_name, a.full_name AS assigned_name " +
               "FROM inquiries i " +
               "LEFT JOIN properties p ON i.property_id = p.id " +
               "LEFT JOIN users u ON i.user_id = u.id " +
               "LEFT JOIN users a ON i.assigned_to = a.id";
    }
}
