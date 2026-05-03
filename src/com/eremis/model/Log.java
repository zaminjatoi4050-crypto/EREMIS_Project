package com.eremis.model;

import java.time.LocalDateTime;

/**
 * Audit log entry — records every significant user action.
 */
public class Log {

    private int           id;
    private Integer       userId;
    private String        username;        // joined
    private String        action;
    private String        entityType;
    private Integer       entityId;
    private String        details;
    private String        ipAddress;
    private LocalDateTime createdAt;

    public Log() {}

    public Log(Integer userId, String action, String entityType,
               Integer entityId, String details) {
        this.userId     = userId;
        this.action     = action;
        this.entityType = entityType;
        this.entityId   = entityId;
        this.details    = details;
    }

    public int           getId()           { return id; }
    public void          setId(int id)     { this.id = id; }

    public Integer       getUserId()       { return userId; }
    public void          setUserId(Integer v){ this.userId = v; }

    public String        getUsername()     { return username; }
    public void          setUsername(String v){ this.username = v; }

    public String        getAction()       { return action; }
    public void          setAction(String v){ this.action = v; }

    public String        getEntityType()   { return entityType; }
    public void          setEntityType(String v){ this.entityType = v; }

    public Integer       getEntityId()     { return entityId; }
    public void          setEntityId(Integer v){ this.entityId = v; }

    public String        getDetails()      { return details; }
    public void          setDetails(String v){ this.details = v; }

    public String        getIpAddress()    { return ipAddress; }
    public void          setIpAddress(String v){ this.ipAddress = v; }

    public LocalDateTime getCreatedAt()   { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ this.createdAt = v; }

    // Backwards-compatible accessor used by UI code
    public LocalDateTime getTimestamp() { return createdAt; }
}
