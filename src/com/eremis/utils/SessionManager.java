package com.eremis.utils;

import com.eremis.model.User;
import java.time.LocalDateTime;

/**
 * Thread-safe singleton session manager.
 * Holds the currently logged-in user for the duration of the desktop session.
 */
public class SessionManager {

    private static SessionManager instance;
    private User   currentUser;
    private LocalDateTime loginTime;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void startSession(User user) {
        this.currentUser = user;
        this.loginTime   = LocalDateTime.now();
    }

    public void endSession() {
        this.currentUser = null;
        this.loginTime   = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public boolean isSeller() {
        return currentUser != null && currentUser.isSeller();
    }

    public boolean isBuyer() {
        return currentUser != null && currentUser.isBuyer();
    }

    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }
}
