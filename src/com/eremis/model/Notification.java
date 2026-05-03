package com.eremis.model;

import java.time.LocalDateTime;

/**
 * In-app notification — linked to a specific user.
 */
public class Notification {

    public enum NotifType { INFO, SUCCESS, WARNING, ERROR }

    private int           id;
    private int           userId;
    private String        title;
    private String        message;
    private NotifType     type;
    private boolean       read;
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(int userId, String title, String message, NotifType type) {
        this.userId  = userId;
        this.title   = title;
        this.message = message;
        this.type    = type;
        this.read    = false;
    }

    public int           getId()          { return id; }
    public void          setId(int id)    { this.id = id; }

    public int           getUserId()      { return userId; }
    public void          setUserId(int v) { this.userId = v; }

    public String        getTitle()       { return title; }
    public void          setTitle(String v){ this.title = v; }

    public String        getMessage()     { return message; }
    public void          setMessage(String v){ this.message = v; }

    public NotifType     getType()        { return type; }
    public void          setType(NotifType v){ this.type = v; }

    public boolean       isRead()         { return read; }
    public void          setRead(boolean v){ this.read = v; }

    public LocalDateTime getCreatedAt()   { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ this.createdAt = v; }
}
