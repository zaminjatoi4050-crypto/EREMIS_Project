package com.eremis.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton database configuration with a lightweight connection pool.
 *
 * FIX [PERF]: Replaced bare DriverManager.getConnection() — which opened a
 * new TCP socket on every query — with a bounded, blocking pool.
 *   • Pool size     : db.pool.size property (default 10)
 *   • Borrow timeout: 5 s — fails fast instead of hanging the Swing EDT
 *   • Validation    : connections are ping-validated before being handed out
 *
 * Production tip: For 100+ concurrent users swap this with HikariCP.
 */
public class DatabaseConfig {

    private static final Logger LOGGER          = Logger.getLogger(DatabaseConfig.class.getName());
    private static final int    DEFAULT_POOL     = 10;
    private static final long   DEFAULT_BORROW_TIMEOUT_MS = 5000;

    // Double-checked locking singleton
    private static volatile DatabaseConfig instance;

    private String url;
    private String username;
    private String password;
    private String driver;
    private int    poolSize;
    private long   borrowTimeoutMs;

    private BlockingQueue<Connection> pool;
    private int pooledConnectionCount;

    private DatabaseConfig() {
        loadProperties();
        initPool();
        ensureSchemaCompatibility();
    }

    public static DatabaseConfig getInstance() {
        if (instance == null) {
            synchronized (DatabaseConfig.class) {
                if (instance == null) instance = new DatabaseConfig();
            }
        }
        return instance;
    }

    // ── Configuration loading ──────────────────────────────────────────────

    private void loadProperties() {
        Properties props = new Properties();
        InputStream in = null;
        String configSource = null;
        try {
            File resourceFile = new File("resources/db.properties");
            if (resourceFile.exists()) {
                in = new FileInputStream(resourceFile);
                configSource = resourceFile.getPath();
            }
            if (in == null) {
                File buildFile = new File("build/classes/db.properties");
                if (buildFile.exists()) {
                    in = new FileInputStream(buildFile);
                    configSource = buildFile.getPath();
                }
            }
            if (in == null) {
                in = getClass().getClassLoader().getResourceAsStream("db.properties");
                if (in != null) {
                    configSource = "classpath:db.properties";
                }
            }
            if (in == null)
                throw new RuntimeException("db.properties not found.");

            props.load(in);

            this.url      = getConfigValue(props, "db.url", "EREMIS_DB_URL", true);
            this.username = getConfigValue(props, "db.username", "EREMIS_DB_USERNAME", true);
            this.password = getConfigValue(props, "db.password", "EREMIS_DB_PASSWORD", false);
            this.driver   = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
            this.poolSize = Integer.parseInt(
                                props.getProperty("db.pool.maxConnections",
                                props.getProperty("db.pool.size", String.valueOf(DEFAULT_POOL))));
            this.borrowTimeoutMs = Long.parseLong(
                                props.getProperty("db.pool.timeout", String.valueOf(DEFAULT_BORROW_TIMEOUT_MS)));

            Class.forName(this.driver);
            LOGGER.info("DB config loaded from " + configSource
                + " | driver=" + driver + " | pool=" + poolSize + " | timeoutMs=" + borrowTimeoutMs);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load DB config", e);
            throw new RuntimeException("Database configuration failed", e);
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
        }
    }

    private static String require(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank())
            throw new RuntimeException("Missing required property: " + key);
        return v.trim();
    }

    private static String getConfigValue(Properties props, String propertyKey,
                                         String environmentKey, boolean required) {
        String environmentValue = System.getenv(environmentKey);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue.trim();
        }

        String propertyValue = props.getProperty(propertyKey);
        if (required) {
            if (propertyValue == null || propertyValue.isBlank()) {
                throw new RuntimeException("Missing required property: " + propertyKey);
            }
            return propertyValue.trim();
        }
        return propertyValue == null ? "" : propertyValue.trim();
    }

    // ── Pool lifecycle ─────────────────────────────────────────────────────

    private void initPool() {
        pool = new ArrayBlockingQueue<>(poolSize);
        int ready = 0;
        boolean loggedFirstFailure = false;
        for (int i = 0; i < poolSize; i++) {
            try {
                pool.offer(openConnection());
                ready++;
            } catch (SQLException e) {
                if (!loggedFirstFailure) {
                    loggedFirstFailure = true;
                    LOGGER.log(Level.WARNING,
                        "Could not pre-warm database pool: " + summarizeSqlException(e), e);
                } else {
                    LOGGER.warning("Could not pre-warm pool connection #" + i + ": " + summarizeSqlException(e));
                }
                if (isAuthenticationFailure(e)) {
                    break;
                }
            }
        }
        pooledConnectionCount = ready;
        LOGGER.info("Connection pool ready: " + ready + "/" + poolSize);
    }

    /**
     * Apply safe schema migrations needed by the current app version.
     * This keeps older databases usable without a manual SQL step.
     */
    private void ensureSchemaCompatibility() {
        try (Connection conn = getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            ensureColumnExists(conn, stmt, "users", "password_hash", "VARCHAR(255) NOT NULL");
            ensureColumnExists(conn, stmt, "users", "phone", "VARCHAR(20) NULL");
            ensureColumnExists(conn, stmt, "users", "city", "VARCHAR(100) NULL");
            ensureRoleColumnCompatible(stmt);
            ensurePropertyStatusCompatible(stmt);
            ensureTransactionsTable(stmt);
            ensureTransactionColumnsCompatible(conn, stmt);
            LOGGER.info("Schema compatibility check complete: users.password_hash, users.phone, users.city verified");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not apply schema compatibility migration for users.password_hash", e);
        }
    }

    private void ensureRoleColumnCompatible(java.sql.Statement stmt) throws SQLException {
        // FIXED: Preserve ENUM type for DB-level validation; only ensure NOT NULL default
        // Using VARCHAR(20) dropped enum constraint — now keeping ENUM definition
        stmt.executeUpdate("ALTER TABLE users MODIFY role ENUM('ADMIN','SELLER','USER','AGENT','ANALYST') NOT NULL DEFAULT 'USER'");
        LOGGER.info("Verified column definition: users.role as ENUM('ADMIN','SELLER','USER','AGENT','ANALYST')");
    }

    private void ensurePropertyStatusCompatible(java.sql.Statement stmt) throws SQLException {
        stmt.executeUpdate("ALTER TABLE properties MODIFY status ENUM('AVAILABLE','LOCKED','RESERVED','SOLD') NOT NULL DEFAULT 'AVAILABLE'");
        LOGGER.info("Verified column definition: properties.status with LOCKED lifecycle");
    }

    private void ensureTransactionsTable(java.sql.Statement stmt) throws SQLException {
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transactions (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "buyer_id INT NOT NULL, " +
            "seller_id INT NOT NULL, " +
            "property_id INT NOT NULL, " +
            "amount DECIMAL(15,2) NOT NULL, " +
            "bank_name VARCHAR(150) NOT NULL, " +
            "account_number_encrypted TEXT NOT NULL, " +
            "status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING', " +
            "rejection_reason TEXT NULL, " +
            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            "approved_at DATETIME NULL, " +
            "rejected_at DATETIME NULL, " +
            "CONSTRAINT fk_tx_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE RESTRICT, " +
            "CONSTRAINT fk_tx_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE RESTRICT, " +
            "CONSTRAINT fk_tx_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE RESTRICT" +
            ") ENGINE=InnoDB");
        try { stmt.executeUpdate("CREATE INDEX idx_tx_status ON transactions(status)"); } catch (SQLException ignored) {}
        try { stmt.executeUpdate("CREATE INDEX idx_tx_buyer ON transactions(buyer_id)"); } catch (SQLException ignored) {}
        try { stmt.executeUpdate("CREATE INDEX idx_tx_property ON transactions(property_id)"); } catch (SQLException ignored) {}
        LOGGER.info("Verified transactions table and indexes");
    }

    private void ensureTransactionColumnsCompatible(Connection conn, java.sql.Statement stmt) throws SQLException {
        // Standardize on rejection_reason (legacy deployments may still have rejected_reason).
        if (!columnExists(conn, "transactions", "rejection_reason")) {
            stmt.executeUpdate("ALTER TABLE transactions ADD COLUMN rejection_reason TEXT NULL");
        }
        if (columnExists(conn, "transactions", "rejected_reason")) {
            try {
                stmt.executeUpdate("UPDATE transactions SET rejection_reason = COALESCE(rejection_reason, rejected_reason)");
            } catch (SQLException ignored) {}
            stmt.executeUpdate("ALTER TABLE transactions DROP COLUMN rejected_reason");
        }
        LOGGER.info("Verified transactions column compatibility: rejection_reason");
    }

    private void ensureColumnExists(Connection conn, java.sql.Statement stmt, String tableName, String columnName, String columnDefinition) throws SQLException {
        if (!columnExists(conn, tableName, columnName)) {
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
            LOGGER.info("Added missing column: " + tableName + "." + columnName);
            return;
        }

        if ("users".equalsIgnoreCase(tableName) && "password_hash".equalsIgnoreCase(columnName)) {
            stmt.executeUpdate("ALTER TABLE users MODIFY password_hash " + columnDefinition);
            LOGGER.info("Verified column definition: users.password_hash");
        }
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Borrow a connection. MUST be closed (try-with-resources) — the wrapper's
     * close() returns it to the pool instead of dropping the TCP socket.
     */
    public Connection getConnection() throws SQLException {
        try {
            Connection conn = null;
            if (pooledConnectionCount > 0) {
                conn = pool.poll(borrowTimeoutMs, TimeUnit.MILLISECONDS);
            }

            if (conn == null) {
                LOGGER.warning("Pool exhausted — creating overflow connection.");
                return openConnection();
            }
            // Validate: replaces stale connections silently
            if (!conn.isValid(1)) {
                LOGGER.warning("Stale pool connection detected — replacing.");
                try { conn.close(); } catch (Exception ignored) {}
                conn = openConnection();
            }
            return new PooledConnection(conn);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a DB connection.", e);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private static String summarizeSqlException(SQLException e) {
        StringBuilder summary = new StringBuilder(e.getMessage());
        if (e.getSQLState() != null) {
            summary.append(" [SQLState=").append(e.getSQLState()).append("]");
        }
        if (e.getErrorCode() != 0) {
            summary.append(" [ErrorCode=").append(e.getErrorCode()).append("]");
        }
        return summary.toString();
    }

    private static boolean isAuthenticationFailure(SQLException e) {
        return "28000".equals(e.getSQLState()) || e.getErrorCode() == 1045;
    }

    public static void closeQuietly(AutoCloseable r) {
        if (r != null) try { r.close(); } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error closing resource", e);
        }
    }

    public boolean testConnection() {
        try (Connection c = getConnection()) { return c != null && !c.isClosed(); }
        catch (SQLException e) { LOGGER.log(Level.WARNING, "Connection test failed", e); return false; }
    }

    /** Call on JVM shutdown to close all physical connections. */
    public void shutdown() {
        List<Connection> all = new ArrayList<>();
        pool.drainTo(all);
        for (Connection c : all) try { c.close(); } catch (SQLException ignored) {}
        LOGGER.info("Connection pool shut down.");
    }

    public String getUrl()      { return url; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }  // used by BackupService

    /**
     * Replace the active DB credentials and rebuild the pool.
     * Used when the app asks the user for missing or incorrect MySQL details.
     */
    public synchronized void updateCredentials(String url, String username, String password) {
        if (url != null && !url.isBlank()) {
            this.url = url.trim();
        }
        if (username != null && !username.isBlank()) {
            this.username = username.trim();
        }
        this.password = password != null ? password : "";

        shutdown();
        initPool();
        ensureSchemaCompatibility();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Inner class: returns connection to pool on close() instead of closing it
    // ══════════════════════════════════════════════════════════════════════
    private class PooledConnection implements Connection {
        private final Connection delegate;
        private volatile boolean returned = false;

        PooledConnection(Connection delegate) { this.delegate = delegate; }

        @Override
        public void close() throws SQLException {
            if (!returned) {
                returned = true;
                try {
                    if (!delegate.isClosed() && !delegate.getAutoCommit()) {
                        delegate.rollback(); // safety: rollback any uncommitted txn
                        delegate.setAutoCommit(true);
                    }
                } catch (SQLException ignored) {}
                if (!delegate.isClosed()) pool.offer(delegate);
            }
        }

        @Override public boolean isClosed() throws SQLException { return returned || delegate.isClosed(); }

        // ── Delegate all other Connection methods ──────────────────────────
        @Override public java.sql.Statement createStatement() throws SQLException { return delegate.createStatement(); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException { return delegate.prepareStatement(sql); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { return delegate.prepareStatement(sql, autoGeneratedKeys); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { return delegate.prepareStatement(sql, columnIndexes); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { return delegate.prepareStatement(sql, columnNames); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int rst, int rsc) throws SQLException { return delegate.prepareStatement(sql, rst, rsc); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int rst, int rsc, int rsh) throws SQLException { return delegate.prepareStatement(sql, rst, rsc, rsh); }
        @Override public java.sql.CallableStatement prepareCall(String sql) throws SQLException { return delegate.prepareCall(sql); }
        @Override public java.sql.CallableStatement prepareCall(String sql, int rst, int rsc) throws SQLException { return delegate.prepareCall(sql, rst, rsc); }
        @Override public java.sql.CallableStatement prepareCall(String sql, int rst, int rsc, int rsh) throws SQLException { return delegate.prepareCall(sql, rst, rsc, rsh); }
        @Override public String nativeSQL(String sql) throws SQLException { return delegate.nativeSQL(sql); }
        @Override public boolean getAutoCommit() throws SQLException { return delegate.getAutoCommit(); }
        @Override public void setAutoCommit(boolean ac) throws SQLException { delegate.setAutoCommit(ac); }
        @Override public void commit() throws SQLException { delegate.commit(); }
        @Override public void rollback() throws SQLException { delegate.rollback(); }
        @Override public void rollback(java.sql.Savepoint sp) throws SQLException { delegate.rollback(sp); }
        @Override public java.sql.DatabaseMetaData getMetaData() throws SQLException { return delegate.getMetaData(); }
        @Override public boolean isReadOnly() throws SQLException { return delegate.isReadOnly(); }
        @Override public void setReadOnly(boolean ro) throws SQLException { delegate.setReadOnly(ro); }
        @Override public String getCatalog() throws SQLException { return delegate.getCatalog(); }
        @Override public void setCatalog(String catalog) throws SQLException { delegate.setCatalog(catalog); }
        @Override public int getTransactionIsolation() throws SQLException { return delegate.getTransactionIsolation(); }
        @Override public void setTransactionIsolation(int level) throws SQLException { delegate.setTransactionIsolation(level); }
        @Override public java.sql.SQLWarning getWarnings() throws SQLException { return delegate.getWarnings(); }
        @Override public void clearWarnings() throws SQLException { delegate.clearWarnings(); }
        @Override public java.util.Map<String,Class<?>> getTypeMap() throws SQLException { return delegate.getTypeMap(); }
        @Override public void setTypeMap(java.util.Map<String,Class<?>> map) throws SQLException { delegate.setTypeMap(map); }
        @Override public int getHoldability() throws SQLException { return delegate.getHoldability(); }
        @Override public void setHoldability(int h) throws SQLException { delegate.setHoldability(h); }
        @Override public java.sql.Savepoint setSavepoint() throws SQLException { return delegate.setSavepoint(); }
        @Override public java.sql.Savepoint setSavepoint(String name) throws SQLException { return delegate.setSavepoint(name); }
        @Override public void releaseSavepoint(java.sql.Savepoint sp) throws SQLException { delegate.releaseSavepoint(sp); }
        @Override public java.sql.Statement createStatement(int rst, int rsc) throws SQLException { return delegate.createStatement(rst, rsc); }
        @Override public java.sql.Statement createStatement(int rst, int rsc, int rsh) throws SQLException { return delegate.createStatement(rst, rsc, rsh); }
        @Override public java.sql.Clob createClob() throws SQLException { return delegate.createClob(); }
        @Override public java.sql.Blob createBlob() throws SQLException { return delegate.createBlob(); }
        @Override public java.sql.NClob createNClob() throws SQLException { return delegate.createNClob(); }
        @Override public java.sql.SQLXML createSQLXML() throws SQLException { return delegate.createSQLXML(); }
        @Override public boolean isValid(int timeout) throws SQLException { return delegate.isValid(timeout); }
        @Override public void setClientInfo(String name, String value) throws java.sql.SQLClientInfoException { try { delegate.setClientInfo(name, value); } catch (java.sql.SQLClientInfoException e) { throw e; } }
        @Override public void setClientInfo(java.util.Properties properties) throws java.sql.SQLClientInfoException { try { delegate.setClientInfo(properties); } catch (java.sql.SQLClientInfoException e) { throw e; } }
        @Override public String getClientInfo(String name) throws SQLException { return delegate.getClientInfo(name); }
        @Override public java.util.Properties getClientInfo() throws SQLException { return delegate.getClientInfo(); }
        @Override public java.sql.Array createArrayOf(String typeName, Object[] elements) throws SQLException { return delegate.createArrayOf(typeName, elements); }
        @Override public java.sql.Struct createStruct(String typeName, Object[] attributes) throws SQLException { return delegate.createStruct(typeName, attributes); }
        @Override public String getSchema() throws SQLException { return delegate.getSchema(); }
        @Override public void setSchema(String schema) throws SQLException { delegate.setSchema(schema); }
        @Override public void abort(java.util.concurrent.Executor executor) throws SQLException { delegate.abort(executor); }
        @Override public void setNetworkTimeout(java.util.concurrent.Executor executor, int ms) throws SQLException { delegate.setNetworkTimeout(executor, ms); }
        @Override public int getNetworkTimeout() throws SQLException { return delegate.getNetworkTimeout(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return delegate.unwrap(iface); }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return delegate.isWrapperFor(iface); }
    }
}
