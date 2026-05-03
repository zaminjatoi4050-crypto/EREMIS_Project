package com.eremis.service;

import com.eremis.dao.PropertyDAO;
import com.eremis.dao.SearchHistoryDAO;
import com.eremis.model.Property;
import com.eremis.model.SearchHistory;
import com.eremis.model.enums.PropertyStatus;
import com.eremis.model.enums.PropertyType;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendationService {
    private final SearchHistoryDAO histDAO = new SearchHistoryDAO();
    private final PropertyDAO      propDAO = new PropertyDAO();

    public List<Property> getRecommendations(int userId) {
        try {
            List<SearchHistory> history = histDAO.findRecentByUser(userId, 10);
            if (history.isEmpty()) return Collections.emptyList();
            Map<String, Long> cityFreq = new HashMap<>();
            for (SearchHistory sh : history)
                if (sh.getCity() != null) cityFreq.merge(sh.getCity(), 1L, Long::sum);
            String topCity = cityFreq.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
            Map<String, Long> typeFreq = new HashMap<>();
            for (SearchHistory sh : history)
                if (sh.getPropertyType() != null) typeFreq.merge(sh.getPropertyType(), 1L, Long::sum);
            String topTypeStr = typeFreq.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
            PropertyType topType = null;
            if (topTypeStr != null) try { topType = PropertyType.valueOf(topTypeStr); } catch (Exception ignored) {}
            return propDAO.search(null, topCity, null, null, topType,
                PropertyStatus.AVAILABLE, "created_at", "DESC").stream().limit(5).collect(Collectors.toList());
        } catch (SQLException e) { return Collections.emptyList(); }
    }

    public void saveSearch(int userId, String keyword, String city,
                           BigDecimal min, BigDecimal max, String type) {
        try { histDAO.create(new SearchHistory(userId, keyword, city, min, max, type)); }
        catch (SQLException ignored) {}
    }
}
