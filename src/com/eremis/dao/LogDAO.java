package com.eremis.dao;

import com.eremis.config.DatabaseConfig;
import com.eremis.model.Log;
import java.sql.*;
import java.util.*;

public class LogDAO {
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public Log create(Log log) throws SQLException {
        String sql = "INSERT INTO logs (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (log.getUserId() != null) ps.setInt(1, log.getUserId());
            else ps.setNull(1, Types.INTEGER);
            ps.setString(2, log.getAction());
            ps.setString(3, log.getEntityType());
            if (log.getEntityId() != null) ps.setInt(4, log.getEntityId());
            else ps.setNull(4, Types.INTEGER);
            ps.setString(5, log.getDetails());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) log.setId(keys.getInt(1));
            }
            return log;
        }
    }

    public List<Log> findAll(int limit) throws SQLException {
        List<Log> list = new ArrayList<>();
        String sql = "SELECT l.*, u.username FROM logs l " +
                     "LEFT JOIN users u ON l.user_id = u.id " +
                     "ORDER BY l.created_at DESC LIMIT ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Log l = new Log();
                    l.setId(rs.getInt("id"));
                    l.setAction(rs.getString("action"));
                    l.setEntityType(rs.getString("entity_type"));
                    int eid = rs.getInt("entity_id");
                    if (!rs.wasNull()) l.setEntityId(eid);
                    l.setDetails(rs.getString("details"));
                    l.setUsername(rs.getString("username"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) l.setCreatedAt(ts.toLocalDateTime());
                    list.add(l);
                }
            }
        }
        return list;
    }
}
