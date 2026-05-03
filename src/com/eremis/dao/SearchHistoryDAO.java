package com.eremis.dao;

import com.eremis.config.DatabaseConfig;
import com.eremis.model.SearchHistory;

import java.sql.*;
import java.util.*;

/**
 * DAO for search history records — used by the recommendation engine.
 */
public class SearchHistoryDAO {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public SearchHistory create(SearchHistory sh) throws SQLException {
        String sql = "INSERT INTO search_history " +
                     "(user_id, keyword, city, min_price, max_price, property_type) " +
                     "VALUES (?,?,?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sh.getUserId());
            ps.setString(2, sh.getKeyword());
            ps.setString(3, sh.getCity());
            ps.setBigDecimal(4, sh.getMinPrice());
            ps.setBigDecimal(5, sh.getMaxPrice());
            ps.setString(6, sh.getPropertyType());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) sh.setId(keys.getInt(1));
            }
            return sh;
        }
    }

    /** Returns the last N searches for a user (most-recent first). */
    public List<SearchHistory> findRecentByUser(int userId, int limit) throws SQLException {
        List<SearchHistory> list = new ArrayList<>();
        String sql = "SELECT * FROM search_history WHERE user_id = ? " +
                     "ORDER BY searched_at DESC LIMIT ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SearchHistory sh = new SearchHistory();
                    sh.setId(rs.getInt("id"));
                    sh.setUserId(rs.getInt("user_id"));
                    sh.setKeyword(rs.getString("keyword"));
                    sh.setCity(rs.getString("city"));
                    sh.setMinPrice(rs.getBigDecimal("min_price"));
                    sh.setMaxPrice(rs.getBigDecimal("max_price"));
                    sh.setPropertyType(rs.getString("property_type"));
                    Timestamp ts = rs.getTimestamp("searched_at");
                    if (ts != null) sh.setSearchedAt(ts.toLocalDateTime());
                    list.add(sh);
                }
            }
        }
        return list;
    }

    /** Top cities searched across all users — returns [city, count] pairs. */
    public List<String[]> getTopCities(int limit) throws SQLException {
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
}
