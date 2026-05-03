package com.eremis.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application-wide configuration constants.
 * Loaded from db.properties (the same file, extended with app.* keys).
 */
public class AppConfig {

    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static AppConfig instance;
    private Properties props;

    // ── Defaults (used if key is missing from properties file) ────────────
    private static final String DEFAULT_APP_NAME         = "EREMIS";
    private static final String DEFAULT_VERSION          = "1.0.0";
    private static final String DEFAULT_THEME            = "LIGHT";
    private static final int    DEFAULT_MAX_ATTEMPTS     = 3;
    private static final int    DEFAULT_LOCKOUT_MINUTES  = 15;
    private static final int    DEFAULT_OTP_MINUTES      = 10;
    private static final int    DEFAULT_MAIL_TIMEOUT_MS  = 15000;

    private AppConfig() {
        props = new Properties();
        loadProperties();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) instance = new AppConfig();
        return instance;
    }

    public String getAppName() {
        return props.getProperty("app.name", DEFAULT_APP_NAME);
    }

    public String getVersion() {
        return props.getProperty("app.version", DEFAULT_VERSION);
    }

    public String getDefaultTheme() {
        return props.getProperty("app.theme", DEFAULT_THEME);
    }

    public int getMaxLoginAttempts() {
        try {
            return Integer.parseInt(props.getProperty("app.login.maxAttempts",
                                                      String.valueOf(DEFAULT_MAX_ATTEMPTS)));
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_ATTEMPTS;
        }
    }

    public int getLockoutMinutes() {
        try {
            return Integer.parseInt(props.getProperty("app.login.lockoutMinutes",
                                                      String.valueOf(DEFAULT_LOCKOUT_MINUTES)));
        } catch (NumberFormatException e) {
            return DEFAULT_LOCKOUT_MINUTES;
        }
    }

    public boolean isMailEnabled() {
        return Boolean.parseBoolean(getValue("mail.enabled", "EREMIS_MAIL_ENABLED", "false"));
    }

    public String getMailHost() {
        return getValue("mail.host", "EREMIS_MAIL_HOST", "smtp.gmail.com");
    }

    public int getMailPort() {
        try {
            return Integer.parseInt(getValue("mail.port", "EREMIS_MAIL_PORT", "465"));
        } catch (NumberFormatException e) {
            return 465;
        }
    }

    public String getMailUsername() {
        return getValue("mail.username", "EREMIS_MAIL_USERNAME", "");
    }

    public String getMailPassword() {
        return getValue("mail.password", "EREMIS_MAIL_PASSWORD", "");
    }

    public String getMailFromName() {
        return getValue("mail.fromName", "EREMIS_MAIL_FROM_NAME", getAppName());
    }

    public boolean isMailSslEnabled() {
        String defaultValue = String.valueOf(getMailPort() == 465);
        return Boolean.parseBoolean(getValue("mail.ssl", "EREMIS_MAIL_SSL", defaultValue));
    }

    public boolean isMailStartTlsEnabled() {
        String defaultValue = String.valueOf(getMailPort() == 587);
        return Boolean.parseBoolean(getValue("mail.startTls", "EREMIS_MAIL_STARTTLS", defaultValue));
    }

    public int getMailTimeoutMillis() {
        try {
            return Integer.parseInt(getValue("mail.timeoutMillis", "EREMIS_MAIL_TIMEOUT_MS",
                                             String.valueOf(DEFAULT_MAIL_TIMEOUT_MS)));
        } catch (NumberFormatException e) {
            return DEFAULT_MAIL_TIMEOUT_MS;
        }
    }

    public boolean isRegistrationEmailOtpRequired() {
        return Boolean.parseBoolean(getValue("app.registration.requireEmailOtp",
                                             "EREMIS_REQUIRE_REGISTRATION_OTP", "true"));
    }

    public int getPasswordResetOtpMinutes() {
        try {
            return Integer.parseInt(props.getProperty("app.passwordReset.otpMinutes",
                                                      String.valueOf(DEFAULT_OTP_MINUTES)));
        } catch (NumberFormatException e) {
            return DEFAULT_OTP_MINUTES;
        }
    }

    public String getPaymentEncryptionKey() {
        return getValue("app.payment.encryptionKey", "EREMIS_PAYMENT_KEY", "");
    }

    private String getValue(String key, String envKey, String fallback) {
        String env = System.getenv(envKey);
        return env != null && !env.isBlank() ? env.trim() : props.getProperty(key, fallback);
    }

    private void loadProperties() {
        String loadedFrom = null;
        try {
            File resourceFile = new File("resources/db.properties");
            if (resourceFile.exists()) {
                try (InputStream in = new FileInputStream(resourceFile)) {
                    props.load(in);
                    loadedFrom = resourceFile.getPath();
                }
            } else {
                File buildFile = new File("build/classes/db.properties");
                if (buildFile.exists()) {
                    try (InputStream in = new FileInputStream(buildFile)) {
                        props.load(in);
                        loadedFrom = buildFile.getPath();
                    }
                } else {
                    try (InputStream in = getClass().getClassLoader()
                                                   .getResourceAsStream("db.properties")) {
                        if (in != null) {
                            props.load(in);
                            loadedFrom = "classpath:db.properties";
                        }
                    }
                }
            }

            if (loadedFrom != null) {
                LOGGER.info("App config loaded from " + loadedFrom);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load app config, using defaults", e);
        }
    }
}
