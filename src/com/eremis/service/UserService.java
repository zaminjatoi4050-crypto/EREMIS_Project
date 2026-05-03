package com.eremis.service;

import com.eremis.dao.UserDAO;
import com.eremis.model.User;
import com.eremis.utils.PasswordUtil;
import com.eremis.utils.SessionManager;
import com.eremis.utils.ValidationUtil;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/**
 * Business logic for User management.
 *
 * FIX [BUG-5]: updateUser() no longer allows a plaintext password to be
 * persisted. Password changes now go through a dedicated changePassword()
 * method that hashes properly before writing.
 */
public class UserService {

    private final UserDAO        userDAO = new UserDAO();
    private final LoggingService log     = new LoggingService();

    // ── Create ─────────────────────────────────────────────────────────────

    public User createUser(User user) {
        validateUser(user, true);
        user.setFullName(user.getFullName().trim());
        user.setEmail(user.getEmail().trim());
        user.setUsername(user.getUsername().trim());
        user.setPasswordHash(PasswordUtil.hash(user.getPasswordHash()));
        try {
            if (userDAO.isUsernameExists(user.getUsername()))
                throw new IllegalArgumentException("Username already taken.");
            if (userDAO.isEmailExists(user.getEmail()))
                throw new IllegalArgumentException("Email already registered.");
            user = userDAO.create(user);
            log.log(SessionManager.getInstance().getCurrentUserId(),
                    "USER_CREATE", "USER", user.getId(), "Created: " + user.getUsername());
            return user;
        } catch (SQLIntegrityConstraintViolationException e) {
            String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (message.contains("username") || message.contains("email") || message.contains("duplicate")) {
                throw new IllegalArgumentException("Username or email already exists.");
            }
            throw new RuntimeException("Database constraint error creating user.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Database error creating user: " + e.getMessage(), e);
        }
    }

    // ── Update profile (no password field) ────────────────────────────────

    /**
     * Update non-sensitive profile fields: full name, email, role, active flag.
     * The password_hash column is NOT touched here.
     *
     * FIX [BUG-5]: Previously called userDAO.update() which overwrites
     * password_hash with whatever is in the User object — if the caller
     * passes a plaintext string it gets stored unsalted.
     */
    public User updateUser(User user) {
        validateUser(user, false);
        try {
            userDAO.findByUsername(user.getUsername()).ifPresent(existing -> {
                if (existing.getId() != user.getId()) {
                    throw new IllegalArgumentException("Username already taken.");
                }
            });
            userDAO.findByEmail(user.getEmail()).ifPresent(existing -> {
                if (existing.getId() != user.getId()) {
                    throw new IllegalArgumentException("Email already registered.");
                }
            });
            userDAO.updateProfile(user);   // profile-only update, no password column
            log.log(SessionManager.getInstance().getCurrentUserId(),
                    "USER_UPDATE", "USER", user.getId(), "Updated: " + user.getUsername());
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating user.", e);
        }
    }

    /**
     * Explicitly change a user's password.
     * Validates strength, hashes, then persists.
     *
     * @param userId      target user
     * @param newPassword plaintext new password (will be BCrypt-hashed)
     */
    public void changePassword(int userId, String newPassword) {
        if (!ValidationUtil.isValidPassword(newPassword))
            throw new IllegalArgumentException(
                "Password must be at least 8 characters and contain at least one digit.");
        try {
            String hash = PasswordUtil.hash(newPassword);
            userDAO.updatePasswordHash(userId, hash);
            log.log(SessionManager.getInstance().getCurrentUserId(),
                    "PASSWORD_CHANGE", "USER", userId, "Password changed for user #" + userId);
        } catch (SQLException e) {
            throw new RuntimeException("Database error changing password.", e);
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    public void deleteUser(int id) {
        if (id == SessionManager.getInstance().getCurrentUserId())
            throw new IllegalStateException("Cannot delete your own account.");
        try {
            userDAO.delete(id);
            log.log(SessionManager.getInstance().getCurrentUserId(),
                    "USER_DELETE", "USER", id, "Deleted user #" + id);
        } catch (SQLException e) {
            throw new RuntimeException("Database error deleting user.", e);
        }
    }

    // ── Queries ─────────────────────────────────────────────────────────────

    public List<User> getAllUsers() {
        try { return userDAO.findAll(); }
        catch (SQLException e) { throw new RuntimeException("Cannot load users.", e); }
    }

    public int countActiveUsers() {
        try { return userDAO.countActiveUsers(); }
        catch (SQLException e) { return 0; }
    }

    // ── Validation ──────────────────────────────────────────────────────────

    private void validateUser(User u, boolean newUser) {
        if (ValidationUtil.isNullOrBlank(u.getFullName()))
            throw new IllegalArgumentException("Full name is required.");
        if (!ValidationUtil.isValidEmail(u.getEmail()))
            throw new IllegalArgumentException("Valid email is required.");
        if (!ValidationUtil.isValidUsername(u.getUsername()))
            throw new IllegalArgumentException("Username must be 3-50 alphanumeric characters.");
        if (!ValidationUtil.isValidPhone(u.getPhone()))
            throw new IllegalArgumentException("Phone number is invalid.");
        if (newUser && !ValidationUtil.isValidPassword(u.getPasswordHash()))
            throw new IllegalArgumentException("Password must be at least 8 characters.");
    }
}
