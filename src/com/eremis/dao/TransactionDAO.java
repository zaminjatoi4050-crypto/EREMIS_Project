package com.eremis.dao;

import com.eremis.config.DatabaseConfig;
import com.eremis.model.Transaction;
import com.eremis.model.enums.TransactionStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for transaction persistence.
 */
public class TransactionDAO {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction tx = new Transaction();
        tx.setId(rs.getInt("id"));
        tx.setBuyerId(rs.getInt("buyer_id"));
        tx.setSellerId(rs.getInt("seller_id"));
        tx.setPropertyId(rs.getInt("property_id"));
        tx.setAmount(rs.getBigDecimal("amount"));
        tx.setBankName(rs.getString("bank_name"));
        tx.setAccountNumberEncrypted(rs.getString("account_number_encrypted"));
        tx.setStatus(TransactionStatus.valueOf(rs.getString("status")));
        tx.setRejectionReason(rs.getString("rejection_reason"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) tx.setCreatedAt(created.toLocalDateTime());
        Timestamp approved = rs.getTimestamp("approved_at");
        if (approved != null) tx.setApprovedAt(approved.toLocalDateTime());
        Timestamp rejected = rs.getTimestamp("rejected_at");
        if (rejected != null) tx.setRejectedAt(rejected.toLocalDateTime());
        try { tx.setBuyerName(rs.getString("buyer_name")); } catch (SQLException ignored) {}
        try { tx.setSellerName(rs.getString("seller_name")); } catch (SQLException ignored) {}
        try { tx.setPropertyTitle(rs.getString("property_title")); } catch (SQLException ignored) {}
        return tx;
    }

    public Transaction create(Connection conn, Transaction tx) throws SQLException {
        String sql = "INSERT INTO transactions " +
            "(buyer_id, seller_id, property_id, amount, bank_name, account_number_encrypted, status, rejection_reason, created_at, approved_at, rejected_at) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tx.getBuyerId());
            ps.setInt(2, tx.getSellerId());
            ps.setInt(3, tx.getPropertyId());
            ps.setBigDecimal(4, tx.getAmount());
            ps.setString(5, tx.getBankName());
            ps.setString(6, tx.getAccountNumberEncrypted());
            ps.setString(7, tx.getStatus().name());
            ps.setString(8, tx.getRejectionReason());
            ps.setTimestamp(9, Timestamp.valueOf(tx.getCreatedAt() != null ? tx.getCreatedAt() : LocalDateTime.now()));
            ps.setTimestamp(10, tx.getApprovedAt() != null ? Timestamp.valueOf(tx.getApprovedAt()) : null);
            ps.setTimestamp(11, tx.getRejectedAt() != null ? Timestamp.valueOf(tx.getRejectedAt()) : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) tx.setId(keys.getInt(1));
            }
            return tx;
        }
    }

    public Optional<Transaction> findById(Connection conn, int id) throws SQLException {
        String sql = joinedSelect() + " WHERE t.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Transaction> findPending(Connection conn) throws SQLException {
        return findByStatus(conn, TransactionStatus.PENDING);
    }

    public List<Transaction> findByBuyerId(Connection conn, int buyerId) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = joinedSelect() + " WHERE t.buyer_id = ? ORDER BY t.created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public boolean hasActiveForProperty(Connection conn, int propertyId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE property_id = ? AND status = 'PENDING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, propertyId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean updateStatus(Connection conn, int transactionId, TransactionStatus status,
                                LocalDateTime approvedAt, LocalDateTime rejectedAt, String rejectionReason)
            throws SQLException {
        String sql = "UPDATE transactions SET status=?, approved_at=?, rejected_at=?, rejection_reason=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setTimestamp(2, approvedAt != null ? Timestamp.valueOf(approvedAt) : null);
            ps.setTimestamp(3, rejectedAt != null ? Timestamp.valueOf(rejectedAt) : null);
            ps.setString(4, rejectionReason);
            ps.setInt(5, transactionId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Transaction> findByStatus(Connection conn, TransactionStatus status) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = joinedSelect() + " WHERE t.status = ? ORDER BY t.created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Transaction> findAll(Connection conn) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = joinedSelect() + " ORDER BY t.created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private String joinedSelect() {
        return "SELECT t.*, p.title AS property_title, " +
            "buyer.full_name AS buyer_name, seller.full_name AS seller_name " +
            "FROM transactions t " +
            "LEFT JOIN properties p ON t.property_id = p.id " +
            "LEFT JOIN users buyer ON t.buyer_id = buyer.id " +
            "LEFT JOIN users seller ON t.seller_id = seller.id";
    }
}