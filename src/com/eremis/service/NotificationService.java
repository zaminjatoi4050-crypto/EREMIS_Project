package com.eremis.service;

import com.eremis.dao.NotificationDAO;
import com.eremis.dao.UserDAO;
import com.eremis.model.Notification;
import com.eremis.model.enums.UserRole;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationService {
    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());
    private final NotificationDAO notifDAO = new NotificationDAO();
    private final UserDAO         userDAO  = new UserDAO();

    public void notifyUser(int userId, String title, String message, Notification.NotifType type) {
        try { notifDAO.create(new Notification(userId, title, message, type)); }
        catch (SQLException e) { LOGGER.log(Level.WARNING, "Could not create notification for user " + userId, e); }
    }

    public void notifyAdmins(String title, String message) {
        try {
            userDAO.findByRole(UserRole.ADMIN).forEach(admin ->
                notifyUser(admin.getId(), title, message, Notification.NotifType.INFO));
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "Could not load admins for notification broadcast", e); }
    }

    public List<Notification> getForUser(int userId) {
        try { return notifDAO.findByUser(userId); }
        catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not load notifications for user " + userId, e);
            return Collections.emptyList();
        }
    }

    public int countUnread(int userId) {
        try { return notifDAO.countUnread(userId); }
        catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not count unread notifications for user " + userId, e);
            return 0;
        }
    }

    public void markAllRead(int userId) {
        try { notifDAO.markAllRead(userId); }
        catch (SQLException e) { LOGGER.log(Level.WARNING, "Could not mark notifications as read for user " + userId, e); }
    }
}
