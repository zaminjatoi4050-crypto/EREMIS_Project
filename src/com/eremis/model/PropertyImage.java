package com.eremis.model;

import java.time.LocalDateTime;

/**
 * Represents one image path linked to a Property.
 */
public class PropertyImage {

    private int           id;
    private int           propertyId;
    private String        filePath;
    private boolean       primary;
    private LocalDateTime createdAt;

    public PropertyImage() {}

    public PropertyImage(int propertyId, String filePath, boolean primary) {
        this.propertyId = propertyId;
        this.filePath   = filePath;
        this.primary    = primary;
    }

    public int           getId()            { return id; }
    public void          setId(int id)      { this.id = id; }

    public int           getPropertyId()    { return propertyId; }
    public void          setPropertyId(int v){ this.propertyId = v; }

    public String        getFilePath()      { return filePath; }
    public void          setFilePath(String v){ this.filePath = v; }

    public boolean       isPrimary()        { return primary; }
    public void          setPrimary(boolean v){ this.primary = v; }

    public LocalDateTime getCreatedAt()     { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ this.createdAt = v; }
}
