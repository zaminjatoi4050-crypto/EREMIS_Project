package com.eremis.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records each search a user performs — used by the recommendation engine.
 */
public class SearchHistory {

    private int           id;
    private int           userId;
    private String        keyword;
    private String        city;
    private BigDecimal    minPrice;
    private BigDecimal    maxPrice;
    private String        propertyType;
    private LocalDateTime searchedAt;

    public SearchHistory() {}

    public SearchHistory(int userId, String keyword, String city,
                         BigDecimal minPrice, BigDecimal maxPrice, String propertyType) {
        this.userId       = userId;
        this.keyword      = keyword;
        this.city         = city;
        this.minPrice     = minPrice;
        this.maxPrice     = maxPrice;
        this.propertyType = propertyType;
    }

    public int           getId()            { return id; }
    public void          setId(int id)      { this.id = id; }

    public int           getUserId()        { return userId; }
    public void          setUserId(int v)   { this.userId = v; }

    public String        getKeyword()       { return keyword; }
    public void          setKeyword(String v){ this.keyword = v; }

    public String        getCity()          { return city; }
    public void          setCity(String v)  { this.city = v; }

    public BigDecimal    getMinPrice()      { return minPrice; }
    public void          setMinPrice(BigDecimal v){ this.minPrice = v; }

    public BigDecimal    getMaxPrice()      { return maxPrice; }
    public void          setMaxPrice(BigDecimal v){ this.maxPrice = v; }

    public String        getPropertyType()  { return propertyType; }
    public void          setPropertyType(String v){ this.propertyType = v; }

    public LocalDateTime getSearchedAt()   { return searchedAt; }
    public void          setSearchedAt(LocalDateTime v){ this.searchedAt = v; }
}
