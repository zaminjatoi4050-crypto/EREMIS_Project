package com.eremis.service;

import com.eremis.dao.LogDAO;
import com.eremis.model.Log;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingService {
    private static final Logger LOGGER = Logger.getLogger(LoggingService.class.getName());
    private final LogDAO logDAO = new LogDAO();

    public void log(Integer userId, String action, String entityType, Integer entityId, String details) {
        Integer persistedUserId = userId != null && userId > 0 ? userId : null;
        try {
            logDAO.create(new Log(persistedUserId, action, entityType, entityId, details));
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to persist audit log: " + action, e);
        }
        LOGGER.info("[AUDIT] " + action + " | " + entityType + "#" + entityId + " | " + details);
    }

    public List<Log> getRecentLogs(int limit) {
        try { return logDAO.findAll(limit); }
        catch (SQLException e) { return Collections.emptyList(); }
    }
}
