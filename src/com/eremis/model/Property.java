package com.eremis.model;

import com.eremis.model.enums.PropertyStatus;
import com.eremis.model.enums.PropertyType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Property entity — maps to the 'properties' table.
 */
public class Property {

    private int            id;
    private String         title;
    private String         description;
    private String         location;
    private String         city;
    private BigDecimal     price;
    private BigDecimal     areaSqft;
    private int            bedrooms;
    private int            bathrooms;
    private PropertyType   type;
    private PropertyStatus status;
    private String         ownerName;
    private String         ownerContact;
    private int            listedBy;      // user id
    private String         listedByName;  // joined display
    private LocalDateTime  createdAt;
    private LocalDateTime  updatedAt;

    // Transient: loaded separately when needed
    private List<PropertyImage> images = new ArrayList<>();

    public Property() {}

    // ── Getters & Setters ──────────────────────────────────────────────────
    public int            getId()               { return id; }
    public void           setId(int id)         { this.id = id; }

    public String         getTitle()             { return title; }
    public void           setTitle(String v)     { this.title = v; }

    public String         getDescription()       { return description; }
    public void           setDescription(String v){ this.description = v; }

    public String         getLocation()          { return location; }
    public void           setLocation(String v)  { this.location = v; }

    public String         getCity()              { return city; }
    public void           setCity(String v)      { this.city = v; }

    public BigDecimal     getPrice()             { return price; }
    public void           setPrice(BigDecimal v) { this.price = v; }

    public BigDecimal     getAreaSqft()           { return areaSqft; }
    public void           setAreaSqft(BigDecimal v){ this.areaSqft = v; }

    public int            getBedrooms()          { return bedrooms; }
    public void           setBedrooms(int v)     { this.bedrooms = v; }

    public int            getBathrooms()         { return bathrooms; }
    public void           setBathrooms(int v)    { this.bathrooms = v; }

    public PropertyType   getType()              { return type; }
    public void           setType(PropertyType v){ this.type = v; }

    public PropertyStatus getStatus()            { return status; }
    public void           setStatus(PropertyStatus v){ this.status = v; }

    public String         getOwnerName()         { return ownerName; }
    public void           setOwnerName(String v) { this.ownerName = v; }

    public String         getOwnerContact()      { return ownerContact; }
    public void           setOwnerContact(String v){ this.ownerContact = v; }

    public int            getListedBy()          { return listedBy; }
    public void           setListedBy(int v)     { this.listedBy = v; }

    public String         getListedByName()      { return listedByName; }
    public void           setListedByName(String v){ this.listedByName = v; }

    public LocalDateTime  getCreatedAt()          { return createdAt; }
    public void           setCreatedAt(LocalDateTime v){ this.createdAt = v; }

    public LocalDateTime  getUpdatedAt()          { return updatedAt; }
    public void           setUpdatedAt(LocalDateTime v){ this.updatedAt = v; }

    public List<PropertyImage> getImages()           { return images; }
    public void                setImages(List<PropertyImage> v){ this.images = v; }

    public String getPrimaryImagePath() {
        return images.stream()
                     .filter(PropertyImage::isPrimary)
                     .map(PropertyImage::getFilePath)
                     .findFirst()
                     .orElse(images.isEmpty() ? null : images.get(0).getFilePath());
    }

    @Override
    public String toString() {
        return "Property{id=" + id + ", title='" + title + "', status=" + status + "}";
    }
}
