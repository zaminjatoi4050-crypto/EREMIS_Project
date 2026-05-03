package com.eremis;

import com.eremis.config.DatabaseConfig;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Schema Migration Tool - Applies schema.sql changes to MySQL database
 * Usage: java -cp 'build/classes;lib/*' com.eremis.SchemaMigration
 */
public class SchemaMigration {
    public static void main(String[] args) {
        System.out.println("=== EREMIS Schema Migration Tool ===\n");
        System.out.println("Reading schema from: sql/schema.sql");
        
        try {
            // Initialize database connection via singleton
            DatabaseConfig dbConfig = DatabaseConfig.getInstance();
            Connection conn = dbConfig.getConnection();
            
            if (conn == null) {
                System.err.println("ERROR: Failed to establish database connection!");
                System.exit(1);
            }
            
            System.out.println("✓ Connected to MySQL database: eremis_db\n");
            
            // Read schema.sql file
            List<String> statements = readStatements("sql/schema.sql");
            int successCount = 0;
            int failCount = 0;
            
            try (Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    String trimmed = sql.trim();

                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    
                    try {
                        System.out.println("Executing: " + trimmed.substring(0, Math.min(80, trimmed.length())) + "...");
                        stmt.execute(trimmed);
                        System.out.println("  ✓ Success\n");
                        successCount++;
                    } catch (Exception e) {
                        System.out.println("  ⚠ Skipped: " + e.getMessage() + "\n");
                        failCount++;
                    }
                }
            }
            
            System.out.println("\n=== Migration Summary ===");
            System.out.println("✓ Statements executed: " + successCount);
            System.out.println("⚠ Statements skipped/failed: " + failCount);
            System.out.println("\nSchema migration complete!");
            
        } catch (Exception e) {
            System.err.println("ERROR during migration: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<String> readStatements(String path) throws Exception {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.startsWith("/*") || trimmed.isEmpty()) {
                    continue;
                }
                current.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    statements.add(current.toString());
                    current.setLength(0);
                }
            }
        }

        if (current.length() > 0) {
            statements.add(current.toString());
        }

        return statements;
    }
}
