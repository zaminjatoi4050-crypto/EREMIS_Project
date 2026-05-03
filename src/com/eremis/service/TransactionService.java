package com.eremis.service;

import com.eremis.config.DatabaseConfig;
import com.eremis.dao.PropertyDAO;
import com.eremis.dao.TransactionDAO;
import com.eremis.dao.UserDAO;
import com.eremis.model.Property;
import com.eremis.model.Transaction;
import com.eremis.model.User;
import com.eremis.model.enums.PropertyStatus;
import com.eremis.model.enums.TransactionStatus;
import com.eremis.utils.EncryptionUtil;
import com.eremis.utils.SessionManager;
import com.eremis.utils.ValidationUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Purchase/approval workflow for property transactions.
 */
public class TransactionService {

    private final DatabaseConfig db = DatabaseConfig.getInstance();
    private final PropertyDAO propertyDAO = new PropertyDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final UserDAO userDAO = new UserDAO();
    private final LoggingService logSvc = new LoggingService();
    private final NotificationService notificationService = new NotificationService();

    public Transaction initiatePurchase(int propertyId, String bankName, String accountNumber, BigDecimal amount) {
        User buyer = requireAuthenticatedBuyer();
        if (!ValidationUtil.isValidBankName(bankName)) {
            throw new IllegalArgumentException("Bank name is required.");
        }
        if (!ValidationUtil.isValidAccountNumber(accountNumber)) {
            throw new IllegalArgumentException("Account number must be 8 to 20 digits.");
        }

        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Property property = propertyDAO.findByIdForUpdate(conn, propertyId)
                    .orElseThrow(() -> new IllegalArgumentException("Property not found."));

                if (property.getStatus() != PropertyStatus.AVAILABLE) {
                    throw new IllegalStateException("Property is not available for purchase.");
                }
                if (amount == null || amount.compareTo(property.getPrice()) < 0) {
                    throw new IllegalArgumentException("Payment amount must cover the property price.");
                }
                if (transactionDAO.hasActiveForProperty(conn, propertyId)) {
                    throw new IllegalStateException("Another active purchase already exists for this property.");
                }

                User seller = userDAO.findById(property.getListedBy())
                    .orElseThrow(() -> new IllegalStateException("Seller account not found."));

                Transaction tx = new Transaction();
                tx.setBuyerId(buyer.getId());
                tx.setSellerId(seller.getId());
                tx.setPropertyId(propertyId);
                tx.setAmount(amount);
                tx.setBankName(bankName.trim());
                tx.setAccountNumberEncrypted(EncryptionUtil.encrypt(accountNumber.replaceAll("\\s+", "")));
                tx.setCreatedAt(LocalDateTime.now());

                if (seller.isAdmin()) {
                    tx.setStatus(TransactionStatus.APPROVED);
                    tx.setApprovedAt(LocalDateTime.now());
                    tx = transactionDAO.create(conn, tx);
                    propertyDAO.updateStatus(conn, propertyId, PropertyStatus.SOLD);
                    propertyDAO.transferOwnership(conn, propertyId, buyer.getId());
                    conn.commit();
                    logSvc.log(buyer.getId(), "PROPERTY_BUY_AUTO_APPROVED", "TRANSACTION", tx.getId(),
                        "Auto-approved purchase for property #" + propertyId);
                    notificationService.notifyUser(buyer.getId(),
                        "Purchase Approved", "Your purchase was auto-approved and completed.",
                        com.eremis.model.Notification.NotifType.SUCCESS);
                    notificationService.notifyUser(seller.getId(),
                        "Property Sold", "A property under your administration was purchased.",
                        com.eremis.model.Notification.NotifType.INFO);
                    return tx;
                }

                tx.setStatus(TransactionStatus.PENDING);
                tx = transactionDAO.create(conn, tx);
                propertyDAO.updateStatus(conn, propertyId, PropertyStatus.LOCKED);
                conn.commit();
                logSvc.log(buyer.getId(), "PROPERTY_BUY_PENDING", "TRANSACTION", tx.getId(),
                    "Purchase pending for property #" + propertyId);
                notificationService.notifyUser(seller.getId(),
                    "New Purchase Pending", "A buyer has submitted payment for your property.",
                    com.eremis.model.Notification.NotifType.INFO);
                return tx;
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof RuntimeException) throw (RuntimeException) ex;
                throw new RuntimeException(ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to initiate purchase.", e);
        }
    }

    public List<Transaction> getPendingTransactions() {
        try (Connection conn = db.getConnection()) {
            return transactionDAO.findPending(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load pending transactions.", e);
        }
    }

    public List<Transaction> getMyTransactions(int buyerId) {
        try (Connection conn = db.getConnection()) {
            return transactionDAO.findByBuyerId(conn, buyerId);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load your transactions.", e);
        }
    }

    public Transaction approve(int transactionId, int adminUserId) {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Transaction tx = transactionDAO.findById(conn, transactionId)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));
                if (tx.getStatus() != TransactionStatus.PENDING) {
                    throw new IllegalStateException("Only pending transactions can be approved.");
                }
                Property property = propertyDAO.findByIdForUpdate(conn, tx.getPropertyId())
                    .orElseThrow(() -> new IllegalStateException("Property not found."));

                transactionDAO.updateStatus(conn, transactionId, TransactionStatus.APPROVED,
                    LocalDateTime.now(), null, null);
                propertyDAO.updateStatus(conn, property.getId(), PropertyStatus.SOLD);
                propertyDAO.transferOwnership(conn, property.getId(), tx.getBuyerId());
                conn.commit();

                logSvc.log(adminUserId, "TRANSACTION_APPROVED", "TRANSACTION", transactionId,
                    "Approved purchase for property #" + property.getId());
                notificationService.notifyUser(tx.getBuyerId(), "Purchase Approved",
                    "Your property purchase was approved.", com.eremis.model.Notification.NotifType.SUCCESS);
                notificationService.notifyUser(tx.getSellerId(), "Funds Released",
                    "Payment released to your account for property #" + property.getId(),
                    com.eremis.model.Notification.NotifType.INFO);
                tx.setStatus(TransactionStatus.APPROVED);
                tx.setApprovedAt(LocalDateTime.now());
                return tx;
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof RuntimeException) throw (RuntimeException) ex;
                throw new RuntimeException(ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to approve transaction.", e);
        }
    }

    public Transaction reject(int transactionId, int adminUserId, String reason) {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Transaction tx = transactionDAO.findById(conn, transactionId)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));
                if (tx.getStatus() != TransactionStatus.PENDING) {
                    throw new IllegalStateException("Only pending transactions can be rejected.");
                }
                Property property = propertyDAO.findByIdForUpdate(conn, tx.getPropertyId())
                    .orElseThrow(() -> new IllegalStateException("Property not found."));

                transactionDAO.updateStatus(conn, transactionId, TransactionStatus.REJECTED,
                    null, LocalDateTime.now(), reason);
                propertyDAO.updateStatus(conn, property.getId(), PropertyStatus.AVAILABLE);
                conn.commit();

                logSvc.log(adminUserId, "TRANSACTION_REJECTED", "TRANSACTION", transactionId,
                    "Rejected purchase for property #" + property.getId());
                notificationService.notifyUser(tx.getBuyerId(), "Purchase Rejected",
                    reason == null || reason.isBlank() ? "Your payment was rejected and refunded." : reason,
                    com.eremis.model.Notification.NotifType.WARNING);
                tx.setStatus(TransactionStatus.REJECTED);
                tx.setRejectedAt(LocalDateTime.now());
                tx.setRejectionReason(reason);
                return tx;
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof RuntimeException) throw (RuntimeException) ex;
                throw new RuntimeException(ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to reject transaction.", e);
        }
    }

    private User requireAuthenticatedBuyer() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new SecurityException("Please log in to buy a property.");
        }
        if (!user.isBuyer()) {
            throw new SecurityException("Only buyer accounts can complete purchases.");
        }
        return user;
    }
}