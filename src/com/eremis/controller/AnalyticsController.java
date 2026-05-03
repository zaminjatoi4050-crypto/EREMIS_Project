package com.eremis.controller;

import com.eremis.service.AnalyticsService;
import java.util.Map;

/**
 * FIX [PERF - N+1 BUG]: Original code called getDashboardStats() once per
 * KPI card — 6 separate DB round-trips for a single dashboard load.
 * Now fetchStats() is called once and all 6 values are read from that
 * single result map.
 */
public class AnalyticsController {

    private final AnalyticsService analyticsService = new AnalyticsService();

    /**
     * Fetch all dashboard KPIs in a single database round-trip.
     * DashboardPanel calls this once and distributes values to each StatCard.
     */
    public Map<String, Object> fetchStats() {
        return analyticsService.getDashboardStats();
    }

    // ── Convenience accessors (still use a pre-fetched map) ───────────────

    public int getTotalProperties()        { return intFrom(fetchStats(), "totalProperties"); }
    public int getAvailableProperties()    { return intFrom(fetchStats(), "availableProps"); }
    public int getSoldProperties()         { return intFrom(fetchStats(), "soldProps"); }
    public int getActiveUsers()            { return intFrom(fetchStats(), "activeUsers"); }
    public int getPendingInquiries()       { return intFrom(fetchStats(), "pendingInquiries"); }
    public int getPropertiesAddedThisMonth(){ return intFrom(fetchStats(), "addedThisMonth"); }

    @SuppressWarnings("unchecked")
    public java.util.List<String> getTopCities(int limit) {
        Object v = fetchStats().get("topCities");
        if (v instanceof java.util.List) return (java.util.List<String>) v;
        return java.util.Collections.emptyList();
    }

    private static int intFrom(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }
}
