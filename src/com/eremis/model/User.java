package com.eremis.model;

import com.eremis.model.enums.UserRole;
import java.time.LocalDateTime;

/**
 * User entity — maps to the 'users' table.
 */
public class User {

    private int           id;
    private String        fullName;
    private String        email;
    private String        username;
    private String        passwordHash;
    private String        phone;
    private String        city;
    private UserRole      role;
    private boolean       active;
    private int           loginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {}

    public User(String fullName, String email, String username,
                String passwordHash, UserRole role) {
        this.fullName     = fullName;
        this.email        = email;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.active       = true;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────
    public int           getId()            { return id; }
    public void          setId(int id)      { this.id = id; }

    public String        getFullName()               { return fullName; }
    public void          setFullName(String v)        { this.fullName = v; }

    public String        getEmail()                  { return email; }
    public void          setEmail(String v)           { this.email = v; }

    public String        getUsername()               { return username; }
    public void          setUsername(String v)        { this.username = v; }

    public String        getPhone()                  { return phone; }
    public void          setPhone(String v)          { this.phone = v; }

    public String        getCity()                   { return city; }
    public void          setCity(String v)           { this.city = v; }

    public String        getPasswordHash()            { return passwordHash; }
    public void          setPasswordHash(String v)    { this.passwordHash = v; }

    public UserRole      getRole()                   { return role; }
    public void          setRole(UserRole v)          { this.role = v; }

    public boolean       isActive()                  { return active; }
    public void          setActive(boolean v)         { this.active = v; }

    public int           getLoginAttempts()           { return loginAttempts; }
    public void          setLoginAttempts(int v)      { this.loginAttempts = v; }

    public LocalDateTime getLockedUntil()             { return lockedUntil; }
    public void          setLockedUntil(LocalDateTime v){ this.lockedUntil = v; }

    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void          setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public LocalDateTime getUpdatedAt()               { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    public boolean isAdmin() {
        return role != null && role.isAdminLike();
    }

    public boolean isSeller() {
        return role != null && role.isSellerLike();
    }

    public boolean isBuyer() {
        return role != null && role.isBuyerLike();
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role + "}";
    }
}
