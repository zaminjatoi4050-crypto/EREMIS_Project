package com.eremis.model;

import com.eremis.model.enums.InquiryStatus;
import java.time.LocalDateTime;

/**
 * CRM Inquiry entity — maps to the 'inquiries' table.
 */
public class Inquiry {

    private int           id;
    private int           propertyId;
    private String        propertyTitle;   // joined
    private int           userId;
    private String        userName;        // joined
    private String        subject;
    private String        message;
    private InquiryStatus status;
    private String        notes;
    private Integer       assignedTo;
    private String        assignedToName;  // joined
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Inquiry() {}

    // ── Getters & Setters ──────────────────────────────────────────────────
    public int           getId()               { return id; }
    public void          setId(int id)         { this.id = id; }

    public int           getPropertyId()       { return propertyId; }
    public void          setPropertyId(int v)  { this.propertyId = v; }

    public String        getPropertyTitle()    { return propertyTitle; }
    public void          setPropertyTitle(String v){ this.propertyTitle = v; }

    public int           getUserId()           { return userId; }
    public void          setUserId(int v)      { this.userId = v; }

    public String        getUserName()         { return userName; }
    public void          setUserName(String v) { this.userName = v; }

    public String        getSubject()          { return subject; }
    public void          setSubject(String v)  { this.subject = v; }

    public String        getMessage()          { return message; }
    public void          setMessage(String v)  { this.message = v; }

    public InquiryStatus getStatus()           { return status; }
    public void          setStatus(InquiryStatus v){ this.status = v; }

    public String        getNotes()            { return notes; }
    public void          setNotes(String v)    { this.notes = v; }

    public Integer       getAssignedTo()       { return assignedTo; }
    public void          setAssignedTo(Integer v){ this.assignedTo = v; }

    public String        getAssignedToName()   { return assignedToName; }
    public void          setAssignedToName(String v){ this.assignedToName = v; }

    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ this.createdAt = v; }

    public LocalDateTime getUpdatedAt()        { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime v){ this.updatedAt = v; }

    @Override
    public String toString() {
        return "Inquiry{id=" + id + ", subject='" + subject + "', status=" + status + "}";
    }
}
