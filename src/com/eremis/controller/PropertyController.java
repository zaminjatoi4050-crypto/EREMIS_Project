package com.eremis.controller;

import com.eremis.model.*;
import com.eremis.model.enums.*;
import com.eremis.service.PropertyService;

import java.math.BigDecimal;
import java.util.List;

public class PropertyController {
    private final PropertyService propertyService = new PropertyService();
    public Property createProperty(Property p)              { return propertyService.createProperty(p); }
    public Property updateProperty(Property p)              { return propertyService.updateProperty(p); }
    public void deleteProperty(int id)                       { propertyService.deleteProperty(id); }
    public List<Property> getAllProperties()                  { return propertyService.getAllProperties(); }
    public List<Property> search(String keyword, String city,
                                  BigDecimal minPrice, BigDecimal maxPrice,
                                  PropertyType type, PropertyStatus status,
                                  String sortBy, String direction) {
        return propertyService.search(keyword, city, minPrice, maxPrice, type, status, sortBy, direction);
    }
    public void advanceStatus(int propertyId, PropertyStatus newStatus) {
        propertyService.advanceStatus(propertyId, newStatus);
    }
    public BigDecimal suggestPrice(PropertyType type, String city) {
        return propertyService.suggestPrice(type, city);
    }
}
