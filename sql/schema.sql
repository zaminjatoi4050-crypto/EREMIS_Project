-- ============================================================
--  EREMIS — Enterprise Real Estate Management & Intelligence System
--  Database Schema v1.0
--  Run this file FIRST before starting the application
-- ============================================================

CREATE DATABASE IF NOT EXISTS eremis_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE eremis_db;

-- ============================================================
-- TABLE: users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100)  NOT NULL,
    email         VARCHAR(150)  NOT NULL UNIQUE,
    username      VARCHAR(50)   NOT NULL UNIQUE,
    password_hash VARCHAR(255)  NOT NULL,        -- BCrypt hash (60 chars) or legacy SHA-256 (64 chars)
    phone         VARCHAR(20)   NULL,
    city          VARCHAR(100)  NULL,
    role          ENUM('ADMIN','SELLER','USER','AGENT','ANALYST') NOT NULL DEFAULT 'USER',
    is_active     TINYINT(1)    NOT NULL DEFAULT 1,
    login_attempts INT          NOT NULL DEFAULT 0,
    locked_until  DATETIME      NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: properties
-- ============================================================
CREATE TABLE IF NOT EXISTS properties (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(200)  NOT NULL,
    description   TEXT,
    location      VARCHAR(200)  NOT NULL,
    city          VARCHAR(100)  NOT NULL,
    price         DECIMAL(15,2) NOT NULL,
    area_sqft     DECIMAL(10,2),
    bedrooms      INT           DEFAULT 0,
    bathrooms     INT           DEFAULT 0,
    type          ENUM('HOUSE','APARTMENT','COMMERCIAL','LAND','VILLA','CONDO') NOT NULL,
    status        ENUM('AVAILABLE','LOCKED','RESERVED','SOLD') NOT NULL DEFAULT 'AVAILABLE',
    owner_name    VARCHAR(100),
    owner_contact VARCHAR(20),
    listed_by     INT           NOT NULL,        -- FK → users.id
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_property_user FOREIGN KEY (listed_by) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_prop_city     ON properties(city);
CREATE INDEX idx_prop_status   ON properties(status);
CREATE INDEX idx_prop_type     ON properties(type);
CREATE INDEX idx_prop_price    ON properties(price);

-- ============================================================
-- TABLE: property_images
-- ============================================================
CREATE TABLE IF NOT EXISTS property_images (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    property_id   INT           NOT NULL,
    file_path     VARCHAR(500)  NOT NULL,
    is_primary    TINYINT(1)    NOT NULL DEFAULT 0,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_img_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: inquiries  (CRM)
-- ============================================================
CREATE TABLE IF NOT EXISTS inquiries (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    property_id   INT           NOT NULL,
    user_id       INT           NOT NULL,
    subject       VARCHAR(300)  NOT NULL,
    message       TEXT          NOT NULL,
    status        ENUM('PENDING','CONTACTED','CLOSED') NOT NULL DEFAULT 'PENDING',
    notes         TEXT,
    assigned_to   INT           NULL,            -- FK → users.id (admin)
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inq_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    CONSTRAINT fk_inq_user     FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_inq_admin    FOREIGN KEY (assigned_to) REFERENCES users(id)      ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_inq_status ON inquiries(status);

-- ============================================================
-- TABLE: transactions  (purchase workflow)
-- ============================================================
CREATE TABLE IF NOT EXISTS transactions (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    buyer_id                INT           NOT NULL,
    seller_id               INT           NOT NULL,
    property_id             INT           NOT NULL,
    amount                  DECIMAL(15,2) NOT NULL,
    bank_name               VARCHAR(150)  NOT NULL,
    account_number_encrypted TEXT          NOT NULL,
    status                  ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    rejection_reason        TEXT          NULL,
    created_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at             DATETIME      NULL,
    rejected_at             DATETIME      NULL,
    CONSTRAINT fk_tx_buyer   FOREIGN KEY (buyer_id)   REFERENCES users(id)      ON DELETE RESTRICT,
    CONSTRAINT fk_tx_seller  FOREIGN KEY (seller_id)   REFERENCES users(id)      ON DELETE RESTRICT,
    CONSTRAINT fk_tx_prop    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_tx_status   ON transactions(status);
CREATE INDEX idx_tx_buyer    ON transactions(buyer_id);
CREATE INDEX idx_tx_property ON transactions(property_id);

-- ============================================================
-- TABLE: search_history
-- ============================================================
CREATE TABLE IF NOT EXISTS search_history (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT           NOT NULL,
    keyword       VARCHAR(200),
    city          VARCHAR(100),
    min_price     DECIMAL(15,2),
    max_price     DECIMAL(15,2),
    property_type VARCHAR(50),
    searched_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_sh_user ON search_history(user_id);
CREATE INDEX idx_sh_city ON search_history(city);

-- ============================================================
-- TABLE: logs  (audit trail)
-- ============================================================
CREATE TABLE IF NOT EXISTS logs (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT           NULL,
    action        VARCHAR(100)  NOT NULL,
    entity_type   VARCHAR(50),
    entity_id     INT,
    details       TEXT,
    ip_address    VARCHAR(45),
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_log_created ON logs(created_at);
CREATE INDEX idx_log_user    ON logs(user_id);

-- ============================================================
-- TABLE: notifications
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT           NOT NULL,
    title         VARCHAR(200)  NOT NULL,
    message       TEXT          NOT NULL,
    type          ENUM('INFO','SUCCESS','WARNING','ERROR') NOT NULL DEFAULT 'INFO',
    is_read       TINYINT(1)    NOT NULL DEFAULT 0,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_notif_user_read ON notifications(user_id, is_read);
