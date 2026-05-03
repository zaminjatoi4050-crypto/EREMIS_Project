package com.eremis.service;

import com.eremis.dao.InquiryDAO;
import com.eremis.dao.PropertyDAO;
import com.eremis.model.enums.InquiryStatus;
import com.eremis.model.enums.PropertyStatus;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Analytics service — aggregates dashboard metrics.
 *
 * FIX [BUG]: Original implementation wrapped all 8 stat queries in a single
 * try-catch that silently dropped ALL results if any one query failed.
 * Now each stat is fetched individually; failures are logged and that slot
 * defaults to 0/empty, allowing the rest of the dashboard to render normally.
 */
public class AnalyticsService {

    private static final Logger LOGGER = Logger.getLogger(AnalyticsService.class.getName());

    private final PropertyDAO propDAO = new PropertyDAO();
    private final UserService userSvc = new UserService();
    private final InquiryDAO  inqDAO  = new InquiryDAO();

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalProperties",  safeCount(() -> propDAO.count(),                             "totalProperties"));
        stats.put("availableProps",   safeCount(() -> propDAO.countByStatus(PropertyStatus.AVAILABLE), "availableProps"));
        stats.put("reservedProps",    safeCount(() -> propDAO.countByStatus(PropertyStatus.RESERVED),  "reservedProps"));
        stats.put("soldProps",        safeCount(() -> propDAO.countByStatus(PropertyStatus.SOLD),       "soldProps"));
        stats.put("activeUsers",      safeCount(() -> userSvc.countActiveUsers(),                        "activeUsers"));
        stats.put("pendingInquiries", safeCount(() -> inqDAO.countByStatus(InquiryStatus.PENDING),      "pendingInquiries"));
        stats.put("addedThisMonth",   safeCount(() -> propDAO.countAddedThisMonth(),                    "addedThisMonth"));

        // Top cities — returns empty list on failure instead of crashing the dashboard
        try {
            stats.put("topCities", propDAO.getMostSearchedCities(5));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load top cities", e);
            stats.put("topCities", Collections.emptyList());
        }

        return stats;
    }

    /** Supplier that can throw SQLException. */
    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }

    private int safeCount(SqlSupplier<Integer> supplier, String metricName) {
        try {
            Integer val = supplier.get();
            return val != null ? val : 0;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Analytics metric failed: " + metricName, e);
            return 0;
        }
    }
}
