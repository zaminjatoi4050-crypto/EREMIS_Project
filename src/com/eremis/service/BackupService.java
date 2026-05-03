package com.eremis.service;

import com.eremis.config.DatabaseConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database backup and restore service.
 *
 * FIX [BUG-2]: Original BackupService passed username to mysqldump but
 * OMITTED the password argument — causing every backup to fail silently
 * on password-protected MySQL instances (i.e., every real deployment).
 *
 * FIX [FEATURE]: Added restore() method — the original had no way to
 * reimport a backup file.
 */
public class BackupService {

    private static final Logger  LOGGER  = Logger.getLogger(BackupService.class.getName());
    private final DatabaseConfig dbConf  = DatabaseConfig.getInstance();

    /**
     * Dump the eremis_db database to a timestamped .sql file.
     *
     * @param backupDir directory to write the file (created if absent)
     * @return absolute path of the written file, or null on failure
     */
    public String backup(String backupDir) {
        String ts       = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "eremis_backup_" + ts + ".sql";
        File   dir      = new File(backupDir);
        if (!dir.exists()) dir.mkdirs();
        File outFile = new File(dir, filename);

        try {
            List<String> command = mysqlCommand("mysqldump");
            command.add("--result-file=" + outFile.getAbsolutePath());
            command.add("--single-transaction");
            command.add("--routines");
            command.add("--triggers");
            command.add(getDatabaseName());
            ProcessBuilder pb = new ProcessBuilder(command);
            applyPasswordEnvironment(pb);
            pb.redirectErrorStream(true);

            Process proc   = pb.start();
            String  output = readOutput(proc);
            int     exit   = proc.waitFor();

            if (exit == 0 && outFile.exists() && outFile.length() > 0) {
                LOGGER.info("Backup completed: " + outFile.getAbsolutePath()
                            + " (" + outFile.length() / 1024 + " KB)");
                return outFile.getAbsolutePath();
            }

            LOGGER.warning("mysqldump failed (exit=" + exit + "): " + output);
            return null;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                "Backup failed — ensure mysqldump is on PATH and MySQL is accessible", e);
            return null;
        }
    }

    /**
     * Restore the database from a .sql dump file produced by backup().
     *
     * WARNING: This drops and recreates all tables — existing data is lost.
     *
     * @param sqlFilePath absolute or relative path to the dump file
     * @return true on success, false on failure
     */
    public boolean restore(String sqlFilePath) {
        File sqlFile = new File(sqlFilePath);
        if (!sqlFile.exists() || !sqlFile.isFile()) {
            LOGGER.warning("Restore failed — file not found: " + sqlFilePath);
            return false;
        }

        try {
            List<String> command = mysqlCommand("mysql");
            command.add(getDatabaseName());
            ProcessBuilder pb = new ProcessBuilder(command);
            applyPasswordEnvironment(pb);
            pb.redirectInput(sqlFile);
            pb.redirectErrorStream(true);

            Process proc   = pb.start();
            String  output = readOutput(proc);
            int     exit   = proc.waitFor();

            if (exit == 0) {
                LOGGER.info("Restore completed from: " + sqlFilePath);
                return true;
            }

            LOGGER.warning("mysql restore failed (exit=" + exit + "): " + output);
            return false;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                "Restore failed — ensure mysql client is on PATH", e);
            return false;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private static String readOutput(Process proc) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        } catch (Exception ignored) {}
        return sb.toString().trim();
    }

    private List<String> mysqlCommand(String executable) {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("-u");
        command.add(dbConf.getUsername());
        return command;
    }

    private void applyPasswordEnvironment(ProcessBuilder pb) {
        if (dbConf.getPassword() != null && !dbConf.getPassword().isBlank()) {
            pb.environment().put("MYSQL_PWD", dbConf.getPassword());
        }
    }

    private String getDatabaseName() {
        String url = dbConf.getUrl();
        int slash = url.lastIndexOf('/');
        if (slash < 0 || slash == url.length() - 1) return "eremis_db";
        String dbName = url.substring(slash + 1);
        int query = dbName.indexOf('?');
        return query >= 0 ? dbName.substring(0, query) : dbName;
    }
}
