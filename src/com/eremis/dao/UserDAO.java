package com.eremis.dao;

import com.eremis.config.DatabaseConfig;
import com.eremis.dao.interfaces.GenericDAO;
import com.eremis.model.User;
import com.eremis.model.enums.UserRole;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * JDBC implementation of GenericDAO for User entities.
 */
public class UserDAO implements GenericDAO<User, Integer> {

    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());
    private final DatabaseConfig db    = DatabaseConfig.getInstance();

    // ── Column → field mapping helper ─────────────────────────────────────
    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setPhone(readOptionalString(rs, "phone"));
        u.setCity(readOptionalString(rs, "city"));
        String roleStr = rs.getString("role");
        if (roleStr != null) {
            try {
                u.setRole(UserRole.valueOf(roleStr.toUpperCase()));
            } catch (IllegalArgumentException iae) {
                // Fallback: attempt to match by display name
                for (UserRole r : UserRole.values()) {
                    if (r.getDisplayName().equalsIgnoreCase(roleStr)) { u.setRole(r); break; }
                }
            }
        }
        u.setActive(rs.getInt("is_active") == 1);
        u.setLoginAttempts(rs.getInt("login_attempts"));
        Timestamp locked = rs.getTimestamp("locked_until");
        if (locked != null) u.setLockedUntil(locked.toLocalDateTime());
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) u.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) u.setUpdatedAt(updated.toLocalDateTime());
        return u;
    }

    private String readOptionalString(ResultSet rs, String column) throws SQLException {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if ("S0022".equals(e.getSQLState()) || message.contains("column")) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public User create(User u) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, username, password_hash, role, phone, city) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getUsername());
            ps.setString(4, u.getPasswordHash());
            ps.setString(5, u.getRole() != null ? u.getRole().name() : UserRole.USER.name());
            ps.setString(6, u.getPhone());
            ps.setString(7, u.getCity());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) u.setId(keys.getInt(1));
            }
            LOGGER.info("Created user: " + u.getUsername());
            return u;
        }
    }

    @Override
    public Optional<User> findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public boolean update(User u) throws SQLException {
        String sql = "UPDATE users SET full_name=?, email=?, username=?, " +
                     "password_hash=?, role=?, is_active=? WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getUsername());
            ps.setString(4, u.getPasswordHash());
            ps.setString(5, u.getRole().name());
            ps.setInt(6, u.isActive() ? 1 : 0);
            ps.setInt(7, u.getId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(Integer id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Domain-specific queries ────────────────────────────────────────────

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<User> findByRole(UserRole role) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY full_name";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public int countActiveUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE is_active = 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Increment failed login attempts and optionally set lockout time.
     */
    public void updateLoginAttempts(int userId, int attempts, LocalDateTime lockedUntil)
            throws SQLException {
        String sql = "UPDATE users SET login_attempts=?, locked_until=? WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attempts);
            ps.setTimestamp(2, lockedUntil != null
                               ? Timestamp.valueOf(lockedUntil) : null);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Reset attempts and lockout after successful login.
     */
    public void resetLoginAttempts(int userId) throws SQLException {
        String sql = "UPDATE users SET login_attempts=0, locked_until=NULL WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Update password hash and clear lockout metadata after password reset.
     */
    public boolean updatePasswordHash(int userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash=?, login_attempts=0, locked_until=NULL WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean isUsernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean isEmailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Update non-sensitive profile fields only — does NOT touch password_hash.
     * FIX [BUG-5]: Introduced to support UserService.updateUser() safely.
     */
    public boolean updateProfile(User u) throws SQLException {
        String sql = "UPDATE users SET full_name=?, email=?, username=?, " +
                     "role=?, is_active=?, phone=?, city=? WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getUsername());
            ps.setString(4, u.getRole() != null ? u.getRole().name() : UserRole.USER.name());
            ps.setInt(5, u.isActive() ? 1 : 0);
            ps.setString(6, u.getPhone());
            ps.setString(7, u.getCity());
            ps.setInt(8, u.getId());
            return ps.executeUpdate() > 0;
        }
    }

}
